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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.ResourceBundle;

public class DiarioController implements Initializable {
//    @FXML
//    Button btnFicheiros;
//    @FXML
//    Button btnNovo;
//    @FXML
//    Button btnApagar;

    @FXML
    MenuItem menuNovo;
    @FXML
    MenuItem menuApagar;
    @FXML
    MenuItem menuGuardar;
    @FXML
    MenuItem menuSobre;

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


    // about program
    @FXML
    private void aboutProgram(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Sobre o programa");
        alert.setContentText("Este programa foi desenvolvido por:\n" +
                "Diogo Carvalho, 45716\n" +
                "João Marques, 45779\n" +
                "para a Unidade Curricular de Interação Humana com o Computador\n");
        alert.showAndWait();
    }

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
                menuGuardar.fire();
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
//        savedFile = false;
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

    @FXML
    // criar ficheiro na diretoria src/files
    private void btnNew(ActionEvent e) throws IOException {
        String fileName;
        LocalDate localDate = LocalDate.now();
        fileName = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        fileName = fileName + ".txt";
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
    }

    //function to delete file
    @FXML
    private void btnDelete(ActionEvent e) {
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
    private void pickDate(ActionEvent e) throws IOException {
        LocalDate localDate = datePick.getValue();
        String dataFinal = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        System.out.println(dataFinal);
        String fileName = "src/files/" + dataFinal + ".txt";
        System.out.println(fileName);
        File f = new File(fileName);
        // open file to textarea
        if (f.exists()) {
            // abrir ficheiro na textarea
            lstFiles.getSelectionModel().select(fileName);
            lstFiles.scrollTo(fileName);
            File f2 = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(f2));
            String line = br.readLine();
            while (line != null) {
                txaFicheiro.appendText(line + "\n");
                line = br.readLine();
            }
            br.close();
        }
        else{
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro!");
            alert.setHeaderText("O ficheiro da data selecionada não existe!");
            alert.setContentText("");
            alert.showAndWait().get();
        }

    }



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
//        FilteredList<String> filteredData = new FilteredList<>(items, p -> true);
//        txfProcura.textProperty().addListener((observable, oldValue, newValue) -> {
//            lstFiles.refresh();
//            filteredData.setPredicate(item -> {
//                if (newValue == null || newValue.isEmpty()) {
//                    return true;
//                }
//                String lowerCaseFilter = newValue.toLowerCase();
//                if (item.toLowerCase().contains(lowerCaseFilter)) {
//                    return true;
//                }
//                return false;
//
//            });
//        });

//        SortedList<String> sortedData = new SortedList<>(filteredData);
//        lstFiles.setItems(sortedData);



    }
}

