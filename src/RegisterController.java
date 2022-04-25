import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    @FXML
    private Label registerMessageLB;

    @FXML
    private PasswordField passwordTF;

    @FXML
    private Button registoBTN;

    @FXML
    private TextField usernameTF;

    @FXML
    private void registo(ActionEvent e) throws IOException {
        if(usernameTF.getText().isEmpty() || passwordTF.getText().isEmpty()){
            registerMessageLB.setText("Preencha todos os campos");
        }else{
            validarRegisto();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Registo");
            alert.setHeaderText("Registo efetuado com sucesso");

            Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Auth.fxml")));
            Scene scene = new Scene(parent);
            Stage appStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            appStage.setScene(scene);
            appStage.show();
        }
    }
    public void validarRegisto(){
        DatabaseConnection db = new DatabaseConnection();
        Connection connection = db.getConnection();

        String username = usernameTF.getText();
        String password = passwordTF.getText();

        // verificar se o username já existe
        // se não existir, registar o utilizador
        String query1 = "SELECT COUNT(1) FROM userAccounts WHERE username = '" + username + "'";
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query1);
            while (resultSet.next()) {
                if(resultSet.getInt(1) == 1) {
                    registerMessageLB.setText("Utilizador já existente");
                    usernameTF.clear();
                    passwordTF.clear();
                } else {
                    KeyGenerator gen = KeyGenerator.getInstance("AES");
                    gen.init(128);
                    SecretKey secretKey = gen.generateKey();
                    byte[] binary = secretKey.getEncoded();
                    String key = String.format("%032X", new BigInteger(+1, binary));
                    System.out.println(key);
                    String query = "INSERT INTO userAccounts (username, password, keyUser) VALUES ('" + username + "', '" + password + "', '" + key + "')";

                    try{
                        Statement st = connection.createStatement();
                        st.executeUpdate(query);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
