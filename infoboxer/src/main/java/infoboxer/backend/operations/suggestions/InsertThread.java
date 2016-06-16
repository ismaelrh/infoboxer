package infoboxer.backend.operations.suggestions;

import infoboxer.backend.common.dto.CountObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * This thread inserts CountObjects into the database asynchronously.
 */
public class InsertThread implements Runnable {


    private Connection connection;
    private String tableName;
    private List<CountObject> list;

    public  InsertThread(Connection connection, String tableName, List<CountObject> list){
        this.connection= connection;
        this.tableName=tableName;
        this.list=list;
    }
    @Override
    public void run() {

        try{
            PreparedStatement insertStatement;

            for(CountObject object: list){

                insertStatement = connection
                        .prepareStatement("INSERT INTO " + tableName + "(label,uri,count) VALUES(?,?,?)");


                insertStatement.setString(1,object.getLabel());
                insertStatement.setString(2,object.get_id());
                insertStatement.setInt(3,object.getCount());

                insertStatement.executeUpdate();

                insertStatement.close();

            }

            //Commit the results
            connection.commit();

        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
}
