package infoboxer.backend.dataObtaining;


import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;

import infoboxer.backend.common.dto.*;
import infoboxer.backend.ontology.OntologyManager;
import infoboxer.backend.common.utils.StringManipulations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by ismaro3 on 13/08/15.
 */
@Component
public class DataObtaining {


    @Autowired
    private OntologyManager manager;

    @Autowired
    StringManipulations stringManipulations;

    @Value("#{'${ontology.allowedClasses}'.split(',')}")
    private List<String> allowedClasses;

    @Value("#{'${ontology.classTransformations}'.split('\\),\\(')}")
    private List<String> classTransformations;

    @Value("${unknownType.resource.value}")
    String unknownResourceValue;

    @Value("${unknownType.resource.type}")
    String unknownResourceType;


    @Value("${unknownType.string.type}")
    String unknownStringType;

    @Value("${unknownType.langString.type}")
    String unknownLangStringType;

    @Value("${unknownType.numeric.type}")
    String unknownNumericType;

    @Value("${unknownType.else.type}")
    String unknownElseType;


    @Value("${instanceLabel.fromKB}")
    boolean getInstanceLabel;

    @Value("${instanceLabel.property}")
    String instanceLabelProperty;

    @Value("${sparql.url}")
    String endpoint;


    public static final boolean VERBOSE = true;

    Pattern[] allowedPatterns;
    Pattern unknownResourcePattern;
    HashMap<String,String> transformations;




    @PostConstruct
    public void generateEndpointURL(){

        if(endpoint.charAt(endpoint.length()-1)!='/'){
            endpoint+="/";
        }
        endpoint+= "query?output=tsv&query=";
    }
    @PostConstruct
    public void compilePatterns(){

        unknownResourcePattern = Pattern.compile(unknownResourceValue);

        //Compile patterns
        allowedPatterns = new Pattern[allowedClasses.size()];
        int index = 0;
        for(String regex:allowedClasses){
            if(regex.charAt(0)!='<'){
                regex = '<' + regex;

            }
            if(regex.charAt(regex.length()-1)!='>'){
                regex = regex + '>';
            }

            Pattern p = Pattern.compile(regex);
            allowedPatterns[index] = p;
            index++;
        }

        //Create hash of transformations
        transformations = new HashMap<>();

        if(classTransformations != null && classTransformations.size()>0 && classTransformations.get(0).length()>0){

            for(String pair:classTransformations){
                pair = pair.replace("(","");
                pair = pair.replace(")","");
                String[] classes = pair.split(",");
                if(classes[0].charAt(0)!='<'){
                    classes[0] = '<' + classes[0];
                }
                if(classes[1].charAt(0)!='<'){
                    classes[1] = '<' + classes[1];
                }
                if(classes[0].charAt(classes[0].length()-1)!='>'){
                    classes[0] += '>';
                }
                if(classes[1].charAt(classes[1].length()-1)!='>'){
                    classes[1] += '>';
                }
                transformations.put(classes[0],classes[1]);


            }
        }

    }

