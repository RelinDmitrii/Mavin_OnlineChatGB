package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    Server server = null;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;
    private String nickName;
    private String login; // login для отсутсивя возможности заходить под одинаковыми никами с разных окон
    private boolean avtoriz = false;

    public ClientHandler(Server server, Socket socket) {
        try{
        this.server = server;
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        //socket.setSoTimeout(45000);


        new Thread(() -> { // Поток для запуска работы каждый раз с новым клиентом
                try {
                    // ЦИКЛ АУТЕНТИФИКАЦИИ (ПОЛЬЗОВАТЕЛЬ ИМЕЕТ НЕСКОЛЬКО ПОПЫТОК)
                    while (true){
                        String str = in.readUTF();
                        if (str.startsWith("/auth")){ // если начинается с пртокола
                        String [] token = str.split("\\s");//разбиваем строку для получения логина и пароля
                        String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1],token[2]); // логин в 1 токен, пароль во 2
                        login = token[1]; // запоминаем авторизацию


                            if(newNick!=null){
                                if(!server.isLoginAuthenticated(token[1])) {
                                    nickName = newNick;
                                    sendMsg("/authok " + nickName); // ПО ПРОТОКОЛУ ИДЕНТИФИКАЦИЯ ПРОШЛА
                                    server.subscribe(this); // ДОБАВИЛИ КЛИЕНТА в МАССИВ
                                    System.out.println("Клиент " + nickName + " подключился.");
                                    break;
                                } else {
                                    sendMsg("С такой учетной записью уже зашли");
                                }
                            } else {
                                sendMsg("Неверный Логин / Пароль");
                            }
                        }
                        if(str.startsWith("/reg")){
                            String[] token = str.split("\\s");
                            if(token.length<4){
                                continue;
                            }
                            boolean isRegistration = server.getAuthService().registration(token[1], token[2],token[3]);
                            if(isRegistration){
                                sendMsg("/regok");
                            } else{
                                sendMsg("/regno");
                            }
                        }

                    }
                   // socket.setSoTimeout(0);
                    // ЦИКЛ РАБОТЫ
                    while (true) {
                        String str = in.readUTF();

                        if(str.startsWith("/")) {

                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }

                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length <3){
                                    continue;
                                }
                                server.privateCastMsg(this, token[1], token[2]);
                            }

                        }else {
                            server.broadCastMsg(this, str);
                        }
                    }
                } catch (IOException e){
//                        e.printStackTrace();
                    } finally {
                    System.out.println("Клиент отключился");
                    server.unsubscribe(this); // Удаление клиента из массива после закрытия соединения
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }).start();
    } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод отвечающий за отправку сообщения клиенту
     * @param msg Само сообщение
     */
    void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickName(){
        return nickName;
    }

    public String getLogin() {
        return login;
    }

}
