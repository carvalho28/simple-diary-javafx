import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

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

    public static String keyUser = "";
    public static String userName = "";

    @FXML
    private void login(ActionEvent e) throws Exception {
        keyUser = "";
        if (!usernameTF.getText().isBlank() && !passwordTF.getText().isBlank()){
            validateLogin();
            if (loginMessageLB.getText().equals("Login successful")){
                userName = usernameTF.getText();
                Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("DiarioController.fxml")));
                Scene scene = new Scene(parent);
                scene.getStylesheets().add("/styles/diario.css");
                Stage appStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                appStage.setScene(scene);
                appStage.show();
            }
        }
        else {
            loginMessageLB.setText("Please enter username and password");
        }
    }

    @FXML
    private void cancel(ActionEvent e){
        Stage stage = (Stage) cancelBTN.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void registo(ActionEvent e) throws IOException {
        Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Register.fxml")));
        Scene scene = new Scene(parent);
        Stage appStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
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
                if(resultSet.getInt(1) == 1) {
                    loginMessageLB.setText("Login successful");
                    //get key from sdatabase and store in keyUser
                    String getKey = "SELECT keyUser FROM userAccounts WHERE username = '" + usernameTF.getText() + "'";
                    Statement statement1 = connection.createStatement();
                    ResultSet resultSet1 = statement1.executeQuery(getKey);
                    while (resultSet1.next()) {
                        keyUser = resultSet1.getString(1);
                    }
                } else {
                    loginMessageLB.setText("Login failed");
                    usernameTF.clear();
                    passwordTF.clear();
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
