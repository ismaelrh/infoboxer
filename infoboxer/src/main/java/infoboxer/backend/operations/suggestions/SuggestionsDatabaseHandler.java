package infoboxer.backend.operations.suggestions;

import infoboxer.backend.common.dto.CountObject;
import infoboxer.backend.common.utils.StringManipulations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.*;

/**
 * Singleton to handle suggestions.
 * Declaring it a Service makes it a singleton for Spring.
 */
@Service
public class SuggestionsDatabaseHandler {


    @Value("${suggestions.db.host}")
    private   String HOST;

    @Value("${suggestions.db.port}")
    private   String PORT;

    @Value("${suggestions.db.username}")
    private   String USERNAME;

    @Value("${suggestions.db.password}")
    private   String PASSWORD;

    @Value("${suggestions.db.database}")
    private   String DATABASE;

    @Autowired
    StringManipulations stringManipulations;

    private Connection connection;


    public SuggestionsDatabaseHandler(){

        try {

            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[SUGGESTIONS] Error acquiring JDBC driver.");
            e.printStackTrace();
            return;
        }
        catch(Exception ex){
            System.err.println("[SUGGESTIONS] Error while loading stats.db information from application.properties file");
            ex.printStackTrace();
        }



        System.out.println("[SUGGESTIONS] MySQL JDBC Driver Registered!");

    }

    @PostConstruct
    /*
    Connects to DB. Called when bean has been created and all injected values are accessible.
     */
    private void connectToDB(){


        try {
            DriverManager.setLoginTimeout(5);
            connection = DriverManager
                    .getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE, USERNAME, PASSWORD);
            connection.setAutoCommit(false);


            disableOnlyFullGroupByMode();

        } catch (SQLException e) {
            System.err.println("[SUGGESTIONS] Connection to database Failed! Check output console");
            e.printStackTrace();
            return;
        }

