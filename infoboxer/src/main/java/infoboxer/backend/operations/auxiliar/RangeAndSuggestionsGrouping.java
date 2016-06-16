package infoboxer.backend.operations.auxiliar;

import com.fasterxml.jackson.databind.ObjectMapper;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.dto.CountObjectComparator;
import infoboxer.backend.common.dto.KeyValuePair;
import infoboxer.backend.ontology.OntologyManager;
import infoboxer.backend.common.utils.StringManipulations;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

/**
 * Contains functions for grouping ranges and suggestions according to the ranges grouping.
 * Also saves suggestions to disk.
 */
public class RangeAndSuggestionsGrouping {


    OntologyManager manager;


    public RangeAndSuggestionsGrouping(OntologyManager manager){
        this.manager = manager;
    }


    //GROUP the ranges for each property -> if a property has more than 3 ranges, the rest are grouped in a new semantic range.
    //If the semantic range already existed, the count is added to the existing range.
    //If a property has 3 or less ranges, it is unmodified.
    //Also, adds, for each property, the list of shown ranges to "shownRanges"
    public  List<KeyValuePair<List<CountObject>>> generateRangeLists(HashMap<String,List<CountObject>> uses,
                                                                            HashMap<String,List<String>> shownRanges ){
        Iterator usesIterator = uses.entrySet().iterator();

        List<KeyValuePair<List<CountObject>>> list = new ArrayList<KeyValuePair<List<CountObject>>>();
        usesIterator = uses.entrySet().iterator();
        while (usesIterator.hasNext()) {
            Map.Entry pair = (Map.Entry) usesIterator.next();
            String currentProperty = (String) pair.getKey();
            List<CountObject> currentRangeList = (List<CountObject>) pair.getValue();

            List<String> currentRangosMostrados = agruparRangos(currentProperty,currentRangeList);
            shownRanges.put(currentProperty,currentRangosMostrados);

            list.add(new KeyValuePair<List<CountObject>>(currentProperty,currentRangeList));

        }

        return list;

    }

    /**
     * It extracts values for each range of each property and saves them to cache, with "intersect" or "union" as a part
     * of the name, depending of intersection parameter value.
     * The saved values will be used to calculate suggestions.
     */
    public  void saveSuggestions(boolean intersection,
                                       HashMap<String,HashMap<String,List<CountObject>>> suggestionValues,
                                       HashMap<String,List<String>> shownRanges,
                                       String POPULAR_VALUES_RANGE_PREFIX,
                                       String POPULAR_VALUES_RANGE_DIR,
                                       String classList,StringManipulations stringManipulations) throws Exception{

        ObjectMapper mapper = new ObjectMapper();

        Iterator valuesIterator = suggestionValues.entrySet().iterator();

        while(valuesIterator.hasNext()){

            Map.Entry pair = (Map.Entry) valuesIterator.next();
            String currentProperty = (String) pair.getKey();
            HashMap<String,List<CountObject>> obtainedRanges = (HashMap<String,List<CountObject>>) pair.getValue();

            agruparSugerenciasComoRangos( currentProperty, shownRanges, obtainedRanges);

            Iterator rangesIterator = obtainedRanges.entrySet().iterator();

            while(rangesIterator.hasNext()){
                //Para cada rango, miramos los valores
                pair = (Map.Entry) rangesIterator.next();
                String currentRange = (String) pair.getKey();

                List<CountObject> valueList = (List<CountObject>) pair.getValue();
                //Ordeno segun las cuentas de mayor a menor


                for(CountObject co: valueList){
                    stringManipulations.cleanValue(co);
                }


                Collections.sort(valueList,new CountObjectComparator());


                String jsonValues = mapper.writeValueAsString(valueList);


                String file = POPULAR_VALUES_RANGE_PREFIX + "-" + classList + "-" + currentProperty + "-" + currentRange;
                String fileHash =   POPULAR_VALUES_RANGE_DIR;

                if(intersection){
                    fileHash+= "intersect" + stringManipulations.calculateHash(file);
                }
                else{
                    fileHash+= "union" + stringManipulations.calculateHash(file);
                }


                PrintWriter writer = new PrintWriter(new File(fileHash + ".json"), "UTF-8");
                writer.println(jsonValues);
                writer.close();

            }


        }
    }




