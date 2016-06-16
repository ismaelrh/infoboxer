package infoboxer.backend.statsDatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.Calendar;

/**
 * Singleton to handle statsDatabase.
 * Declaring it a Service makes it a singleton for Spring.
 */
@Service
public class StatsDatabaseManager {


    @Value("${stats.db.host}")
    private   String HOST;

    @Value("${stats.db.port}")
    private   String PORT;

    @Value("${stats.db.username}")
    private   String USERNAME;

    @Value("${stats.db.password}")
    private   String PASSWORD;

    @Value("${stats.db.database}")
    private   String DATABASE;

    private Connection connection;


    public StatsDatabaseManager(){

        try {

            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[STATS] Error acquiring JDBC driver.");
            e.printStackTrace();
            return;
        }
        catch(Exception ex){
            System.err.println("[STATS] Error while loading stats.db information from application.properties file");
            ex.printStackTrace();
        }

        System.out.println("[STATS] MySQL JDBC Driver Registered!");

    }

    @PostConstruct
    /*
    Connects to DB. Called when bean has been created and all injected values are accesible.
     */
    private void connectToDB(){

        System.out.println("conectin");

        try {

            DriverManager.setLoginTimeout(0);
            System.out.println("conectin2");
            connection = DriverManager
                    .getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE, USERNAME, PASSWORD);
            System.out.println("conectin3");
            connection.setAutoCommit(false);
            System.out.println("conectin4");

        } catch (SQLException e) {
            System.err.println("[STATS] Connection to database Failed! Check output console");
            e.printStackTrace();
            return;
        }

