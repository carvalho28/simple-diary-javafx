import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    /**
     * @param args designed without arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Auth.fxml")));
        Scene scene = new Scene(root);
        stage.setTitle("Diário");
        stage.setScene(scene);
        stage.show();
    }

}
