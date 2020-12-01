package client.app.javaFX;

import info.FileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    ComboBox<String> diskBox;

    @FXML
    TextField pathField;

    private List<info.FileInfo> list = null;
    private static Client openConnected;
    private boolean isServerPanel = false;
    private String pathFieldServer;

    public void setList(List<info.FileInfo> list) {
        this.list = list;
    }

    public static void setOpenConnected(Client client) {
        openConnected = client;
    }

    public Client getOpenConnected() {
        return openConnected;
    }

    public List<info.FileInfo> getList() {
        return list;
    }

    public boolean isServerPanel() {
        return isServerPanel;
    }

    public void setPathFieldServer(String pathFieldServer) {
        this.pathFieldServer = pathFieldServer;
    }

    public String getPathFieldServer() {
        return pathFieldServer;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(246);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(100);

        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменений");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        diskBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            diskBox.getItems().add(p.toString());
        }
        diskBox.getSelectionModel().select(0);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    if (!isServerPanel) {
                        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFileName());
                        if (Files.isDirectory(path)) {
                            updateList(path);
                        }

                    } else {
                        Path path = Paths.get(pathFieldServer).resolve(filesTable.getSelectionModel().getSelectedItem().getFileName());
                        if (Files.isDirectory(path)) {
                            System.out.println("this directory");
                            cdServer(path.toString());
                        }
                    }

                }

            }


        });

        updateList(Paths.get("."));
    }

    private void cdServer(String msg) {
        if (openConnected.cd(msg)) {
            System.out.println("update");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            openConnected.updateListServer(this);
            updateListClientInServer();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось перейти", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void updateList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void updateListClientInServer() {

        if (list != null) {
            filesTable.getItems().clear();
            filesTable.getItems().addAll(list);
            pathField.setText(pathFieldServer);
            filesTable.sort();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Список файлов не загрузился", ButtonType.OK);
            alert.showAndWait();
        }

    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        if (!isServerPanel) {
            Path upperPath = Paths.get(pathField.getText()).getParent();
            if (upperPath != null) {
                updateList(upperPath);
            }
        } else {
            openConnected.upServer("up");
            openConnected.updateListServer(this);
            updateListClientInServer();
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
        isServerPanel = false;
    }

    public void btnSelectServer(ActionEvent actionEvent) {
        openConnected.updateListServer(this);
        updateListClientInServer();
        isServerPanel = true;
    }

    public String getSelectedFileName() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFileName();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }

    public void closeConnectedClient() {
        openConnected.closeChannel();
    }

    public void copyServer(String path,  String currentPath, PanelController dstPC) {
        try {
            if (openConnected.copyServerFromServer(path, currentPath)) {
                Thread.sleep(100);
                dstPC.updateListClientInServer();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Файл не удалось скопировать", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean downloadOutServer(String path, String fileName, String currentPath) {
        byte[] btf = null;
        btf = openConnected.downloadForServer(path + fileName);
        System.out.println("копирую " + currentPath + fileName);
        if (!Files.exists(Paths.get(currentPath + fileName))) {
            try {
                Files.createFile(Paths.get(currentPath + "\\" + fileName));
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Файл не удалось создать", ButtonType.OK);
                alert.showAndWait();
            }
        }
        if (btf != null) {
            try {
                Files.write(Paths.get(currentPath + "\\" + fileName), btf);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Files isEmpty");
        }
        return false;
    }

    public boolean deleteFromServer(String s) {
        if (openConnected.deleteServer(s)) {
            return true;
        }
        return false;
    }

    public void authAction(String s) throws IOException {
        //openConnected.authSend(s);
    }

    public boolean downloadInServer(String s, String fileName, String currentPath) {
        if (openConnected.downloadInServer(s, fileName, currentPath)) {
            return true;
        }
        return false;
    }
}
