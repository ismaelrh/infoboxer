package infoboxer.backend.common.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ismaro3 on 1/09/15.
 */
public class ServerUtils {


    public static String decodeURLParameter(String param){
        try{
            if(param!=null){
                return java.net.URLDecoder.decode(param, "UTF-8");
            }
            else{
                return null;
            }

        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }


    /**
     * Given a URL-encoded string representing a comma-separated URI list
     * returns an arraylist containing such strings, but decoded.
     */
    public static ArrayList<String> decodeURLParameterList(String list){
        list = decodeURLParameter(list);

        //Process classList parameter (mandatory)
        String[] classes = list.split(",");
        ArrayList<String> classesList = new ArrayList<String>();
        for (String _class : classes) {
            classesList.add(_class);
            //System.out.println(decodeURLParameter(_class));
        }
        return classesList;

    }


    public static void removeDuplicates(List<String> original){
        Set<String> hs = new HashSet<String>();
        hs.addAll(original);
        original.clear();
        original.addAll(hs);
    }

    public static void mkdir(String dir){


        File theDir = new File(dir);

            // if the directory does not exist, create it
        if (!theDir.exists()) {
            //System.out.println("creating directory: " + directoryName);
            boolean result = false;

            try{
                theDir.mkdir();
                result = true;
            }
            catch(SecurityException se){
                //handle it
                se.printStackTrace();
            }
        }

    }

    public static void printTitle(String message){
        String adorno = "";
        for(int i = 0; i < message.length();i++){
            adorno+="*";
        }
        System.out.println(adorno);
        System.out.println(message);
        System.out.println(adorno);
    }
}