        if (connection != null) {

            System.out.println("[STATS] Successfully connected to stats database.");
        } else {
            System.err.println("[STATS] Failed to make connection!");
        }
    }


    /**
     * Introduce una accion basica "subject-action-value".
     * El timestamp viene en millis.
     * En caso de error devuelve -1. Si no, devuelve 0.
     */
    public synchronized int newAction(int sessionId,long timestamp,String subject, String action, String value) throws SQLException{


            refreshConnection();
            PreparedStatement insertStatement = connection
                    .prepareStatement("insert into  register(sessionId,timestamp,subject,action,value)VALUES(?,?,?,?,?)");

            Calendar c = Calendar.getInstance();

            insertStatement.setInt(1, sessionId);
            insertStatement.setTimestamp(2, new Timestamp(timestamp));
            insertStatement.setString(3, subject);
            insertStatement.setString(4, action);
            insertStatement.setString(5, value);

            insertStatement.executeUpdate();
            connection.commit();
            return 0;

    }


    /**
     * Introduce un registro de simulador de infobox de wikimedia.
     * La hora es la de recepción de la petición.
     */
    public synchronized int insertWikimediaTime(String username, String infobox, int time) throws SQLException{
        refreshConnection();
        PreparedStatement insertStatement = connection
                .prepareStatement("insert into wikimediaTime(username,date,infobox,time) VALUES(?,?,?,?)");


        Calendar c = Calendar.getInstance();


        insertStatement.setString(1,username);
        insertStatement.setDate(2,new java.sql.Date(c.getTime().getTime()));
        insertStatement.setString(3,infobox);
        insertStatement.setInt(4,time);

        insertStatement.executeUpdate();
        connection.commit();
        return 0;




    }
    /**
     * Inserta un registro de nueva sesión con el username indicado y el timestamp,
     * y devuelve el sessionId asignado.
     *
     * @return el sessionId asignado, o -1 si ha habido un error
     */
    public synchronized int newSession(String username, long timestamp){
        try{


            refreshConnection();

            connection.setAutoCommit(false);

            //En primer lugar se tiene que averiguar qué sessionId toca
            //Se obtiene el maximo

            PreparedStatement maxStatement = connection
                    .prepareStatement("SELECT MAX(sessionID) FROM register");

            int sessionId = 1;
            try {
                ResultSet maxResult = maxStatement.executeQuery();

                maxResult.next(); //Skip header
                int maxSessionId = maxResult.getInt(1);

                 sessionId = maxSessionId + 1;
            }
            catch(NullPointerException ex){
                //No habia nada insertado todavia.
            }

            //Se inserta la tupla de registro
            PreparedStatement insertStatement = connection
                    .prepareStatement("insert into  register(sessionId,timestamp,subject,action,value)VALUES(?,?,?,?,?)");
            insertStatement.setInt(1,sessionId);
            insertStatement.setTimestamp(2, new Timestamp(timestamp));
            insertStatement.setString(3, "SYSTEM INFORMATION");
            insertStatement.setString(4, "SESSION OPENED");
            insertStatement.setString(5,username);

            insertStatement.executeUpdate();

            connection.commit();

            return sessionId;

        }
        catch(Exception ex){
            System.out.println("Error detecting while creating session: " + ex.getMessage());
            ex.printStackTrace();
            return -1;
        }

    }


    /**
     * Guarda el RDF indicado en la base de datos.
     * En primer lugar, guarda un registro de acción "RDF SAVED".
     * Tras ello, inserta en la tabla "rdf" los datos del RDF ligados a ese registro.
     * En caso de error devuelve -1. Si no, devuelve 0.
     */
    public synchronized int  saveRdf(int sessionId, long timestamp, String categories, String pageName,  String rdfCode){
        try{


            refreshConnection();

            connection.setAutoCommit(false);

            //Se inserta la tupla de accion indicando que se ha guardado
            PreparedStatement insertStatement = connection
                    .prepareStatement("insert into  register(sessionId,timestamp,subject,action,value)VALUES(?,?,?,?,?)");
            insertStatement.setInt(1, sessionId);
            Timestamp time = new Timestamp(timestamp);
            insertStatement.setTimestamp(2, time);
            insertStatement.setString(3, "SYSTEM INFORMATION");
            insertStatement.setString(4, "RDF SAVED");
            insertStatement.setString(5,"");
            insertStatement.executeUpdate();

            //Se obtiene el id del registro insertado
            PreparedStatement idStatement = connection
                    .prepareStatement("SELECT MAX(registerId) FROM register WHERE sessionId = ?");
            idStatement.setInt(1, sessionId);
            ResultSet maxResult =  idStatement.executeQuery();
            maxResult.next(); //Skip header
            int registerId= maxResult.getInt(1);


            //Se guarda el infobox con la clave ajena apuntando al registro
            insertStatement = connection.prepareStatement("insert into rdf(registerId,sessionId, categories, pageName,rdfCode) VALUES(?,?,?,?,?)");
            insertStatement.setInt(1,registerId);
            insertStatement.setInt(2, sessionId);
            insertStatement.setString(3, categories);
            insertStatement.setString(4, pageName);
            insertStatement.setString(5, rdfCode);

            insertStatement.executeUpdate();

            connection.commit();

            return 0;

        }
        catch(SQLException ex){

            ex.printStackTrace();
            return -1;
        }

    }



    /**
     * Guarda el Infobox indicado en la base de datos, junto con la transofmracion realizada
     * En primer lugar, guarda un registro de acción "INFOBOX GENERATED AND SAVED" si transformed = true, o
     * "INFOBOX SAVED" si transformed = false (esto significa que no es transformado, solo almacenado).
     * Tras ello, inserta en la tabla "infoboxer" los datos del Infobox ligados a ese registro.
     * En caso de error devuelve -1. Si no, devuelve 0.
     */
    public synchronized int saveInfobox(boolean transformed, int sessionId, long timestamp, String category, String pageName,  String givenInfobox, String resultInfobox){
        try{


            if(resultInfobox==null){
                resultInfobox = "No transformation was made";
            }

            refreshConnection();

            connection.setAutoCommit(false);


            //Se inserta la tupla de accion indicando que se ha guardado
            PreparedStatement insertStatement = connection
                    .prepareStatement("insert into  register(sessionId,timestamp,subject,action,value)VALUES(?,?,?,?,?)");
            insertStatement.setInt(1, sessionId);
            Timestamp time = new Timestamp(timestamp);
            insertStatement.setTimestamp(2, time);
            insertStatement.setString(3, "SYSTEM INFORMATION");
            if(transformed){
                insertStatement.setString(4, "INFOBOX GENERATED AND SAVED");
            }
            else{
                insertStatement.setString(4, "INFOBOX SAVED");
            }

            insertStatement.setString(5,category);
            insertStatement.executeUpdate();

            //Se obtiene el id del registro insertado
            PreparedStatement idStatement = connection
                    .prepareStatement("SELECT MAX(registerId) FROM register WHERE sessionId = ?");
            idStatement.setInt(1, sessionId);
            ResultSet maxResult =  idStatement.executeQuery();
            maxResult.next(); //Skip header
            int registerId= maxResult.getInt(1);


            //Se guarda el infobox con la clave ajena apuntando al registro
            insertStatement = connection.prepareStatement("insert into infobox(registerId,sessionId, category, pageName,givenInfobox,resultInfobox) VALUES(?,?,?,?,?,?)");
            insertStatement.setInt(1,registerId);
            insertStatement.setInt(2, sessionId);
            insertStatement.setString(3, category);
            insertStatement.setString(4, pageName);
            insertStatement.setString(5, givenInfobox);
            insertStatement.setString(6, resultInfobox);

            insertStatement.executeUpdate();

            connection.commit();

            return 0;

        }
        catch(SQLException ex){

            ex.printStackTrace();
            return -1;
        }

    }




    /**
     * Guarda la encuesta en la base de datos.
     * En primer lugar, guarda un registro de acción "SURVEY SAVED".
     * Tras ello, inserta en la tabla "survey" los datos de encuesta ligados a ese registro.
     * En caso de error devuelve -1. Si no, devuelve 0.
     */
    public synchronized int saveSurvey(int sessionId, long timestamp, int response1, int response2, int response3, String freeText){
        try{


            refreshConnection();

            connection.setAutoCommit(false);


            //Se inserta la tupla de accion indicando que se ha guardado
            PreparedStatement insertStatement = connection
                    .prepareStatement("insert into  register(sessionId,timestamp,subject,action,value)VALUES(?,?,?,?,?)");
            insertStatement.setInt(1, sessionId);
            Timestamp time = new Timestamp(timestamp);
            insertStatement.setTimestamp(2, time);
            insertStatement.setString(3, "SYSTEM INFORMATION");
            insertStatement.setString(4, "SURVEY SAVED");
            insertStatement.setString(5,"");
            insertStatement.executeUpdate();

            //Se obtiene el id del registro insertado
            PreparedStatement idStatement = connection
                    .prepareStatement("SELECT MAX(registerId) FROM register WHERE sessionId = ?");
            idStatement.setInt(1, sessionId);
            ResultSet maxResult =  idStatement.executeQuery();
            maxResult.next(); //Skip header
            int registerId= maxResult.getInt(1);


            //Se guarda el infobox con la clave ajena apuntando al registro
            insertStatement = connection.prepareStatement("insert into survey(registerId,sessionId,response1,response2,response3,freeText) VALUES(?,?,?,?,?,?)");
            insertStatement.setInt(1,registerId);
            insertStatement.setInt(2, sessionId);
            insertStatement.setInt(3, response1);
            insertStatement.setInt(4, response2);
            insertStatement.setInt(5, response3);
            insertStatement.setString(6,freeText);

            insertStatement.executeUpdate();

            connection.commit();

            return 0;

        }
        catch(SQLException ex){

            ex.printStackTrace();
            return -1;
        }

    }



    /**
     * Inserta el registro de cerrar sesión.
     * Devuelve 0 si ok, -1 si error.
     */
    public synchronized int closeSession(int sessionId, long timestamp){

        refreshConnection();

        try {
            return newAction(sessionId,timestamp,"SYSTEM INFORMATION","SESSION CLOSED","");
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }


    }

    public static void main(String[] args){

        /*int result = handler.newAction(94,System.currentTimeMillis(),"campo propiedad Name","introducir texto","Alberto Ruíz");
        for(int i = 0; i < 100; i++){
            handler.newAction(94,System.currentTimeMillis(),"campo propiedad Name","introducir texto","Alberto Ruíz");
        }*/


    }


    private  void refreshConnection(){
        try{
            if(connection.isClosed()){
                System.err.println("Connection expired. Reconnecting...");
                connectToDB();
            }
        }
        catch(Exception ex){
            System.err.println("Connection expired. Reconnecting...");
            connectToDB();
        }

    }





}
