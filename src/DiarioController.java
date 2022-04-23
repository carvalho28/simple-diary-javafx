import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;
import java.util.ResourceBundle;

public class DiarioController implements Initializable {
    @FXML
    Button btnFicheiros;

    @FXML
    private ListView<String> lstFiles;

    @FXML
    private TextArea txaFicheiro;

    private String pathFile = "";
    private boolean savedFile = true;

    //Saves file
    @FXML
    private void btnClick(ActionEvent e) throws IOException {
        ObservableList<CharSequence> paragraph = txaFicheiro.getParagraphs();
        Iterator<CharSequence> iter = paragraph.iterator();
        BufferedWriter bf = new BufferedWriter(new FileWriter((pathFile)));
        while (iter.hasNext()) {
            CharSequence seq = iter.next();
            bf.append(seq);
            bf.newLine();
        }
        bf.flush();
        bf.close();
    }

    @FXML
    private void btnOpen(MouseEvent e) {
        if (!savedFile && !pathFile.equals("")) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Aviso!");
            alert.setHeaderText("Deseja guardar as alterações?");
            alert.setContentText("");

            ButtonType b = alert.showAndWait().get();

            if(b == ButtonType.OK){
                System.out.println("File Saved!");
                btnFicheiros.fire();
            }
            if (b == ButtonType.CANCEL){
                System.out.println("Cancel");
                lstFiles.getSelectionModel().selectPrevious();
            }
        }
        txaFicheiro.clear();
        String filePath = lstFiles.getSelectionModel().getSelectedItem();
        pathFile = "src/files/" + filePath;
        try {
            InputStream inputstream = new FileInputStream(pathFile);
            int data = inputstream.read();

            while (data != -1) {
                char aChar = (char) data;
                txaFicheiro.appendText(String.valueOf(aChar));
                data = inputstream.read();
            }
            inputstream.close();
        } catch (Exception err) {
            System.err.println(err.getMessage());
        }
    }

    @FXML
    private void keyPressedChange(KeyEvent e){savedFile = false;}

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> items = FXCollections.observableArrayList();
        File folder = new File("src/files");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile()) {
                items.add(listOfFiles[i].getName());
            }
        }

        lstFiles.setItems(items);
    }
}
