package infoboxer.backend.operations.suggestions;

import infoboxer.backend.common.dto.CountObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 *
 * Returns a JSON list of instances with type "range" that are relevant for the context (classList, property and range).
 * It uses values of that range for all the dataset, and for the current classList-property-range combination.
 * They are sorted by popularity (number of uses), from higher to lower. Popularity is the name of uses by instances
 * of one or more of the classes of classList, in that property and range.
 * It also stores retrieved data in Database in order to avoid executing the query.
 * E.g: [ {"_id":"<http://dbpedia.org/resource/Madrid>", "label":"Madrid", "count": 999},...]
 * On error, it returns null.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetSuggestions{


    @Autowired
    GetPopularValuesForRange getPopularValuesForRange;

    @Autowired
    GetInstancesForClass getInstancesForClass;

    @Autowired
    SuggestionsDatabaseHandler suggestionsDatabaseHandler;

    //Params
    private List<String> categoryList;
    private String property;
    private String range;
    private String query;

    //Results
    private List<CountObject> operationResult;


    public GetSuggestions(){

    }

    public void initializeOperation(List<String> categoryList, String property, String range,String query){



        this.categoryList = categoryList;
        this.property = property;
        this.range = range;
        this.query = query;

        Collections.sort(this.categoryList);

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

            //2.- VALUES FOR RANGE. Check if table exist.
            String tableName = suggestionsDatabaseHandler.getTableName(categoryList,property,range);

            //If table doesn't exist -> Calculate results and save them
            if(!suggestionsDatabaseHandler.checkIfTableExists(tableName)){
                System.out.println("No existe tabla de sugerencias " + tableName);
                //Get values for range
                getPopularValuesForRange.initializeOperation(categoryList,property,range);
                List<CountObject> list = (List<CountObject>) getPopularValuesForRange.doOperation();

                //Insert records
                suggestionsDatabaseHandler.insertRecords(list,tableName);
            }

            //Results are available -> query them
            long time1 = System.currentTimeMillis();
            List<CountObject> result = suggestionsDatabaseHandler.retrieveRecords(query,tableName,rangeTableName);
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


        //1.- Get popular values for category-property-range
        getPopularValuesForRange.initializeOperation(categoryList,property,range);
        List<CountObject> popularValuesForRange = (List<CountObject>) getPopularValuesForRange.doOperation();

        //1.1.- Transform 'popularValuesForRange' to hash in order to improve compare speed
        HashMap<String,Integer> popularValuesForRangeMap = new HashMap<String,Integer>();
        for(CountObject popularValue: popularValuesForRange){
            popularValuesForRangeMap.put(popularValue.get_id(),popularValue.getCount());
        }

        //2.- Get all values for category
        getInstancesForClass.initializeOperation(range);
        List<CountObject> instancesForClass = (List<CountObject>) getInstancesForClass.doOperation();


        //3.- For every global instance, search for a match in 'popularValuesForRange'
        //If a match is found, the value is assigned.
        for(CountObject value: instancesForClass){
            Integer count = popularValuesForRangeMap.getOrDefault(value.get_id(),null);
            if(count!=null){
                //Match
                value.setCount(count);
                //Remove from popularValuesForRange
                popularValuesForRange.remove(value.get_id());

            }
        }

        if(instancesForClass.size()==0) {
            //3.1.- Special case, 0 instances for that class because it doesn't contain "dbpedia.org"
            instancesForClass = popularValuesForRange;
        }
        else{
            //4.- All the values that remain in "popularValuesForRange" are added also to the final list
            //This is used when owl#Thing contains unknownRDFType and owl#Thing values so we want to show
            //also the "unknownRDFType" values and count
            Iterator it = popularValuesForRangeMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                instancesForClass.add(new CountObject((String)pair.getKey(),(Integer)pair.getValue()));
            }

        }


        //Have to sort before inserting to give the user significant results the first time a query is done
        Collections.sort(instancesForClass);



        this.operationResult = instancesForClass;
        return this.operationResult;
    }






}
