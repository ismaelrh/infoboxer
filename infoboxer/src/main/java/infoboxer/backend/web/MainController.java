package infoboxer.backend.web;

import com.google.gson.Gson;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.operations.*;
import infoboxer.backend.common.utils.ServerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ismaro3 on 9/02/16.
 */
@RestController
@RequestMapping("/infoboxer")
public class MainController {


    @Autowired
    private WebApplicationContext context;



    /**
     * Returns the number of instances of a list of categories 'classList'.
     * @param classList is a list of classes in URI format separated by commas. MANDATORY
     */
    @RequestMapping(value = "instanceCount", method = RequestMethod.GET , produces ="application/json")
    public synchronized Object instanceCount(
            @RequestParam(value="classList", required=true) String classList
    ) {


        //Process classList parameter (mandatory)
        ArrayList<String> _classList = ServerUtils.decodeURLParameterList(classList);

        //Asked count of instances of a set of classes
        GetInstanceCount getInstanceCount = (GetInstanceCount) context.getBean("getInstanceCount");
        getInstanceCount.initializeOperation(_classList);
        return getInstanceCount.doOperation();


    }





    /**
     * Returns the list of available classes, children of a certain 'superClass' super-class.
     * @param superClass the super class. MANDATORY.
     * @param label used to filter in real-time. OPTIONAL.
     */
    @RequestMapping(value = "classList", method = RequestMethod.GET , produces ="application/json")
    public synchronized String getClasses(
            @RequestParam(value="superClass", required=true) String superClass,
            @RequestParam(value="label", required=false, defaultValue = "") String label,
            HttpServletResponse response
    )
    {

        Gson gson = new Gson();

        superClass = ServerUtils.decodeURLParameter(superClass);
        label = ServerUtils.decodeURLParameter(label);
        label = label.replaceAll(" ",""); //From "American Player" to "AmericanPlayer".


        GetAvailableClasses getAvailableClasses = (GetAvailableClasses) context.getBean("getAvailableClasses");
        getAvailableClasses.initializeOperation(superClass,"es");
        List<CountObject> listaResultados = (List<CountObject>) getAvailableClasses.doOperation();


        //If no label, only 10 results
        if((label.equalsIgnoreCase("top10") || label.length() == 0) && listaResultados!=null){
            List<CountObject> result = new ArrayList<CountObject>();
            for(int i = 0; i < listaResultados.size() && i < 50; i++){
                result.add(listaResultados.get(i));
            }
            return gson.toJson(result);
        }
        else {
            //Filter by label
            System.out.println("Filtering by " + label.toLowerCase());
            List<CountObject> result = new ArrayList<CountObject>();
            for (CountObject co : listaResultados) {
                String name = co.get_id().toLowerCase();
                label = label.toLowerCase();
                if (name.contains(label)) {
                    result.add(co);
                }
            }
            return gson.toJson(result);
        }

    }


    /**
     * Returns the number of uses of a property 'property' in a list of categories or classes 'classList'.
     * @param classList is a list of classes in URI format separated by commas. MANDATORY.
     * @param property is a property in URI format. MANDATORY.
     *                 NOT USED...
     */
    /*@RequestMapping(value = "getPropUses", method = RequestMethod.GET , produces ="application/json")
    public synchronized Object getNumberUsesOfProperty(
            @RequestParam(value="classList", required=true) String classList,
            @RequestParam(value="property", required=true) String property
    )
    {

        //Process classList parameter (mandatory)
        ArrayList<String> _classList = ServerUtils.decodeURLParameterList(classList);
        //Process property parameter
        property = ServerUtils.decodeURLParameter(property);

        GetNumberUsesOfProperty getNumberUsesOfProperty = (GetNumberUsesOfProperty) context.getBean("getNumberUsesOfProperty");
        getNumberUsesOfProperty.initializeOperation(_classList,property);
        return getNumberUsesOfProperty.doOperation();


    }*/


    /**
     * Returns a list that contains, for each property used in categories 'classList', the list of its ranges (4 at most)
     * with the count of how many times it has been used (not instantiated) in that classes.
     * @param classList a list of classes in URI format, separated by commas. MANDATORY.
     * @param language a language. Default is "en". OPTIONAL.
     */
    @RequestMapping(value = "rangesAndUses", method = RequestMethod.GET , produces ="application/json")
    public synchronized Object getRangesAndUses(
            @RequestParam(value="classList", required=true) String classList,
            @RequestParam(value="language", required=false, defaultValue = "en") String language
    )
    {

        //Process classList parameter (mandatory)
        ArrayList<String> _classList = ServerUtils.decodeURLParameterList(classList);

        try{

            GetRangesAndUsesWithTexts getRangesAndUses = (GetRangesAndUsesWithTexts) context.getBean("getRangesAndUsesWithTexts");
            getRangesAndUses.initializeOperation(_classList,language);
            return getRangesAndUses.doOperation();

        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * -> "propertyList" returns a list of properties for the given class and, for each one, the
     * count of instances that manifest it. Called when only "className" is supplied. If "semantic"
     * is passed, then also properties from the ontology are returned, with count 0.
     *
     * @param classList a list of classes in URI format separated by commas.
     * @param semantic true if semantic properties to be retrieved. OPTIONAL
     * @param language "en", "es"... default is "en". OPTIONAL
     */
    @RequestMapping(value = "propertyList", method = RequestMethod.GET , produces ="application/json")
    public synchronized Object getPropertiesList(
            @RequestParam(value="classList", required=true) String classList,
            @RequestParam(value="semantic", required=false, defaultValue = "false") boolean semantic,
            @RequestParam(value="language", required=false, defaultValue = "en") String language
    )
    {

        //Process classList parameter (mandatory)
        ArrayList<String> _classList = ServerUtils.decodeURLParameterList(classList);


        try{

            GetPropertiesListWithTexts getPropertiesList = (GetPropertiesListWithTexts) context.getBean("getPropertiesListWithTexts");
            getPropertiesList.initializeOperation(_classList,language,semantic);
            return getPropertiesList.doOperation();

        }
        catch(Exception ex){
            ex.printStackTrace();
            return "Error: " + ex.getMessage();
        }


    }











}


