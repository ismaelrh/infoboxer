package infoboxer.backend.operations.suggestions;

import infoboxer.backend.common.dto.CountObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 *
 * Returns a JSON list of instances with type "range" on all the dataset that match the query.
 * It also stores retrieved data in Database in order to avoid executing the query.
 * Used for obtaining suggestions of a semantic property.
 * E.g: [ {"_id":"<http://dbpedia.org/resource/Madrid>", "label":"Madrid", "count": 999},...]
 * On error, it returns null.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetSuggestionsForSemantic {



    @Autowired
    GetInstancesForClass getInstancesForClass;

    @Autowired
    SuggestionsDatabaseHandler suggestionsDatabaseHandler;


    private String range;
    private String query;

    //Results
    private List<CountObject> operationResult;


    public GetSuggestionsForSemantic(){

    }

    public void initializeOperation(String range,String query){


        this.range = range;
        this.query = query;



    }


    /**
     * Returns suggestions for the range.
     * If table doesn't exist -> It calculates the full result and save them.
     * Else, only queries the database.
     */
    public List<CountObject> getSuggestions(){


        try{

            //1.- RANGE TABLE. Check if range table exists, create it otherwise
            String rangeTableName = suggestionsDatabaseHandler.getTableNameForRange(range);
            if(!suggestionsDatabaseHandler.checkIfTableExists(rangeTableName)){
                System.out.println("No existe tabla de rango para " + range);

                //Range table (that contains instances for range) doesn't exists. Create it
                getInstancesForClass.initializeOperation(range);
                List<CountObject> instancesForClass = (List<CountObject>) getInstancesForClass.doOperation();

                //Save them
                suggestionsDatabaseHandler.insertRecords(instancesForClass,rangeTableName);

            }


            //Results are available -> query them
            long time1 = System.currentTimeMillis();
            List<CountObject> result = suggestionsDatabaseHandler.retrieveRecordsForSemantic(query,rangeTableName);
            long time2 = System.currentTimeMillis();
            System.out.println("La consulta llevó " + (time2 - time1) + " ms");
            return result;

        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }

    }


    /**
     * Returns the full list of suggestions for the class-property-range combination, without filtering by query.
     * Used before saving to DB.
     */
    private List<CountObject> calculate() throws Exception {


        System.out.println("[OPERATION] Calculating " + " suggestions " + "...");


        //2.- Get all values for category
        getInstancesForClass.initializeOperation(range);
        List<CountObject> instancesForClass = (List<CountObject>) getInstancesForClass.doOperation();


        //Have to sort before inserting to give the user significant results the first time a query is done
        Collections.sort(instancesForClass);



        this.operationResult = instancesForClass;
        return this.operationResult;
    }






}
