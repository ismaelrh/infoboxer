package infoboxer.backend.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.dto.SimpleCountObject;
import infoboxer.backend.ontology.OntologyManager;
import infoboxer.backend.common.utils.StringManipulations;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
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
public class GetPropertiesList extends Operation {


    @Value("${translator.enabled}")
    private boolean translatorEnabled;

    @Value("${messages.enabled}")
    private boolean messagesEnabled;




    @Autowired
    StringManipulations stringManipulations;

    @Autowired
    GetNumberGlobalInstancesOfProperty getNumberGlobalInstancesOfProperty;

    @Autowired
    DataObtaining dataObtaining;

    @Autowired
    OntologyManager manager;


    //Prams
    private List<String> classes;
    private List<CountObject> operationResult;



    public GetPropertiesList(){
        super("GetPropertiesList","propertiesListCache/","propertiesList-");

    }
    public void initializeOperation(List<String> classes){

        this.classes = classes;


        try {

            //Normalize
            ServerUtils.removeDuplicates(classes);
            Collections.sort(classes);

            //Calculate file path and obtain cacheFile File object.

            String fileIdentifier = PREFIX;
            for (String _class : classes) {
                fileIdentifier += _class + "-";
            }


            String filePath = DIR + stringManipulations.calculateHash(fileIdentifier) + "-core" +  ".json";


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

                operationResult = mapper.readValue(text, new TypeReference<List<CountObject>>() {});
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
    public List<CountObject> calculate() throws Exception {
        if(messagesEnabled)
            System.out.println("[OPERATION] Calculating " + OPERATION_NAME + "...");


        List<CountObject> result;

        List<CountObject> semanticProps = new ArrayList<>();

        //1.- Get statistic properties
        result = dataObtaining.getPropertiesList(classes);


        //2.- Get semantic properties from every class
        //Every property is inserted in the "semanticProp" list only once.
        for(String _class:classes){
            Set<CountObject> obtainedSemanticProps = manager.getSemanticProperties(_class);
            for(CountObject semanticProp: obtainedSemanticProps){
                //Check if not already in list
                boolean found = false;
                Iterator<CountObject> iterator = semanticProps.iterator();
                while(!found && iterator.hasNext()){
                    CountObject current = iterator.next();
                    if (current.get_id().equalsIgnoreCase(semanticProp.get_id())) {
                        found = true;
                    }
                }
                if(!found){
                    semanticProps.add(semanticProp);
                }
            }
        }


        /*
         * Mixes semantic and statistical property list. If a property is not in statistic properties list,
         * the semantic property is included in the result list with the count.
         */
        for (CountObject semanticProp : semanticProps) {

            //Check if semantic property is in statistic property list
            boolean found = false;
            Iterator<CountObject> iterator = result.iterator();
            while (!found && iterator.hasNext()) {
                CountObject current = iterator.next();
                if (current.get_id().equalsIgnoreCase(semanticProp.get_id())) {
                    found = true;
                }
            }

            //Not found in statistical properties -> it is a semantic property
            if(!found) {


                //Get count of uses in the whole KB
                getNumberGlobalInstancesOfProperty.initializeOperation(semanticProp.get_id());
                SimpleCountObject sco = (SimpleCountObject) getNumberGlobalInstancesOfProperty.doOperation();
                semanticProp.setCount(sco.getCount());

                //Get semantic range
                String semanticRange = manager.getSpecificRangeForProperty(semanticProp.get_id());



                //Add range for semantic
                CountObject.RangeForSemantic rangeForSemantic = new CountObject.RangeForSemantic(semanticRange,null);
                semanticProp.setRangeForSemantic(rangeForSemantic);

                //Add the property to the results list, as is not repeated
                result.add(semanticProp);
            }
        }


        operationResult =  result;
        return result;

    }

}




