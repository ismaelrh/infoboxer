package infoboxer.backend.infoboxCreation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by ismaro3 on 14/11/15.
 */
public class MappingFileManager {

    private File file;
    private HashMap<String,String> mapping;
    private String templateName;

    /*
    Pre: mappingFile has to exist and be formatted as a mapping file.
     */
    public MappingFileManager(String mappingFile){

        file = new File(mappingFile);
        mapping = new HashMap<String,String>();

        loadFromFile();

    }


    public String getByKey(String key){
        key = key.toLowerCase();
        return mapping.get(key);
    }


    public String getTemplateName(){
        return this.templateName;
    }
    /**
     * Fills the handler from the file.
     */
    private void loadFromFile(){
        try{
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                //Check if valid
                if(lineIsValid(line)){

                    String key = extractKey(line).toLowerCase();
                    String infoboxKey = extractInfoboxKey(line);

                    if(key!=null && infoboxKey != null
                            && key.length()>0 && infoboxKey.length()>0){
                        this.mapping.put(key,infoboxKey);
                    }
                }
                else{
                    //Check if it is templateName
                    if(line.contains("templateName:")){
                        this.templateName = line.substring("templateName:".length()).trim();
                    }
                }

            }
            scanner.close();
        }
        catch(FileNotFoundException ex){
            ex.printStackTrace();
        }


    }


    /**
     * Returns true only if the line is a valid mapping line.
     */
    private boolean lineIsValid(String line){
        return line.contains("key:") && line.contains("infobox_key:");
    }


    /**
     * Extracts the key from the line.
     * Returns null if error.
     */
    private String extractKey(String line){

        try {
            String sub = line.substring(4);
            int nextIndex = sub.indexOf("infobox_key:");
            String key = sub.substring(0, nextIndex - 1);
            key = key.trim();
            return key;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }


    }


    /**
     * Extracts the infoboxKey from the line.
     * Returns null if error.
     */
    private String extractInfoboxKey(String line){

        try {
            int index = line.indexOf("infobox_key:");
            int start = index + "infobox_key:".length();
            line = line.substring(start, line.length());
            return line.trim();
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}
