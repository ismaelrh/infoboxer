package infoboxer.backend.web;


import infoboxer.backend.operations.GetDataForInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

/**
 * Created by ismaro3 on 9/02/16.
 */
@RestController
@RequestMapping("/fromWikipedia")
public class WikipediaController {


    @Autowired
    private WebApplicationContext context;




    @RequestMapping(value = "", method = RequestMethod.GET, produces="application/json")
    public Object getPropertiesForInstance(
            @RequestParam(value="wikipediaUrl",required=true) String wikipediaUrl
    ){

        //1.- Extract language from Wikipedia URL
        String language = extractLanguageFromWikipediaURL(wikipediaUrl);


        //2.- Extract instance from Wikipedia URL
        String instance = extractInstanceFromWikipediaURL(wikipediaUrl);


        //3.- Extract dbpedia URI from language and instance
        String dbpediaURI = formDbpediaURI(language,instance);

        GetDataForInstance operation = (GetDataForInstance) context.getBean("getDataForInstance");
        operation.initializeOperation(dbpediaURI);

        return operation.doOperation();


    }


    private static String extractLanguageFromWikipediaURL(String url){

        int protocolEnd = url.indexOf("://") + 2;
        int languageEnd = url.indexOf("wikipedia.org");

        if(protocolEnd<0 || languageEnd<0 || protocolEnd +1 >= languageEnd){
            //No language
            return "en";
        }
        else{
            return url.substring(protocolEnd +1,languageEnd-1);
        }


    }

    private static String extractInstanceFromWikipediaURL(String url){

      int lastIndex = url.lastIndexOf("/");
        return url.substring(lastIndex+1,url.length());


    }

    private static String formDbpediaURI(String language, String instance){

        if(language.equalsIgnoreCase("en")){
            language = "";
        }
        else{
            language = language + ".";
        }
        return "<http://" +  language + "dbpedia.org/resource/" + instance + ">";

    }

    public static void main(String[] args){

        System.out.println(formDbpediaURI("es",extractInstanceFromWikipediaURL("http://es.wikipedia.org/wiki/Regents_of_the_University_of_California_v._Bakke")));
    }
}
