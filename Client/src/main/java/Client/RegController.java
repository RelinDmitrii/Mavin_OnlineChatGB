package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RegController {
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nickField;
    @FXML
    public TextArea textArea;

    private Controller controller;

    /**
     * Метод отвеающий за открытие окна регистрации при нажатии кнопки в главном меню
     * @param actionEvent
     */
    public void tryToReg(ActionEvent actionEvent) {
        controller.tryToReg(loginField.getText().trim(),passwordField.getText().trim(),nickField.getText().trim());

    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    /**
     * Метод отвечающий за вывод на экран результатов регистрации
     * @param msg
     */
    public void addMsgToTextArea(String msg){
        textArea.appendText(msg+"\n");

    }
}
