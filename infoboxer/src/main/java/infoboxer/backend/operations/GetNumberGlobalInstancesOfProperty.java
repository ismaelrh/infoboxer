package infoboxer.backend.operations;

import infoboxer.backend.common.utils.ServerUtils;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.SimpleCountObject;
import infoboxer.backend.common.utils.StringManipulations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Created by ismaro3 on 9/02/16.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetNumberGlobalInstancesOfProperty extends Operation {



    @Value("${messages.enabled}")
    private boolean messagesEnabled;

    @Autowired
    DataObtaining dataObtaining;

    @Autowired
    StringManipulations stringManipulations;

    //Params
    private String _property;

    //Result
    private SimpleCountObject operationResult;

    public GetNumberGlobalInstancesOfProperty(){
        super("GetNumberGlobalInstancesOfProperty","propertiesGlobalInstances/","propertiesGlobalInstances-");
    }

    public void initializeOperation(String _property){


        this._property = _property;

        //Calculate file path and obtain cacheFile File object.
        try{
            String fileIdentifier = PREFIX + _property;
            String filePath =  DIR + stringManipulations.calculateHash(fileIdentifier);
            cacheFile = new File(filePath + ".json");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }

    //Loads object from cache
    public SimpleCountObject loadFromCache(){

        try{

            ServerUtils.mkdir(DIR); //Create directory if it doesn't exist

            //Check if data is in cache
            if (cacheFile.exists() && !cacheFile.isDirectory()) {
                if(messagesEnabled)
                    System.out.println("[READ CACHE OK] " + OPERATION_NAME);

                String text = readTextFromFile(cacheFile);
                SimpleCountObject result = mapper.readValue(text, SimpleCountObject.class);
                operationResult = result;

                return result;
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

    /*
    getNumberGlobalInstancesOfProperty
     */


    public SimpleCountObject calculate() {
        if(messagesEnabled)
            System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");
        operationResult = dataObtaining.getNumberGlobalInstancesOfProperty(_property);
        return operationResult;
    }
}
