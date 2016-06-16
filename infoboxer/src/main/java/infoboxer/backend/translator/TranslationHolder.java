package infoboxer.backend.translator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Manages a translations list.
 */
public class TranslationHolder {


    private String language;
    private String filePath;
    private String auxiliarPath = "toTranslate/aux.txt";
    private HashMap<String,String> holder;

    public TranslationHolder(String language,String filePath){
        this.language = language;
        this.filePath = filePath;
        this.holder = new HashMap<String,String>();
    }

    /** Returns language */
    public String getLanguage(){
        return this.language;
    }


    public int getSize(){
        return this.holder.size();
    }
    /**
     * Returns the translation for the key.
     * If not present, returns null.
     */
    public String getTranslation(String key){
        return holder.get(key);
    }

    /**
     * Adds a translation for a key.
     */
    public void setTranslation(String key, String value){
        holder.put(key,value);
        this.appendToFile(key,value);
    }

    /**
     * Loads the saved translations from a file. Creates it if it doesn't exists.
     */
    public boolean loadFromFile(){

        try{
            File f = new File(this.filePath);
            if(f.exists()) {
                Scanner scanner = new Scanner(new File(this.filePath));
                this.holder = new HashMap<String, String>();
                while (scanner.hasNextLine()) {

                    String line = scanner.nextLine();

                    if (line.length() > 0) {
                        //Valid line
                        int keyIndex = line.indexOf("key:") + 4;
                        int valueIndex = line.indexOf("value:") + 6;
                        if(keyIndex > 0 && (valueIndex-7)>keyIndex){
                            String key = line.substring(keyIndex, valueIndex - 7);
                            String value = line.substring(valueIndex, line.length());

                            //Save on memory
                            this.holder.put(key, value);
                        }
                    }
                }

            }
            else{
                f.createNewFile();
            }

            return true;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return false;
        }


    }


    /** Adds a single translation to the file, appending it */
    public boolean appendToFile(String key, String value){
        System.out.println("Appending " + key + "-" + value + " to file " + this.filePath);
        String text = "key:" + key + " value:" + value + "\n" ;
        File file = new File(this.filePath);
        File aux = new File(this.auxiliarPath);


        try {

            if(!file.exists()){
                file.createNewFile();
            }
            //Write it to cache
            PrintWriter writer = new PrintWriter(new FileOutputStream(file, true));
            writer.append(text);
            writer.close();

        }catch (IOException e) {
            //exception handling left as an exercise for the reader
            e.printStackTrace();
            return false;
        }


        return true;
    }


    public static void main(String[] args){


    }




}
