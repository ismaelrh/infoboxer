package infoboxer.backend;

import infoboxer.backend.common.utils.ServerUtils;
import infoboxer.backend.dataObtaining.DataObtaining;
import infoboxer.backend.common.dto.*;
import infoboxer.backend.operations.*;
import infoboxer.backend.common.utils.StringManipulations;
import infoboxer.backend.common.utils.Timer;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ismaro3 on 12/04/16.
 */
public class Simulator {

    private ApplicationContext ctx;

    private DataObtaining dataObtaining;

    public Simulator(ApplicationContext ctx) {
        //First we trigger the ontologyManager
        dataObtaining = new DataObtaining();
        dataObtaining.launchOntologyManager();
        this.ctx = ctx;
    }


    public void clearCacheFiles(){
        try{
            FileUtils.deleteDirectory(new File("cacheFiles"));
        }
        catch(IOException ex){
            ex.printStackTrace();
        }

    }

    public void monocategorySimulation(String category) throws Exception{
        List<String> categories = new ArrayList<String>();
        categories.add(category);
        processSimulation(categories);

    }

    public void multicategorySimulation(List<String> categories) throws Exception{
        processSimulation(categories);
    }


    private void processSimulation(List<String> categories) throws Exception {

        clearCacheFiles();

        if(categories.size()==1){
            ServerUtils.printTitle("Starting simulation for mono-category " + StringManipulations.normalizeCamelCase(categories.get(0)));
        }
        else{
            String cats = "";
            for (String _class : categories) {
                cats += StringManipulations.normalizeCamelCase(_class) + ",";
            }
            ServerUtils.printTitle("Starting simulation for multi-categories: " + cats);
        }


        Timer timer = new Timer();
        Timer timerGlobal = new Timer();



        //1.- OP1 Obtain count
        timerGlobal.start("Total time");
        timer.start("OP1: Instance count");
        GetInstanceCount getInstanceCount = (GetInstanceCount) ctx.getBean("getInstanceCount");
        getInstanceCount.initializeOperation(categories);
        SimpleCountObject sco = (SimpleCountObject) getInstanceCount.doOperation();
        int count = sco.getCount();
        timer.stop();

        //2.- OP2 Obtain properties list
        timer.start("OP2: Properties list");
        GetPropertiesListWithTexts getPropertiesList = (GetPropertiesListWithTexts) ctx.getBean("getPropertiesListWithText");
        getPropertiesList.initializeOperation(categories, "en", false);
        List<CountObject> propertiesList = (List<CountObject>)getPropertiesList.doOperation();
        timer.stop();

        //3.- OP4 Obtain range uses
        timer.start("OP4: Ranges and  uses");
        GetRangesAndUsesWithTexts getRangesAndUses = (GetRangesAndUsesWithTexts) ctx.getBean("getRangesAndUsesWithTexts");
        getRangesAndUses.initializeOperation(categories,"en");
        List<KeyValuePair<List<CountObject>>> rangesAndUses = ( List<KeyValuePair<List<CountObject>>>) getRangesAndUses.doOperation();
        timer.stop();


        double total = timerGlobal.stop();
        ServerUtils.printTitle("Finished. Processed " + count + " instances in " + total + "s. (" + (double) count / (double) total + " instances/sec.)");


    }


}
