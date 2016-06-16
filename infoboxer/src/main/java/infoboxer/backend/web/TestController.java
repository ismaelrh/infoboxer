package infoboxer.backend.web;

import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.operations.suggestions.SuggestionsDatabaseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ismaro3 on 9/02/16.
 */
@RestController
@RequestMapping("/test")
public class TestController {


    @Autowired
    private WebApplicationContext context;


    @Autowired
    SuggestionsDatabaseHandler suggestionsDatabaseHandler;




    @RequestMapping(value = "", method = RequestMethod.GET , produces ="application/json")
    public  String getSuggestions()
    {


        List<CountObject> list = new ArrayList<CountObject>();

        for(int i = 0; i < 20000; i++){
            list.add(new CountObject("sugerencia" + i,i));
        }




        try{
            //String res = suggestionsDatabaseHandler.createTableForSuggestions("cat","prop","range");
            //System.out.println("CREATED TABLE " + res);
            long init = System.currentTimeMillis();
           // suggestionsDatabaseHandler.clearTable("catproprange");
            //boolean exists = suggestionsDatabaseHandler.insertRecords(list,"catproprange");
            //suggestionsDatabaseHandler.createTableForSuggestions("tablewidthindex");
            long end = System.currentTimeMillis();
            return "Time: " + (end - init)/1000.0 + " s.";
        }
       catch(Exception ex){
           ex.printStackTrace();
           return ex.getMessage();
       }



    }
}
