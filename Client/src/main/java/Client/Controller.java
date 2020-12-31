package Client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    // Инициализация соединения
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8179;
    @FXML
    public ListView<String> clientList;

    private Socket socket;
    DataInputStream in; //ВХОДНОЙ ПОТОК
    DataOutput out; // ВЫХОДНОЙ ПОТОК

    @FXML
    public HBox authPanel;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox msgPanel;

    private boolean authenticated; // ФЛАГ ОТВЕЧАЮЩИЙ ПРОШЛА ЛИ АУТЕНТИФИКАЦИЯ ИЛИ НЕТ (ДЛЯ ПЕРЕКЛЮЧЕНИЯ ИНТЕРФЕЙСА)
    private String nickName;
    private final String TITLE = "ГикЧат";

    private Stage stage;
    private Stage regStage;
    private RegController regController;


    private static BufferedWriter writerLog;
    private static long logFileLength;
    private static PrintWriter printWriter;


    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated); // если не прошли аутенфикацию Видна верхняя панель
        authPanel.setManaged(!authenticated); // занимает место
        msgPanel.setVisible(authenticated); // если прошли аутентифкацию видна нижняя панель
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);


        if (!authenticated) {
            nickName = ""; // обнуление никНейма
        }
        textArea.clear(); // Чистим поле чата после выхода
        setTitle(nickName);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {     // Проводит инициализацию после того как отрисовываются графические элементы
        setAuthenticated(false); // ИНИЦИАЛИЗАЦИЯ НЕ ПРОШЛА
        createRegWindow();
        connection();
        // Работа с графикой должна осуществляться внутри графического модуля !!!!!!
        Platform.runLater(() -> {

            stage = (Stage) textField.getScene().getWindow(); // Обращение к Stage (основание окна) через textField
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() { // Закрытие соединения по крестику, setOnCloseRequest реакция сробатывания на крестик
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("byu");
                    try {
                        out.writeUTF("/end"); // После закрытия на крестик отправляет на сервер сообщение о закрытии соединения
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    /**
     * Метод отвечающий за подключение к серверу
     */
    private void connection() {
        try {
            socket = new Socket(IP_ADDRESS, PORT); // Создаем соединение (IP_address и PORT к кому полдключаемся)
            in = new DataInputStream(socket.getInputStream()); // Инициализация потоков
            out = new DataOutputStream(socket.getOutputStream()); // Инициализация потоков

            new Thread(new Runnable() { // Поток добавляется, что бы не блокировался графический интерфейс
                @Override
                public void run() {
                    try {
                        // ЦИКЛ АУТЕНТИФИКАЦИИ
                        while (true) {
                            String str = in.readUTF(); // ЧИТАЕМ ДАННЫЕ
                            if (str.startsWith("/authok")) { // если начинается с пртокола
                                nickName = str.split("\\s", 2)[1];//ПОЛУЧИЛИ НИК ОТ СЕРВЕРА
                                setAuthenticated(true); // меняем визуальное изображение
                                //addNewClientLog(nickName);
                                break;
                            }
                            if (str.startsWith("/regok")) {
                                regController.addMsgToTextArea("Регистрация прошла успешно");
                            }
                            if (str.startsWith("/regno")) {
                                regController.addMsgToTextArea("Регистрация не получилась \n возможно логин или пароль заняты");
                            }
                            textArea.appendText(str + "\n");

                        }

                        Logs.addNewClientLog(loginField.getText(), textArea);
                        //ЦИКЛ РАБОТЫ
                        while (true) {

                            String str = null; // Переменная для сообщений от сервера
                            str = in.readUTF(); // Считываем сообщенние от сервера

                            if (str.startsWith("/")) { // Разбиваем на служебные сообщения
                                if (str.equals("/end")) {
                                    System.out.println("Клиент отключился");
                                    break;
                                }

                                if (str.startsWith("/clientList")) { // Протокол для внесения пользователй в чат-лист
                                    String[] token = str.split("\\s+"); // разбиваем строку
                                    Platform.runLater(() -> { // Запускаем графический поток
                                        clientList.getItems().clear(); // Каждый раз очищаем списко перед добавлением ногово пользоватедя
                                        for (int i = 1; i < token.length; i++) {
                                            clientList.getItems().add(token[i]);
                                        }
                                    });
                                }
                            } else {
                                Logs.addMsgToClientLog(str);
                                textArea.appendText(str + "\n");

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        printWriter.close();
                        System.out.println("МЫ отключились от сервера");
                        setAuthenticated(false); // МЕНЯЕМ ВИЗУАЛИЗАЦИЮ ПРИ ОТКЛЮЧЕНИИ
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод который отправляе сообщение при нажатии кнопки.
     */
    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText()); // Отправляем в исходящий поток текст из TextField
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод передающий запрос серверу на авторизацию пользователя согласно протоколу!!! (/auth)
     *
     * @param actionEvent
     */
    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) { // Если сокет сервера закрыт или отсутсвует подключение к серверу
            connection();
        }
        try {
            out.writeUTF(String.format("/auth %s %s", loginField.getText().trim().toLowerCase(),
                    passwordField.getText().trim())); // исходящий поток, который передает login и password согласно протоколу
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Метод который меняет поле после авторизации пользователя на "TITLE+" "+nick""
     *
     * @param nick
     */
    private void setTitle(String nick) {
        Platform.runLater(() -> { // ДОБОВЛЯЕТ В ОЧЕРДЬ НА ОБРАБОТКУ ГРАФИЧЕСКОМУ ПОТОКУ
            ((Stage) textField.getScene().getWindow()).setTitle(TITLE + " " + nick);
        });
    }

    public void registration(ActionEvent actionEvent) {
        regStage.show();
    }

    /**
     * Метод отвечает за отправку сообщений выбранному клиенту из списка чата и делает заготовку с троке чата
     *
     * @param mouseEvent сам клик
     */
    public void clickClientList(MouseEvent mouseEvent) {
        String receiver = clientList.getSelectionModel().getSelectedItem(); // обращаемся к оперделенному эллемету из списка чата
        textField.setText("/w " + receiver + " ");
    }


    /**
     * Метод отвечающий за создание окна регистрации
     */
    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml")); // показываем от куда загружать
            Parent root = fxmlLoader.load(); //ЗАгружает root который мы подкладывае для Scene
            regStage = new Stage();
            regStage.setTitle("Reg Window");
            regStage.setScene(new Scene(root, 400, 250));

            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage.initModality(Modality.APPLICATION_MODAL); // когда открыто окно регистрации другими пользоваться нельзя

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод отвечающий за сбор данных из кона регистрации
     *
     * @param login
     * @param password
     * @param nickName
     */
    public void tryToReg(String login, String password, String nickName) {
        String msg = String.format("/reg %s %s %s", login, password, nickName);

        if (socket == null || socket.isClosed()) { // Если сокет сервера закрыт или отсутсвует подключение к серверу
            connection();
        }

        try {
            out.writeUTF(msg); // Отправляем серверу запрос на регистрацию нового пользователя
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void addNewClientLog (String login) {
//        String fileName = "history_"+ login +".txt";
//        File file = new File ("C:\\Users\\79260\\Desktop\\Основной факультет\\Mavin_OnlineChat\\client\\src\\main\\java\\Client\\Logs\\"+fileName);
//        if(file.exists()){
//            try {
//                int counter = 0;
//                int n_lines = 100;
//                ArrayList<String> list = new ArrayList<>();
//                //FileReader fileReader = new FileReader(file);
//                ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(file);
//                try {
//                    while (counter < n_lines) {
//                        list.add(reversedLinesFileReader.readLine());
//                        counter++;
//                    }
//                } catch (NullPointerException o){
//
//                } finally {
//                    reversedLinesFileReader.close();
//                }
//                Collections.reverse(list);
//                for (String st: list){
//                    if(st!=null){
//                    textArea.appendText(st+"\n");}
//                }
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//        try {
//            printWriter = new PrintWriter(new FileWriter(file, true));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void addMsgToClientLog (String msg){
//        printWriter.println(msg);
//        printWriter.flush();
//    }
}