package infoboxer.backend;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;


@SpringBootApplication
public class Application {

    @Autowired
    private static WebApplicationContext context;


    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);

        System.out.println("Infoboxer started! :)");
        System.out.println("ENCODING: " + System.getProperty("file.encoding"));

        //doSimulation(ctx);
    }


    public static void doSimulation(ApplicationContext ctx) {

        try{
            Simulator sim = new Simulator(ctx);

           /* GetPropertiesList getPropertiesList = (GetPropertiesList) ctx.getBean("getPropertiesList");
           // getPropertiesList.initializeOperation("<http://dbpedia.org/ontology/Person>","en",true);
            List<CountObject> co = ( List<CountObject>) getPropertiesList.doOperation();
            Collections.sort(co);
            List<CountObject> semantic = new ArrayList<CountObject>();
            for(CountObject c: co){
                String id = c.get_id().replace("<http://dbpedia.org/ontology/","dbo:").replace("<http://xmlns.com/foaf/0.1/","foaf:").replace("<http://purl.org/dc/elements/1.1/","purl:").replace(">","");

                if(!c.isSemantic()){
                       System.out.println(id + "\t" + c.isSemantic());
                }
                else{
                    semantic.add(c);

                }

            }
            for(CountObject c: semantic){
                String id = c.get_id().replace("<http://dbpedia.org/ontology/","dbo:").replace("<http://xmlns.com/foaf/0.1/","foaf:").replace("<http://purl.org/dc/elements/1.1/","purl:").replace(">","");

                System.out.println(id + "\t" + c.isSemantic() + " - ");
            }*/
            sim.monocategorySimulation("<http://dbpedia.org/ontology/Actor>");
            //sim.monocategorySimulation("<http://dbpedia.org/ontology/Actor>");
            //sim.monocategorySimulation("<http://dbpedia.org/ontology/Bodybuilder>");
            //sim.monocategorySimulation("<http://dbpedia.org/ontology/SoccerPlayer>");

           /* ArrayList<String> lista = new ArrayList<String>();

            lista.add("<http://dbpedia.org/ontology/Governor>");
            lista.add("<http://dbpedia.org/ontology/Actor>");
            lista.add("<http://dbpedia.org/ontology/Bodybuilder>");
            sim.multicategorySimulation(lista);*/


           /*ArrayList<String> lista = new ArrayList<String>();

            lista.add("<http://dbpedia.org/ontology/Governor>");
            lista.add("<http://dbpedia.org/ontology/Actor>");
            lista.add("<http://dbpedia.org/ontology/Bodybuilder>");
            sim.multicategoryData(lista);*/

            /*lista.clear();
            lista.add("<http://dbpedia.org/ontology/Governor>");
            lista.add("<http://dbpedia.org/ontology/Bodybuilder>");
            sim.multicategoryData(lista);

            lista.clear();
            lista.add("<http://dbpedia.org/ontology/Actor>");
            lista.add("<http://dbpedia.org/ontology/Bodybuilder>");
            sim.multicategoryData(lista);

            lista.clear();
            lista.add("<http://dbpedia.org/ontology/SoccerPlayer>");
            lista.add("<http://dbpedia.org/ontology/BasketballPlayer>");
            sim.multicategoryData(lista);*/


        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
}