    //Only call if outside Spring
    public  void launchOntologyManager(){
        manager = new OntologyManager();
    }
    /**
     * Returns a dto.SimpleCountObject containing the number of instances
     * of the given classes.
     * E.g: {"count": 15}
     * On error, it returns null.
     */
    public SimpleCountObject getInstanceCount(List<String> classes) {
        String query = "SELECT (COUNT (DISTINCT ?name) AS ?pcount)" +
                "{";
        int actual = 0;
        for (String _class : classes) {
            String st = "";
            if (actual != 0) {
                st += " UNION ";
            }
            st += " { ?name a " + _class + " } ";
            query += st;
            actual++;
        }
        query += " }";

        query = stringManipulations.stringToUnicodeHex(query);

        try {
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            int result = s.nextInt();
            s.close();
            return new SimpleCountObject(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * Returns a list that represent the properties manifested by
     * "_class" instances and, for each one, the number of "_class" instances
     * that use it.
     * WARNING: property names are in URI-format
     * * E.g: [ {"_id":"<http://dbpedia.org/resource/team></http://dbpedia.org/resource/team>","count":88000},...]
     * On error, it returns null.
     */
    public  List<CountObject> getPropertiesList(List<String> classes) {
        String query = "SELECT DISTINCT ?property (COUNT(DISTINCT ?name) as ?count) " +
                "{";

        int actual = 0;
        for (String _class : classes) {
            String st = "";
            if (actual != 0) {
                st += " UNION ";
            }
            st += " { ?name a " + _class + " } ";
            query += st;
            actual++;
        }

        query += "?name ?property ?value" +
                "}" +
                "GROUP BY ?property";


        List<CountObject> result = new ArrayList<CountObject>();
        try {
            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            while (s.hasNextLine()) {

                String line = s.nextLine();
                String[] terms = line.split("\t",-1);

                String property = stringManipulations.unicodeHexToString(terms[0]);
                int count = Integer.parseInt(terms[1]);



                //Filter label and type
                if (!property.toLowerCase().contains("rdf-schema#label") && !property.toLowerCase().contains("rdf-syntax-ns#type")) {

                    CountObject co = new CountObject();
                    co.set_id(property);
                    co.setCount(count);
                    result.add(co);

                    //todo: Hay que filtrar y no agnadir aquellas propiedades cuyo valor sea algo del opengis y skos, o se nos colaran
                    //algunas que luego no salen en lo de rangos
                }

            }

            s.close();
            return result;


        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }


    }
    /**
     * Returns, for the given class, its direct and indirect childs and a count
     * of instances of each one.
     */
    public  ArrayList<CountObject> getAvailableClasses(String superClass) {

        ArrayList<CountObject> result = new ArrayList<CountObject>();


        Set<String> classList = manager.getSubClasses(superClass, false);

        for (String clase : classList) {

            boolean classAllowed = false;
            for(Pattern p:allowedPatterns){
                if(p.matcher(clase).matches()){
                    classAllowed = true;
                }
            }
            if(classAllowed){
                //Sparql query for obtaining count
                ArrayList<String> a = new ArrayList<String>();
                a.add(clase);
                int count = getInstanceCount(a).getCount();
                if (count > 0) {
                    result.add(new CountObject(clase, count));
                }
            }

        }

        return result;

    }

    /**
     * Returns a list of instances of classes <classes>.
     * If intersection is true, only instances of all of the classes (intersection).
     * Else, instances of at least one of the classes (union).
     * If an error occurs, returns null
     */
    public  ArrayList<String> getInstanceListFromClasses(List<String> classes, boolean intersection) {


        String query = "";


        if (!intersection) {

            if(VERBOSE){
                System.out.println("Obtaining UNION instance list for classes...");
            }

            /**
             * UNION OF SETS
             * SELECT DISTINCT ?name
             * {
             *    {?name a class1}
             *    UNION
             *    {?name a class2}
             *    UNION
             *    {?name a classN}
             * }
             *
             */
            query = "SELECT DISTINCT ?name  " +
                    "{";


            int actual = 0;
            for (String _class : classes) {
                String st = "";
                if (actual != 0) {
                    st += " UNION ";
                }
                st += " { ?name a " + _class + " } ";
                query += st;
                actual++;
            }
            query += " }";

        } else {
            if(VERBOSE){
                System.out.println("Obtaining INTERSECTION instance list for classes...");
            }

            /**
             * INTERSECTION OF SETS
             * SELECT DISTINCT ?name
             * {
             *    ?name a class1.
             *    ?name a class2.
             *    ?name a class3
             *
             * }
             *
             */
            query = "SELECT DISTINCT ?name  " +
                    "{";
            for (String _class : classes) {
                String st = "";
                st += " ?name a " + _class + ". ";
                query += st;
            }
            query += " }";
        }



        /**/

        try {

            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header

            ArrayList<String> instances = new ArrayList<String>();
            while (s.hasNextLine()) {
                instances.add(stringManipulations.unicodeHexToString(s.nextLine()));
            }

            s.close();
            return instances;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }


    }




    /**
     * Returns a CountObjectList pair that containt two lists that represent:
     * <p>
     * -> For the given class "_class" ,returns, for each property, a list of the ranges that are used on that properties for
     * that class and, for each range, the count of instances that manifest it.
     * -> For the given class "_class", returns, for each property, a list of the ranges that are used on that properties for
     * that class and, for each range, the count of uses.
     * <p>
     * E.g: [ {"_id":"soccer club","count":90898},...]
     * On error, it returns null.
     */
    public  CountObjectListCuatriplete getRangesUsesAndInstances(List<String> classes) throws Exception {


        if (VERBOSE) {
            System.out.println("*Obtaining class list...");
        }




        //List of instances of at least one of  the classes (Union)
        ArrayList<String> unionInstanceList = getInstanceListFromClasses(classes, false);

        //List of instances of all of the classes (Intersection)
        ArrayList<String> intersectionInstanceList = getInstanceListFromClasses(classes, true);

        //Create a hash used to look if every instance of the union is in the intersection
        HashSet<String> intersectionInstanceSet = new HashSet<>(intersectionInstanceList);


        HashMap<String, HashMap<String, Integer>> usesCount = new HashMap<String, HashMap<String, Integer>>();
        HashMap<String, HashMap<String, Integer>> instancesCount = new HashMap<String, HashMap<String, Integer>>();

        //<propiedad,<rango,<valor,usos>>>
        //Used to generate suggestions with instances of the intersection of classes
        HashMap<String, HashMap<String, ValueStore>> intersectionValuesCount = new HashMap<String, HashMap<String, ValueStore>>();

        //Used to generate suggestions with instances of the union of classes
        HashMap<String, HashMap<String, ValueStore>> unionValuesCount = new HashMap<String, HashMap<String, ValueStore>>();


        int i = 0;
        int instanceCount = unionInstanceList.size();
        System.out.println("Count: " + instanceCount);

        try {

            for (String instance : unionInstanceList) {

                i++;
                if (VERBOSE && i % 1000 == 0) {
                    System.out.println("Processing instance " + i + " of " + instanceCount + " (" + (float) i * 100 / (float) instanceCount + "%)");
                }

                //Check if instance is in intersection. Used to calculate suggestions only of instances of the intersection,
                //while range information is based on the union.
                boolean instanceInIntersection = intersectionInstanceSet.contains(instance);


                processRangeInstance(instance, manager, usesCount, instancesCount, intersectionValuesCount, unionValuesCount, instanceInIntersection);


            }


            //Transformamos a Lista de dto.CountObject

            HashMap<String, List<CountObject>> usosTotal = new HashMap<String, List<CountObject>>();
            HashMap<String, List<CountObject>> instanciasTotal = new HashMap<String, List<CountObject>>();
            HashMap<String, HashMap<String, List<CountObject>>> intersectionValoresFinal = new HashMap<String, HashMap<String, List<CountObject>>>();
            HashMap<String, HashMap<String, List<CountObject>>> unionValoresFinal = new HashMap<String, HashMap<String, List<CountObject>>>();
            //List<dto.CountObject> usosProp = new ArrayList<>();


            //Procesamiento de la lista de usos
            Iterator iteradorUsos = usesCount.entrySet().iterator();

            while (iteradorUsos.hasNext()) {
                //Para cada par, se saca el tipo y la cuenta de URI's que lo usan
                Map.Entry pair = (Map.Entry) iteradorUsos.next();
                String property = (String) pair.getKey();


                HashMap<String, Integer> hashProp = (HashMap<String, Integer>) pair.getValue();
                //Ahora iteramos ese hash que contiene los valores para la propiedad property

                List<CountObject> usosProp = new ArrayList<CountObject>();
                usosTotal.put(property, usosProp);

                Iterator iteradorRangoUsos = hashProp.entrySet().iterator();

                while (iteradorRangoUsos.hasNext()) {
                    Map.Entry pair2 = (Map.Entry) iteradorRangoUsos.next();
                    String _type = (String) pair2.getKey();
                    int count = (Integer) pair2.getValue();

                    CountObject rc = new CountObject(_type, count);
                    usosProp.add(rc);

                    iteradorRangoUsos.remove();
                }


                iteradorUsos.remove(); // avoids a ConcurrentModificationException
            }


            //Procesamiento de la lista de instancias
            Iterator iteradorInstancias = instancesCount.entrySet().iterator();

            while (iteradorInstancias.hasNext()) {

                //Para cada par, se saca el tipo y la cuenta de URI's que lo usan
                Map.Entry pair = (Map.Entry) iteradorInstancias.next();
                String property = (String) pair.getKey();

                HashMap<String, Integer> hashProp = (HashMap<String, Integer>) pair.getValue();
                //Ahora iteramos ese hash que contiene los valores para la propiedad property

                List<CountObject> instanciasProp = new ArrayList<CountObject>();
                instanciasTotal.put(property, instanciasProp);

                Iterator iteradorRangoInstancias = hashProp.entrySet().iterator();

                while (iteradorRangoInstancias.hasNext()) {
                    Map.Entry pair2 = (Map.Entry) iteradorRangoInstancias.next();
                    String _type = (String) pair2.getKey();
                    int count = (Integer) pair2.getValue();

                    CountObject rc = new CountObject(_type, count);
                    instanciasProp.add(rc);

                    iteradorRangoInstancias.remove();
                }

                iteradorInstancias.remove(); // avoids a ConcurrentModificationException
            }


            //Procesamos la lista de sugerencias de intersección

            Iterator intersectionValuesIterator = intersectionValuesCount.entrySet().iterator();
            //Para cada propiedad...
            while (intersectionValuesIterator.hasNext()) {

                //Obtenemos la propiedad actual
                Map.Entry pair = (Map.Entry) intersectionValuesIterator.next();
                String property = (String) pair.getKey();

                //Añadimos propiedad actual al hashmap resultado
                HashMap<String, List<CountObject>> hashPropResult = new HashMap<String, List<CountObject>>();
                intersectionValoresFinal.put(property, hashPropResult);


                HashMap<String, ValueStore> hashProp = (HashMap<String, ValueStore>) pair.getValue();

                //Procesamos, para la propiedad actual, los rangos
                Iterator intersectionRangesIterator = hashProp.entrySet().iterator();

                //Para cada rango...
                while (intersectionRangesIterator.hasNext()) {
                    Map.Entry pair2 = (Map.Entry) intersectionRangesIterator.next();
                    String range = (String) pair2.getKey();
                    ValueStore values = (ValueStore) pair2.getValue();


                    //Para cada rango, añadimos al resultado su lista, de momento vacia
                    List<CountObject> cuentaRango = new ArrayList<CountObject>();
                    hashPropResult.put(range, cuentaRango);

                    HashMap<ValueStore.IdAndLabel, Integer> valueCounts = values.getMap();

                    //Procesamos, para propiedad y rango actual, los valores
                    Iterator intersectionCountIterator = valueCounts.entrySet().iterator();

                    //Para cada valor...
                    while (intersectionCountIterator.hasNext()) {
                        Map.Entry pair3 = (Map.Entry) intersectionCountIterator.next();
                        ValueStore.IdAndLabel valor = (ValueStore.IdAndLabel) pair3.getKey();
                        Integer cuenta = (Integer) pair3.getValue();

                        CountObject co = new CountObject(valor._id, cuenta);
                        //Creamos el CountObject y lo añadimos a la lista resultado
                        if(valor._label != null && valor._label.length() > 0){
                            //Label valida obtenida de la KB

                            co.setLabel(valor._label);

                        }
                        else{
                            co.setLabel(stringManipulations.URItoLabel(valor._id));
                        }

                        cuentaRango.add(co);

                        //Tenemos valor y cuenta
                        //System.out.println("Sugerencias: " + property + " - " + range + " - " + valor + " - " + cuenta);

                    }
                }

            }


            //Procesamos la lista de sugerencias de unión

            Iterator unionValuesIterator = unionValuesCount.entrySet().iterator();
            //Para cada propiedad...
            while (unionValuesIterator.hasNext()) {

                //Obtenemos la propiedad actual
                Map.Entry pair = (Map.Entry) unionValuesIterator.next();
                String property = (String) pair.getKey();

                //Añadimos propiedad actual al hashmap resultado
                HashMap<String, List<CountObject>> hashPropResult = new HashMap<String, List<CountObject>>();
                unionValoresFinal.put(property, hashPropResult);


                HashMap<String, ValueStore> hashProp = (HashMap<String, ValueStore>) pair.getValue();

                //Procesamos, para la propiedad actual, los rangos
                Iterator unionRangesIterator = hashProp.entrySet().iterator();

                //Para cada rango...
                while (unionRangesIterator.hasNext()) {
                    Map.Entry pair2 = (Map.Entry) unionRangesIterator.next();
                    String range = (String) pair2.getKey();
                    ValueStore values = (ValueStore) pair2.getValue();


                    //Para cada rango, añadimos al resultado su lista, de momento vacia
                    List<CountObject> cuentaRango = new ArrayList<CountObject>();
                    hashPropResult.put(range, cuentaRango);

                    HashMap<ValueStore.IdAndLabel, Integer> valueCounts = values.getMap();

                    //Procesamos, para propiedad y rango actual, los valores
                    Iterator unionCountIterator = valueCounts.entrySet().iterator();

                    //Para cada valor...
                    while (unionCountIterator.hasNext()) {


                        Map.Entry pair3 = (Map.Entry) unionCountIterator.next();
                        ValueStore.IdAndLabel valor = (ValueStore.IdAndLabel) pair3.getKey();
                        Integer cuenta = (Integer) pair3.getValue();

                        CountObject co = new CountObject(valor._id, cuenta);
                        //Creamos el CountObject y lo añadimos a la lista resultado
                        if(valor._label != null && valor._label.length() > 0){
                            //Label valida obtenida de la KB
                            co.setLabel(valor._label);

                        }
                        else{
                            co.setLabel(stringManipulations.URItoLabel(valor._id));
                        }

                        cuentaRango.add(co);

                    }
                }

            }


            //Returns two hashlists with dto.CountObject lists.


            CountObjectListCuatriplete pair = new CountObjectListCuatriplete(usosTotal, instanciasTotal, intersectionValoresFinal, unionValoresFinal);
            //System.out.println(gson.toJson(pair.getUses()));
            return pair;
        } catch (Exception ex) {
            System.out.println("Excepcion");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return null;

        }

    }

    /**
     * Dada una instancia, la procesa obteniendo, para cada una de sus propiedades, sus rangos y el
     * número de instancias que la usan y los usos.
     */
    private  void processRangeInstance(String instance, OntologyManager manager, HashMap<String, HashMap<String, Integer>> cuentas,
                                             HashMap<String, HashMap<String, Integer>> manifestaciones,
                                             HashMap<String, HashMap<String, ValueStore>> intersectionValuesCount,
                                             HashMap<String, HashMap<String, ValueStore>> unionValuesCount,
                                             boolean instanceIsInIntersection) throws Exception {


        //Procesa la cuenta de usos y de instancias de los rangos para una instancia

        String query = buildQuery(instance);
        String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
        URL url = new URL(url_path);
        Scanner s = new Scanner(url.openStream());
        s.nextLine(); //Skip header


        String currentProperty = "";
        String currentValue = "";
        String currentLabel = "";
        ArrayList<String> currentTypes = null;

        //Representa los tipos manifestados por esta instancia
        ArrayList<String> tiposManifestados = new ArrayList<String>();

        String line;
        String[] triple;
        String property;
        String value;
        String type;
        String label;


        while (s.hasNextLine()) {


            line = stringManipulations.unicodeHexToString(s.nextLine());
            triple = line.split("\t",-1);
            if (triple.length >= 2) {
                property = triple[0];
                value = triple[1];
                type = triple[2];
                if(getInstanceLabel){


                        label = triple[3];


                }
                else{
                    label = "";
                }



                //Si cambia property o value, es un valor nuevo
                //Si es la ultima tupla tambien tenemos que procesar
                if (!value.equalsIgnoreCase(currentValue) || !property.equalsIgnoreCase(currentProperty) || !s.hasNextLine()) {


                    //Es un nuevo valor, pasamos a procesar los del anterior.
                    if (currentTypes != null) {


                        insertarTipos(manager, currentTypes, currentProperty, currentValue, currentLabel, tiposManifestados, cuentas, manifestaciones, intersectionValuesCount, unionValuesCount, instanceIsInIntersection);

                    }
                    currentValue = value;
                    currentProperty = property;
                    currentLabel = label;


                    //Ya se han procesado los tipos del valor y propiedad actual, se crea nuevo Array
                    currentTypes = new ArrayList<String>();
                }
                //Si el tipo es UNDEFINED se procesa
                if (type==null || type.equals("")) {


                    //Puede ser:
                    //Ua cadena "Que pasa"@en , "Que pasa"
                    //Una instancia que no se tiene info de su tipo <http://s
                    //Algo con tiopo "valor"^^<http://tipo>

                    if (value.contains("^^")) {
                        //El tipo es lo que hay a la derecha de  ^^. Tipos primitivos
                        int index = value.indexOf("^^");
                        type = value.substring(index + 2, value.length());


                        //value.contains("dbpedia.org/resource")
                    } else if (unknownResourcePattern.matcher(value).matches()) {

                        //Es una URI sin informacion de su tipo -> Ponemos owl#Thing
                        //type = "unknownRDFType";
                        type = unknownResourceType;

                    } else if (value.contains("@")) {
                        //Cadenas con informacion de lenguaje
                        type = unknownLangStringType;
                    } else if (value.charAt(0) == '\"') {
                        //Debe ser una cadena, pues empieza con comillas
                        type = unknownStringType;
                    } else if (isNumeric(value)) {
                        //Es un numero
                        type = unknownNumericType;
                    } else {
                        //Desconocido
                        //type ="unknownRDFType"; -> Ponemos owl#Thing
                        type = unknownElseType;
                    }


                }

                //Check if there is a transformation specified on file. If so, transform.
                //(Used for example to transform <http://xmlns.com/foaf/0.1/Person> to <http://dbpedia.org/ontology/Person>.
                String transformTo = transformations.getOrDefault(type,null);
                if(transformTo!=null){
                    type = transformTo;
                }


                //Añadimos el nuevo tipo parseado a la lista de tipos para el valor
                //Comprobamos que el tipo está en la lista blanca
                boolean classAllowed = false;
                for(Pattern p:allowedPatterns){
                    if(p.matcher(type).matches()){
                        classAllowed = true;
                    }
                }

                if (classAllowed) {

                    currentTypes.add(type);

                }



            }
        }
        s.close();
        //Se ha acabado de leer la entrada, pero OJO, PUEDE QUE QUEDE UN VALOR POR PROCESAR, ya que sale del bucle
        //y puede haber tipos en currentTypes
        if (currentTypes != null && currentTypes.size() > 0) {
            insertarTipos(manager, currentTypes, currentProperty, currentValue, currentLabel, tiposManifestados, cuentas, manifestaciones, intersectionValuesCount, unionValuesCount, instanceIsInIntersection);
        }

        //Se ha terminado de procesar la instancia.
        //El metodo invocante se encargara de ordenar los resultados como convenga.
    }

    public  boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Una vez obtenidas todos los tripletes de un valor, obtiene el especifico y lo añade a los hashmaps.
     * Ademas, lo añade a sugerencias si "instanceIsInIntersection" es true.
     */
    private  void insertarTipos(OntologyManager manager, ArrayList<String> currentTypes, String currentProperty, String currentValue, String currentLabel,
                                      ArrayList<String> tiposManifestados, HashMap<String, HashMap<String, Integer>> cuentas,
                                      HashMap<String, HashMap<String, Integer>> manifestaciones,
                                      HashMap<String, HashMap<String, ValueStore>> intersectionValuesCount,
                                      HashMap<String, HashMap<String, ValueStore>> unionValuesCount,
                                      boolean instanceIsInIntersection) throws Exception {


        //Se obtiene una lista de los tipos más específicos
        List<String> specificList = manager.getConcreteClass(currentTypes.toArray(new String[currentTypes.size()]));

        //Para cada tipo específico, se marca que ha sido usado/instanciado/marcado para sugerencia
        //Tiene como consecuencia que pueden aparecer más usos de los calculados, TODO
        for (String specific : specificList) {


            if (specific.contains("XMLSchema")) {
                specific = stringManipulations.extractXMLSchema(specific);
            }


            if (specific.length() > 0) {

                //Se suma uno en el hashmap correspondiente al tipo más específico

                //Obtenemos el hashmap especifico de la propiedad
                HashMap<String, Integer> hashPropiedad = cuentas.get(currentProperty);
                if (hashPropiedad == null) {
                    //No habia ninguno, lo creo yo y lo añado
                    hashPropiedad = new HashMap<String, Integer>();
                    cuentas.put(currentProperty, hashPropiedad);
                }

                //System.out.println(gson.toJson(cuentas));


                //Sumamos uno
                Integer cuenta = hashPropiedad.get(specific);
                if (cuenta == null) {
                    cuenta = 0;
                }
                hashPropiedad.put(specific, cuenta + 1);


                //Si esta instancia todavia no ha manifestado la propiedad

                if (!tiposManifestados.contains(currentProperty + "-" + specific)) {
                    tiposManifestados.add(currentProperty + "-" + specific);


                    //Sumamos uno
                    //Obtenemos el hashmap especifico de la propiedad
                    HashMap<String, Integer> hashPropiedad2 = manifestaciones.get(currentProperty);
                    if (hashPropiedad2 == null) {
                        //No habia ninguno, lo creo yo y lo añado
                        hashPropiedad2 = new HashMap<String, Integer>();
                        manifestaciones.put(currentProperty, hashPropiedad2);
                    }

                    //Sumamos uno
                    Integer cuenta2 = hashPropiedad2.get(specific);
                    if (cuenta2 == null) {
                        cuenta2 = 0;
                    }
                    hashPropiedad2.put(specific, cuenta2 + 1);

                }


                //Procesado de sugerencias para instancias pertenecientes a la intersección de clases
                //Hay que añadir, para la propiedad "currentProperty" y el rango "specific",
                //que se ha usado un nuevo valor "currentValue".

                if (currentValue.length() > 0 && instanceIsInIntersection) {

                    //Obtenemos el hashmap especifico de la propiedad
                    HashMap<String, ValueStore> hashProp = intersectionValuesCount.get(currentProperty);
                    if (hashProp == null) {
                        //No habia ninguno, lo creo yo y lo añado
                        hashProp = new HashMap<String, ValueStore>();
                        intersectionValuesCount.put(currentProperty, hashProp);
                    }


                    ValueStore valueStore = hashProp.get(specific);
                    if (valueStore == null) {
                        //No existía valueStore para el rango, lo creamos
                        valueStore = new ValueStore();
                    }

                    //Sumamos el valor
                    ValueStore.IdAndLabel idAndLabel = new ValueStore.IdAndLabel();
                    idAndLabel._id = currentValue;
                    idAndLabel._label = currentLabel;

                    valueStore.add(idAndLabel, 1);

                    //Guardamos el ValueStore
                    hashProp.put(specific, valueStore);
                }


                //Procesado de sugerencias para instancias pertenecientes a la unión de clases
                //Hay que añadir, para la propiedad "currentProperty" y el rango "specific",
                //que se ha usado un nuevo valor "currentValue".

                if (currentValue.length() > 0) {

                    //Obtenemos el hashmap especifico de la propiedad
                    HashMap<String, ValueStore> hashProp = unionValuesCount.get(currentProperty);
                    if (hashProp == null) {
                        //No habia ninguno, lo creo yo y lo añado
                        hashProp = new HashMap<String, ValueStore>();
                        unionValuesCount.put(currentProperty, hashProp);
                    }


                    ValueStore valueStore = hashProp.get(specific);
                    if (valueStore == null) {
                        //No existía valueStore para el rango, lo creamos
                        valueStore = new ValueStore();
                    }

                    //Sumamos el valor
                    ValueStore.IdAndLabel idAndLabel = new ValueStore.IdAndLabel();
                    idAndLabel._id = currentValue;
                    idAndLabel._label = currentLabel;
                    valueStore.add(idAndLabel, 1);

                    //Guardamos el ValueStore
                    hashProp.put(specific, valueStore);
                }


            }
        }
    }




    /**
     * Returns the list of all the properties manifested by _class instances.
     * If an error occurs, returns null.
     */
    public  List<String> getPropertiesForClass(String _class) {
        String query = "SELECT DISTINCT ?property " +
                "{" +
                "?name a " + _class + " ." +
                "?name ?property ?value" +
                "}";

        List<String> result = new ArrayList<String>();
        try {
            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            while (s.hasNextLine()) {
                String property = stringManipulations.unicodeHexToString(s.nextLine());

                //Filter label and type
                if (!property.toLowerCase().contains("rdf-schema#label") && !property.toLowerCase().contains("rdf-syntax-ns#type")) {

                    //todo: Hay que filtrar y no agnadir aquellas propiedades cuyo valor sea algo del opengis y skos, o se nos colaran
                    //algunas que luego no salen en lo de rangos
                    result.add(property);
                }

            }

            s.close();
            return result;


        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }


    }

    /**
     * Returns the number of instances of the class _class that manifest
     * the property _property.
     * If an error occurs, returns -1.
     */
    public  SimpleCountObject getNumberInstancesOfProperty(ArrayList<String> classes, String _property) {

        String query = "SELECT (COUNT(DISTINCT ?name) AS ?pcount)" +
                "{";
        int actual = 0;
        for (String _class : classes) {
            String st = "";
            if (actual != 0) {
                st += " UNION ";
            }
            st += " { ?name a " + _class + " } ";
            query += st;
            actual++;
        }

        query += "?name " + _property + "   ?value" +
                "}";

        try {
            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            int result = s.nextInt();
            s.close();
            return new SimpleCountObject(result);


        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }


    /**
     * Returns the number of instances  that manifest
     * the property _property, without considering the class.
     * If an error occurs, returns -1.
     */
    public  SimpleCountObject getNumberGlobalInstancesOfProperty(String _property) {

        String query = "SELECT (COUNT(DISTINCT ?name) AS ?pcount)" +
                "{ ?name " + _property + " ?value }";


        try {
            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            int result = s.nextInt();
            s.close();
            return new SimpleCountObject(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * Returns the number of instances of the class _class that manifest
     * the property _property.
     * If an error occurs, returns -1.
     */
    public  SimpleCountObject getNumberUsesOfProperty(ArrayList<String> classes, String _property) {

        /**
         * WORK WITH UNION OF INSTANCES FROM EACH CLASS
         *  SELECT (COUNT(*) AS ?count)
         *  {
         *    {?name a class1}
         *    UNION
         *    {?name a class2}
         *    UNION
         *    {?name a class3}
         *    ...
         *    ?name PROPERTY ?value
         *  }
         */

        String query = "SELECT (COUNT(DISTINCT *) AS ?count)" +
                "{";
        int actual = 0;
        for (String _class : classes) {
            String st = "";
            if (actual != 0) {
                st += " UNION ";
            }
            st += " { ?name a " + _class + " } ";
            query += st;
            actual++;
        }

        query += "?name " + _property + "   ?value" +
                "}";


        /**
         * WORK WITH INTERSECTION OF INSTANCES FROM EACH CLASS
         *  SELECT (COUNT(*) AS ?count)
         *  {
         *    ?name a class1.
         *    ?name a class2.
         *    ?name a class3.
         *    ...
         *    ?name PROPERTY ?value
         *  }
         */

        /*String query = "SELECT (COUNT(*) AS ?count)" +
                "{";
        for(String _class: classes){
            String st = "";
            st += "?name a " + _class + ".";
            query+=st;
        }

        query += "?name " + _property + "   ?value" +
                "}";
        */

        try {
            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            int result = s.nextInt();
            s.close();
            return new SimpleCountObject(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }


    /**
     * This operation returns all the instances for a given class.
     * If the class doesn't contain "dbpedia.org" -> Empty list
     */
    public  List<CountObject> getInstancesForClass(String type) {

        //todo: Filter types that are not from URI, like String, DATE, etc.
        //todo: currently it is done via filtering dbpedia.org
        if (!type.contains("unknownRDFType") && (type.contains("dbpedia.org") || type.contains("owl#Thing"))) {
            List<CountObject> result = new ArrayList<CountObject>();
            String query = "SELECT ?name{ ?name a " + type + "}";

            try {
                query = stringManipulations.stringToUnicodeHex(query);
                String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
                URL url = new URL(url_path);
                Scanner s = new Scanner(url.openStream());

                s.nextLine(); //Skip header
                while (s.hasNextLine()) {
                    String instance = stringManipulations.unicodeHexToString(s.nextLine());
                    //id = URI, label = URItoLabel
                    CountObject co = new CountObject(instance, 0);
                    co.setLabel(stringManipulations.URItoLabel(instance));

                    result.add(co);

                }

                s.close();

                return result;


            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        } else {
            //todo: If unknownRDFType, what should we do?
            return new ArrayList<CountObject>();
        }


    }


    /**
     * Returns a List of instances with type "range" that are used with property "property" on
     * instances that belong at least to one of the classes contained in "classes". They are sorted
     * by popularity (number of uses), from higher to lower.
     * <p>
     * E.g: [ {"_id":"Stoke City F.C.", "count": 999},...]
     * On error, it returns -1.
     */
    public  List<CountObject> getPopularValuesForRange(List<String> classes, String property, String range) {

        List<CountObject> result = new ArrayList<CountObject>();


        //Complex query
        //Used for counting complex types (Values are URIs)
        String queryComplex = "SELECT ?value (COUNT(*) AS ?count)"
                + "{";
        int actual = 0;
        for (String _class : classes) {
            String st = "";
            if (actual != 0) {
                st += " UNION ";
            }
            st += " { ?name a " + _class + " } ";
            queryComplex += st;
            actual++;
        }

        queryComplex += " .";
        queryComplex += "?name " + property + " ?value .";
        queryComplex += "?value a " + range;
        queryComplex += "}";
        queryComplex += " GROUP BY ?value ";
        queryComplex += " ORDER BY DESC(?count)";

        //Simple query
        //Used for counting simple types (value^^type)
        String querySimple = "SELECT ?value (COUNT(*) AS ?count)"
                + "{";
        actual = 0;
        for (String _class : classes) {
            String st = "";
            if (actual != 0) {
                st += " UNION ";
            }
            st += " { ?name a " + _class + " } ";
            querySimple += st;
            actual++;
        }


        String rangeWithoutTriangle = range.replace("<", "");
        rangeWithoutTriangle = rangeWithoutTriangle.replace(">", "");
        querySimple += " .";
        querySimple += "?name " + property + " ?value .";
        querySimple += "FILTER regex(str(datatype(?value))," + "\"" + rangeWithoutTriangle + "\",\"i\")";
        querySimple += "}";
        querySimple += "GROUP BY ?value ";
        querySimple += "ORDER BY DESC(?count)";


        int simple_res = 0;
        try {
            querySimple = stringManipulations.stringToUnicodeHex(querySimple);
            String url_simple = endpoint + URLEncoder.encode(querySimple, "UTF-8");
            URL url_s = new URL(url_simple);
            Scanner simple = new Scanner(url_s.openStream());

            simple.nextLine(); //Skip header

            while (simple.hasNext()) {

                //We look for simple results (value^^range)

                String line = stringManipulations.unicodeHexToString(simple.nextLine());
                String[] triple = line.split("\t",-1);

                if (triple.length == 2) {


                    String name = triple[0];
                    int count = Integer.parseInt(triple[1]);
                    if (name.length() > 0) {
                        simple_res++;
                        CountObject co = new CountObject(name, count);
                        co.setLabel(stringManipulations.URItoLabel(name));
                        result.add(co);
                    }


                }

            }

            simple.close();

            if (simple_res == 0 && range.contains("<http://")) {

                //System.out.println("Looking complex with " + range);
                //No simple results, look for complex results (But only if URI)

                queryComplex = stringManipulations.stringToUnicodeHex(queryComplex);
                String url_complex = endpoint + URLEncoder.encode(queryComplex, "UTF-8");
                URL url_c = new URL(url_complex);
                Scanner complex = new Scanner(url_c.openStream());

                complex.nextLine(); //Skip header

                while (complex.hasNext()) {

                    String line = stringManipulations.unicodeHexToString(complex.nextLine());
                    String[] triple = line.split("\t",-1);

                    if (triple.length == 2) {
                        String name = triple[0];
                        int count = Integer.parseInt(triple[1]);


                        CountObject co = new CountObject(name, count);
                        co.setLabel(stringManipulations.URItoLabel(name));
                        result.add(co);
                    }

                }
                complex.close();

            }


            return result;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }


    }

    public  CountObject numberOfUsesByClass(String instance, String clase) {


        String query = "SELECT (COUNT(?instancia) AS ?p)" +
                "{" +
                "?instancia a " + clase + "." +
                "?instancia ?prop " + instance +

                "}";
        try {
            query = stringManipulations.stringToUnicodeHex(query);
            String url_path = endpoint + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(url_path);
            Scanner s = new Scanner(url.openStream());

            s.nextLine(); //Skip header
            int result = s.nextInt();
            s.close();
            return new CountObject(instance, result);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * Returns a list of key-value pairs, each pair belongs to one property of the instance. Each
     * pair has a list of resources (values with their label, uri and type) that are manifested.
     */
    public  List<KeyValuePair<List<Resource>>> getDataForInstance(String instance) {

        List<KeyValuePair<List<Resource>>> result = new ArrayList<>();

        HashMap<String, HashMap<String, Integer>> cuentas = new HashMap<>(); //not used
        HashMap<String, HashMap<String, Integer>> manifestaciones = new HashMap<>(); //not used


        HashMap<String, HashMap<String, ValueStore>> unionValuesCount = new HashMap<>(); //used
        HashMap<String, HashMap<String, ValueStore>> intersectionValuesCount = new HashMap<>(); //not used

        try {
            processRangeInstance(instance, manager, cuentas, manifestaciones, intersectionValuesCount, unionValuesCount, true);

            //Process intersectionValuesCount to extract a list of values, every one with its type

            //Iterate over each property
            Iterator iteradorValores = unionValuesCount.entrySet().iterator();
            while (iteradorValores.hasNext()) {

                //Obtain current property
                Map.Entry pair = (Map.Entry) iteradorValores.next();
                String property = (String) pair.getKey();

                //Create keyValuePair for current property
                KeyValuePair<List<Resource>> currentKeyValuePair = new KeyValuePair<>();
                currentKeyValuePair.setKey(property);
                currentKeyValuePair.setValue(new ArrayList<>());

                //Add keyValuePair to result list
                result.add(currentKeyValuePair);

                HashMap<String, ValueStore> hashProp = (HashMap<String, ValueStore>) pair.getValue();

                //We process ranges for current property
                Iterator iteradorRangos = hashProp.entrySet().iterator();

                //For every range...
                while (iteradorRangos.hasNext()) {

                    Map.Entry pair2 = (Map.Entry) iteradorRangos.next();
                    String currentType = (String) pair2.getKey();
                    ValueStore values = (ValueStore) pair2.getValue();


                    HashMap<ValueStore.IdAndLabel, Integer> valueCounts = values.getMap();

                    //Process values for property and current range (type)
                    Iterator iteradorCuenta = valueCounts.entrySet().iterator();

                    //Add every value to the result, with its label, uri and concrete type.
                    while (iteradorCuenta.hasNext()) {

                        Map.Entry pair3 = (Map.Entry) iteradorCuenta.next();

                        ValueStore.IdAndLabel uri = (ValueStore.IdAndLabel) pair3.getKey();
                        String label = "";
                        if(uri._label!=null && uri._label.length()>0){
                            label = uri._label;
                        }
                        else{
                            label = stringManipulations.cleanValue(stringManipulations.URItoLabel(uri._id));
                        }

                        Resource currentResource = new Resource(label, uri._id, currentType);

                        currentKeyValuePair.getValue().add(currentResource);

                    }
                }
            }



            return result;


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }


    private  String buildQuery(String _instance) {


        String consulta = "";
        if(!getInstanceLabel){
            consulta = "SELECT DISTINCT ?prop ?value ?type " +
                    "{ " +

                    _instance + " ?prop  ?value . " +
                    "OPTIONAL {?value a ?type }" +
                    "}";
        }
        else{
            consulta = "SELECT DISTINCT ?prop ?value ?type ?label " +
                    "{ " +

                    _instance + " ?prop  ?value . " +
                    "OPTIONAL {?value a ?type }." +
                    "OPTIONAL {?value " + instanceLabelProperty + " ?label}" +
                    "}";
        }


        return stringManipulations.stringToUnicodeHex(consulta);
    }

    private  int numberOfdbpediaPropertiesWithValuesOfInstance(String _instance) throws Exception {

        String consulta = "SELECT DISTINCT ?prop ?value" +
                "{ " +
                    _instance + " ?prop  ?value . " +
                "}";


        String url_path = endpoint + URLEncoder.encode(stringManipulations.stringToUnicodeHex(consulta), "UTF-8");
        URL url = new URL(url_path);
        Scanner s = new Scanner(url.openStream());
        s.nextLine(); //Skip header

        int number = 0;
        while (s.hasNextLine()) {

            String line = s.nextLine();
            String[] split =line.split("\t",-1);

            if(split[0].contains("dbpedia.org/ontology") || split[0].contains("xmlns.com/foaf/0.1") || split[0].contains("purl.org/dc/elements/1.1")){
               number++;
            }

        }
        return number;
    }




    public static void main(String[] args) throws Exception{

        DataObtaining dato = new DataObtaining();

        List<String> classes = new ArrayList<String>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Scientist>");
        ArrayList<String> instanceList = dato.getInstanceListFromClasses(classes,true);
        int numberOfInstances = 0;
        int numberOfProperties = 0;
        double results[] = new double[instanceList.size()];
        int v = 0;
        for(String instance:instanceList){
            numberOfInstances++;
            results[v] = dato.numberOfdbpediaPropertiesWithValuesOfInstance(instance);
            numberOfProperties+= results[v];
            v++;

        }
        double media = (double)numberOfProperties/(double)numberOfInstances;
        double varianza = 0.0;

        v = 0;
        for(String instance:instanceList){
            double rango;
            rango = Math.pow(results[v]-media,2f);
            varianza = varianza + rango;
            v++;

        }
        varianza = varianza/(double)numberOfInstances;


        System.out.println("props: " + numberOfProperties + " instancias:" + numberOfInstances);
        System.out.println("media: " + media);
        System.out.println("varianza: " + varianza);

       // String r = ServerWrappers.rangeSuggestions("<http://dbpedia.org/ontology/AdministrativeRegion>", "<http://dbpedia.org/ontology/Scientist>");
        //System.out.println(r);

    }


}
