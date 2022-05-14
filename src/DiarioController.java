import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
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
    private int dateSelectionIndex = 0; // 0->"Dia", 1->"Intervalo de Datas", 2->"Todas as Datas"
    private LocalDate dataInicio;
    private String file1 = "";
    private LocalDate dataFim;
    private String file2 = "";

    private final boolean savedFile = true;
    /* SHORTCUTS */
    private final KeyCombination saveCombo = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.META_DOWN); //command Mac e Ctrl Windows
    private final KeyCombination zoomInCombo = new KeyCodeCombination(KeyCode.EQUALS, KeyCodeCombination.META_DOWN);
    private final KeyCombination zoomOutCombo = new KeyCodeCombination(KeyCode.MINUS, KeyCodeCombination.META_DOWN);
    private final KeyCombination refreshCombo = new KeyCodeCombination(KeyCode.R, KeyCodeCombination.META_DOWN);
    String chave = AuthController.keyUser;
    String nomeUtilizador = AuthController.userName;
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
    @FXML
    private DatePicker datePick2;
    @FXML
    private ChoiceBox<String> dateSelectionType;
    private String pathFile = "";
    private String fileName = "";
    private LocalDate fileDate = null;
    private boolean autoSaveToggle = false;
    private boolean newFile = false;

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
        alert.setContentText("""
                Este programa foi desenvolvido por:
                Diogo Carvalho, 45716
                João Marques, 45722
                para a Unidade Curricular de Interação Humana com o Computador
                """);
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

    @FXML
    //refresh listfiles
    private void refreshListFiles(ActionEvent e) {
        lstFiles.getItems().clear();
        ArrayList<String> items = new ArrayList<>();
        File folder = new File("src/files/" + nomeUtilizador + "/");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".txt")) {
                items.add(listOfFiles[i].getName());
            }
        }
        lstFiles.getItems().addAll(items);
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

    private boolean isTabOpen(String newTitle) {
        return tituloTabs.contains(newTitle);
    }

    //Funcao que abre novo tab
    //Changes -> O antigo String FilePath tornou-se na var. global fileName
    //openType: 0 -> 1 entrada; 1 -> intervalo de entradas; 2 -> todas as entradas
    private void openFileFunction(int openType) {
//        System.out.println("Opening file: " + filePath);
        // set firstTab Text

        if (openType == 0) {
            Tab t1 = new Tab(fileName.substring(0, fileName.length() - 4));

            if (isTabOpen(t1.getText())) {
                int index = tituloTabs.indexOf(t1.getText());
                tabPane.getSelectionModel().select(index);
            } else {
                StyleClassedTextArea textArea1 = new StyleClassedTextArea();
                textArea1.setWrapText(true);
                textArea1.setPadding(new Insets(10, 10, 10, 10));
                if (fileDate == null) {
                    if (newFile) {
                        fileDate = LocalDate.now();
                    } else {
                        fileDate = LocalDate.parse((lstFiles.getSelectionModel().getSelectedItem()).substring(0, 10));
                    }
                }
                newFile = false;
                if (fileDate.equals(LocalDate.now())) {
                    textArea1.setEditable(true);
                } else {
                    textArea1.setEditable(false);
                }
                fileDate = null;
                t1.setContent(textArea1);
                tabPane.getTabs().add(t1);
                tabPane.getSelectionModel().select(t1);
                tituloTabs.add(t1.getText());
                // formatter na textArea
                textArea1.setOnKeyTyped(event -> {
                    try {
                        keyPressedAutoSave(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                tabPane.setOnKeyPressed(event -> {
                    try {
                        textAreaShortcuts(event);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                t1.setOnClosed(event -> {
                    tituloTabs.remove(t1.getText());
                });
                try {
                    File f = new File("src/files/" + nomeUtilizador + "/" + fileName);
                    decrypt(chave, f, f);
                    InputStream inputstream = new FileInputStream("src/files/" + nomeUtilizador + "/" + fileName);
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

                Separator separator = new Separator();
                separator.setOrientation(Orientation.HORIZONTAL);
                separator.setPadding(new Insets(0, 0, 0, 0));
                separator.setMaxWidth(Double.MAX_VALUE);
                separator.setHalignment(HPos.CENTER);



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

                //prevent user to select all text and delete it
//                textArea1.setOnKeyPressed(event -> {
//                    if (event.getCode() == KeyCode.A && event.isShortcutDown()) {
//                        event.consume();
//                    }
//                });
            }
        }
        if (openType == 1) {
            Tab t1 = new Tab(file1.substring(0, file1.length() - 4) + " - " + file2.substring(0, file2.length() - 4));

            if (isTabOpen(t1.getText())) {
                int index = tituloTabs.indexOf(t1.getText());
                tabPane.getSelectionModel().select(index);
            } else {
                StyleClassedTextArea textArea1 = new StyleClassedTextArea();
                textArea1.setEditable(false);
                textArea1.setWrapText(true);
                textArea1.setPadding(new Insets(10, 10, 10, 10));
                t1.setContent(textArea1);
                tabPane.getTabs().add(t1);
                tabPane.getSelectionModel().select(t1);
                tituloTabs.add(t1.getText());
                // formatter na textArea
//                textArea1.setOnKeyTyped(event -> {
//                    try {
//                        keyPressedAutoSave(event);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                });
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
                    String pathAux = "src/files/" + nomeUtilizador + "/" + dateAux + ".txt";
                    try {
                        File f = new File(pathAux);
                        if (f.exists()) {
                            fileCounter++;
                            decrypt(chave, f, f);
                            InputStream inputstream = new FileInputStream(pathAux);
                            InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                            int data = inputStreamReader.read();

                            while (data != -1) {
                                char aChar = (char) data;
                                textArea1.appendText(String.valueOf(aChar));
                                data = inputStreamReader.read();
                            }
                            textArea1.appendText("\n\n--------------------------------\n\n");
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
                StyleClassedTextArea textArea1 = new StyleClassedTextArea();
                textArea1.setEditable(false);
                textArea1.setWrapText(true);
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
                t1.setOnClosed(event -> {
                    tituloTabs.remove(t1.getText());
                });

                File folder = new File("src/files/" + nomeUtilizador + "/");
                //list of files with .txt
                File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));

                if (listOfFiles.length == 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro");
                    alert.setHeaderText("Não existem entradas entre estas datas!");

                    alert.showAndWait();
                    t1.getTabPane().getTabs().remove(t1);
                } else {
                    for (File f: listOfFiles) {
                        try {
                            decrypt(chave, f, f);
                            InputStream inputstream = new FileInputStream("src/files/" + nomeUtilizador + "/" + f.getName());
                            InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                            int data = inputStreamReader.read();

                            while (data != -1) {
                                char aChar = (char) data;
                                textArea1.appendText(String.valueOf(aChar));
                                data = inputStreamReader.read();
                            }
                            textArea1.appendText("\n\n--------------------------------\n\n");
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

//        Tab t1 = new Tab(filePath.substring(0, filePath.length() - 4));
//        // verificar se o texto da tab ja esta aberto
//        if (tituloTabs.contains(t1.getText())) {
//            int index = tituloTabs.indexOf(t1.getText());
//            tabPane.getSelectionModel().select(index);
//        } else {
//            StyleClassedTextArea textArea1 = new StyleClassedTextArea();
//            textArea1.setWrapText(true);
//            t1.setContent(textArea1);
//            tabPane.getTabs().add(t1);
//            tabPane.getSelectionModel().select(t1);
//            tituloTabs.add(t1.getText());
//            // formatter na textArea
//            textArea1.setOnKeyTyped(event -> {
//                try {
//                    keyPressedAutoSave(event);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//            textArea1.setOnKeyPressed(event -> {
//                try {
//                    textAreaShortcuts(event);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//            t1.setOnClosed(event -> {
//                tituloTabs.remove(t1.getText());
//            });
//            try {
//                File f = new File("src/files/" + filePath);
//                decrypt(chave, f, f);
//                InputStream inputstream = new FileInputStream(pathFile);
//                InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
//                int data = inputStreamReader.read();
//
//                while (data != -1) {
//                    char aChar = (char) data;
//                    textArea1.appendText(String.valueOf(aChar));
//                    data = inputStreamReader.read();
//                }
//                inputstream.close();
//
//                encrypt(chave, f, f);
//
//            } catch (CryptoException e) {
//                Alert alert = new Alert(Alert.AlertType.ERROR);
//                alert.setTitle("Erro");
//                alert.setHeaderText("Este ficheiro não lhe pertence!");
//
//                alert.showAndWait();
//                t1.getTabPane().getTabs().remove(t1);
//                tituloTabs.remove(t1.getText());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            String dataDia = (new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
////            bw.write("\n");
////            bw.write("------------\n");
//            String conteudoImo = dataDia + "\n------------\n";
//            // textarea cursor listener
//            textArea1.caretPositionProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
//                int caretPosition = textArea1.getCaretPosition();
//
//                if (caretPosition >= 0 && caretPosition < 24) {
////                    textArea1.displaceCaret(25);
//                    String remaining = "";
//                    char[] remainigChars = conteudoImo.toCharArray();
//                    for (int i = caretPosition; i < 24; i++) {
//                        remaining += remainigChars[i];
//                    }
//                    textArea1.replaceText(caretPosition, 23, remaining);
//                }
//
//
//            });
//
//        }
    }



    private void saveFuntion() throws IOException, CryptoException {
        if (lstFiles.getSelectionModel().getSelectedItem() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não existe nenhum ficheiro selecionado");
            alert.showAndWait();
            return;
        }

        //Perguntar Diogo: Deixar ou n deixar user gravar ficheiro com intervalo de datas
        if (tabPane.getSelectionModel().getSelectedItem().getText().length() == 10) {
            //obter da tab aberta txarea
            StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            LiveList<Paragraph<Collection<String>, String, Collection<String>>> paragraph = textArea.getParagraphs();
            Iterator<Paragraph<Collection<String>, String, Collection<String>>> iter = paragraph.iterator();
            BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFile + ".txt"), StandardCharsets.UTF_8));
            while (iter.hasNext()) {
                Paragraph<Collection<String>, String, Collection<String>> seq = iter.next();
                bf.append(seq.getText());
                bf.newLine();
            }
            bf.flush();
            bf.close();
            File f = new File(pathFile  + ".txt");
            encrypt(chave, f, f);
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não é possível guardar um ficheiro com diversas entradas!");

            alert.showAndWait();
        }
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
            }
            if (b == ButtonType.CANCEL) {
                lstFiles.getSelectionModel().selectPrevious();
            }
        }
        //Sera que falta + ".txt" ?????
        fileName = lstFiles.getSelectionModel().getSelectedItem();
        if (fileName != null) {
            System.out.println(fileName);
            pathFile = "src/files/" + nomeUtilizador + "/" + fileName.substring(0, (fileName).length() - 4);
//        fileName += ".txt";
            openFileFunction(0);
        }
    }

    //Auto save on
    @FXML
    private void keyPressedAutoSave(KeyEvent e) throws IOException, CryptoException {
        if (autoSaveToggle) {
            String name = lstFiles.getSelectionModel().getSelectedItem();
            File f = new File("src/files/" + nomeUtilizador + "/" + name);
            if (f.exists()) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
                bw.write(textArea.getText());
                bw.close();
                encrypt(chave, f, f);
            }

        }
    }

    @FXML
    // criar ficheiro na diretoria src/files
    private void btnNew(ActionEvent e) throws IOException, CryptoException {
        LocalDate localDate = LocalDate.now();
        String name = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File f = new File("src/files/" + nomeUtilizador + "/" + name + ".txt");
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
            pathFile = "src/files/" + nomeUtilizador + "/" + name;
            fileName = name + ".txt";
            newFile = true;
            openFileFunction(0);
            // abrir ficheiro na textarea
//            String substring = name.substring(0, name.length() - 4);
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
            }
        }
    }

    // obter data
    @FXML
    private void pickDate1(ActionEvent e) throws IOException {

        if (dateSelectionIndex == 0) {
            fileDate = datePick.getValue();
            if (fileDate != null) {
                String dataFinal = fileDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String path = "src/files/" + nomeUtilizador + "/" + dataFinal + ".txt";
                File f = new File(path);
                // open file to textarea
                if (f.exists()) {
                    // abrir ficheiro na textarea
                    lstFiles.getSelectionModel().select(dataFinal);
                    lstFiles.scrollTo(dataFinal);
                    pathFile = "src/files/" + nomeUtilizador + "/" + dataFinal;
                    fileName = dataFinal + ".txt";
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
                System.out.println(dataInicio);
                String dataFinal = dataInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                file1 = dataFinal + ".txt";
                datePick2.setDisable(false);
                datePick2.setOpacity(1);
            }
        }
    }

    @FXML
    private void pickDate2(ActionEvent e) {
        dataFim = datePick2.getValue();
        if (dataFim != null) {
            String dataFinal = dataFim.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            file2 =dataFinal + ".txt";
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

    @FXML
    private void toPDF(ActionEvent e) {
        try {
            StyleClassedTextArea textArea = (StyleClassedTextArea) tabPane.getSelectionModel().getSelectedItem().getContent();
            String diaryEntry = textArea.getText();
            String name = lstFiles.getSelectionModel().getSelectedItem();
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
            doc.save("src/files/" + nomeUtilizador + "/" + name + ".pdf");
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
        for (int i = 0; i < errosInicio.size(); i++) {
            textArea.setStyleClass(errosInicio.get(i), errosFim.get(i), "wrong-words");
        }
        
        // on tabpane click word postiion show corrections
        tabPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    int pos = textArea.getCaretPosition();
                    for (int i = 0; i < errosInicio.size(); i++) {
                        if (pos >= errosInicio.get(i) && pos <= errosFim.get(i)) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Correção");
                            alert.setHeaderText("Correção");
                            alert.setContentText(correcoes.get(i));
                            alert.showAndWait();
                        }
                    }
                }
            }
        }
        );


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
        File f = new File("src/files/" + nomeUtilizador + "/" + path);
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
        File folder = new File("src/files/" + nomeUtilizador + "/");
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

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> items = FXCollections.observableArrayList();
        File folder = new File("src/files/" + nomeUtilizador + "/");
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

//FALTA: Verificar PDF, verificar search, fix \n bug when backspacing at index 24