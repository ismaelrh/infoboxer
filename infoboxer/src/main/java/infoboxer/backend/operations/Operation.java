package infoboxer.backend.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Abstract class that represents an operation.
 * An operation has methods for loading result from cache, saving to cache and calculate new results.
 * Also, a public method doOperation is provided.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class Operation {

    //Options
    @Value("${cache.enabled}")
    private boolean cacheEnabled;

    @Value("${messages.enabled}")
    private boolean messagesEnabled;


    protected static ObjectMapper mapper = new ObjectMapper(); //JSON Mapper for all operations

    protected File cacheFile; //File used to read/write to filesystem cache
    protected String OPERATION_NAME; //Operation's name
    protected String DIR;
    protected String PREFIX;
    protected String CACHE_DIR = "cacheFiles/";//Cache directory


    public Operation(String OPERATION_NAME, String DIR, String PREFIX){

        this.OPERATION_NAME = OPERATION_NAME;
        this.DIR = CACHE_DIR + DIR;
        this.PREFIX =  PREFIX;

        ServerUtils.mkdir(CACHE_DIR);

    }

    /**
     * Saves the calculated result to cache file "cacheFile".
     * Pre: operationResult != null && cacheFile!=null
     */
    public boolean saveResultToCache(Object operationResult) {

        if(messagesEnabled)
            System.out.println("[WRITE CACHE] " + OPERATION_NAME + " operation");
        try{

            //Obtain JSON
            String json = mapper.writeValueAsString(operationResult);

            //Write JSON to file
            PrintWriter writer = new PrintWriter(cacheFile, "UTF-8");
            writer.println(json);
            writer.close();
            return true;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return false;
        }

    }

    public abstract Object calculate() throws Exception;

    public abstract Object loadFromCache();

    protected String readTextFromFile(File file) throws FileNotFoundException{
        //File exists

        try{
            String text = "";
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while((line=br.readLine())!=null)
            {
                // do something
                text+=line;
            }


            br.close();

            return text;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }


    }


    /**
     * Retrieves the result of the operation, either by getting it of cache, or calling the "calculate()" method.
     * If cache is not activated, always calls the "calculate()" method.
     * @return result or null if there was a problem.
     */
    public Object doOperation(){


        Object result = null;
        boolean previouslyOnCache = false;


        try{


            //If cache is enabled, try to load from there
            if(cacheEnabled){

                result = this.loadFromCache();
                if(result!=null){
                    //There was a valid result on cache.
                    previouslyOnCache = true;
                }
            }


            //Calculate if cache not enabled or not valid result
            if(!cacheEnabled || result==null) {
                result = this.calculate();
            }

            //Save to cache if cache is enabled, a valid result has been retrieved and no valid
            //result was previously on cache (If a valid one was present, there is no need to save again).
            if(cacheEnabled && result!=null && !previouslyOnCache){
                this.saveResultToCache(result);
            }

            return result;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }


    }



}
