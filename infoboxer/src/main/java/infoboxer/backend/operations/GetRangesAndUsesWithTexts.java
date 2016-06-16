package infoboxer.backend.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.dto.KeyValuePair;
import infoboxer.backend.ontology.OntologyManager;
import infoboxer.backend.translator.LabelManager;
import infoboxer.backend.common.utils.StringManipulations;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Esta operacion, dada una lista de clases obtiene, para todas las propiedades, todos los rangos y los usos de cada uno.
 * Además, agrupa los rangos, es decir, da los 3 mas importantes, y si quedan, los junta en un cuarto con rango el semántico.
 * Además, calcula los valores de sugerencia tanto de itnersección como de union (esto ultimo si hay mas de dos clases),
 * esos valores seran usados por suggestion para formar la tabla de la BD.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetRangesAndUsesWithTexts extends  Operation {

    public  String POPULAR_VALUES_RANGE_DIR;
    public  String POPULAR_VALUES_RANGE_PREFIX;

    @Value("${translator.enabled}")
    private boolean translatorEnabled;

    @Value("${messages.enabled}")
    private boolean messagesEnabled;

    @Autowired
    private WebApplicationContext context;



    @Autowired
    StringManipulations stringManipulations;

    @Autowired
    GetRangesAndUses getRangesAndUses;

    @Autowired
    OntologyManager manager;

    public static LabelManager labelManager_ranges;

    //Params
    private List<String> classes;
    private String targetLanguage;

    //Result for all properties of that combination of classes. The one getting saved.
    private List<KeyValuePair<List<CountObject>>> operationResult;

    public GetRangesAndUsesWithTexts(){
        super("GetRangesAndUsesWithTexts","rangesAndUsesCache/","rangesAndUses-");
        POPULAR_VALUES_RANGE_DIR = CACHE_DIR + "valuesForRangeCache/";
        POPULAR_VALUES_RANGE_PREFIX = "valuesForRange-";
    }


    public void initializeOperation(List<String> classes, String language){

        //Initialize label manager for ranges
        labelManager_ranges = (LabelManager) context.getBean("labelManager");
        labelManager_ranges.initLabelManager(LabelManager.CLASSES_MODE);

        this.classes = classes;
        this.targetLanguage = language;

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


            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier) + "-" + language;
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


        getRangesAndUses.initializeOperation(classes);
        List<KeyValuePair<List<CountObject>>> result = (List<KeyValuePair<List<CountObject>>>) getRangesAndUses.doOperation();


        //Attach translations to each range
        for(KeyValuePair<List<CountObject>> propiedad: result){
            for (CountObject obj : propiedad.getValue()) {

                //1st.- Check if range label is in ontology
                obj.setLabel(manager.getRdfsLabel(obj.get_id(),targetLanguage));

                //2nd.- If not in ontology, derive it from URI
                if(obj.getLabel() == null){

                    //Extract label
                    String id = obj.get_id();

                    //Translate label
                    String human = stringManipulations.typeToHumanReadable(id);

                    //Add label to object
                    if (translatorEnabled && !targetLanguage.equalsIgnoreCase("en")) {

                        ///Translate
                        try {
                            String translated = labelManager_ranges.getTranslation(targetLanguage, human);
                            obj.setLabel(translated);
                        } catch (IOException ex) {
                            //Can't obtain translation -> use default
                            obj.setLabel(human);
                        }

                    } else {
                        //Only add "label"
                        obj.setLabel(human);
                    }
                }


            }
        }


        operationResult = result;
        return result; //Return result



    }


}

