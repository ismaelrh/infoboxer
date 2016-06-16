package infoboxer.backend.operations.suggestions;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.utils.ServerUtils;
import infoboxer.backend.operations.Operation;
import infoboxer.backend.common.utils.StringManipulations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the "GetPopularValuesForRange" operation.
 * This operation returns a list of values for a list of classes, property and range,
 * with the count of uses of each one.
 *
 * By default, the results are previously obtained by getRangesAndUses and stored in cache.
 * If no data in cache, they can be calculated again.
 *
 * If a basic type or unknownRDFType is asked, then the returned suggestions are retrieved
 * from the "union" intersection cache.
 *
 * Else, the result is from the "intersection" cache.
 */

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetPopularValuesForRange extends Operation {


    @Autowired
    StringManipulations stringManipulations;

    //Parameters
    private List<String> categoryList;
    private String property;
    private String range;

    //Result
    private List<CountObject> operationResult;

    public GetPopularValuesForRange(){
        super("GetPopularValuesForRange","valuesForRangeCache/", "valuesForRange-");
    }


    public void initializeOperation(List<String> categoryList, String property, String range){

        this.categoryList = categoryList;
        this.property = property;
        this.range = range;

        String categoryListString = "";
        for(String category:categoryList){
            categoryListString+=category + "-";
        }


        try{

            String fileIdentifier = PREFIX + "-" + categoryListString + "-" + property + "-" + range;
            String filePath = DIR;

            //If categoryList.size()==1, then always get suggestions from union (or intersection, they are the same)
            if(categoryList.size()==1){
                filePath += "intersect" + stringManipulations.calculateHash(fileIdentifier);
            }
            else{ //More than 1 class

                //owl#Thing is also get from union!!!
                if(!range.contains("dbpedia.org")){
                    //If not contains dbpedia.org  -> Is unknown or is a basic type -> get suggestion from union
                    filePath += "union" + stringManipulations.calculateHash(fileIdentifier);

                }
                else{
                    //If contains dbpedia.org -> complex type, get suggestions from intersection
                    filePath += "intersect" + stringManipulations.calculateHash(fileIdentifier);

                }
            }


            System.out.println("File should exist: " + filePath + ".json");
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

                System.out.println("[READ CACHE OK] " + OPERATION_NAME);

                String text = readTextFromFile(cacheFile);
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
            ex.printStackTrace();
            return null;
        }

    }


    @Override
    public List<CountObject> calculate() throws Exception {

        //It should only be called when the range is not in the intersection.
        System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "... NOT IN CACHE, SO IT IS EMPTY LIST");


        this.operationResult =  Collections.emptyList();
        return this.operationResult;


       /* List<CountObject> result = DataObtaining.getPopularValuesForRange(categoryList,property,range);

        //Clean values, removing quotes, arrobas, etc.
        for(CountObject co: result){
            stringManipulations.cleanValue(co);
        }

        this.operationResult = result;
        return this.operationResult;*/
    }




}
