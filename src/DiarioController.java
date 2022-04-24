import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DiarioController implements Initializable {
    @FXML
    Button btnFicheiros;
    @FXML
    Button btnNovo;
    @FXML
    Button btnApagar;

    @FXML
    private TextField txfProcura;

    @FXML
    private ListView<String> lstFiles;

    @FXML
    private TextArea txaFicheiro;

    @FXML
    private DatePicker datePick;

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

            if (b == ButtonType.OK) {
                System.out.println("File Saved!");
                btnFicheiros.fire();
            }
            if (b == ButtonType.CANCEL) {
                System.out.println("File not Saved!");
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

    //Auto save off
    @FXML
    private void keyPressedChange(KeyEvent e) {
        savedFile = false;
    }

    //  POSSIBLY IMPLEMENT AUTOSAVE TOGGLE ON MENU BAR
    //Auto save on
    @FXML
    private void keyPressedAutoSave(KeyEvent e) throws IOException {
        String fileName = lstFiles.getSelectionModel().getSelectedItem();
        File f = new File("src/files/" + fileName);
        if (f.exists()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(txaFicheiro.getText());
            bw.close();
        }

    }

    //procura palavra selecionada e coloca numa lista
    @FXML
    private void btnSearch(ActionEvent e) throws IOException {
        String word = txaFicheiro.getSelectedText();
        String[] files = new File("src/files").list();
        ObservableList<String> list = FXCollections.observableArrayList();
        for (String file : files) {
            String path = "src/files/" + file;
            File f = new File(path);
            if (f.isFile()) {
                BufferedReader bf = new BufferedReader(new FileReader(f));
                String line = bf.readLine();
                while (line != null) {
                    if (line.contains(word)) {
                        list.add(file);
                        break;
                    }
                    line = bf.readLine();
                }
                bf.close();
            }
        }
//        lstFiles.setItems(list);
    }

    @FXML
    // criar ficheiro na diretoria src/files
    private void btnNew(ActionEvent e) throws IOException {
        String fileName = "";
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Novo ficheiro");
        dialog.setHeaderText("Por favor, insira o nome do ficheiro:");
//        dialog.setContentText("Please enter your name:");
        Optional<String> result = dialog.showAndWait();
        System.out.println(result);
        fileName = result.get();
        System.out.println(fileName);
        if (!fileName.equals("")) {

            if (!fileName.contains(".txt")) {
                fileName = fileName + ".txt";
            }
            File f = new File("src/files/" + fileName);
            if (f.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro!");
                alert.setHeaderText("O ficheiro já existe!");
                alert.setContentText("");

                alert.showAndWait().get();
            } else {
                f.createNewFile();
                //adicionar data à primeira linha
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                bw.close();

                txaFicheiro.clear();

                // abrir ficheiro na textarea
                lstFiles.getItems().add(fileName);
                lstFiles.getSelectionModel().select(fileName);
                lstFiles.scrollTo(fileName);
                File f2 = new File("src/files/" + fileName);
                BufferedReader br = new BufferedReader(new FileReader(f2));
                String line = br.readLine();
                while (line != null) {
                    txaFicheiro.appendText(line + "\n");
                    line = br.readLine();
                }
                br.close();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Nome do ficheiro inválido");
            alert.setContentText("");

            alert.showAndWait();
        }
    }

    //function to delete file
    @FXML
    private void btnDelete(ActionEvent e) throws IOException {
        String fileName = lstFiles.getSelectionModel().getSelectedItem();
        File f = new File("src/files/" + fileName);
        if (f.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Apagar ficheiro");
            alert.setHeaderText("Tem a certeza que quer apagar o ficheiro?");
            alert.setContentText("");

            ButtonType b = alert.showAndWait().get();
            if (b == ButtonType.OK) {
                f.delete();
                lstFiles.getItems().remove(fileName);
                lstFiles.getSelectionModel().select(0);
                txaFicheiro.clear();
            }
        }
    }

    // obter data
    @FXML
    private void pickDate(ActionEvent e) {
        LocalDate localDate = datePick.getValue();
        String dataFinal = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
//        System.out.println(dataFinal);
    }

    //procura conteudo dos ficheiros atraves do textfield
//    @FXML
//    private void search(ActionEvent e) throws IOException {
//        String fileName = lstFiles.getSelectionModel().getSelectedItem();
//        File f = new File("src/files/" + fileName);
//        if (f.exists()) {
//            BufferedReader br = new BufferedReader(new FileReader(f));
//            String line = br.readLine();
//            while (line != null) {
//                if (line.contains(txfProcura.getText())) {
//                    txaFicheiro.appendText(line + "\n");
//                }
//                line = br.readLine();
//            }
//            br.close();
//        }
//    }


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

        // procura por nome do ficheiro
        FilteredList<String> filteredData = new FilteredList<>(items, p -> true);
        lstFiles.setItems(filteredData);
        txfProcura.textProperty().addListener(((observable, oldValue, newValue) -> {
            filteredData.setPredicate(data -> {
                if (newValue == null || newValue.isEmpty()){
                    return true;
                }
                String lowerCaseSearch=newValue.toLowerCase();
                return Boolean.parseBoolean(String.valueOf(data.contains(lowerCaseSearch)));
            });
        }));


    }
}

