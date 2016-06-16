package infoboxer.backend.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.common.utils.ServerUtils;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.KeyValuePair;
import infoboxer.backend.common.dto.Resource;
import infoboxer.backend.common.utils.StringManipulations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Created by ismaro3 on 29/02/16.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetDataForInstance extends Operation{


    @Value("${messages.enabled}")
    private boolean messagesEnabled;

    @Autowired
    DataObtaining dataObtaining;

    @Autowired
    StringManipulations stringManipulations;

    //Method params
    private String instanceURI;

    //Method result
    private List<KeyValuePair<List<Resource>>> operationResult;


    public GetDataForInstance(){
        super("GetDataForInstance","dataForInstanceCache/", "dataForInstance-");
    }


    //Constructor
    public void initializeOperation(String instanceURI){


        this.instanceURI = instanceURI;


        //Calculate file path and obtain cacheFile File object.
        try{
            String fileIdentifier = PREFIX + instanceURI;
            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier);
            cacheFile = new File(filePath + ".json");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }

    //Loads object from cache
    public List<KeyValuePair<List<Resource>>> loadFromCache(){

        try{

            ServerUtils.mkdir(DIR); //Create directory if it doesn't exist

            //Check if data is in cache
            if (cacheFile.exists() && !cacheFile.isDirectory()) {

                if(messagesEnabled)
                    System.out.println("[READ CACHE OK] " + OPERATION_NAME);

                String text = readTextFromFile(cacheFile);
                List<KeyValuePair<List<Resource>>> result = mapper.readValue(text, new TypeReference<List<KeyValuePair<List<Resource>>>>() {});
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


    public List<KeyValuePair<List<Resource>>> calculate() {
        if(messagesEnabled)
            System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");
        operationResult = dataObtaining.getDataForInstance(instanceURI);
        return operationResult;
    }



}
