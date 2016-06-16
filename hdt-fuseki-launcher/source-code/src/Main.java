import org.rdfhdt.hdt.fuseki.FusekiHDTCmd;
import org.rdfhdt.hdt.fuseki.HDTGenerateIndex;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        String[] argu = args;

        //Find the "--hdt" argument value
        int i;
        for(i = 0; i < argu.length; i++){
            if(argu[i].equalsIgnoreCase("--hdt")){
                break;
            }
        }

        if(i>= argu.length -1){
            System.err.println("Please, specify a file name");
        }
        else{
            String hdtFile = argu[i+1];
            File hdtF = new File(hdtFile);
            File hdtIndex = new File(hdtFile + ".index");
            if(hdtF.exists() && !hdtIndex.exists()){
                //If index does not exists, generate it
                System.out.println("Generating index...");
                HDTGenerateIndex.main(new String[]{hdtFile});
            }
            //Launch server
            FusekiHDTCmd.main(argu);


        }


    }
}
