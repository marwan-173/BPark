package clientGui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import client.ClientUI;
import common.ClientRequest;
public class RegisterSubscriberController {

    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtLastName;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtPhone;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblStatus;

    /** לחצן רישום מנוי */
    @FXML
    void handleRegister(ActionEvent event) {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()
                || username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please fill in all fields.");
            return;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.REGISTER_SUBSCRIBER.name());
        request.add(firstName);
        request.add(lastName);
        request.add(email);
        request.add(phone);
        request.add(username);
        request.add(password);

        ClientUI.ParkClient.setSubscriberController(this);
        ClientUI.ParkClient.request(request);
    }

    
    /** פעולה שמופעלת עם קבלת תגובה מהשרת */
    public void displayResponse(String message) {
        Platform.runLater(() -> {
            lblStatus.setText(message);

            if (message.toLowerCase().contains("success")) {
                lblStatus.setStyle("-fx-text-fill: green;");
            } else {
                lblStatus.setStyle("-fx-text-fill: red;");
            }
        });
    }



    /** לחצן חזרה */
    @FXML
    void handleBack(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
} 
