package infoboxer.backend.ontology;

import es.unizar.semantic.DLQueryEngine;
import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.utils.ConcreteClassException;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class OntologyManager {



    @Value("${ontology.location}")
    String ontologyURL;

    @Value("#{'${ontology.allowedClasses}'.split(',')}")
    private List<String> allowedClasses;


    Pattern[] allowedPatterns;
    public OWLOntologyManager manager;
    public OWLDataFactory factory;
    public OWLOntology ontology;
    public OWLReasoner reasoner;
    public DLQueryEngine queryEngine;
    public HashMap<String,String> cache;


    public BidirectionalShortFormProvider bidiShortFormProvider;

    //Constructor
    public OntologyManager() {


        //Ontology location -> from file
        //startOntologyManager();
    }

    @PostConstruct
    public void compilePatterns() {
        //Compile patterns
        allowedPatterns = new Pattern[allowedClasses.size()];
        int index = 0;
        for(String regex:allowedClasses){
            if(regex.charAt(0)!='<'){
                regex = '<' + regex;

            }
            if(regex.charAt(regex.length()-1)!='>'){
                regex = regex + '>';
            }

            Pattern p = Pattern.compile(regex);
            allowedPatterns[index] = p;
            index++;
        }

    }


    //Inicia el ontology.OntologyManager
    @PostConstruct
    public void startOntologyManager() {

        manager = OWLManager.createOWLOntologyManager();
        factory = OWLManager.getOWLDataFactory();
        cache = new HashMap<String,String>();

        System.out.println("Starting Ontology Manager...");
        try {
            System.out.println("Loading " + ontologyURL + "...");


            //Carga la ontología
            ontology = manager.loadOntologyFromOntologyDocument(new File(
                    ontologyURL));

            //Crea el razonador
            reasoner = createOWLReasoner(ontology);

            //Motor de consultas
            queryEngine = createDLQueryEngine(reasoner);

            Set<OWLOntology> importsClosure = ontology.getImportsClosure();

            // Create a bidirectional short form provider to do the actual
            // mapping.
            // It will generate names using the input
            // short form provider.
            bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(
                    manager, importsClosure, queryEngine.getShortFormProvider());

        } catch (OWLOntologyCreationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Creates a HermiT OWLReasoner with the given ontology.
     *
     * @return OWLReasoner The reasoner created
     */
    public OWLReasoner createOWLReasoner(OWLOntology ontology)
            throws IllegalArgumentException {
        //return new Reasoner(ontology);
        return new Reasoner.ReasonerFactory().createNonBufferingReasoner(ontology);
    }

    /**
     * Creates a query engine to process DL queries.
     *
     * @return DLQueryEngine The engine to process DL queries
     */
    public DLQueryEngine createDLQueryEngine(OWLReasoner reasoner)
            throws IllegalArgumentException {
        if (reasoner == null) {
            throw new IllegalArgumentException("OWLReasoner is null");
        }
        return new DLQueryEngine(reasoner, new SimpleShortFormProvider());
    }


    public void addAxiomsOntology(Set<OWLAxiom> axioms) {
        manager.addAxioms(ontology, axioms);
    }



    /**
     * Given a class name, returns a set containing its subclasses.
     * It is has no subclasses, the Set contains "owl:nothing".
     * The class names can contain "<" and ">" characters, they are removed before querying the ontology.
     * Direct specifies if only direct childs "true" or all childs "false"
     */
    public Set<String> getSubClasses(String clase, boolean direct){
        clase = clase.replace("<","");
        clase = clase.replace(">","");

        OWLClass person = this.ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(clase));
        // org.semanticweb.owlapi.reasoner.NodeSet<OWLClass> d = this.reasoner.getSubClasses(person, direct);


        Set<OWLClass> lista = this.reasoner.getSubClasses(person, direct).getFlattened();

        Set<String> result = new HashSet<String>();
        for(OWLClass c:lista){
            result.add(c.toString());
            //System.out.println(c.toString() + " - ");
        }
        return result;


    }


    /**
     * Gets super classes for a class according to te ontology, direct or indirect.
     * It filters classes according to the configuration.
     */
    public Set<String> getSupperClasses(String clase){
        clase = clase.replace("<","");
        clase = clase.replace(">","");

        OWLClass person = this.ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(clase));
        // org.semanticweb.owlapi.reasoner.NodeSet<OWLClass> d = this.reasoner.getSubClasses(person, direct);


        Set<OWLClass> lista = this.reasoner.getSuperClasses(person, false).getFlattened();



        Set<String> result = new HashSet<String>();
        for(OWLClass c:lista){

            boolean classAllowed = false;
            for(Pattern p:allowedPatterns){
                if(p.matcher(c.toString()).matches()){
                    classAllowed = true;
                    break;
                }
            }
            if(classAllowed){
                result.add(c.toString());
            }



        }

        System.out.println("Super classes of " + clase + ":");
        for(String a: result){
            System.out.println(a);
        }
        return result;
    }



    /**
     * Returns the semantic properties for a given class and its superclasses.
     * If available, with each property, a label and a comment in the current in the current language is retrieved.
     */
    public  Set<CountObject> getSemanticProperties(String clase){



        Set<CountObject> foundProperties = new HashSet<CountObject>();

        Set<String> classes = getSupperClasses(clase);
        classes.add(clase); //Classes has the class and its superclasses




        //Extract all properties (data and object properties) from ontology
        Set<OWLDataProperty> dataProperties = this.ontology.getDataPropertiesInSignature();
        Set<OWLObjectProperty> objectProperties = this.ontology.getObjectPropertiesInSignature();

        OWLDataFactory df = OWLManager.getOWLDataFactory();

        //For every data property
        for(OWLDataProperty prop: dataProperties){




            NodeSet<OWLClass> domainsNodes = reasoner.getDataPropertyDomains(prop,true);
            for(Node<OWLClass> node: domainsNodes){

                //For every node, get its domains
                Set<OWLClass> domains =  node.getEntities();

                //For every domain
                for(OWLClass domain: domains){

                    //Check if given class of one of their superclasses is in the domain
                    if(classes.contains(domain.toString())){
                        CountObject co = new CountObject();
                        //Add to found properties. Label and comment could be null.
                        co.set_id(prop.toString());
                        co.setSemantic(true);
                        foundProperties.add(co);
                    }

                }


            }
        }

        //For every object property
        for(OWLObjectProperty prop: objectProperties){
            //System.out.println("-------Para " + prop.toString());
            //Obtain its domains node




            NodeSet<OWLClass> domainsNodes = reasoner.getObjectPropertyDomains(prop,true);
            for(Node<OWLClass> node: domainsNodes){

                //For every node, get its domains
                Set<OWLClass> domains =  node.getEntities();

                //For every domain
                for(OWLClass domain: domains){


                    //Check if given class of one of their superclasses is in the domain
                    if(classes.contains(domain.toString())){
                        CountObject co = new CountObject();
                        //Add to found properties. Label and comment could be null.
                        co.set_id(prop.toString());
                        co.setSemantic(true);
                        foundProperties.add(co);
                        foundProperties.add(co);
                    }

                }


            }
        }

        return foundProperties;


    }


    public  Set<String> getProperties2(String clase){

        Set<String> foundProperties = new HashSet<String>();

        Set<String> classes = new HashSet<String>();
        classes.add(clase); //Classes has the class and its superclasses




        //Extract all properties (data and object properties) from ontology
        Set<OWLDataProperty> dataProperties = this.ontology.getDataPropertiesInSignature();
        Set<OWLObjectProperty> objectProperties = this.ontology.getObjectPropertiesInSignature();


        //For every data property
        for(OWLDataProperty prop: dataProperties){
            //System.out.println("-------Para " + prop.toString());
            //Obtain its domains node
            NodeSet<OWLClass> domainsNodes = reasoner.getDataPropertyDomains(prop,true);
            for(Node<OWLClass> node: domainsNodes){

                //For every node, get its domains
                Set<OWLClass> domains =  node.getEntities();

                //For every domain
                for(OWLClass domain: domains){

                    //Check if given class of one of their superclasses is in the domain
                    if(classes.contains(domain.toString())){
                        foundProperties.add(prop.toString());
                    }

                }


            }
        }

        //For every data property
        for(OWLObjectProperty prop: objectProperties){
            //System.out.println("-------Para " + prop.toString());
            //Obtain its domains node
            NodeSet<OWLClass> domainsNodes = reasoner.getObjectPropertyDomains(prop,true);
            for(Node<OWLClass> node: domainsNodes){

                //For every node, get its domains
                Set<OWLClass> domains =  node.getEntities();

                //For every domain
                for(OWLClass domain: domains){

                    //Check if given class of one of their superclasses is in the domain
                    if(classes.contains(domain.toString())){
                        foundProperties.add(prop.toString());
                    }

                }


            }
        }

        return foundProperties;


    }

    public int getDepth(String clase){
        clase = clase.replace("<","");
        clase = clase.replace(">","");

        OWLClass person = this.ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(clase));
        org.semanticweb.owlapi.reasoner.NodeSet<OWLClass> d = this.reasoner.getSuperClasses(person,false);

        return this.reasoner.getSuperClasses(person, false).getFlattened().size();

    }


    public boolean isFatherOf(String father, String child){

        father = father.replace("<","");
        father = father.replace(">","");

        Set<String> childClasses = getSubClasses(father,false);
        return childClasses.contains(child);
    }




    HashMap<String,List<String>> concreteClassCache = new HashMap<>();
    /**
     * Given a list of class names, it returns a list with the most concrete classes.
     * Also, if several most concrete classes are the same (following owl:sameAs and some reasoning) then only
     * one is left and the others are removed.
     * It maintains a cache on memory.
     */
    public List<String> getConcreteClass(String[] classes) throws ConcreteClassException {



        //Generate key for cache
        String cacheKey = "";
        for(String _class:classes){
            cacheKey+=_class;
        }

        List<String> cachedList = concreteClassCache.getOrDefault(cacheKey,null);
        if(cachedList!=null){
            return cachedList;
        }
        else{

            List<String> classesList = new ArrayList<>(Arrays.asList(classes));

            //We will be removing items of this list as we detect one class is parent of another class of the list.
            List<String> finalList = new ArrayList<>(Arrays.asList(classes));


            for(Iterator<String> iteratorToCheck = classesList.iterator();iteratorToCheck.hasNext();){
                //Element to check
                String currentClass = iteratorToCheck.next();


                Iterator<String> iterator = classesList.iterator();
                while(iterator.hasNext()){
                    //Check if the current class is father of any of the other classes.
                    //If so, remove it from the list
                    String currentPosibleChild = iterator.next();
                    if(!currentPosibleChild.equalsIgnoreCase(currentClass) && isFatherOf(currentClass,currentPosibleChild) ){
                        //System.out.println(currentClass + " contains " + currentPosibleChild);
                        //currentClass is father of currentPosiblechild -> remove currentClass as it is not the most specific
                        finalList.remove(currentClass);
                        break;
                    }
                }
            }



            //Now, we remove entries that are the same (based on owl:sameAs relations). Only we leave the first one
            for(int i = 0; i < finalList.size(); i++){


                String currentClass = finalList.get(i);
                //System.out.println("Checking " + currentClass);

                for(int j = i +1; j < finalList.size(); j++){

                    String currentPosibleSameClass = finalList.get(j);



                    if(!currentPosibleSameClass.equalsIgnoreCase(currentClass) && isSame(currentClass,currentPosibleSameClass)){
                        finalList.remove(j);
                    }

                }

            }


            concreteClassCache.put(cacheKey,finalList);

            return finalList;
        }


    }


    /**
     * Returns rdfs:comment for a property, obtained from ontology file, for a language.
     * If it doesn't exist, returns null.
     */
    public String getRdfsComment(String property, String language){

        property = property.replace("<","");
        property = property.replace(">","");

        try{

            OWLOntology o =this.ontology;
            OWLDataFactory df = OWLManager.getOWLDataFactory();

            //Class of interest
            OWLClass cls = df.getOWLClass(IRI.create(property));

            // Get the annotations on the class that use the comment property (rdfs:comment)
            for (OWLAnnotation annotation : cls.getAnnotations(o, df.getRDFSComment())) {

                if (annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral val = (OWLLiteral) annotation.getValue();
                    // look for portuguese labels - can be skipped
                    if(val.hasLang(language)){
                        return val.getLiteral();
                    }

                }
            }
            return null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * Returns rdfs:label for a property, obtained from ontology file, for a language.
     * If it doesn't exist, returns null.
     */
    public String getRdfsLabel(String property, String language){

        property = property.replace("<","");
        property = property.replace(">","");

        try{

            OWLOntology o =this.ontology;
            OWLDataFactory df = OWLManager.getOWLDataFactory();

            //Class of interest
            OWLClass cls = df.getOWLClass(IRI.create(property));

            // Get the annotations on the class that use the comment property (rdfs:comment)
            for (OWLAnnotation annotation : cls.getAnnotations(o, df.getRDFSLabel())) {

                if (annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral val = (OWLLiteral) annotation.getValue();
                    // look for portuguese labels - can be skipped
                    if(val.hasLang(language)){
                        String lit = val.getLiteral();
                        return lit.substring(0, 1).toUpperCase() + lit.substring(1);
                    }

                }
            }
            return null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }

    }




    /**
     * Returns true if class1 is same as class2, because of owl:sameAs relations.
     * (Gets the classes that are sameAss class1 and checks if class2 is inside it.
     */
    public boolean isSame(String class1, String class2){
        class1 = class1.replace("<","");
        class1 = class1.replace(">","");

        class2 = class2.replace("<","");
        class2 = class2.replace(">","");


        List<String> equivalentClasses  = equivalentClasses(class1);
        //System.out.println("Is " + class1 + " same as " + class2 + "? " + equivalentClasses.contains(class2));

        return equivalentClasses.contains(class2);


    }


    private HashMap<String,List<String>> equivalentClassesCache = new HashMap<>();

    /**
     * Returns a list of equivalent classes (included itself) to the class 'classToCheck'.
     * Uses 'sameAs' relationships iterating over the results.
     * Maintains a memory cache
     */
    public List<String> equivalentClasses(String classToProcess){

        List<String> cachedResult = equivalentClassesCache.getOrDefault(classToProcess,null);
        if(cachedResult!=null){
            return cachedResult;
        }
        else{
            List<String> equivalent = new ArrayList<>();
            equivalent.add(classToProcess);

            ListIterator<String> iterator = equivalent.listIterator();
            while(iterator.hasNext()){

                String currentClass = iterator.next();

                currentClass = currentClass.replace("<","");
                currentClass = currentClass.replace(">","");

                //Get equivalent classes
                OWLClass data = this.ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(currentClass));
                Set<OWLClassExpression> equivalentClasses = data.getEquivalentClasses(this.ontology);

                //System.out.println("Checking for " + currentClass);
                for(OWLClassExpression classToCheck: equivalentClasses ){

                    String classString = classToCheck.toString().replace("<","").replace(">","");

                    //System.out.println("Adding " + classString);
                    if(!equivalent.contains(classString)){
                        iterator.add(classString);
                        iterator.previous();

                    }
                }

            }
            equivalentClassesCache.put(classToProcess,equivalent);

            return equivalent;
        }


    }


    /**
     * Gets from the ontology the direct range for the given property.
     * If no range is present, returns owl#Thing.
     * Else, returns the most specific direct range.
     */


    public String getSpecificRangeForProperty(String property){

        final  String  default_range= "<http://www.w3.org/2002/07/owl#Thing>";

        try{

            OWLOntology o =this.ontology;

            //Obtain object for given property
            OWLObjectProperty queriedProperty= null;
            Set<OWLObjectProperty> props = o.getObjectPropertiesInSignature();
            for(OWLObjectProperty p: props){
                if(p.toString().equals(property)){
                    queriedProperty = p;
                }
            }


            //If property is found in ontology, process ranges
            if(queriedProperty!=null){

                ArrayList<String> ranges = new ArrayList<>();
                NodeSet<OWLClass> set = this.reasoner.getObjectPropertyRanges(queriedProperty,true);
                for(OWLClass c: set.getFlattened()){
                    //Aceptar solo rangos de dbPedia, tipos básicos y thing
                    if(c.toString().contains("dbpedia.org") || c.toString().contains("XMLSchema") || c.toString().contains("owl#Thing")){
                        ranges.add(c.toString());
                    }

                }

                if(ranges.size()>0){
                    //Ranges found, return most concrete
                    String[] rangesArray = ranges.toArray(new String[ranges.size()]);
                    return getConcreteClass(rangesArray).get(0); //Only the first range found

                }
                else{
                    //No ranges found, return default
                    return default_range;
                }

            }
            else{
                //Property is not found, return default value -> Thing
                return default_range;
            }



        }catch(Exception ex){
            ex.printStackTrace();
            return default_range;
        }
    }




    public void prueba() {
        List<String> props = new ArrayList<String>();
        int totalCount = 0;
        int withoutDomain = 0;
        int withoutRange = 0;
        int withoutDomainNorRange = 0;
        int withDomainAndRange = 0;

        System.out.println(ontology.getSignature());
        Set<OWLObjectProperty> a = ontology.getObjectPropertiesInSignature();
        for (OWLObjectProperty b : a) {
            if (!props.contains(b.getSimplified().toString())) { //No procesada la propiedad ya
                totalCount++;

                boolean hasDomains = false;
                boolean hasRanges = false;
                Set<OWLClassExpression> domains = b.getDomains(ontology);
                Set<OWLClassExpression> ranges = b.getRanges(ontology);
                if (domains.size() >= 1 || (domains.size() == 1 && !domains.toArray()[0].toString().toLowerCase().contains("owl:thing"))) {
                    hasDomains = true;
                }
                if (ranges.size() >= 1 || (ranges.size() == 1 && !ranges.toArray()[0].toString().toLowerCase().contains("owl:thing"))) {
                    hasRanges = true;
                }

                if (!hasDomains && hasRanges) {
                    withoutDomain++;
                }
                else if (!hasRanges && hasDomains) {
                    withoutRange++;
                }
                else if (!hasDomains && !hasRanges) {
                    withoutDomainNorRange++;
                }
                else if(hasDomains && hasRanges){
                    withDomainAndRange++;
                }
                props.add(b.getSimplified().toString());


                //
                //if(domains.size() >1 || (domains.size()==1 && domains.toArray()[0].toString()("")))

            }

            Set<OWLDataProperty> c = ontology.getDataPropertiesInSignature();
            for (OWLDataProperty d : c) {
                if (!props.contains(d.toString())) { //No procesada la propiedad ya
                    totalCount++;

                    boolean hasDomains = false;
                    boolean hasRanges = false;
                    Set<OWLClassExpression> domains = d.getDomains(ontology);
                    Set<OWLDataRange> ranges = d.getRanges(ontology);
                    if (domains.size() >= 1 || (domains.size() == 1 && !domains.toArray()[0].toString().toLowerCase().contains("owl:thing"))) {
                        hasDomains = true;
                    }
                    if (ranges.size() >= 1 || (ranges.size() == 1 && !ranges.toArray()[0].toString().toLowerCase().contains("owl:thing"))) {
                        hasRanges = true;
                    }

                    if (!hasDomains && hasRanges) {
                        withoutDomain++;
                    }
                    else if (!hasRanges && hasDomains) {
                        withoutRange++;
                    }
                    else if (!hasDomains && !hasRanges) {
                        withoutDomainNorRange++;
                    }
                    else if(hasDomains && hasRanges){
                        withDomainAndRange++;
                    }
                    props.add(d.toString());


                    //
                    //if(domains.size() >1 || (domains.size()==1 && domains.toArray()[0].toString()("")))

                }



            }

        }

        System.out.println("Total properties: " + totalCount);
        System.out.println("Properties without domain: " + withoutDomain + " (" + (double)withoutDomain*100/(double)totalCount + " %)");
        System.out.println("Properties without range: " + withoutRange + " (" + (double)withoutRange*100/(double)totalCount + " %)");
        System.out.println("Properties without domain nor range: " + withoutDomainNorRange + " (" + (double)withoutDomainNorRange*100/(double)totalCount + " %)");
        System.out.println("Properties with domain and range: " + withDomainAndRange + " (" + (double)withDomainAndRange*100/(double)totalCount + " %)");
    }
    public String notSpecific(String file){

        try{
            Scanner s = new Scanner(new File(file));
            //Skip three first lines
            for(int i = 0; i < 3; i++){
                s.nextLine();
            }

            while(s.hasNextLine()){
                String l = s.nextLine();
                String split[] = l.split("\t");
                if(split.length>1){
                    String one = split[0];
                    String two = split[1];
                    Integer count = Integer.parseInt(split[2]);

                    //Check if one or two are disjoint
                    boolean family = false;
                    family = family | isFatherOf(one,two);
                    family = family | isFatherOf(two,one);
                    family = family | isSame(one,two);



                }


            }


        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        return "a";
    }


    public static void main(String[] args){

        OntologyManager m = new OntologyManager();
        m.ontologyURL = "/home/ismaro3/Downloads/";
        m.startOntologyManager();


        m.prueba();
       /* Set<String> a = m.getProperties2("<http://dbpedia.org/ontology/Person>");
        for(String b:a){
            System.out.println(b);
        }*/

        // boolean b = m.isSame("<http://dbpedia.org/ontology/Person>","<http://xmlns.com/foaf/0.1/Person>");
        //System.out.println(b);
        // m.notSpecific("/home/ismaro3/a.txt");

        // m.generate("/home/ismaro3/classes.txt");

        /*Long timeInit = System.currentTimeMillis();
        System.out.println(m.isSame("<http://dbpedia.org/ontology/AdministrativeRegion>","<http://dbpedia.org/ontology/Region>"));
        Long timeEnd = System.currentTimeMillis();
        System.out.println("Took " + (timeEnd - timeInit) + "ms");
        */



        /*Set<String> l  =m.isFatherOf("<http://dbpedia.org/ontology/Writer>","<http://dbpedia.org/ontology/Writer>");
        for(String s:l){
            System.out.println(s);
        }*/
        System.out.println("-----");
       /* Set<String> n  =m.getSupperClasses("<http://dbpedia.org/ontology/AdministrativeRegion>",false,"");
        for(String s:n){
            System.out.println(s);
        }*/
        //System.out.println(m.getSpecifiRangeForProperty("<http://dbpedia.org/ontology/adfafd>"));
        //m.getRangeForProperty("<http://dbpedia.org/ontology/deathPlace>");


       /*Set<String> ss = m.getSupperClassesForPerson(clase);
        for(String s:ss){

                System.out.println(s);

        }*/


       /* String[] misClases = new String[]{
                "<http://dbpedia.org/ontology/Agent>","<http://dbpedia.org/ontology/Organisation>","<http://dbpedia.org/ontology/SoccerClub>",
                "<http://dbpedia.org/ontology/SportsTeam>","<http://schema.org/Organization>","<http://schema.org/SportsTeam>",
                "<http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Agent>", "<http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#SocialPerson>",
                "<http://www.w3.org/2002/07/owl#Thing>","<http://www.wikidata.org/entity/Q43229>","<http://dbpedia.org/ontology/Artist>",
        "<http://dbpedia.org/ontology/AdministrativeRegion>","<http://dbpedia.org/ontology/Region>"};



       try{
           List<String> specific = m.getConcreteClass(misClases);
           for(String s:specific){
             System.out.println(s);
           }
           specific = m.getConcreteClass(misClases);
           for(String s:specific){
               System.out.println(s);
           }

       }
       catch(Exception ex){
           ex.printStackTrace();
       }*/










    }


}

