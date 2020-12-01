package client.app.javaFX;

import info.FileInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    @FXML
    StackPane auth;

    @FXML
    TextField login, password;

    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    HBox primaryPanel;

    public void btnExitAction(ActionEvent actionEvent) {
        PanelController panelController = (PanelController) leftPanel.getProperties().get("ctrl");
        panelController.closeConnectedClient();
        Platform.exit();
    }

    public void copyBtnAction(ActionEvent actionEvent) throws InterruptedException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFileName() == null && rightPC.getSelectedFileName() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFileName() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }

        if (rightPC.getSelectedFileName() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        String fileName = srcPC.getSelectedFileName();

        if (srcPC.isServerPanel() & dstPC.isServerPanel()) {
            srcPC.updateListClientInServer();
            dstPC.updateListClientInServer();
            if (!dstPC.getList().isEmpty()) {
                for (FileInfo f : dstPC.getList()) {
                    if (fileName.equals(f.getFileName())) {

                        return;
                    }
                }
                srcPC.copyServer(srcPC.getPathFieldServer() + "\\" + fileName, dstPC.getPathFieldServer() + "\\" + fileName, dstPC);
                System.out.println(srcPC.getPathFieldServer() + "\\" + fileName + " " + dstPC.getPathFieldServer());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            return;
        } else if (srcPC.isServerPanel() & !dstPC.isServerPanel()) {
            System.out.println("download file Server -> Client");
            if (!srcPC.downloadOutServer(srcPC.getPathFieldServer() + "\\", fileName, dstPC.getCurrentPath())) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать", ButtonType.OK);
                alert.showAndWait();
            }
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
            return;

        } else if (!srcPC.isServerPanel() & dstPC.isServerPanel()) {
            System.out.println("download file Client -> Server");
            if (!srcPC.downloadInServer(srcPC.getCurrentPath() + "\\", fileName, dstPC.getPathFieldServer())) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать", ButtonType.OK);
                alert.showAndWait();
            }
            Thread.sleep(1000);
            dstPC.updateListClientInServer();
            return;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFileName());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPC.getSelectedFileName());

        try {
            Files.copy(srcPath, dstPath);
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void deleteBtnAction(ActionEvent actionEvent) {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFileName() == null && rightPC.getSelectedFileName() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            if (leftPC.getSelectedFileName() != null) {
                deleteFileAndDirectory(leftPC);
            } else if (rightPC.getSelectedFileName() != null) {
                deleteFileAndDirectory(rightPC);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось удалить файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void deleteFileAndDirectory(PanelController panelController) throws IOException {
        Path path = Paths.get(panelController.getCurrentPath()).resolve(panelController.getSelectedFileName());
        if (panelController.isServerPanel()) {
            System.out.println("delete " + panelController.getPathFieldServer() + "\\" + panelController.getSelectedFileName());
            if (!panelController.deleteFromServer(panelController.getPathFieldServer() + "\\" + panelController.getSelectedFileName())) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось удалить файл", ButtonType.OK);
                alert.showAndWait();
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                panelController.updateListClientInServer();
            }
        } else {
            Files.delete(path);
            panelController.updateList(Paths.get(panelController.getCurrentPath()));
        }
    }

    public void authAction(ActionEvent actionEvent) throws IOException {
        PanelController PC = (PanelController) leftPanel.getProperties().get("ctrl");
        PC.authAction(login.getText() + " " + password.getText());
        if (PC.getOpenConnected().isAuth()) {
            auth.setVisible(false);
            primaryPanel.setVisible(true);
        }
    }
}
