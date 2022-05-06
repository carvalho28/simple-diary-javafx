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
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.bouncycastle.crypto.CryptoException;
import org.languagetool.JLanguageTool;
import org.languagetool.language.PortugalPortuguese;
import org.languagetool.rules.RuleMatch;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
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
    MenuItem ortografi;
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

            textArea1.setTextFormatter(new TextFormatter<String>((TextFormatter.Change c) -> {
                String proposed = c.getControlNewText();
                if (proposed.startsWith(textArea1.getText(0, 23))) {
                    return c;
                } else {
                    return null;
                }
            }));

//            textArea1.requestFocus();
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
                TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
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
            TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            String diaryEntry = textArea.getText();
            System.out.println(diaryEntry);
            String fileName = lstFiles.getSelectionModel().getSelectedItem();
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.beginText();
            content.newLineAtOffset(25,750);
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
        TextArea textArea = (TextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        //get text from the third line in textarea
        String texto = textArea.getText().substring(textArea.getText().indexOf("\n") + 1).substring(textArea.getText().indexOf("\n") + 2).trim();
        System.out.println(texto);
        JLanguageTool langTool = new JLanguageTool(new PortugalPortuguese());
        List<RuleMatch> matches = langTool.check(texto);
        ArrayList<String> errosInicio = new ArrayList<>();
        ArrayList<String> errosFim = new ArrayList<>();
        for (RuleMatch match : matches) {
//            System.out.println("Potential error at characters " +
//                    match.getFromPos() + "-" + match.getToPos() + ": " +
//                    match.getMessage());
//            System.out.println("Suggested correction(s): " +
//                    match.getSuggestedReplacements());
            errosInicio.add(String.valueOf(match.getFromPos()));
            errosFim.add(String.valueOf(match.getToPos()));

        }
        ArrayList<String> palavras = new ArrayList<>();
        if (errosInicio.size() > 0) {
            for (int i = 0; i < errosInicio.size(); i++) {
                palavras.add(texto.substring(Integer.parseInt(errosInicio.get(i)), Integer.parseInt(errosFim.get(i))));
            }
        }
        // underline words inside palavras in textarea

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

    }
}

