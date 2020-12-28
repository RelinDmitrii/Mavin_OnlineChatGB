package Server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleAuthService implements AuthService{



    private class UserData{ // Приватный класс для хранения логинов и паролей
        String login;
        String  password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    List<UserData> users;


    /**
     * Метод по заполнению списка
     */
    public SimpleAuthService() throws SQLException {
        users = usersConverter(ConnectionToSql.getAllClients());


//        for (int i = 0; i < 10; i++) {
//            users.add(new UserData("login"+i, "pass"+i, "nick"+i));
//        }
//        users.add(new UserData("qwe", "qwe", "qwe")); // для теста
//        users.add(new UserData("asd", "asd", "asd"));
//        users.add(new UserData("zxc", "zxc", "zxc"));

    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (UserData user: users) { // Проходим по списку пользователей
            if(user.login.equals(login) && user.password.equals(password)){ // если есть совпадения логина и пароля
                return user.nickname; // возвращаем ник
            }
        }
        return null; // В противном случае null
    }

    /**
     * Метод отвечающий за внесение пользователя в списко и проверку уже имеющегося (из интерфейса)
     * @param login
     * @param password
     * @param nickname
     * @return
     */
    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            List<Map<String,String>> listMapClients = ConnectionToSql.getAllClients();
            users.clear();
            users = usersConverter(listMapClients);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        for(UserData user : users){
            if(user.login.equals(login) || user.nickname.equals(nickname)){
                return false;
            }
        }
        try {
            ConnectionToSql.insertUser(password,login,nickname);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        users.add(new UserData(login,password,nickname));
        return true;
    }

    private List<UserData> usersConverter(List<Map<String,String>> listMap) throws SQLException {
        List<Map<String,String>> listMapClients = ConnectionToSql.getAllClients();
        List<UserData> usersData = new ArrayList<>();
        for (Map<String,String> mapClient: listMapClients) {
            UserData userData = new UserData(
                    mapClient.get("Login"),
                    mapClient.get("Password"),
                    mapClient.get("NickName"));
            usersData.add(userData);
        } return usersData;
    }

}
