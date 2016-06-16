package infoboxer.backend.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.common.dto.CountObject;
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
import java.util.*;

/**
 /**
 * Returns a JSON list that represent the properties manifested by
 * instances which belong to at least one class of "classes" list  and, for each one, gives the number of  instances
 * that manifest it.
 * It also stores retrieved data in order to avoid executing the query.
 * * E.g: [ {"_id":"team","count":88000},...]
 * On error, it returns null.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetPropertiesListWithTexts extends Operation {


    @Value("${translator.enabled}")
    private boolean translatorEnabled;

    @Value("${messages.enabled}")
    private boolean messagesEnabled;

    @Autowired
    GetPropertiesList getPropertiesList;

    @Autowired
    StringManipulations stringManipulations;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    OntologyManager manager;

    private boolean semanticPropertiesEnabled = true;

    public static LabelManager labelManager_properties ;
    public static LabelManager labelManager_ranges ;
    //Prams
    private List<String> classes;
    private String targetLanguage;
    private List<CountObject> operationResult;



    public GetPropertiesListWithTexts(){
        super("GetPropertiesListWithLabels","propertiesListCache/","propertiesList-");

    }
    public void initializeOperation(List<String> classes,String targetLanguage, boolean semanticPropertiesEnabled){

        //Initialize label manager for properties
        labelManager_properties = (LabelManager) context.getBean("labelManager");
        labelManager_properties.initLabelManager(LabelManager.PROPERTIES_MODE);

        //Initialize label manager for ranges
        labelManager_ranges = (LabelManager) context.getBean("labelManager");
        labelManager_ranges.initLabelManager(LabelManager.CLASSES_MODE);

        this.classes = classes;
        this.targetLanguage = targetLanguage;
        this.semanticPropertiesEnabled = semanticPropertiesEnabled;

        try {

            //Normalize
            ServerUtils.removeDuplicates(classes);
            Collections.sort(classes);

            //Calculate file path and obtain cacheFile File object.

            String fileIdentifier = PREFIX;
            for (String _class : classes) {
                fileIdentifier += _class + "-";
            }


            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier) + "-" + targetLanguage + ".json";


            cacheFile = new File(filePath);

            ServerUtils.mkdir(DIR);


        }

        catch(Exception ex){
            ex.printStackTrace();
        }

    }



    @Override
    public List<CountObject> loadFromCache() {


        ServerUtils.mkdir(DIR);

        try{
            if (cacheFile.exists() && !cacheFile.isDirectory()) {

                if(messagesEnabled)
                    System.out.println("[READ CACHE OK] " + OPERATION_NAME);


                String text = readTextFromFile(cacheFile);

                operationResult= mapper.readValue(text, new TypeReference<List<CountObject>>() {});

                return operationResult;
            } else {
                if(messagesEnabled)
                    System.out.println("[READ CACHE FAIL] " + OPERATION_NAME);
                return null;
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }




    @Override
    public Object doOperation(){
        List<CountObject> result = (List<CountObject>) super.doOperation();
        if(!semanticPropertiesEnabled){
            removeSemantics(result);
        }
        return result;
    }

    @Override
    public List<CountObject> calculate() throws Exception {

        if(messagesEnabled)
            System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");


        getPropertiesList.initializeOperation(classes);
        List<CountObject> properties = (List<CountObject>) getPropertiesList.doOperation();
        addLabelsAndComments(properties,targetLanguage);
        operationResult = properties;
        return properties;


    }

    /**
     * Given a list of properties, it fills it with property label and comments, and range label, in the given
     * language. If not available, the label is derived from the URI and then translated from english to the
     * target language.
     */
    private void addLabelsAndComments(List<CountObject> properties, String language){

        for(CountObject co: properties){

            //1.- Check if label is in ontology
            co.setLabel(manager.getRdfsLabel(co.get_id(),language));

            //2.- Check if comment is in ontology
            co.setComment(manager.getRdfsComment(co.get_id(),language));

            if(co.getLabel()==null || co.getLabel().length()==0){
                //Label is not in ontology -> derive it from URI
                String humanLabel = stringManipulations.URItoHumanReadable(co.get_id());

                //Add label to object
                if(translatorEnabled && !targetLanguage.equalsIgnoreCase("en")){
                    ///Translate
                    String res = humanLabel;
                    try{
                        res = labelManager_properties.getTranslation(targetLanguage,humanLabel);
                        co.setLabel(res);

                    }
                    catch(IOException ex){
                        //Can't obtain translation -> use default
                        co.setLabel(humanLabel);
                    }

                }
                else{
                    //Only add "label"
                    co.setLabel(humanLabel);
                }
            }


            if(co.isSemantic() && co.getRangeForSemantic()!=null){
                co.getRangeForSemantic().setLabel(manager.getRdfsLabel(co.getRangeForSemantic().get_id(),targetLanguage));
            }


            if(co.isSemantic() && (co.getRangeForSemantic().getLabel()==null || co.getRangeForSemantic().getLabel().length()==0)){

                //Translate label of semanticRange
                String humanLabel = stringManipulations.URItoHumanReadable(co.getRangeForSemantic().get_id());

                //Add label to object
                if(translatorEnabled && !targetLanguage.equalsIgnoreCase("en")){
                    ///Translate
                    try{
                        String res = labelManager_ranges.getTranslation(targetLanguage,humanLabel);
                        humanLabel = res;
                        co.getRangeForSemantic().setLabel(humanLabel);
                    }
                    catch(IOException ex){
                        //Can't obtain translation -> use default
                        co.getRangeForSemantic().setLabel(humanLabel);
                    }
                }
                else{
                    co.getRangeForSemantic().setLabel(humanLabel);
                }


            }
        }
    }

    private void removeSemantics(List<CountObject> properties){

        for(Iterator<CountObject> iterator = properties.iterator(); iterator.hasNext();){
            CountObject co = iterator.next();
            if(co.isSemantic()){
                iterator.remove();
            }
        }
    }

}




