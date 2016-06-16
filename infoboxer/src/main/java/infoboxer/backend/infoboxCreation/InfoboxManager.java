package infoboxer.backend.infoboxCreation;


import infoboxer.backend.common.utils.StringManipulations;

import java.io.File;
import java.util.Scanner;

/**
 * Created by ismaro3 on 14/11/15.
 */
public class InfoboxManager {


    public static final String TEMPLATES_DIR = "templates";




    public static String convert(String category, String data){




        category = StringManipulations.normalizeCamelCase(category);
        category = category.replace(" ","");

        System.out.println("Converting for category " + category);
        System.out.println("Given data: " + data);


        File templateDir = new File(TEMPLATES_DIR);
        String filePath = TEMPLATES_DIR + "/" + category + ".txt";

        if(!templateDir.exists()){ //Create directory if not exists
            templateDir.mkdir();
        }

        File templateFile = new File(filePath);

        if(!templateFile.exists()){
            System.out.println("No existe el fichero " + templateFile.getAbsolutePath());
            //No template available
            return null;
        }

        //File exists, load mapping file
        MappingFileManager mfh = new MappingFileManager(filePath);

        String infoboxCode = "{{" + mfh.getTemplateName() +"\n";
        Scanner dataScanner = new Scanner(data);
        boolean end = false;
        while(dataScanner.hasNext() && !end){

            String line = dataScanner.nextLine();
            if(line.contains("{{Infobox")){
                //Start
            }
            else if(line.equals("}}")){
                //End
                end = true;
            }
            else if(line.contains("|")){
                //Content

                //Extract key
                int equalsIndex = line.indexOf("=");
                String inputKey = line.substring(1,equalsIndex).trim().toLowerCase();

                //Extract value
                String inputValue = line.substring(equalsIndex+1,line.length()).trim();

                //Uncomment this if want property mapping with files.
                //If commented, only templateName is used.
                //String outputKey = mfh.getByKey(inputKey);
                String outputKey = inputKey;
                if(outputKey!=null){
                    //Exists in mapping
                    infoboxCode += "| " + outputKey + " = " + inputValue + "\n";
                }

            }
            else{
                //Nothing
            }
        }
        infoboxCode += "}}";



        return infoboxCode;
    }


    public static void main(String[] args){

        String text = "{{Infobox Soccer Player\n" +
                "| title = \n" +
                "| asdf = Juanito\n" +
                "| País = [[ República Popular China ]]\n" +
                "| Ciudad = Madrid\n" +
                "| Presidente = [[ España ]]\n" +
                "| Años Año De Inicio = [[ 1939 ]]\n" +
                "}}";

        //String text = "key:Nombre   value:Ismael\nkey:Ciudad    value:Huesca\nkey:Presidente value:Rajoy";
        String category = "<http://dbpedia.org/ontology/SoccerPlayer>";

       String newCode =  InfoboxManager.convert(category,text);
        System.out.println(newCode);

    }
}
