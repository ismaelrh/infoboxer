package infoboxer.backend.translator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.tools.ant.DirectoryScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

/**
 * Used to manage property labels in different languages.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LabelManager {

    @Value("${translator.apiURL}")
    private String TRANSLATE_URL;


    @Value("${translator.apiKEY}")
    private String API_KEY;


    public final static String TRANSLATIONS_DIR = "translations/";
    public final static String MANUAL_TRANSLATIONS_DIR = "translations-manual/";
    public final static String PROPERTIES_TRANSLATION_FILE = "properties-translation-";
    public final static String CLASSES_TRANSLATION_FILE = "classes-translation-";

    public final static int PROPERTIES_MODE = 0;
    public final static int CLASSES_MODE = 1;

    private Gson gson;


    private HashMap<String,TranslationHolder> manual = new HashMap<>();
    private HashMap<String,TranslationHolder> cache =  new HashMap<>();



    private String FILE = "";

    public LabelManager(){

    }

    public void initLabelManager(int mode){

        gson = new Gson();

        //Create directories
        File manual_translations_dir = new File(MANUAL_TRANSLATIONS_DIR);
        if(!manual_translations_dir.exists()) {
            manual_translations_dir.mkdir();
        }
        File translations_dir = new File(TRANSLATIONS_DIR);
        if(!translations_dir.exists()){
            translations_dir.mkdir();
        }

        String regex = "";
        if(mode==PROPERTIES_MODE){
            FILE = PROPERTIES_TRANSLATION_FILE;
            regex = "properties-translation-*.txt";

        }
        else{
            FILE = CLASSES_TRANSLATION_FILE;
            regex = "classes-translation-*.txt";
        }


        //Search in translation dir
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{regex});
        scanner.setBasedir(translations_dir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        for(String file: files){
            int lastIndex = file.lastIndexOf("-");
            int lastPoint = file.lastIndexOf(".");
            String language = file.substring(lastIndex+1,lastPoint);


            TranslationHolder th2 = new TranslationHolder(language,TRANSLATIONS_DIR + FILE + language + ".txt");
            th2.loadFromFile();
            cache.put(language,th2);


        }

        //Search in translation dir
        scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{regex});
        scanner.setBasedir(manual_translations_dir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        files = scanner.getIncludedFiles();

        for(String file: files){
            int lastIndex = file.lastIndexOf("-");
            int lastPoint = file.lastIndexOf(".");
            String language = file.substring(lastIndex+1,lastPoint);


            TranslationHolder th2 = new TranslationHolder(language,MANUAL_TRANSLATIONS_DIR + FILE + language + ".txt");
            th2.loadFromFile();
            cache.put(language,th2);


        }




    }


    /** Returns translation for one key, in the specified language.
     * It loads from manual list, cache, or internet
     */
    public String getTranslation(String language, String key) throws IOException{

        //Search on manual
        String resultManual = searchOnManual(language, key);
        if(resultManual!=null){
            // System.out.println("[LABEL-MAN] Key '" + key + "' loaded from manual list.");
            return resultManual;
        }
        else{
            //Search on cache
            String resultCached = searchOnCached(language,key);
            if(resultCached!=null){
                //System.out.println("[LABEL-MAN] Key '" + key + "' loaded from cache.");
                return resultCached;
            }

            else{
                //Need to retrieve it from internet

                String internetResult = searchOnInternet(language,key);
                //Save on cache

                if(internetResult!=null){
                    //  System.out.println("[LABEL-MAN] Key '" + key + "' loaded from Internet.");
                    return internetResult;
                }
                else{
                    //System.out.println("[LABEL-MAN] Error retrieving Key '" + key + "' from Internet.");
                    return "ERROR";
                }


            }
        }

    }




    /**
     * Searchs on manual list.
     * Returns null if not found.
     */
    public String searchOnManual(String language, String key){
        TranslationHolder current = manual.getOrDefault(language,null);
        if(current==null) {
            current = new TranslationHolder(language,MANUAL_TRANSLATIONS_DIR + FILE + language + ".txt");
            manual.put(language,current);

        }
        return current.getTranslation(key);

    }

    /**
     * Searchs on cache.
     * Returns null if not found.
     */
    public String searchOnCached(String language, String key){
        TranslationHolder current = cache.getOrDefault(language,null);
        if(current==null) {
            current = new TranslationHolder(language,TRANSLATIONS_DIR + FILE + language + ".txt");
            cache.put(language,current);

        };
        return current.getTranslation(key);
    }


    /**
     * Searchs on internet for a single value.
     * Returns null if error.
     * TODO : Adapt for an array of keys. Improve JSON analysis.
     */
    public String searchOnInternet(String language, String key) throws IOException{
        TranslationHolder current = cache.getOrDefault(language,null);
        if(current==null) {
            current = new TranslationHolder(language,TRANSLATIONS_DIR + PROPERTIES_TRANSLATION_FILE + language + ".txt");
            cache.put(language,current);

        };



        String url_path = TRANSLATE_URL + "?key=" + API_KEY + "&lang=en-"+language + "&text=" + URLEncoder.encode(key, "UTF-8");

        URL url = new URL(url_path);
        //Timeout: 10s (5 connection, 5 read)
        URLConnection con = url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        Scanner s = new Scanner(con.getInputStream());

        String result = s.nextLine();
        result = result.replace("[","");
        result = result.replace("]",""); //Only one value, remove array

        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> resultMap = gson.fromJson(result, type);

        String code = resultMap.get("code");
        String text = resultMap.get("text");
        if(code.equals("200")){
            //OK
            current.setTranslation(key,text);
            return text;
        }
        else{
            return null;
        }


    }




    public static void main(String[] args){
        // LabelManager lm = new LabelManager();
        //String result = lm.getTranslation("es","I acted on my behalf");
        //String result2 = lm.getTranslation("es","I acted on his behalf");
        //String result3 = lm.getTranslation("es","I acted on her behalf");
        //String result4 = lm.getTranslation("es","I acted on their behalf");
        //System.out.println(result);
    }


}
