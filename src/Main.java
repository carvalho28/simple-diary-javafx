import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Auth.fxml")));

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
        if (e.getCode() == KeyCode.ENTER) {
            Button btn = (Button) scene.lookup("#loginBTN");
            btn.fire();
        }
    }
        );
        stage.setTitle("Diário");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args designed without arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

}
