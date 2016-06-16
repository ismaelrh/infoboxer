package infoboxer.backend.operations.suggestions;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.common.utils.ServerUtils;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.operations.Operation;
import infoboxer.backend.common.utils.StringManipulations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * This operation returns all the instances for a given class.
 * If the class doesn't contain "dbpedia.org" -> Empty list
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetInstancesForClass extends Operation {


    @Autowired
    DataObtaining dataObtaining;

    @Autowired
    StringManipulations stringManipulations;

    //Parameters
    private String category;

    //Result
    private List<CountObject> operationResult;

    public GetInstancesForClass(){
        super("GetInstancesForClass","instancesForClass/","instancesForClass-");
    }

    public void initializeOperation(String category){

        this.category = category;

        try{
            String fileIdentifier = PREFIX + category;
            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier);
            cacheFile = new File(filePath + ".json");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }



    //Loads object from cache
    public List<CountObject> loadFromCache(){

        String text = "";
        try{

            ServerUtils.mkdir(DIR); //Create directory if it doesn't exist

            //Check if data is in cache
            if (cacheFile.exists() && !cacheFile.isDirectory()) {

                System.out.println("[READ CACHE OK] ");

                 text = readTextFromFile(cacheFile);

                List<CountObject> result = mapper.readValue(text, new TypeReference<List<CountObject>>() {});
                operationResult = result;

                return result;
            }
            else{
                System.out.println("[READ CACHE FAIL] " + OPERATION_NAME);
                return null; //No data in cache
            }
        }
        catch(Exception ex){
            //Something bad happened.
            System.err.println(text);
            ex.printStackTrace();
            return null;
        }

    }


    @Override
    public List<CountObject> calculate() throws Exception {
        System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");
        this.operationResult = dataObtaining.getInstancesForClass(category);
        return this.operationResult;

    }




}
