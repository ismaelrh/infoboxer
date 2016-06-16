package infoboxer.backend.web;

import infoboxer.backend.common.dto.RdfRequest;
import infoboxer.backend.common.dto.SaveInfoboxRequest;
import infoboxer.backend.common.dto.StatusMessage;
import infoboxer.backend.common.dto.WikimediaTimeRequest;
import infoboxer.backend.statsDatabase.StatsDatabaseManager;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by ismaro3 on 10/02/16.
 */
@RestController
@RequestMapping("/stats")
public class StatsController {


    @Autowired
    StatsDatabaseManager statsDatabaseManager;


    /**
     * Se encarga de procesar una acción de registro ordinario.
     * Devolverá {"status":"OK o ERROR", "message": "Mensaje de error si procede}
     */
    @RequestMapping(value = "newAction", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage newAction(
            @RequestParam(value = "sessionId", required = true) int sessionId,
            @RequestParam(value = "timestamp", required = true) long time,
            @RequestParam(value = "subject", required = true) String subject,
            @RequestParam(value = "action", required = true) String action,
            @RequestParam(value = "value", required = false, defaultValue = "") String value
    ) {

        //Process parameters
        subject = ServerUtils.decodeURLParameter(subject);
        action = ServerUtils.decodeURLParameter(action);
        value = ServerUtils.decodeURLParameter(value);


        //If correct, insert into database
        if (sessionId > -2 && time > -1 && subject.length() > 0 && action.length() > 0) {


            int result = -1;
            try {
                result = statsDatabaseManager.newAction(sessionId, time, subject, action, value);

                if (result >= 0) {
                    //Return OK Code
                    return new StatusMessage("OK");
                } else {
                    return new StatusMessage("ERROR", "Error while inserting record in database");
                }
            } catch (Exception ex) {
                return new StatusMessage("ERROR", "Error while inserting record in database. Exception: " + ex.getMessage());
            }


        } else {
            //Incorrect query
            return new StatusMessage("ERROR", "Please provide correct sessionId, timestamp, subject and action fields.");
        }


    }

    /**
     * Se encarga de procesar una acción de creacion de nueva sesion
     * Devolverá {"status":"OK", "sessionId": idDeSesion} en caso de exito, o
     * {"status":"ERROR","message":MensajeDeError} en caso de fallo.
     */
    @RequestMapping(value = "newSession", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage newSession(
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "timestamp", required = true) long time
    ) {

        username = ServerUtils.decodeURLParameter(username);

        //Correct
        if (time > -1 && username.length() > 0) {


            //Insert in database
            int result = statsDatabaseManager.newSession(username, time);

            if (result >= 0) {
                //Return OK Code
                StatusMessage message = new StatusMessage("OK");
                message.setSessionId(result);
                return message;
            } else {
                return new StatusMessage("ERROR", "Error while inserting new session into database");
            }

        } else {
            //Incorrect petition
            return new StatusMessage("ERROR", "Please provide at least username and timestamp.");
        }

    }

    /**
     * Se encarga de procesar una acción de creacion de nueva sesion
     * Devolverá {"status":"OK", "sessionId": idDeSesion} en caso de exito, o
     * {"status":"ERROR","message":MensajeDeError} en caso de fallo.
     */
    @RequestMapping(value = "closeSession", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage closeSession(
            @RequestParam(value = "sessionId", required = true) int sessionId,
            @RequestParam(value = "timestamp", required = true) long time
    ) {


        if (time > -1 && sessionId > -1) {
            //Correct

            //Insert in database
            int result = statsDatabaseManager.closeSession(sessionId, time);

            if (result >= 0) {
                //Return OK Code
                return new StatusMessage("OK");
            } else {
                return new StatusMessage("ERROR", "Error  while closing session");
            }

        } else {
            //Incorrect petition
            return new StatusMessage("ERROR", "Please provide correct sessionId and timestamp");
        }


    }


    /**
     * Request:
     * {
     * sessionId
     * timestamp
     * categories
     * pageName
     * rdfCode
     * }
     */
    @RequestMapping(value = "saveRdf", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage saveRdf(@RequestBody RdfRequest rdf) {


        long time = rdf.getTimestamp();             //MANDATORY
        int sessionId = rdf.getSessionId();         //MANDATORY
        String categories = rdf.getCategories();    //MANDATORY
        String rdfCode = rdf.getRdfCode();          //MANDATORY
        String pageName = rdf.getPageName();        //OPTIONAL


        if (time > -1 && sessionId > -1 && categories.length() > 0 && rdfCode.length() > 0) {
            //Correct

            //Insert in database
            int result = statsDatabaseManager.saveRdf(sessionId, time, categories, pageName, rdfCode);

            if (result >= 0) {
                //Return OK Code
                return new StatusMessage("OK");
            } else {
                return new StatusMessage("ERROR", "Error while storing infobox");
            }

        } else {
            //Incorrect petition
            return new StatusMessage("ERROR", "Please provide at least sessionId, timestamp, categories and rdfCode.");
        }


    }

    /**
     * Request:
     * {
     * sessionId
     * timestamp
     * categories
     * pageName
     * infoboxCode
     * }
     */
    @RequestMapping(value = "saveInfobox", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage saveInfobox(@RequestBody SaveInfoboxRequest ir) {


        long time = ir.getTimestamp();             //MANDATORY
        int sessionId = ir.getSessionId();         //MANDATORY
        String categories = ir.getCategories();    //MANDATORY
        String infoboxCode = ir.getInfoboxCode();          //MANDATORY
        String pageName = ir.getPageName();        //OPTIONAL




        if (time > -1 && sessionId > -1 && infoboxCode.length() > 0 && categories.length() > 0) {
            //Correct

            //Insert in database
            int result = statsDatabaseManager.saveInfobox(false,sessionId, time, categories, pageName, infoboxCode, null);

            if (result >= 0) {
                //Return OK Code
                return new StatusMessage("OK");
            } else {
                return new StatusMessage("ERROR", "Error while storing infobox");
            }

        } else {
            //Incorrect petition
            return new StatusMessage("ERROR", "Please provide at least sessionId, timestamp, categories and rdfCode.");
        }


    }


    @RequestMapping(value = "saveSurvey", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage saveSurvey(
            @RequestParam(value = "sessionId", required = true) int sessionId,
            @RequestParam(value = "timestamp", required = true) long time,
            @RequestParam(value = "response1", required = true) int response1,
            @RequestParam(value = "response2", required = true) int response2,
            @RequestParam(value = "response3", required = true) int response3,
            @RequestParam(value = "freeText", required = false, defaultValue = "") String freeText
    ) {

        freeText = ServerUtils.decodeURLParameter(freeText);


        if (time > -1 && sessionId > -1 && response1 > -1 && response2 > -1 && response3 > -1) {
            //Correct

            //Insert in database
            int result = statsDatabaseManager.saveSurvey(sessionId, time, response1, response2, response3, freeText);

            if (result >= 0) {
                //Return OK Code
                return new StatusMessage("OK");
            } else {
                return new StatusMessage("ERROR", "Error while storing survey");
            }

        } else {
            //Incorrect petition
            return new StatusMessage("ERROR", "Please provide at least sessionId, timestamp, response1, response2 & response3");
        }

    }


    /**
     * Request:
     * {
     * username,
     * infobox,
     * time
     * }
     */
    @RequestMapping(value = "wikimediaTime", method = RequestMethod.POST, produces = "application/json")
    public StatusMessage insertWikimediaTime(@RequestBody WikimediaTimeRequest req) {


        //First we insert a dummy register to "activate" the connection with the DB
        try{
            statsDatabaseManager.insertWikimediaTime("dummyRegister", "dummyRegister", 0);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }


        String username = req.username;
        String infobox = req.infobox;
        int time = req.time;


        int result = -1;
        try {
            result = statsDatabaseManager.insertWikimediaTime(username, infobox, time);

            if (result >= 0) {
                //Return OK Code
                return new StatusMessage("OK");
            } else {
                return new StatusMessage("ERROR", "Error while inserting record in database");
            }
        } catch (Exception ex) {
            return new StatusMessage("ERROR", "Error while inserting record in database. Exception: " + ex.getMessage());
        }


    }


}