    /**
     * Dada una lista de rangos y sus cuentas (Lista de CountObject) la modifica de forma que, si tenía más de 3 rangos,
     * los demás se agrupan (sus cuentas) en un nuevo rango semántico correspondiente a la propiedad tratada. Si ese rango semántico
     * ya estaba entre los tres primeros, las cuentas se suman a él, quedando con tres.
     * Si la lista de rangos tenía 3 o menos, se deja intacta.
     *
     * Devuelve una lista de nombres de los rangos que finalmente se devolverán.
     */
    public  List<String> agruparRangos(String propiedad, List<CountObject> listaRangos){


        List<String> rangosDevueltos = new ArrayList<>();
        //Primero ordenamos por número de usos
        Collections.sort(listaRangos, new CountObjectComparator());


        //Si tiene 3 o menos rangos, lo dejamos como está
        //Si no, hay que juntar los demás en uno solo
        if(listaRangos.size()>3){

            //Sacamos el rango más concreto de la propiedad, u OWL#Thing en su defecto

            String semanticRange = manager.getSpecificRangeForProperty(propiedad);
            //Obteemos la suma de los demás rangos (a partir del 4º)
            int remainingCount = 0;
            for(int i = 3; i < listaRangos.size(); i++){
                remainingCount+=listaRangos.get(i).getCount();
            }

            //Vemos si dicho rango semántico ya está entre los tres primeros.
            boolean enTresPrimeros = false;
            for(int i = 0; i <=2 && !enTresPrimeros; i++){
                if(listaRangos.get(i).get_id().equalsIgnoreCase(semanticRange)){
                    //Está entre los tres primeros -> sumamos a ese la cuenta.
                    listaRangos.get(i).setCount(listaRangos.get(i).getCount() + remainingCount);
                    //System.out.println("Agrupados " + remainingCount + " en ya existente " + semanticRange + " para propiedad " + propiedad);
                    enTresPrimeros = true;
                }
            }

            //Nos quedamos con los tres primeros
            CountObject primero = listaRangos.get(0);
            CountObject segundo = listaRangos.get(1);
            CountObject tercero = listaRangos.get(2);

            listaRangos.clear();
            listaRangos.add(primero);
            listaRangos.add(segundo);
            listaRangos.add(tercero);

            rangosDevueltos.add(primero.get_id());
            rangosDevueltos.add(segundo.get_id());
            rangosDevueltos.add(tercero.get_id());


            //Si no estaba entre los tres primeros, añadimos un cuarto rango que es la agrupación (con rango semántico).
            if(!enTresPrimeros){
                //System.out.println("Agrupados " + remainingCount + " en " + semanticRange + " para propiedad " + propiedad);
                CountObject remainingRange = new CountObject(semanticRange,remainingCount);
               // System.out.println("Semantic: " + remainingRange.get_id() + " - " + remainingRange.getCount());
                remainingRange.setSemantic(true);
                listaRangos.add(remainingRange);
                rangosDevueltos.add(remainingRange.get_id());

                //Sort
                Collections.sort(listaRangos, new CountObjectComparator());

            }
        }
        else{
            //Hay que añadir todos a rangosDevueltos
            for(CountObject rango:listaRangos){
                rangosDevueltos.add(rango.get_id());
            }
        }

        return rangosDevueltos;
    }


    /**
     * Dada la propiedad actual, una lista de los rangos mostrados (calculada previamente al agrupar rangos para mostrar en la barra),
     * y los rangos con sus valores, agrupa los rangos y sus valores de forma que:
     *
     * - Si un rango está en la lista de rangos mostrados, se deja tal cual.
     * - Si un rango no esta en la lista de rangos mostrados, sus valores se meten en un rango con id el del semántico para dicha propiedad.
     * - Si dicho id estaba en la lista de rangos mostrados, se añadirán los valores al rango ya existente.
     *
     * USADO PARA CALCULAR LAS SUGERENCIAS DEL RANGO SEMÁNTICO QUE AGRUPARÁ TODAS AQUELLAS SUGERENCIAS DE LOS RANGOS
     * QUE NO SON LOS 3 MÁS FRECUENTES.
     */
    public  void agruparSugerenciasComoRangos(String currentProperty, HashMap<String,List<String>> rangosMostrados, HashMap<String,List<CountObject>> obtainedRanges){

        //Lista que agrupará los valores de los rangos que no están entre los mostrados.
        List<CountObject> restoValores = new ArrayList<>();




        //Hay que agrupar según los rangos mostrados en la barra
        Iterator obtainedRangesIterator = obtainedRanges.entrySet().iterator();


        while(obtainedRangesIterator.hasNext()){
            Map.Entry rangePair = (Map.Entry) obtainedRangesIterator.next();
            String currentRange = (String) rangePair.getKey();
            List<CountObject> valueList = (List<CountObject>) rangePair.getValue();

            if( rangosMostrados.get(currentProperty).contains(currentRange)){
                //No hacemos nada
                // System.out.println("PROP: " + currentProperty + " range " + currentRange + " already in lista mostrados( " + valueList.size() + ")" );
            }
            else{
                //El rango no está contenido en los mostrados. Sus valores irán al rango que agrupa al resto
                restoValores.addAll(valueList);
                obtainedRangesIterator.remove(); //Borramos de la lista de rangos obtenidos
                // System.out.println("PROP: " + currentProperty + " range " + currentRange + " NOT in lista mostrados (" + valueList.size() + ")" );
            }
        }

        //Ahora mismo tenemos en "obtainedRanges" los rangos obtenidos que estaban en la lista a mostrar
        //y en restoValores tenemos los valores pertenecientes al resto de rangos (que serán los menos populares).


        //Meteremos los valores del resto donde corresponda, en rango ya existente o uno nuevo. Solo si hay resto.
        if(restoValores.size()>0){

            //Obtenemos rango semántico para la propiedad
            String specificRangeForProperty = manager.getSpecificRangeForProperty(currentProperty);


            //Buscamos si está en lista de rangos obtenidos (contiene coincidencias de obtenidos y mostrados).
            //Si es así, le añadimos los valores
            List<CountObject> valueList = obtainedRanges.get(specificRangeForProperty);
            if(valueList!=null){
                //System.out.println("PROP: " + currentProperty + " valoresResto añadidos a rango mostrado y disponible " + specificRangeForProperty );
                valueList.addAll(restoValores);
            }
            else{
                //Si no estaba, tenemos que añadir el rango al hashmap como uno nuevo
                // System.out.println("PROP: " + currentProperty + " valoresResto añadidos como nuevo rango  " + specificRangeForProperty + "(" + restoValores.size() + ")" );
                obtainedRanges.put(specificRangeForProperty,restoValores);
            }


        }
    }


}
