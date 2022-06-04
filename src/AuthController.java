import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

    public static String keyUser = "";
    public static String userName = "";
    @FXML
    TextField usernameTF;
    @FXML
    TextField passwordTF;
    @FXML
    Button loginBTN;
    @FXML
    Button cancelBTN;
    @FXML
    Hyperlink registoBTN;
    @FXML
    Label loginMessageLB;

    @FXML
    private void login(ActionEvent e) throws Exception {
        keyUser = "";
        if (!usernameTF.getText().isBlank() && !passwordTF.getText().isBlank()) {
            validateLogin();
            if (loginMessageLB.getText().equals("Login efetuado com sucesso!")) {
                userName = usernameTF.getText();
                Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("DiarioController.fxml")));
                Scene scene = new Scene(parent);
                scene.getStylesheets().add("/styles/diario.css");
                Stage appStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                appStage.setOnCloseRequest(evt -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Fechar programa");
                    alert.setHeaderText("Podem haver alterações não gravadas.\nPretende sair?");
                    alert.showAndWait().filter(r -> r != ButtonType.OK).ifPresent(r->evt.consume());
                });
                appStage.setScene(scene);
                appStage.show();
            }
        } else {
            loginMessageLB.setText("Por favor, insira um username e respetiva password.");
        }
    }

    @FXML
    private void cancel(ActionEvent e) {
        Stage stage = (Stage) cancelBTN.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void registo(ActionEvent e) throws IOException {
        Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Register.fxml")));
        Scene scene = new Scene(parent);
        Stage appStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        scene.getStylesheets().add("/styles/auth.css");
        appStage.setScene(scene);
        appStage.show();
    }

    public void validateLogin() throws Exception {
        DatabaseConnection db = new DatabaseConnection();
        Connection connection = db.getConnection();

        String password = RegisterController.encrypt(passwordTF.getText(), "chavesecreta1234");

        String verifyUser = "SELECT COUNT(1) FROM userAccounts WHERE username = '" + usernameTF.getText() + "' AND password = '" + password + "'";

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(verifyUser);

            while (resultSet.next()) {
                if (resultSet.getInt(1) == 1) {
                    loginMessageLB.setText("Login efetuado com sucesso!");
                    //get key from sdatabase and store in keyUser
                    String getKey = "SELECT keyUser FROM userAccounts WHERE username = '" + usernameTF.getText() + "'";
                    Statement statement1 = connection.createStatement();
                    ResultSet resultSet1 = statement1.executeQuery(getKey);
                    while (resultSet1.next()) {
                        keyUser = resultSet1.getString(1);
                    }
                } else {
                    loginMessageLB.setText("Credenciais inválidas!");
                    usernameTF.clear();
                    passwordTF.clear();
                    usernameTF.requestFocus();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void loginOnEnterPress(ActionEvent e) {
//        Scene scene = loginBTN.getScene();
//        scene.setOnKeyPressed(e -> {
//            if (e.getCode() == KeyCode.ENTER) {
//                    loginBTN.fire();
//
//            }
//        });
//    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//        scene.setOnKeyPressed(e -> {
//            if (e.getCode() == KeyCode.ENTER) {
//                loginBTN.fire();
//            }
//        });
    }
}
