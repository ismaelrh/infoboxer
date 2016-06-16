package infoboxer.backend.web;

import infoboxer.backend.common.dto.InfoboxGenerationRequest;
import infoboxer.backend.infoboxCreation.InfoboxManager;
import infoboxer.backend.statsDatabase.StatsDatabaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/infobox")
public class InfoboxController {


    @Autowired
    StatsDatabaseManager statsDatabaseManager;


    /**
     * Used to generate a correct infobox.
     *
     * Request:
     * {
     * sessionId
     * timestamp
     * category
     * pageName
     * givenInfobox
     * }
     *
     */
    @RequestMapping(value = "generate", method = RequestMethod.POST , produces ="application/json")
    public Object generateInfobox(@RequestBody InfoboxGenerationRequest req){

        //Get MANDATORY parameters
        String category = req.getCategory();
        String pageName = req.getPageName();
        String givenInfobox = req.getGivenInfobox();

        //Get OPTIONAL parameters, used for session control
        Long time = req.getTimestamp();
        Integer sessionId = req.getSessionId();

        if (category.length() > 0 && givenInfobox.length() > 0 ){
            //Correct

            //Do the transformation
            String resultInfobox = InfoboxManager.convert(category,givenInfobox);

            if(resultInfobox==null){
                resultInfobox = "ERROR";
            }

            //If session is activated, insert infobox into database.
            if(time!=null && time > 0 && sessionId !=null && sessionId > -1){

                statsDatabaseManager.saveInfobox(true,sessionId, time, category, pageName, givenInfobox,resultInfobox);
            }

            resultInfobox = resultInfobox.replace("\n","\\n"); //Replace to show properly on frontend

            //Always return data, even if an error in DB
            return "{\"resultInfobox\":\"" + resultInfobox + "\"}";

        }
        else{
            //Incorrect petition
            return "{\"status\":\"ERROR\",\"message\":\"Please provide at least sessionId, timestamp, categories and rdfCode.\"}";
        }


    }



}
