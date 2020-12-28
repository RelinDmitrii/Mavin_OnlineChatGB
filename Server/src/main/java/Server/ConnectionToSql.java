package Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionToSql {

    private static Connection connection;
    private static Statement stmt;



    /**
     * Метод для конекта к базе данных
     */

    public static Statement connectToSQL() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC"); // Путь к JDBS
        connection = DriverManager.getConnection("jdbc:sqlite:Clients");
        return stmt = connection.createStatement();

    }

    public static void disconnectSql(){
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


     public static void insertUser(String password, String login, String nickName) throws SQLException {
         stmt.executeUpdate("INSERT INTO Clients (Password, Login, NickName) VALUES ('"+password+"', '"+login+"', '"+nickName+"')");
     }
//
//     public static void multiInsert() throws SQLException {
//         stmt.executeUpdate("INSERT INTO Clients (NickName, Password) VALUES ('ffff','ffff'), "+"('dddd','dddd'), ('kkkk','kkkk')");
//     }

    public static List<Map<String,String>> getAllClients() throws SQLException {
        List<Map<String,String>> list = new ArrayList<>();
        ResultSet resultSet = stmt.executeQuery("SELECT * FROM Clients");
        while (resultSet.next()){
            String loginRs = resultSet.getString("Login");
            String nickNameRs = resultSet.getString("NickName");
            String passwordRs = resultSet.getString("Password");
            Map<String,String> mapClients = new HashMap<>();
            mapClients.put("Login", loginRs);
            mapClients.put("NickName", nickNameRs);
            mapClients.put("Password", passwordRs);
            list.add(mapClients);
        }
        stmt.close();
        return list;
    }





}
