package infoboxer.backend.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.dto.CountObjectListCuatriplete;
import infoboxer.backend.common.dto.KeyValuePair;
import infoboxer.backend.ontology.OntologyManager;
import infoboxer.backend.operations.auxiliar.RangeAndSuggestionsGrouping;
import infoboxer.backend.common.utils.StringManipulations;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * Esta operacion, dada una lista de clases obtiene, para todas las propiedades, todos los rangos y los usos de cada uno.
 * Además, agrupa los rangos, es decir, da los 3 mas importantes, y si quedan, los junta en un cuarto con rango el semántico.
 * Además, calcula los valores de sugerencia tanto de itnersección como de union (esto ultimo si hay mas de dos clases),
 * esos valores seran usados por suggestion para formar la tabla de la BD.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetRangesAndUses extends  Operation {

    public  String POPULAR_VALUES_RANGE_DIR;
    public  String POPULAR_VALUES_RANGE_PREFIX;

    @Value("${translator.enabled}")
    private boolean translatorEnabled;

    @Value("${messages.enabled}")
    private boolean messagesEnabled;

    @Autowired
    DataObtaining dataObtaining;

    @Autowired
    OntologyManager manager;

    @Autowired
    StringManipulations stringManipulations;

    //Params
    private List<String> classes;


    //Result for all properties of that combination of classes. The one getting saved.
    private List<KeyValuePair<List<CountObject>>> operationResult;

    public GetRangesAndUses(){
        super("GetRangesAndUses","rangesAndUsesCache/","rangesAndUses-");
        POPULAR_VALUES_RANGE_DIR = CACHE_DIR + "valuesForRangeCache/";
        POPULAR_VALUES_RANGE_PREFIX = "valuesForRange-";
    }


    public void initializeOperation(List<String> classes){

        this.classes = classes;


        ServerUtils.mkdir(POPULAR_VALUES_RANGE_DIR);

        //Normalize
        ServerUtils.removeDuplicates(classes);
        Collections.sort(classes);

        //Calculate file path and obtain cacheFile File object.
        try{
            String fileIdentifier = PREFIX;
            for (String _class : classes) {
                fileIdentifier += _class + "-";
            }


            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier) + "-core";
            cacheFile = new File(filePath + ".json");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }


    }

    //Loads object from cache
    public List<KeyValuePair<List<CountObject>>>   loadFromCache(){
        try{

            ServerUtils.mkdir(DIR); //Create directory if it doesn't exist

            //Check if data is in cache
            if (cacheFile.exists() && !cacheFile.isDirectory()) {
                if(messagesEnabled)
                    System.out.println("[READ CACHE OK] " + OPERATION_NAME + " from " + cacheFile);

                String text = readTextFromFile(cacheFile);
                List<KeyValuePair<List<CountObject>>> listaPropiedades =
                        mapper.readValue(text, new TypeReference<List<KeyValuePair<List<CountObject>>>>() {});


                operationResult = listaPropiedades;
                return listaPropiedades;

            }

            else{
                if(messagesEnabled)
                    System.out.println("[READ CACHE FAIL] " + OPERATION_NAME);
                return null; //No data in cache
            }
        }
        catch(Exception ex){
            //Something bad happened.
            ex.printStackTrace();
            return null;
        }

    }

    public List<KeyValuePair<List<CountObject>>> calculate() throws Exception{
        if(messagesEnabled)
            System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");


        String classList = "";
        for(String _class:classes){
            classList+=_class + "-";
        }

        //We don't need "instances", only uses, but we retrieve it anyway (Doesn't take more time)
        CountObjectListCuatriplete par = dataObtaining.getRangesUsesAndInstances(classes);

        //Hash of properties. For each property, list of ranges and count of uses.
        HashMap<String, List<CountObject>> uses = par.getUses();

        //Hash of properties. For each property, Hash of ranges. For each range, list of values and its count. Extracted from instances of ALL classes (AND).
        HashMap<String,HashMap<String,List<CountObject>>> intersectionSuggestionValues = par.getIntersectionValues();

        //Hash of properties. For each property, Hash of ranges. For each range, list of values and its count. Extracted from instances of at least ONE class (OR).
        HashMap<String,HashMap<String,List<CountObject>>> unionSuggestionValues = par.getUnionValues();

        //This hashmap will contain, for every property, the list of ranges shown to the user (after grouping)
        HashMap<String,List<String>> shownRanges = new HashMap<>();

        if(messagesEnabled){
            System.out.println("Intersection size: " + intersectionSuggestionValues.size());
            System.out.println("Union size: " + unionSuggestionValues.size());

        }

        RangeAndSuggestionsGrouping rangeAndSuggestionsGrouping = new RangeAndSuggestionsGrouping(manager);



        //GROUP the ranges for each property -> if a property has more than 3 ranges, the rest are grouped in a new semantic range.
        //If the semantic range already existed, the count is added to the existing range.
        //If a property has 3 or less ranges, it is unmodified.
        //Also, adds, for each property, the list of shown ranges to "shownRanges"
        List<KeyValuePair<List<CountObject>>> groupedRangesList = rangeAndSuggestionsGrouping.generateRangeLists(uses,shownRanges);


        //Save AND suggestions
        //Value list procesing (Suggestions). Saves to cache suggestions for ranges, only for the values of the instances that are on all the specified classes (AND).
        rangeAndSuggestionsGrouping.saveSuggestions(true,intersectionSuggestionValues,shownRanges,POPULAR_VALUES_RANGE_PREFIX, POPULAR_VALUES_RANGE_DIR,classList,stringManipulations);


        //Save OR suggestions
        //Value list procesing (Suggestions). Saves to cache suggestions for ranges, only for the values of the instances that are on at least one of the specified classes (OR).
        if(classes.size()>1){
            rangeAndSuggestionsGrouping.saveSuggestions(false,unionSuggestionValues,shownRanges,POPULAR_VALUES_RANGE_PREFIX,POPULAR_VALUES_RANGE_DIR,classList,stringManipulations);
        }

        operationResult = groupedRangesList;
        return groupedRangesList; //Return result



    }


}

