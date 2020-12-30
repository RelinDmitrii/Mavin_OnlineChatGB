package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private static int PORT = 8179; // Порт на котором расположен сервер
    ServerSocket server = null; // Сокет сервера
    Socket socket = null; // Сокет который выделяется сервером под клиента
    List<ClientHandler> clients; // Список всех подключившихся клинетов
    private AuthService authService;
    private static Statement statement;
    private ExecutorService service;

    public Server() {
        service = Executors.newCachedThreadPool();
        clients = new Vector<>();
//        authService = new SimpleAuthService();


        if (!SQLHandler.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД");
        }
        authService = new DBAuthService();




        try {//statement = ConnectionToSql.connectToSQL();
            // authService = new SimpleAuthService();
            server = new ServerSocket(PORT); // Создаем сервер сокет.
            System.out.println("Сервер запущен");

            while (true) {
                socket = server.accept(); // Как только кто-то подключается появляется клиентский соккет
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket, service); // Создаем нового клиента в скисок (Конструктор Сервер/Соккет)
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();
            try {
                service.shutdown();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Метод который отправляет сообщения всем клиентам из списка подключившихся
     *
     * @param msg само сообщение
     */
    void broadCastMsg(ClientHandler sender, String msg) {
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss");
        String message = String.format("%s %s : %s", formater.format(new Date()), sender.getNickName(), msg); // Переменная для соединения клиента и сообщения, которое он отправляет
        for (ClientHandler client : clients) {
            client.sendMsg(message + "\n");
        }
    }

    /**
     * Добавляет информацию о клиенте в массив
     *
     * @param clientHandler
     */
    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadClientList();
    }

    /**
     * Удаляет информацию о клиенте в массив
     *
     * @param clientHandler
     */

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadClientList();
    }

    /**
     * Используем интерфейс который дает возможность при смене реализации не менять код
     * Геттер для authService
     */
    public AuthService getAuthService() {
        return authService;
    }


    /**
     * Метод отвечаюзий за личные сообщения
     *
     * @param sender   Отправитель
     * @param receiver Получатель
     * @param msg      само сообщение
     */
    public void privateCastMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[%s] private [%s] : %s", sender.getNickName(), receiver, msg);
        for (ClientHandler c : clients) {
            if (c.getNickName().equals(receiver)) {
                c.sendMsg(message + "\n");
                if (!c.equals(sender)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }
    }

    /**
     * Метод который проверяет логин на повторное подключение через 2 окно
     * @param login Логин, который проверяется
     */
    public boolean isLoginAuthenticated(String login){
        for (ClientHandler c:clients) {
            if(c.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }

    /**
     * Метод по добавление клиентов в список чата
     */
    public void broadClientList() {
        StringBuilder sb = new StringBuilder("/clientList ");
        for (ClientHandler c: clients){
            sb.append(c.getNickName()).append(" ");
        }
        String msg = sb.toString();
        for (ClientHandler c: clients){
            c.sendMsg(msg);
        }
    }


}

