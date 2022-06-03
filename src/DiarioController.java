import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.bouncycastle.crypto.CryptoException;
import org.fxmisc.richtext.StyleClassedTextArea;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiarioController implements Initializable {

    /* ENCRYTPION/ DECRYPTION */
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    /* SHORTCUTS */
    private final KeyCombination saveCombo = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.META_DOWN); //command Mac e Ctrl Windows
    private final KeyCombination zoomInCombo = new KeyCodeCombination(KeyCode.EQUALS, KeyCodeCombination.META_DOWN);
    private final KeyCombination zoomOutCombo = new KeyCodeCombination(KeyCode.MINUS, KeyCodeCombination.META_DOWN);
    private final KeyCombination refreshCombo = new KeyCodeCombination(KeyCode.R, KeyCodeCombination.META_DOWN);
    private final String[] dateSelectionOptions = {"Dia", "Intervalo de Datas", "Todas as Datas"};
    /* ICONS */
    @FXML
    FontAwesomeIconView fileIcon;
    @FXML
    Rectangle fileBack;
    @FXML
    FontAwesomeIconView searchIcon;
    @FXML
    Rectangle searchBack;
    @FXML
    FontAwesomeIconView calendarIcon;
    @FXML
    Rectangle calendarBack;
    @FXML
    FontAwesomeIconView signOut;
    /* AUTENTICAÇÃO */
    String chave = AuthController.keyUser;
    String nomeUtilizador = AuthController.userName;
    /* ITEMS GERAIS */
    @FXML
    AnchorPane anchoPaneText;
    @FXML
    VBox vBox;
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
    private Button logoutBTN;
    @FXML
    private TextField txfProcura;
    @FXML
    private ListView<String> lstFiles;
    @FXML
    private StyleClassedTextArea txaFicheiro;
    @FXML
    private DatePicker datePick;
    @FXML
    private DatePicker datePick2;
    @FXML
    private ChoiceBox<String> dateSelectionType;
    @FXML
    private Text txtWords;
    /* Outras variáveis */
    private String pathFile = "";
    private String fileName = "";
    private LocalDate fileDate = null;
    private boolean autoSaveToggle = false;
    private boolean newFile = false;
    private int dateSelectionIndex = 0; // 0 -> "Dia", 1 -> "Intervalo de Datas", 2 -> "Todas as Datas"
    private LocalDate dataInicio;
    private String file1 = "";
    private LocalDate dataFim;
    private String file2 = "";

    private boolean savedFile = true;

    /* Função geral de encriptação */
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

    /* Encriptação */
    public static void encrypt(String key, File inputFile, File outputFile) throws CryptoException {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile);
    }

    /* Desemcriptação */
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

    /* Sobre o programa */
    @FXML
    private void aboutProgram(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Sobre o programa");
        alert.setContentText("""
                Este programa foi desenvolvido por:
                Diogo Carvalho, 45716
                João Marques, 45722
                para a Unidade Curricular de Interação Humana com o Computador
                """);
        alert.showAndWait();
    }

    /* Adicionar buttons ao HTML editor */
