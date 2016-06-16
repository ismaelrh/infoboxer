package infoboxer.backend.common.utils;


import infoboxer.backend.common.dto.CountObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
/**
 * Created by ismaro3 on 20/07/15.
 */
@Component
public class StringManipulations {



    @Value("${label.typeDelimiter}")
    private String typeDelimiter;

    @Value("${label.propertyDelimiter}")
    private String propertyDelimiter;

    @Value("${label.uriToLabelMode}")
    private String uriToLabelMode;


    public StringManipulations(){

    }

    public void setDelimiters(String typeDelimiter,String resourceDelimiter){
        this.typeDelimiter = typeDelimiter;
        this.propertyDelimiter = resourceDelimiter;
    }

    public static String normalizeCamelCase(String inputString) {
        String checkForFoaf = inputString;

        inputString = inputString.replace("<", "");
        inputString = inputString.replace(">", "");
        inputString = inputString.replace("_","");
        int len = inputString.length();

        int last = inputString.lastIndexOf("/");
        inputString = inputString.substring(last + 1);
        //inputString = inputString.remove(0, last+1);

        String updated = "";

        for (int idx = 0; idx < inputString.length(); idx++) {
            char currentChar = inputString.charAt(idx);
            int ascii = (int) currentChar;

            if (ascii >= 65 && ascii <= 90) {
                //Found upper case. Add space
                updated = updated + " " + currentChar;
            } else {
                updated = updated + currentChar;
            }

            //Eliminamos espacios de delante y detras
            updated = updated.trim();


        }


        if (checkForFoaf.contains("foaf"))
            //todo: ¿esta bien? El de mongo devuelve con foaf name -> name

        { updated =  updated.toLowerCase().trim(); }

        return updated;
    }



    public  String  extractXMLSchema(String inputURI){

        if(!inputURI.equalsIgnoreCase("XMLSchema#String")){
            inputURI = inputURI.replace("<http://www.w3.org/2001/","");
            inputURI = inputURI.replace(">","");
        }
        //System.out.println(inputURI);
        return inputURI;

    }

    public  String calculateHash(String plaintext) throws Exception{
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(plaintext.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1,digest);
        String hashtext = bigInt.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        while(hashtext.length() < 32 ){
            hashtext = "0"+hashtext;
        }
        return hashtext;
    }

    //Experimental (Should use labels)
    //Transforms a URI Value or value^^type String to label
    //Eg: <http://dbpedia.org/resource/Stoke_City_F.C> to  "Stoke City F.C"
    //Eg: "1997-03-08"^^<http://www.w3.org/2001/XMLSchema#date> to "1997-03-08"
    public  String URItoLabel(String URI){

        if(URI.contains("^^")){
            //value^^type
            int index = URI.indexOf("^^");
            return URI.substring(0,index);
        }
        else if(URI.contains("http://")){
            //URI
            int lastIndex = URI.lastIndexOf("resource/");
            String label  = URI.substring(lastIndex+9,URI.length()-1);
            if(lastIndex < 0){
                lastIndex = URI.lastIndexOf("ontology/");
                label  = URI.substring(lastIndex+9,URI.length()-1);
            }
            if(lastIndex < 0){
                lastIndex = URI.lastIndexOf("/");
                label  = URI.substring(lastIndex+1,URI.length()-1);
            }

            label = label.replace("_"," ");
            label = label.replace(">","");
            label = label.replace("\\u003","");



            return label;
        }
        else{
            //Other
            return URI;
        }
    }





    /**
     * Cleans a suggerence value, removing quotes and @.
     */
    public  void cleanValue(CountObject co){

        String name = co.getLabel();

        if(name.length() > 0 && name.charAt(0) == '\"' && name.charAt(name.length()-1) == '\"'){
            //String or date without @ and with quotes
            //If starts and ends with quotes -> Remove quotes
            co.setLabel(name.substring(1,name.length()-1));
        }
        if( name.contains("@")){
            //String with @ and quotes
            //Remove quotes and text after @ (including it)
            int arroba = name.lastIndexOf("@");
            try{
                co.setLabel(name.substring(1,arroba -1));
            }
            catch(StringIndexOutOfBoundsException ex){
                //Bad formed
                System.err.println("Bad formed value: " + name);
            }

        }


    }

    public  String cleanValue(String name){
        if(name.length() > 0 && name.charAt(0) == '\"' && name.charAt(name.length()-1) == '\"'){
            //String or date without @ and with quotes
            //If starts and ends with quotes -> Remove quotes
            name = name.substring(1,name.length()-1);
        }
        if( name.contains("@")){
            //String with @ and quotes
            //Remove quotes and text after @ (including it)
            int arroba = name.lastIndexOf("@");
            try{
                name = name.substring(1,arroba -1);
            }
            catch(StringIndexOutOfBoundsException ex){
                //Bad formed
                System.err.println("Bad formed value: " + name);
            }

        }
        return name;
    }


    /**
     * Converts a string to Unicode Hex Form: U+XXXX
     * @param input
     * @return
     */
    public  String stringToUnicodeHex(String input){
        return StringEscapeUtils.escapeJava(input).replace("\\u","U+");

    }

    /**
     * Converts a string from Unicode Hex Form: U+XXXX
     * @param input
     * @return
     */
    public  String unicodeHexToString(String input){
       return StringEscapeUtils.unescapeJava(input.replace("U+","\\u"));

    }



    public  String URItoHumanReadable(String URI){
        URI = URI.replace("<", "");
        URI = URI.replace(">","");
        int lastIndexSlash = URI.lastIndexOf(propertyDelimiter);
        if(lastIndexSlash!=-1){
            String name = URI.substring(lastIndexSlash+1,URI.length());


            //Transform to camelCase
            if (uriToLabelMode.equalsIgnoreCase("underscore")) {
                name = LOWER_UNDERSCORE.to(LOWER_CAMEL,name);

            }
            //As camel case

                name = name.substring(0, 1) +
                        name.substring(1).replaceAll(
                                String.format("%s|%s|%s",
                                        "(?<=[A-Z])(?=[A-Z][a-z])",
                                        "(?<=[^A-Z])(?=[A-Z])",
                                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                                ),
                                " "
                        );


           return name.substring(0,1).toUpperCase() + name.substring(1);
        }
        else{
            return URI;
        }
    }

    public  String typeToHumanReadable(String typeURI){
        if(typeURI.contains("XMLSchema#")){
            int index = typeURI.indexOf("XMLSchema#");
            typeURI = typeURI.substring(index + "XMLSchema#".length(),typeURI.length());
            return typeURI.toUpperCase().charAt(0) + typeURI.substring(1);
        }
        else if(typeURI.contains("22-rdf-syntax-ns#langString")){
            return "String";
        }
        else if(typeURI.contains("www.ontologydesignpatterns.org/ont/d0.owl#Location")){
            return "Location";
        }
        else if(typeURI.contains("unknownRDFType")){
            return "Unknown type";
        }
        else{
            return URItoHumanReadable(typeURI);
        }
    }


    public static void main(String[] args){

    }

}
