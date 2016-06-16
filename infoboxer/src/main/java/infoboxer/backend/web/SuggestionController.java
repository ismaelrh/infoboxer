package infoboxer.backend.web;

import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.operations.suggestions.GetSuggestions;
import infoboxer.backend.operations.suggestions.GetSuggestionsForSemantic;
import infoboxer.backend.operations.suggestions.SuggestionsDatabaseHandler;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

/**
 * Created by ismaro3 on 23/05/16.
 */
@RestController
@RequestMapping("/suggestions")
public class SuggestionController {

    @Autowired
    private WebApplicationContext context;


    @Autowired
    SuggestionsDatabaseHandler suggestionsDatabaseHandler;
    /**
     * Returns a list of values for the given range, property and class, ordered by popularity, and filtered by "label".
     * If property and classList are not provided, returns a list of values for the given range, regardless of property
     * and classList.
     * @param classListString is a list of classes.
     * @param property is the property in URI format.
     * @param rangeType is the range in URI format. MANDATORY.
     * @param label label to filter. Can be empty or not provided. OPTIONAL.
     */
    @RequestMapping(value = "", method = RequestMethod.GET , produces ="application/json")
    public Object getSuggestions(
            @RequestParam(value="classList", required=false) String classListString,
            @RequestParam(value="property", required=false) String property,
            @RequestParam(value="rangeType", required=true) String rangeType,
            @RequestParam(value="label", required=false, defaultValue = "") String label

    )
    {


        //Process classList parameter (mandatory)
        List<String> classList = null;
        if(classListString!=null){
            classList = ServerUtils.decodeURLParameterList(classListString);
        }
        if(property!=null){
            property = ServerUtils.decodeURLParameter(property);
        }

        rangeType = ServerUtils.decodeURLParameter(rangeType);
        label = ServerUtils.decodeURLParameter(label).toLowerCase();


        if(classList!=null && classList.size()==0){

            return "error: please fill the parameters";

        }
        else{

            List<CountObject> listaResultados;
            if(classListString==null && property == null){
                //Only range has been provided -> suggestions for semantic
                GetSuggestionsForSemantic getSuggestionsForSemantic = (GetSuggestionsForSemantic) context.getBean("getSuggestionsForSemantic");
                getSuggestionsForSemantic.initializeOperation(rangeType,label);
                listaResultados = (List<CountObject>) getSuggestionsForSemantic.getSuggestions();
            }
            else{
                //classList, property and range have been provided -> suggestions for all
                GetSuggestions getSuggestions = (GetSuggestions) context.getBean("getSuggestions");
                getSuggestions.initializeOperation(classList,property,rangeType,label);
                listaResultados = (List<CountObject>) getSuggestions.getSuggestions();
            }



            return listaResultados;



        }

    }
}
