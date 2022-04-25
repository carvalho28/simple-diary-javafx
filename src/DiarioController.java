import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    MenuItem zoomIn;
    @FXML
    MenuItem zoomOut;
    @FXML
    MenuItem autoSave;
    @FXML
    MenuItem toPDF;


    @FXML
    TabPane tabPane;

    @FXML
    Tab firstTab;
    ArrayList<String> tituloTabs = new ArrayList<>();
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
    private boolean autoSaveToggle = false;
    /* SHORTCUTS */
    private KeyCombination saveCombo = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.META_DOWN); //command Mac e Ctrl Windows
    private KeyCombination zoomInCombo = new KeyCodeCombination(KeyCode.EQUALS, KeyCodeCombination.META_DOWN);
    private KeyCombination zoomOutCombo = new KeyCodeCombination(KeyCode.MINUS, KeyCodeCombination.META_DOWN);
    private KeyCombination refreshCombo = new KeyCodeCombination(KeyCode.R, KeyCodeCombination.META_DOWN);

    // remover string tab tituloTabs on close
    @FXML
    public void closeTab(ActionEvent event) {
        int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            tabPane.getTabs().remove(selectedIndex);
            tituloTabs.remove(selectedIndex);
        }
    }

    // about program
    @FXML
    private void aboutProgram(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Sobre o programa");
        alert.setContentText("Este programa foi desenvolvido por:\n" +
                "Diogo Carvalho, 45716\n" +
                "João Marques, 45722\n" +
                "para a Unidade Curricular de Interação Humana com o Computador\n");
        alert.showAndWait();
    }

    // zoom in
    @FXML
    private void zoomIn(ActionEvent e) {
        TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        textArea.setStyle("-fx-font-size: " + (textArea.getFont().getSize() + 1) + "px;");
    }

    @FXML
    // zoom out
    private void zoomOut(ActionEvent e) {
        TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        textArea.setStyle("-fx-font-size: " + (textArea.getFont().getSize() - 1) + "px;");
    }

    //toggle autoSave
    @FXML
    private void autoSave(ActionEvent e) {
        if (!autoSaveToggle) {
            autoSaveToggle = true;
            autoSave.setText("AutoSave  ✔");
        } else {
            autoSaveToggle = false;
            autoSave.setText("AutoSave");
        }
    }

    //shortcut to save
    @FXML
    private void textAreaShortcuts(KeyEvent e) throws IOException {
        if (saveCombo.match(e)) {
            saveFuntion();
        }
        if (zoomInCombo.match(e)) {
            zoomIn.fire();
        }
        if (zoomOutCombo.match(e)) {
            zoomOut.fire();
        }
        if (refreshCombo.match(e)) {
            openFileFunction(pathFile);
        }
    }

    private void openFileFunction(String filePath) {
//        System.out.println("Opening file: " + filePath);
        // set firstTab Text
        Tab t1 = new Tab(filePath.substring(0, filePath.length() - 4));
        // verificar se o texto da tab ja esta aberto
        if (tituloTabs.contains(t1.getText())) {
            int index = tituloTabs.indexOf(t1.getText());
            tabPane.getSelectionModel().select(index);
        } else {
            TextArea textArea1 = new TextArea();
            t1.setContent(textArea1);
            // formatter na textArea
            textArea1.setOnKeyTyped(event -> {
                try {
                    keyPressedAutoSave(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            textArea1.setOnKeyPressed(event -> {
                try {
                    textAreaShortcuts(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            tabPane.getTabs().add(t1);
            tabPane.getSelectionModel().select(t1);
            tituloTabs.add(t1.getText());
            t1.setOnClosed(event -> {
                tituloTabs.remove(t1.getText());
            });
            try {
                InputStream inputstream = new FileInputStream(pathFile);
                InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                int data = inputStreamReader.read();

                while (data != -1) {
                    char aChar = (char) data;
                    textArea1.appendText(String.valueOf(aChar));
                    data = inputStreamReader.read();
                }
                inputstream.close();
            } catch (Exception err) {
                System.err.println(err.getMessage());
            }

            textArea1.setTextFormatter(new TextFormatter<String>((TextFormatter.Change c) -> {
                String proposed = c.getControlNewText();
                if (proposed.startsWith(textArea1.getText(0, 23))) {
                    return c;
                } else {
                    return null;
                }
            }));
        }
    }

    private void saveFuntion() throws IOException {
        if (lstFiles.getSelectionModel().getSelectedItem() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não existe nenhum ficheiro selecionado");

            alert.showAndWait();
        }
        //obter da tab aberta txarea
        TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        ObservableList<CharSequence> paragraph = textArea.getParagraphs();
        Iterator<CharSequence> iter = paragraph.iterator();
        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFile), StandardCharsets.UTF_8));
        while (iter.hasNext()) {
            CharSequence seq = iter.next();
            bf.append(seq);
            bf.newLine();
        }
        bf.flush();
        bf.close();
    }


    //Saves file
    @FXML
    private void btnClick(ActionEvent e) throws IOException {
        saveFuntion();
    }

    @FXML
    private void btnOpen(MouseEvent e) throws IOException {
        if (!savedFile && !pathFile.equals("")) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Aviso!");
            alert.setHeaderText("Deseja guardar as alterações?");
            alert.setContentText("");

            ButtonType b = alert.showAndWait().get();

            if (b == ButtonType.OK) {
                saveFuntion();
                System.out.println("File Saved!");
            }
            if (b == ButtonType.CANCEL) {
                System.out.println("File not Saved!");
                lstFiles.getSelectionModel().selectPrevious();
            }
        }
        String filePath = lstFiles.getSelectionModel().getSelectedItem();
        pathFile = "src/files/" + filePath;
        openFileFunction(filePath);
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
        if (autoSaveToggle) {
            String fileName = lstFiles.getSelectionModel().getSelectedItem();
            File f = new File("src/files/" + fileName);
            if (f.exists()) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
                bw.write(textArea.getText());
                bw.close();
            }

        }
    }

    @FXML
    // criar ficheiro na diretoria src/files
    private void btnNew(ActionEvent e) throws IOException {
        String fileName;
        LocalDate localDate = LocalDate.now();
        fileName = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        fileName = fileName + ".txt";
        pathFile = "src/files/" + fileName;
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
            bw.newLine();
            bw.write("------------\n");
            bw.close();
            System.out.println("HERE");
            openFileFunction(fileName);
            // abrir ficheiro na textarea
            lstFiles.getItems().add(fileName);
            lstFiles.getSelectionModel().select(fileName);
            lstFiles.scrollTo(fileName);

            TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            textArea.requestFocus();
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
                tabPane.getTabs().remove(tabPane.getSelectionModel().getSelectedIndex());
            }
        }
    }

    // obter data
    @FXML
    private void pickDate(ActionEvent e) throws IOException {
        LocalDate localDate = datePick.getValue();
        String dataFinal = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = "src/files/" + dataFinal + ".txt";
        File f = new File(fileName);
        // open file to textarea
        if (f.exists()) {
            // abrir ficheiro na textarea
            lstFiles.getSelectionModel().select(fileName);
            lstFiles.scrollTo(fileName);
            pathFile = "src/files/" + dataFinal + ".txt";
            openFileFunction(dataFinal + ".txt");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro!");
            alert.setHeaderText("O ficheiro da data selecionada não existe!");
            alert.setContentText("");
            alert.showAndWait().get();
        }
//        datePick.getEditor().clear();
//        datePick.setValue(null);
    }

//    @FXML
//    private void toPDF() {
//        TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
//        String diaryEntry = textArea.getText();
//        PDFont font = PDType1Font.HELVETICA_BOLD;
//        PDDocument doc = new PDDocument();
//        PDPage page = new PDPage();
//        PDPageContentStream content;
//        try {
//            content = new PDPageContentStream(doc, page);
//            content.beginText();
////            content.moveTextPositionByAmount(300, 400);
////            content.setFont(font, 40);
//            content.drawString(textArea.getText());
//
//            content.endText();
//            content.close();
//            doc.addPage(page);
//            doc.save("example.pdf");
//        } catch (Exception ex) {
//            ex.printStackTrace();
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
        tituloTabs.add(firstTab.getText());
//        firstTab.setText("");


    }
}