//    private HTMLEditor addButtonsHTML(HTMLEditor htmlEditor) {
//
//            // get toolbar from html editor
//            ToolBar toolBar = (ToolBar) htmlEditor.lookup("ToolBar");
//
//
//            Node[] nodes = htmlEditor.lookupAll(".tool-bar").toArray(new Node[0]);
//            ToolBar toolBar = (ToolBar) nodes[0];
//            int numItems = toolBar.getItems().size();
//            System.out.println(numItems);
//            toolBar.getItems().add(new Separator());
//            Button buttonLogout = new Button("Logout");
//            buttonLogout.setOnAction(event -> {
//                try {
//                    logoutHandle(event);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });

    /* Zoom In */
    @FXML
    private void zoomIn(ActionEvent e) {
        HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
        tamFonte += 1;
        textArea.setStyle("-fx-font-size: " + (tamFonte + 1) + "px;");
    }

    /* Zoom Out */
    @FXML
    private void zoomOut(ActionEvent e) {
        HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
        tamFonte -= 1;
        textArea.setStyle("-fx-font-size: " + (tamFonte - 1) + "px;");
    }

    /* Refresh lstFiles */
    @FXML
    private void refreshListFiles(ActionEvent e) {
        lstFiles.getItems().clear();
        ArrayList<String> items = new ArrayList<>();
        File folder = new File("src/files/" + nomeUtilizador + "/");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".html")) {
                items.add(listOfFiles[i].getName());
            }
        }
        lstFiles.getItems().addAll(items);
        txtWords.setText("Nº de Entradas: " + items.size());
    }

    /* Toggle autosave */
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

    /* Shortcut functions */
    @FXML
    private void textAreaShortcuts(KeyEvent e) throws IOException, CryptoException, InterruptedException {
        if (saveCombo.match(e)) {
            btnClick(new ActionEvent());
        }
        if (zoomInCombo.match(e)) {
            zoomIn.fire();
        }
        if (zoomOutCombo.match(e)) {
            zoomOut.fire();
        }
        if (refreshCombo.match(e)) {
            refreshListFiles(new ActionEvent());
        }
    }

    /* Verificar se a tab se encontra aberta */
    private boolean isTabOpen(String newTitle) {
        return tituloTabs.contains(newTitle);
    }

    //Funcao que abre novo tab
    //Changes -> O antigo String FilePath tornou-se na var. global fileName
    //openType: 0 -> 1 entrada; 1 -> intervalo de entradas; 2 -> todas as entradas
    private void openFileFunction(int openType) {
        if (openType == 0) {
            Tab t1 = new Tab(fileName.substring(0, fileName.length() - 5));
//            add class to t1
//            t1.getStyleClass().add("anchorTab");

            if (isTabOpen(t1.getText())) {
                int index = tituloTabs.indexOf(t1.getText());
                tabPane.getSelectionModel().select(index);
            } else {
                HTMLEditor textArea1 = new HTMLEditor();
                textArea1.getStyleClass().add("anchorTab");
                textArea1.setPadding(new Insets(10, 10, 10, 10));

                if (fileDate == null) {
                    if (newFile) {
                        fileDate = LocalDate.now();
                    } else {
                        fileDate = LocalDate.parse((lstFiles.getSelectionModel().getSelectedItem()).substring(0, 10));
                    }
                }
                newFile = false;
//                textArea1.setEditable(fileDate.equals(LocalDate.now()));
                if (fileDate.equals(LocalDate.now())) {
                    textArea1.setDisable(true);
                }
                fileDate = null;
                t1.setContent(textArea1);
                tabPane.getTabs().add(t1);
                tabPane.getSelectionModel().select(t1);
                tituloTabs.add(t1.getText());
                tabPane.setOnKeyPressed(event -> {
                    try {
                        textAreaShortcuts(event);
                        keyPressedAutoSave(event);
                        savedFile = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                t1.setOnCloseRequest(event -> {
                    if (!savedFile && !pathFile.equals("")) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Aviso!");
                        alert.setHeaderText("Deseja guardar as alterações?");
                        alert.setContentText("");

                        ButtonType b = alert.showAndWait().get();

                        if (b == ButtonType.OK) {
                            try {
                                saveFuntion();
                            } catch (IOException | CryptoException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    tituloTabs.remove(t1.getText());
                    lstFiles.getSelectionModel().clearSelection();
                });

                try {
                    File f = new File("src/files/" + nomeUtilizador + "/" + fileName);
                    decrypt(chave, f, f);
                    InputStream inputstream = new FileInputStream("src/files/" + nomeUtilizador + "/" + fileName);
                    InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                    int data = inputStreamReader.read();
                    // set text from html file
                    while (data != -1) {
                        char current = (char) data;
                        textArea1.setHtmlText(textArea1.getHtmlText() + current);
                        data = inputStreamReader.read();
                    }
                    inputstream.close();
                    encrypt(chave, f, f);
                } catch (CryptoException e) {
                    e.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro");
                    alert.setHeaderText("Este ficheiro não lhe pertence!");

                    alert.showAndWait();
                    t1.getTabPane().getTabs().remove(t1);
                    tituloTabs.remove(t1.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // text formatter for not being able to edit first line

                //CARRET ATTEMP TO MAKE FIRST 2 LINES UMNEDITABLE
//                String dataDia = (new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
//                String conteudoImo = dataDia + "\n------------\n";
//                // textarea cursor listener
//
//                textArea1.caretPositionProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
//                    int caretPosition = textArea1.getCaretPosition();
//
//                    if (caretPosition >= 0 && caretPosition < 24) {
////                    textArea1.displaceCaret(25);
//                        String remaining = "";
//                        char[] remainigChars = conteudoImo.toCharArray();
//                        for (int i = caretPosition; i < 24; i++) {
//                            remaining += remainigChars[i];
//                        }
//                        textArea1.replaceText(caretPosition, 23, remaining);
//                    }
//                });

            }
        }
        if (openType == 1) {
            Tab t1 = new Tab(file1.substring(0, file1.length() - 5) + " - " + file2.substring(0, file2.length() - 5));

            if (isTabOpen(t1.getText())) {
                int index = tituloTabs.indexOf(t1.getText());
                tabPane.getSelectionModel().select(index);
            } else {
                HTMLEditor textArea1 = new HTMLEditor();
                textArea1.setPadding(new Insets(10, 10, 10, 10));
                t1.setContent(textArea1);
                tabPane.getTabs().add(t1);
                tabPane.getSelectionModel().select(t1);
                tituloTabs.add(t1.getText());
                textArea1.setOnKeyPressed(event -> {
                    try {
                        textAreaShortcuts(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                t1.setOnClosed(event -> tituloTabs.remove(t1.getText()));

                if (dataInicio.isAfter(dataFim)) {
                    LocalDate aux = dataInicio;
                    dataInicio = dataFim;
                    dataFim = aux;
                }
                dataFim = dataFim.plusDays(1);

                String dateAux;
                int fileCounter = 0;
                while (dataInicio.isBefore(dataFim)) {
                    dateAux = dataInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String pathAux = "src/files/" + nomeUtilizador + "/" + dateAux + ".html";
                    try {
                        File f = new File(pathAux);
                        if (f.exists()) {
                            fileCounter++;
                            decrypt(chave, f, f);
                            InputStream inputstream = new FileInputStream(pathAux);
                            InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                            int data = inputStreamReader.read();

                            while (data != -1) {
                                char current = (char) data;
                                textArea1.setHtmlText(textArea1.getHtmlText() + current);
                                data = inputStreamReader.read();
                            }
                            textArea1.setHtmlText(textArea1.getHtmlText());
                            inputstream.close();
                            encrypt(chave, f, f);
                        }
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
                    dataInicio = dataInicio.plusDays(1);
                }
                if (fileCounter == 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro");
                    alert.setHeaderText("Não existem entradas entre estas datas!");

                    alert.showAndWait();
                    t1.getTabPane().getTabs().remove(t1);
                }
            }
        }

        if (openType == 2) {
            Tab t1 = new Tab("Todas as Entradas");

            if (isTabOpen(t1.getText())) {
                int index = tituloTabs.indexOf(t1.getText());
                tabPane.getSelectionModel().select(index);
            } else {
                HTMLEditor textArea1 = new HTMLEditor();
                textArea1.setDisable(true);
                textArea1.setPadding(new Insets(10, 10, 10, 10));
                t1.setContent(textArea1);
                tabPane.getTabs().add(t1);
                tabPane.getSelectionModel().select(t1);
                tituloTabs.add(t1.getText());
                textArea1.setOnKeyPressed(event -> {
                    try {
                        textAreaShortcuts(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                t1.setOnClosed(event -> tituloTabs.remove(t1.getText()));

                File folder = new File("src/files/" + nomeUtilizador + "/");
                File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".html"));

                if (listOfFiles.length == 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro");
                    alert.setHeaderText("Não existem entradas entre estas datas!");

                    alert.showAndWait();
                    t1.getTabPane().getTabs().remove(t1);
                } else {
                    for (File f : listOfFiles) {
                        try {
                            decrypt(chave, f, f);
                            InputStream inputstream = new FileInputStream("src/files/" + nomeUtilizador + "/" + f.getName());
                            InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                            int data = inputStreamReader.read();

                            while (data != -1) {
                                char current = (char) data;
                                textArea1.setHtmlText(textArea1.getHtmlText() + current);
                                data = inputStreamReader.read();
                            }
//                            textArea1.appendText("\n\n--------------------------------\n\n");
                            textArea1.setHtmlText(textArea1.getHtmlText());
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
                    }
                }
            }
        }
    }

    public static void delay(long millis, Runnable continuation) {
        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try { Thread.sleep(millis); }
                catch (InterruptedException e) { }
                return null;
            }
        };
        sleeper.setOnSucceeded(event -> continuation.run());
        new Thread(sleeper).start();
    }

    private void saveFuntion() throws IOException, CryptoException, InterruptedException {
        if (lstFiles.getSelectionModel().getSelectedItem() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não existe nenhum ficheiro selecionado");
            alert.showAndWait();
            return;
        }

        if (tabPane.getSelectionModel().getSelectedItem().getText().length() == 10) {
            HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
            BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFile + ".html"), StandardCharsets.UTF_8));
            bf.write(textArea.getHtmlText());
            bf.flush();
            bf.close();
            File f = new File(pathFile + ".html");
            encrypt(chave, f, f);
            saveIcon.setVisible(true);
            delay(2000, () -> saveIcon.setVisible(false));
            savedFile = true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não é possível guardar um ficheiro com diversas entradas!");
            alert.showAndWait();
        }
    }

    /* Guardar on click */
    @FXML
    private void btnClick(ActionEvent e) throws IOException, CryptoException, InterruptedException {
        saveFuntion();
    }

    /* Abrir on click */
    @FXML
    private void btnOpen(MouseEvent e) throws IOException, CryptoException {
//        if (!savedFile && !pathFile.equals("")) {
//            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//            alert.setTitle("Aviso!");
//            alert.setHeaderText("Deseja guardar as alterações?");
//            alert.setContentText("");
//
//            ButtonType b = alert.showAndWait().get();
//
//            if (b == ButtonType.OK) {
//                saveFuntion();
//            }
//            if (b == ButtonType.CANCEL) {
//                lstFiles.getSelectionModel().selectPrevious();
//            }
//        }
        fileName = lstFiles.getSelectionModel().getSelectedItem();
        if (fileName != null) {
            pathFile = "src/files/" + nomeUtilizador + "/" + fileName.substring(0, (fileName).length() - 5);
            openFileFunction(0);
        }
    }

    /* Guarda on key press */
    @FXML
    private void keyPressedAutoSave(KeyEvent e) throws IOException, CryptoException {
        if (autoSaveToggle) {
            String name = lstFiles.getSelectionModel().getSelectedItem();
            File f = new File("src/files/" + nomeUtilizador + "/" + name);
            if (f.exists()) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
                bw.write(textArea.getHtmlText());
                bw.close();
                encrypt(chave, f, f);
            }

        }
    }

    /* Criar ficheiro na diretoria src/files */
    @FXML
    private void btnNew(ActionEvent e) throws IOException, CryptoException {
        LocalDate localDate = LocalDate.now();
        String name = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File f = new File("src/files/" + nomeUtilizador + "/" + name + ".html");
        if (f.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro!");
            alert.setHeaderText("O ficheiro já existe!");
            alert.setContentText("");
            alert.showAndWait().get();
        } else {
            f.createNewFile();
            //adicionar data à primeira linha
//            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
//            bw.write(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
//            bw.write("\n");
//            bw.write("<hr>");
//            bw.close();
            //adicionar data à primeira linha
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            // create a span that is not editable
            bw.write("<h3>");
            bw.write(new SimpleDateFormat("dd/MM/yyyy").format(new Date()) + "\n");
            bw.write("</h3>");
            bw.write("<hr>\n");
            bw.write("<br>\n");
            bw.close();

            encrypt(chave, f, f);
            pathFile = "src/files/" + nomeUtilizador + "/" + name;
            fileName = name + ".html";
            newFile = true;
            openFileFunction(0);
            lstFiles.getItems().add(fileName);
            lstFiles.getSelectionModel().select(fileName);
            lstFiles.scrollTo(fileName);
            txtWords.setText("Nº de Entradas: " + lstFiles.getItems().size());
        }
    }

    /* Delete file on click */
    @FXML
    private void btnDelete(ActionEvent e) {
        String name = lstFiles.getSelectionModel().getSelectedItem();
        File f = new File("src/files/" + nomeUtilizador + "/" + name);
        if (f.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Apagar ficheiro");
            alert.setHeaderText("Tem a certeza que quer apagar o ficheiro?");
            alert.setContentText("");

            ButtonType b = alert.showAndWait().get();
            if (b == ButtonType.OK) {
                f.delete();
                tituloTabs.remove(tabPane.getSelectionModel().getSelectedIndex());
                lstFiles.getItems().remove(name);
                lstFiles.getSelectionModel().select(0);
                tabPane.getTabs().remove(tabPane.getSelectionModel().getSelectedIndex());
                txtWords.setText("Nº de Entradas: " + lstFiles.getItems().size());
            }
        }
    }

    /* Obter data (1) */
    @FXML
    private void pickDate1(ActionEvent e) {
        if (dateSelectionIndex == 0) {
            fileDate = datePick.getValue();
            if (fileDate != null) {
                String dataFinal = fileDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String path = "src/files/" + nomeUtilizador + "/" + dataFinal + ".html";
                File f = new File(path);
                // open file to textarea
                if (f.exists()) {
                    // abrir ficheiro na textarea
                    lstFiles.getSelectionModel().select(dataFinal);
                    lstFiles.scrollTo(dataFinal);
                    pathFile = "src/files/" + nomeUtilizador + "/" + dataFinal;
                    fileName = dataFinal + ".html";
                    openFileFunction(0);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro!");
                    alert.setHeaderText("O ficheiro da data selecionada não existe!");
                    alert.setContentText("");
                    alert.showAndWait().get();
                }
            }
            datePick.getEditor().clear();
            datePick.setValue(null);
        }
        if (dateSelectionIndex == 1) {
            dataInicio = datePick.getValue();
            if (dataInicio != null) {
                String dataFinal = dataInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                file1 = dataFinal + ".html";
                datePick2.setDisable(false);
                datePick2.setOpacity(1);
            }
        }
    }

    /* Obter data (2) */
    @FXML
    private void pickDate2(ActionEvent e) {
        dataFim = datePick2.getValue();
        if (dataFim != null) {
            String dataFinal = dataFim.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            file2 = dataFinal + ".html";
            if (!dataInicio.equals(dataFim)) {
                openFileFunction(1);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro!");
                alert.setHeaderText("Insira datas diferentes para obter as entradas entre elas!");
                alert.setContentText("");
                alert.showAndWait().get();
            }
            datePick.getEditor().clear();
            datePick.setValue(null);
            datePick2.getEditor().clear();
            datePick2.setDisable(true);
            datePick2.setOpacity(0.5);
            datePick2.setValue(null);
        }
    }

    //DEIXAR ESTAR, COLOCAR DUVIDA
    @FXML
    private void toPDF(ActionEvent e) {
        try {
//            StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
            String diaryEntry = textArea.getHtmlText();
            String name = lstFiles.getSelectionModel().getSelectedItem();
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.beginText();
            content.newLineAtOffset(25, 750);
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
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
            content.endText();
            content.close();
            doc.addPage(page);
            doc.save("src/files/" + nomeUtilizador + "/" + name + ".pdf");
            doc.close();
        } catch (Exception ex) {
//            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro!");
            alert.setHeaderText("Erro ao criar o PDF!");
            alert.setContentText("");
            alert.showAndWait().get();
        }
    }

    /* ORTOGRAFIA */
    @FXML
    private void verificarOrtografia(ActionEvent e) throws IOException {
//        StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
        HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
        String text = textArea.getHtmlText();
        // if there is a paragraph, then new line is needed
        text = text.replaceAll("</p>", "\n");
        String allText = text;
        // html tags minus newlines
        text = text.replaceAll("<[^>]*>", "");
        // non-alphanumeric characters
//        text = text.replaceAll("[^a-zA-ZáàâãéèêíïóôõöúçñÁÀÂÃÉÈÍÏÓÔÕÖÚÇÑ ]", "");
        JLanguageTool langTool = new JLanguageTool(new PortugalPortuguese());
        List<RuleMatch> matches = langTool.check(text);
        ArrayList<String> palavrasErradas = new ArrayList<>();
        ArrayList<String> correcoes = new ArrayList<>();

        for (RuleMatch match : matches) {
            palavrasErradas.add(text.substring(match.getFromPos(), match.getToPos()));
            correcoes.add(match.getSuggestedReplacements().get(0));
        }
        int i = 0;
        StringBuilder mostraErros = new StringBuilder();
        for (String palavra : palavrasErradas) {
//            mostraErros += palavra + " -> " + correcoes.get(i) + "\n";
            mostraErros.append(palavra).append(" -> ").append(correcoes.get(i)).append("\n");
            i++;
        }
        System.out.println(mostraErros);
        if (mostraErros.length() != 0) {
            ButtonType btnCorrigir = new ButtonType("Corrigir", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnIgnorar = new ButtonType("Ignorar", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.WARNING, "Erros encontrados", btnCorrigir, btnIgnorar);
            alert.setTitle("Erros de ortografia");
            alert.setHeaderText("Erros encontrados");
            alert.setContentText(mostraErros.toString());
            alert.showAndWait();
            if (alert.getResult() == btnCorrigir) {
                for (int j = 0; j < palavrasErradas.size(); j++) {
                    allText = allText.replace(palavrasErradas.get(j), correcoes.get(j));
                    textArea.setHtmlText(allText);
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Erros de ortografia");
            alert.setHeaderText("Erros encontrados");
            alert.setContentText("Não foram encontrados erros de ortografia.");
            alert.showAndWait();
        }
    }

    /* Imprimir conteúdo da página para impressora /ou PDF */
    @FXML
    private void print(ActionEvent e) {
        if (tituloTabs.size() == 1 && tituloTabs.get(0).equals("Tab Inicial")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Impossível imprimir");
            alert.setContentText("Não existem textos para imprimir");
            alert.showAndWait();
            return;
        }
        HTMLEditor textArea = (HTMLEditor) tabPane.getSelectionModel().getSelectedItem().getContent();
        PrinterJob job = PrinterJob.createPrinterJob();
        textArea.print(job);

        if (job != null && job.showPrintDialog(textArea.getScene().getWindow())) {
            boolean success = job.printPage(textArea);
            if (success) {
                job.endJob();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Impressão");
                alert.setHeaderText("Impressão concluída");
                alert.setContentText("Impressão concluída com sucesso");
                alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Impossível imprimir");
            alert.setContentText("Não foi possível imprimir");
            alert.showAndWait();
        }

//
//        PrinterJob printerJob = PrinterJob.createPrinterJob();
//
//        if (printerJob != null && printerJob.showPrintDialog(textArea.getScene().getWindow())) {
//            PageLayout pageLayout = printerJob.getJobSettings().getPageLayout();
//            printArea.setMaxWidth(pageLayout.getPrintableWidth());
//            if (printerJob.printPage(printArea)) {
//                printerJob.endJob();
//            } else {
//                Alert alert = new Alert(Alert.AlertType.ERROR);
//                alert.setTitle("Erro");
//                alert.setHeaderText("A impressão falhou!");
//                alert.setContentText("Não existem textos para imprimir");
//                alert.showAndWait();
//            }
//        } else {
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Impressão cancelada");
//            alert.setHeaderText("Impressão cancelada");
//            alert.showAndWait();
//
//        }
    }

    /* Verifica se dada string existe nos ficheiros do utilizador */
    private int checkIfWordExistsFile(String path, String procura) throws CryptoException, IOException {
        File f = new File("src/files/" + nomeUtilizador + "/" + path);
        decrypt(chave, f, f);
        String[] words;
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String str;
        int count = 0;
        while ((str = br.readLine()) != null) {
            words = str.split("(?=[,.])|\\\\s+");
            for (String word : words) {
                if (word.contains(procura.toLowerCase())) {
                    count++;
                }
            }
        }
        encrypt(chave, f, f);
        return count;
    }

    /* Verificar ficheiros na pasta pela string da procura */
    private ArrayList<String> checkAllFilesInFolder(String palavra) throws CryptoException, IOException {
        // Tuplo com os ficheiros e o numero de vezes que aparece a palavra
        ArrayList<Pair<String, Integer>> tuplos = new ArrayList<>();
        File folder = new File("src/files/" + nomeUtilizador + "/");
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".html")) {
                if (checkIfWordExistsFile(file.getName(), palavra) != 0) {
                    tuplos.add(new Pair<>(file.getName(), checkIfWordExistsFile(file.getName(), palavra)));
                }
            }

        }
        // ordenação do tuplo
        tuplos.sort((o1, o2) -> o2.getValue() - o1.getValue());
        return tuplos.stream().map(Pair::getKey).collect(Collectors.toCollection(ArrayList::new));
    }

    /* Seleção por dia, intervalo ou todas */
    public void selectionType(ActionEvent e) {
        if (dateSelectionType.getValue().equals("Dia")) {
            datePick.setVisible(true);
            datePick2.setVisible(false);
            dateSelectionIndex = 0;
        }
        if (dateSelectionType.getValue().equals("Intervalo de Datas")) {
            datePick.setVisible(true);
            datePick2.setDisable(true);
            datePick2.setOpacity(0.5);
            datePick2.setVisible(true);
            dateSelectionIndex = 1;
        }
        if (dateSelectionType.getValue().equals("Todas as Datas")) {
            datePick.setVisible(false);
            datePick2.setVisible(false);
            dateSelectionIndex = 2;
            openFileFunction(2);
        }
    }

    @FXML
    private void fileIconFunction(MouseEvent e) {
        txtWords.setVisible(true);
        fileBack.setStyle("-fx-fill: #b4d2e7;");
        calendarBack.setStyle("-fx-fill: whitesmoke;");
        searchBack.setStyle("-fx-fill: whitesmoke;");
        txfProcura.setVisible(false);
        dateSelectionType.setVisible(false);
        datePick.setVisible(false);
        datePick2.setVisible(false);
        searchBack.setStyle("-fx-fill: whitesmoke;");
    }

    @FXML
    private void searchIconFunction(MouseEvent e) {
        txtWords.setVisible(false);
        fileBack.setStyle("-fx-fill: whitesmoke;");
        calendarBack.setStyle("-fx-fill: whitesmoke;");
        searchBack.setStyle("-fx-fill: #b4d2e7;");
        txfProcura.setVisible(true);
        dateSelectionType.setVisible(false);
        datePick.setVisible(false);
        datePick2.setVisible(false);
    }

    @FXML
    private void calendarIconFunction(MouseEvent e) {
        txtWords.setVisible(false);
        fileBack.setStyle("-fx-fill: whitesmoke;");
        searchBack.setStyle("-fx-fill: whitesmoke;");
        calendarBack.setStyle("-fx-fill: #b4d2e7;");
        txfProcura.setVisible(false);
        dateSelectionType.setVisible(true);
        datePick.setVisible(true);
        datePick2.setVisible(false);
    }

    private void saveAllFiles() throws IOException, CryptoException, InterruptedException {
        // open each tab and save it
        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            if (i != 0) {
                tabPane.getSelectionModel().select(i);
                String file = tabPane.getSelectionModel().getSelectedItem().getText();
                pathFile = "src/files/" + nomeUtilizador + "/" + file;
                lstFiles.getSelectionModel().select(file + ".html");
                saveFuntion();
            }
        }
    }

    @FXML
    private void signOutIconFunction(MouseEvent e) throws IOException, CryptoException, InterruptedException {
        // save all files
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sair");
        alert.setHeaderText("Deseja salvar todos os ficheiros antes de sair?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            saveAllFiles();
        }
        logoutBTN.fire();
    }

    /* Entrada Hoje pelo hyperlink */
    @FXML
    public void entradaHoje(ActionEvent e) throws IOException, CryptoException {
        LocalDate hoje = LocalDate.now();
        String name = hoje.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File f = new File("src/files/" + nomeUtilizador + "/" + name + ".html");
        if (!f.exists()) {
            btnNew(new ActionEvent());
        } else {
            fileDate = hoje;
            pathFile = "src/files/" + nomeUtilizador + "/" + name;
            fileName = name + ".html";
            openFileFunction(0);
        }
    }

    /* Logout */
    @FXML
    public void logoutHandle(ActionEvent e) throws IOException {
        Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Auth.fxml")));
        Scene scene = new Scene(parent);
        Stage appStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        appStage.setScene(scene);
        appStage.show();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> items = FXCollections.observableArrayList();
        File folder = new File("src/files/" + nomeUtilizador + "/");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".html")) {
                items.add(listOfFiles[i].getName());
            }
        }
        txtWords.setText("Nº de Entradas: " + items.size());
        txaFicheiro.getStyleClass().add("anchorTab");
        txaFicheiro.setPadding(new Insets(10, 10, 10, 10));

        firstTab.setClosable(false);

        lstFiles.setItems(items);
        tituloTabs.add(firstTab.getText());
        dateSelectionType.getItems().addAll(dateSelectionOptions);
        dateSelectionType.setValue(dateSelectionOptions[0]);
        dateSelectionType.setOnAction(this::selectionType);
        datePick2.setVisible(false);

        txfProcura.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue.length() > 0) {
                    ArrayList<String> files = checkAllFilesInFolder(newValue);
                    ObservableList<String> items2 = FXCollections.observableArrayList();
                    items2.addAll(files);
                    lstFiles.setItems(items2);
                } else {
                    lstFiles.setItems(items);
                }
            } catch (CryptoException | IOException e) {
                e.printStackTrace();
            }
        });
        txfProcura.setVisible(false);
        datePick.setVisible(false);
        datePick2.setVisible(false);
        dateSelectionType.setVisible(false);

        txaFicheiro.setPadding(new Insets(10, 10, 10, 10));
        String dataHoje = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        txaFicheiro.appendText("Bem-vindo/a ao seu Diário, " + nomeUtilizador + "!\n");
        txaFicheiro.appendText("Hoje é " + dataHoje + ".\n\n\n\n");
        // get current time
        txaFicheiro.appendText("Para ir para a nota de hoje");

    }
}

/**
 * Duvidas:
 * PDF e impressao, podem ser na mesmo icone?
 */