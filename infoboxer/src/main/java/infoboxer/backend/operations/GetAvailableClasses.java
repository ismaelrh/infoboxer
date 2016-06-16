package infoboxer.backend.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.common.utils.ServerUtils;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.utils.StringManipulations;
import infoboxer.backend.translator.LabelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Returns, for the given class, its direct and indirect childs and a count
 * of instances of each one.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetAvailableClasses extends Operation {

    //Options
    @Value("${translator.enabled}")
    private boolean translatorEnabled;

    @Value("${messages.enabled}")
    private boolean messagesEnabled;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    DataObtaining dataObtaining;

    @Autowired
    StringManipulations stringManipulations;

    public LabelManager labelManager_properties;

    //Params
    private String superClass;
    private String targetLanguage;

    //Result
    private List<CountObject> operationResult;


    public GetAvailableClasses(){
        super("GetAvailableClasses","availableClasses/","availableClasses-");
    }

    public void initializeOperation(String superClass,String targetLanguage){

        //Initialize label manager
        labelManager_properties = (LabelManager) context.getBean("labelManager");
        labelManager_properties.initLabelManager(LabelManager.PROPERTIES_MODE);

        this.superClass = superClass;
        this.targetLanguage = targetLanguage;


        try{
            String fileIdentifier = PREFIX + superClass;
            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier);
            cacheFile = new File(filePath + ".json");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }


    //Loads object from cache
    public List<CountObject> loadFromCache(){

        try{

            ServerUtils.mkdir(DIR); //Create directory if it doesn't exist

            //Check if data is in cache
            if (cacheFile.exists() && !cacheFile.isDirectory()) {

                if(messagesEnabled)
                    System.out.println("[READ CACHE OK] " + OPERATION_NAME);

                String text = readTextFromFile(cacheFile);
                List<CountObject> listaClases = mapper.readValue(text, new TypeReference<List<CountObject>>() {});

                //We attach translations
                for (CountObject obj : listaClases) {
                    //Extract label
                    String id = obj.get_id();

                    //Translate label
                    String human = stringManipulations.URItoHumanReadable(id);

                    //Add label to object
                    if(translatorEnabled && !targetLanguage.equalsIgnoreCase("en")){
                        ///Translate
                        try{
                            obj.setLabel(labelManager_properties.getTranslation("es", human));
                        }
                        catch(IOException ex){
                            //Can't obtain translation -> use default
                            ex.printStackTrace();
                            obj.setLabel(human);
                        }
                    }
                    else{
                        //Only add "label"
                        obj.setLabel(human);
                    }

                }

                operationResult = listaClases;

                return operationResult;
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

    public List<CountObject> calculate() {
        if(messagesEnabled)
            System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");
        operationResult = dataObtaining.getAvailableClasses(superClass);
        return operationResult;
    }


}
