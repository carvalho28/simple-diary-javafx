import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.bouncycastle.crypto.CryptoException;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.Paragraph;
import org.languagetool.JLanguageTool;
import org.languagetool.language.PortugalPortuguese;
import org.languagetool.rules.RuleMatch;
import org.reactfx.collection.LiveList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.print.PrintException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
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

    /* ENCRYTPION/ DECRYPTION */
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    private final String[] dateSelectionOptions = {"Dia", "Intervalo de Datas", "Todas as Datas"};
    //private int dateSelectionIndex = 0 // 0->"Dia", 1->"Intervalo de Datas", 2->"Todas as Datas"
    private final boolean savedFile = true;
    /* SHORTCUTS */
    private final KeyCombination saveCombo = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.META_DOWN); //command Mac e Ctrl Windows
    private final KeyCombination zoomInCombo = new KeyCodeCombination(KeyCode.EQUALS, KeyCodeCombination.META_DOWN);
    private final KeyCombination zoomOutCombo = new KeyCodeCombination(KeyCode.MINUS, KeyCodeCombination.META_DOWN);
    private final KeyCombination refreshCombo = new KeyCodeCombination(KeyCode.R, KeyCodeCombination.META_DOWN);
    String chave = AuthController.keyUser;
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
    MenuItem imprimir;
    @FXML
    TabPane tabPane;
    @FXML
    Tab firstTab;
    ArrayList<String> tituloTabs = new ArrayList<>();
    int tamFonte = 15;
    @FXML
    private TextField txfProcura;
    @FXML
    private ListView<String> lstFiles;
    @FXML
    private StyleClassedTextArea txaFicheiro;
    @FXML
    private DatePicker datePick;
    //Ainda n existe datePick2 nem spinner  no scenebuilder
    @FXML
    private DatePicker datePick2;
    @FXML
    private ChoiceBox<String> dateSelectionType;
    private String pathFile = "";
    private boolean autoSaveToggle = false;

    private static void doCrypto(int cipherMode, String key, File inputFile, File outputFile) throws CryptoException {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(cipherMode, secretKey);

            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            throw new CryptoException("Error encrypting/decrypting file", e);
        }
    }

    public static void encrypt(String key, File inputFile, File outputFile) throws CryptoException {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile);
    }

    public static void decrypt(String key, File inputFile, File outputFile) throws CryptoException {
        doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile);
    }

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
        StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        tamFonte += 1;
        textArea.setStyle("-fx-font-size: " + (tamFonte + 1) + "px;");
    }

    @FXML
    // zoom out
    private void zoomOut(ActionEvent e) {
        StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        tamFonte -= 1;
        textArea.setStyle("-fx-font-size: " + (tamFonte - 1) + "px;");
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
    private void textAreaShortcuts(KeyEvent e) throws IOException, CryptoException {
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

    //Funcao que abre novo tab
    private void openFileFunction(String filePath) {
//        System.out.println("Opening file: " + filePath);
        // set firstTab Text
        Tab t1 = new Tab(filePath.substring(0, filePath.length() - 4));
        // verificar se o texto da tab ja esta aberto
        if (tituloTabs.contains(t1.getText())) {
            int index = tituloTabs.indexOf(t1.getText());
            tabPane.getSelectionModel().select(index);
        } else {
            StyleClassedTextArea textArea1 = new StyleClassedTextArea();
            textArea1.setWrapText(true);
            t1.setContent(textArea1);
            tabPane.getTabs().add(t1);
            tabPane.getSelectionModel().select(t1);
            tituloTabs.add(t1.getText());
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t1.setOnClosed(event -> {
                tituloTabs.remove(t1.getText());
            });
            try {
                File f = new File("src/files/" + filePath);
                decrypt(chave, f, f);
                InputStream inputstream = new FileInputStream(pathFile);
                InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                int data = inputStreamReader.read();

                while (data != -1) {
                    char aChar = (char) data;
                    textArea1.appendText(String.valueOf(aChar));
                    data = inputStreamReader.read();
                }
                inputstream.close();

                encrypt(chave, f, f);

            } catch (CryptoException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro");
                alert.setHeaderText("Este ficheiro não lhe pertence!");

                alert.showAndWait();
                t1.getTabPane().getTabs().remove(t1);
                tituloTabs.remove(t1.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }

            String dataDia = (new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
//            bw.write("\n");
//            bw.write("------------\n");
            String conteudoImo = dataDia + "\n------------\n";
            // textarea cursor listener
            textArea1.caretPositionProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                int caretPosition = textArea1.getCaretPosition();

                if (caretPosition >= 0 && caretPosition < 24) {
//                    textArea1.displaceCaret(25);
                    String remaining = "";
                    char[] remainigChars = conteudoImo.toCharArray();
                    for (int i = caretPosition; i < 24; i++) {
                        remaining += remainigChars[i];
                    }
                    textArea1.replaceText(caretPosition, 23, remaining);
                }


            });

        }

    }

    private void saveFuntion() throws IOException, CryptoException {
        if (lstFiles.getSelectionModel().getSelectedItem() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não existe nenhum ficheiro selecionado");

            alert.showAndWait();
        }
        //obter da tab aberta txarea
        StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        LiveList<Paragraph<Collection<String>, String, Collection<String>>> paragraph = textArea.getParagraphs();
        Iterator<Paragraph<Collection<String>, String, Collection<String>>> iter = paragraph.iterator();
        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFile), StandardCharsets.UTF_8));
        while (iter.hasNext()) {
            Paragraph<Collection<String>, String, Collection<String>> seq = iter.next();
            bf.append(seq.getText());
            bf.newLine();
        }
        bf.flush();
        bf.close();
        File f = new File(pathFile);
        encrypt(chave, f, f);
    }


    //Saves file
    @FXML
    private void btnClick(ActionEvent e) throws IOException, CryptoException {
        saveFuntion();
    }

    @FXML
    private void btnOpen(MouseEvent e) throws IOException, CryptoException {
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
                StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
                bw.write(textArea.getText());
                bw.close();
            }

        }
    }

    @FXML
    // criar ficheiro na diretoria src/files
    private void btnNew(ActionEvent e) throws IOException, CryptoException {
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
            bw.write("\n");
            bw.write("------------\n");
            bw.close();
            encrypt(chave, f, f);
            openFileFunction(fileName);
            // abrir ficheiro na textarea
            lstFiles.getItems().add(fileName);
            lstFiles.getSelectionModel().select(fileName);
            lstFiles.scrollTo(fileName);

//            TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
//            textArea.requestFocus();
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
                tituloTabs.remove(tabPane.getSelectionModel().getSelectedIndex());
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
            pathFile = "src/files/" + dataFinal /*+ ".txt"*/;
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

    @FXML
    private void toPDF(ActionEvent e) {
        try {
            StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            String diaryEntry = textArea.getText();
            System.out.println(diaryEntry);
            String fileName = lstFiles.getSelectionModel().getSelectedItem();
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.beginText();
            content.newLineAtOffset(25, 750);
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
//            String aux = "";
//            for (int i = 0; i < diaryEntry.length(); i++){
//                if (diaryEntry.indexOf(i) == '\n'){
//                    System.out.println(aux);
//                    content.showText(aux);
//                    content.newLine();
//                    aux = "";
//                }
//                aux += diaryEntry.charAt(i);
//            }
            StringBuilder b = new StringBuilder();
            content.setLeading(14.5f);
            for (int i = 0; i < diaryEntry.length(); i++) {
                if (WinAnsiEncoding.INSTANCE.contains(diaryEntry.charAt(i))) {
                    b.append(diaryEntry.charAt(i));
                } else {
                    content.showText(b.toString());
                    b = new StringBuilder();
                    content.newLine();
                }
            }
            System.out.println(b);
            content.endText();
            content.close();
            doc.addPage(page);
            doc.save("src/files/" + fileName + ".pdf");
            doc.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ORTOGRAFIA */
    @FXML
    private void verificarOrtografia(ActionEvent e) throws IOException {
        StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        String texto = textArea.getText(); //.substring(textArea.getText().indexOf("\n") + 1).substring(textArea.getText().indexOf("\n") + 2).trim()
        JLanguageTool langTool = new JLanguageTool(new PortugalPortuguese());
        List<RuleMatch> matches = langTool.check(texto);
        ArrayList<Integer> errosInicio = new ArrayList<>();
        ArrayList<Integer> errosFim = new ArrayList<>();
        ArrayList<String> correcoes = new ArrayList<>();
        // add to erros inicio e erros fim  excepto as primeiras duas linhas
        for (RuleMatch match : matches) {
            if (match.getFromPos() > 20) {
                errosInicio.add(match.getFromPos());
                errosFim.add(match.getToPos());
                correcoes.add(String.valueOf(match.getSuggestedReplacements().get(0)));
            }
        }
        System.out.println(correcoes);

        for (int i = 0; i < errosInicio.size(); i++) {
            textArea.setStyleClass(errosInicio.get(i), errosFim.get(i), "wrong-words");
        }

    }


    // print content of textarea to printer
    @FXML
    private void print(ActionEvent e) throws IOException, PrintException {
        System.out.println(tituloTabs);
        if (tituloTabs.size() == 1 && tituloTabs.get(0).equals("Tab")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Impossível imprimir");
            alert.setContentText("Não existem textos para imprimir");
            alert.showAndWait();
            return;
        }
        StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();

        TextFlow printArea = new TextFlow(new Text(textArea.getText()));

        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null && printerJob.showPrintDialog(textArea.getScene().getWindow())) {
            PageLayout pageLayout = printerJob.getJobSettings().getPageLayout();
            printArea.setMaxWidth(pageLayout.getPrintableWidth());
            if (printerJob.printPage(printArea)) {
                printerJob.endJob();
                // done printing
            } else {
                System.out.println("Falhou a impressão");
            }
        } else {
            System.out.println("Cancelado");
        }
    }

    private boolean checkIfWordExistsFile(String path, String word) throws CryptoException {
        File f = new File("src/files/" + path);
        decrypt(chave, f, f);
        // check if word exists in file
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains(word.toLowerCase())) {
                    encrypt(chave, f, f);
                    return true;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        encrypt(chave, f, f);
        return false;
    }

    private ArrayList<String> checkAllFilesInFolder(String palavra) throws CryptoException {
        ArrayList<String> files = new ArrayList<>();
        File folder = new File("src/files");
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                if (checkIfWordExistsFile(file.getName(), palavra)) {
                    files.add(file.getName());
                }
            }

        }
        return files;
    }

    public void selectionType(ActionEvent e) {
        if (dateSelectionType.getValue().equals("Dia")) {
            datePick.setVisible(true);
            datePick2.setVisible(false);
        }
        if (dateSelectionType.getValue().equals("Intervalo de Datas")) {
            datePick.setVisible(true);
            datePick2.setVisible(true);
        }
        if (dateSelectionType.getValue().equals("Todas as Datas")) {
            datePick.setVisible(false);
            datePick2.setVisible(false);
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
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".txt")) {
                items.add(listOfFiles[i].getName());
            }
        }

        lstFiles.setItems(items);
        tituloTabs.add(firstTab.getText());

        dateSelectionType.getItems().addAll(dateSelectionOptions);
        dateSelectionType.setValue(dateSelectionOptions[0]);
        dateSelectionType.setOnAction(this::selectionType);

        datePick2.setVisible(false);


        //listener in txfProcura
        txfProcura.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue.length() > 0) {
                    ArrayList<String> files = checkAllFilesInFolder(newValue);
                    ObservableList<String> items2 = FXCollections.observableArrayList();
                    for (String file : files) {
                        items2.add(file);
                    }
                    lstFiles.setItems(items2);
                }  else {
                    lstFiles.setItems(items);
                }
            } catch (CryptoException e) {
                e.printStackTrace();
            }
        }
        );
    }
}