        if (connection != null) {

            System.out.println("[SUGGESTIONS] Successfully connected to suggestions database.");
        } else {
            System.err.println("[SUGGESTIONS] Failed to make connection!");
        }
    }


    private void disableOnlyFullGroupByMode() throws SQLException{
        PreparedStatement statement = null;
        String result = null;


        statement = connection.prepareStatement("SET sql_mode=\'\'");


        statement.executeUpdate();

        statement.close();

        System.out.println("[SUGGESTIONS] Disabled sql_mode=only_full_group_by");

    }



    /**
     * Returns true if table exists in suggestions database with name "name".
     * False otherwise.
     * @return
     * @throws SQLException if error
     */
    public boolean checkIfTableExists(String tableName){

        refreshConnection();

        ResultSet res = null;
        boolean result = false;
        try{
            DatabaseMetaData meta = connection.getMetaData();
            res = meta.getTables(null, null, tableName,
                    new String[] {"TABLE"});


            int count = 0;
            while (res.next()) {
                count++;
            }

            res.close();
            result =  (count!=0) ;
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally{
            try{
                res.close();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;
        }


    }



    /**
     * Creates a table if it doesn't exist, and creates the index of 30 characters on value field.
     * If it already exists or an error has happened, it returns null.
     * @return
     */
    private String createTableForSuggestions(String tableName){

        refreshConnection();

        PreparedStatement insertStatement = null;
        String result = null;
        try{


            //First, create the table
            insertStatement = connection
                    .prepareStatement("CREATE TABLE " + tableName + "(id int NOT NULL PRIMARY KEY AUTO_INCREMENT , label TEXT, uri TEXT, count INT)");


            insertStatement.executeUpdate();

            insertStatement.close();

            //Then, create the index
            insertStatement = connection
                    .prepareStatement("CREATE INDEX value_index ON " + tableName + "(uri(30))");

            insertStatement.executeUpdate();

            connection.commit();

            result = tableName;
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally{
            try{
                insertStatement.close();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;

        }

    }


    /**
     * Inserts records to the given table. Returns true if success, false otherwise.
     * Also, it creates the table if it doesn't exists.
     * If table exists previously, it doesn't insert records.
     * NOTE: Only 20 records at max are inserted at the moment. A thread is created to insert
     * the remaining objects asynchronously.
     * @param list
     * @param table
     * @return
     */
    public synchronized boolean insertRecords(List<CountObject> list, String table){


        refreshConnection();

        PreparedStatement insertStatement = null;
        boolean result = false;

        try {

            if(checkIfTableExists(table)){
                //Table exists -> no need for inserting records, they are inserted already
                System.err.println("Not inserting into suggestions DB because table " + table + " already exists.");
                return true;
            }

            //Create table
            createTableForSuggestions(table);


            //First insert ONLY 20 records
            Iterator<CountObject> iterator = list.iterator();
            int i = 0;
            while(iterator.hasNext() && i < 20) {

                CountObject object = iterator.next();

                insertStatement = connection
                        .prepareStatement("INSERT INTO " + table + "(label,uri,count) VALUES(?,?,?)");

                insertStatement.setString(1,object.getLabel()); //todo: label, not id
                insertStatement.setString(2,object.get_id());
                insertStatement.setInt(3,object.getCount());


                insertStatement.executeUpdate();


                i++;

                iterator.remove();

            }

            //Commit the inserted results
            connection.commit();


            //Asynchronously, insert remaining records
            Runnable runnable = new InsertThread(connection,table,list);
            Thread t = new Thread(runnable);
            t.start();

            result = true;

        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally{
            try{
                if(insertStatement!=null){
                    insertStatement.close();
                }

            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;
        }


    }

    /**
     * Returns CountObject records from table that match the query.
     * They are ordered by count, length of value field and alphabetically.
     * Only 10 results are returned
     */
    public List<CountObject> retrieveRecords(String query,String table,String rangeTable){

        refreshConnection();


        Statement statement = null;
        ResultSet rs = null;
        List<CountObject> result = null;


        try {

            result = new ArrayList<CountObject>();

            //String selectTableSQL = "SELECT * FROM " + table + " WHERE label LIKE \'%" + query + "%\' ORDER BY count DESC, label ASC, LENGTH(label) ASC limit 10 " ;
            String selectTableSQL = "SELECT label, uri, MAX(count) AS count FROM "
                    + " (" +
                    "     (SELECT label,uri,count " +
                    "     FROM " + table +
                    "     WHERE label LIKE \'%" + query + "%\' " +
                    "     ORDER BY count DESC, LENGTH(label) ASC , label ASC limit 10) " +
                    " UNION ALL" +
                    "     (SELECT label,uri,count " +
                    "     FROM " + rangeTable +
                    "     WHERE label LIKE \'%" + query + "%\'" +
                    "     ORDER BY  LENGTH(label) ASC, label ASC limit 10)" +
                    "   ) T1 "
                    + " GROUP BY uri ORDER BY count DESC,  LENGTH(label) ASC, label ASC  limit 10";


            statement = connection.createStatement();


            // execute select SQL statement
            rs = statement.executeQuery(selectTableSQL);


            while (rs.next()) {

                String id = rs.getString("uri");
                String label = rs.getString("label");
                int count = rs.getInt("count");

                CountObject co = new CountObject(id,count);
                co.setLabel(label);
                result.add(co);


            }



        } catch (SQLException e) {

            System.out.println(e.getMessage());

        }
        finally{
            try{
                rs.close();
                statement.close();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;
        }



    }

    /**
     * Returns CountObject records from table of the range, that match the query.
     * They are ordered by count, length of value field and alphabetically.
     * If no query is provided, order only alphabetically.
     * Only 10 results are returned
     */
    public List<CountObject> retrieveRecordsForSemantic(String query,String rangeTable){

        refreshConnection();


        Statement statement = null;
        ResultSet rs = null;
        List<CountObject> result = null;


        try {

            result = new ArrayList<CountObject>();

            //String selectTableSQL = "SELECT * FROM " + table + " WHERE label LIKE \'%" + query + "%\' ORDER BY count DESC, label ASC, LENGTH(label) ASC limit 10 " ;
            String selectTableSQL =
                    "SELECT label, uri, count " +
                            " FROM " + rangeTable +
                            " WHERE label LIKE \'%" + query + "%\' " +
                            " ORDER BY  LENGTH(label) ASC, label ASC limit 10";

            //If no query, order alphabetically
            if(query.length()==0){
                selectTableSQL =
                        "SELECT label, uri, count " +
                                " FROM " + rangeTable +
                                " WHERE label LIKE \'%" + query + "%\' " +
                                " ORDER BY  label ASC limit 10";
            }



            statement = connection.createStatement();


            // execute select SQL statement
            rs = statement.executeQuery(selectTableSQL);


            while (rs.next()) {

                String id = rs.getString("uri");
                String label = rs.getString("label");
                int count = rs.getInt("count");

                CountObject co = new CountObject(id,count);
                co.setLabel(label);
                result.add(co);


            }



        } catch (SQLException e) {

            System.out.println(e.getMessage());

        }
        finally{
            try{
                rs.close();
                statement.close();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;
        }



    }
    /**
     * Drops the table "tableName", returning true if success.
     * Returns false otherwise.
     * @param tableName
     */
    public boolean dropTable(String tableName){

        refreshConnection();

        boolean result = false;
        PreparedStatement insertStatement = null;
        try{

            insertStatement = connection
                    .prepareStatement("DROP TABLE " + tableName);


            System.out.println(insertStatement.toString());

            insertStatement.executeUpdate();

            connection.commit();


            result = true;
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally{
            try{
                insertStatement.close();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;
        }

    }

    /**
     * Deletes all records from the table "tableName", returning true if success, false otherwise.
     */
    public boolean clearTable(String tableName){

        refreshConnection();

        PreparedStatement insertStatement = null;
        boolean result = false;

        try{

            insertStatement  = connection
                    .prepareStatement("DELETE FROM " + tableName);

            System.out.println(insertStatement.toString());

            insertStatement.executeUpdate();

            connection.commit();

            result = true;
        }
        catch(Exception ex){

            ex.printStackTrace();

        }
        finally{
            try{
                insertStatement.close();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return result;
        }
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


    /**
     * Returns a proper range for the table based on category-list,property and range
     * @param categoryList
     * @param property
     * @param range
     * @return
     */
    public String getTableName(List<String> categoryList,String property, String range) throws Exception{

        Collections.sort(categoryList);

        String categoryListString = "";
        for(String category: categoryList){
            categoryListString+=  stringManipulations.URItoLabel(category) + "-";
        }

        String result = categoryListString+ stringManipulations.URItoLabel(property)+ stringManipulations.URItoLabel(range);
        result = result.replaceAll(" ","");
        result = result.replaceAll("-","");
        result = result.replaceAll("_","");
        result = result.replaceAll("#","");
        result = result.replaceAll("\\.","");


        if(result.length()>64){
            //Generate hash
            result = "a" + stringManipulations.calculateHash(result);
        }


        return result;

    }

    public String getTableNameForRange(String range){



        String result = "rangeTable" + stringManipulations.URItoLabel(range);
        result = result.replaceAll(" ","");
        result = result.replaceAll("-","");
        result = result.replaceAll("_","");
        result = result.replaceAll("#","");
        result = result.replaceAll("\\.","");



        return result;

    }



}
