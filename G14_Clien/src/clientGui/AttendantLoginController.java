package clientGui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.ArrayList;

import client.ClientUI;
import common.ClientRequest;

public class AttendantLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label lblStatus;

    /** לחצן Login */
    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter both username and password.");
            return;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.ATTENDANT_LOGIN.name());
        request.add(username);
        request.add(password);

        ClientUI.ParkClient.setAttendantController(this);
        ClientUI.ParkClient.request(request);
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/BparkHome.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reports Menu");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inputResponse(String message) {
        Platform.runLater(() -> {
            switch (message) {
                case "ATTENDANT_FOUND":
                    openAttendantHomePage();
                    break;
                case "ERROR: USER_NOT_FOUND":
                    showMessage("User not found.");
                    break;
                case "ERROR: WRONG_PASSWORD":
                    showMessage("Incorrect password.");
                    break;
                default:
                    showMessage("Login error: " + message.replace("ERROR:", "").trim());
            }
        });
    }

    public void openAttendantHomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AttendantHomePage.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Attendant Home");
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
            lblStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            System.out.println("⚠ " + message);
        }
    }
}
