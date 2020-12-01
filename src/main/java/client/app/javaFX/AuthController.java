package client.app.javaFX;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

    private Client openConnected;

    public Client getOpenConnected() {
        return openConnected;
    }

    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    Label text;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openConnected = new Client();
        openConnected.connected();
    }


    public void btnConnected(ActionEvent actionEvent) throws Exception {
        if (openConnected.authSend(login.getText(), password.getText())) {
            ((Node) (actionEvent.getSource())).getScene().getWindow().hide();
            Parent parent = FXMLLoader.load(getClass().getResource("/main.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            stage.setTitle("Java Cloud Storage Oct");
            stage.show();
            PanelController.setOpenConnected(openConnected);
        } else {
            //login.setText("");
            login.clear();
            password.clear();
            //password.setText("");
            text.setText("Неверный логин и пароль");
        }
    }
}
