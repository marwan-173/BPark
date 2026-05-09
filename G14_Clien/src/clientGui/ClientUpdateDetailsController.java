package clientGui;

import java.util.ArrayList;

import client.ClientUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import common.ClientRequest;
public class ClientUpdateDetailsController {

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtPhone;

    @FXML
    private Label lblStatus;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnBack;

    private String finalCode;
    private String codeType;

    public void setSubscriberCode(String code, String type) {
        this.finalCode = code;
        this.codeType = type;
    }

    @FXML
    void saveDetails(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (email.isEmpty() && phone.isEmpty()) {
            lblStatus.setText("Please fill at least one field.");
            return;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.UPDATE_DETAILS.name());
        request.add(finalCode);
        request.add(codeType);
        request.add(email.isEmpty() ? "null" : email);
        request.add(phone.isEmpty() ? "null" : phone);

        ClientUI.ParkClient.setUpdateDetailsController(this);
        ClientUI.ParkClient.request(request);
    }

    public void displayResult(String message) {
        Platform.runLater(() -> {
            lblStatus.setText(message);

            String lower = message.toLowerCase();

            if (lower.contains("success") || lower.contains("updated")) {
                lblStatus.setTextFill(Color.GREEN);
            } else if (lower.startsWith("error:") || lower.contains("failed") || lower.contains("no fields")) {
                lblStatus.setTextFill(Color.RED);
            } else {
                lblStatus.setTextFill(Color.BLACK);
            }
        });
    }



    @FXML
    void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscribeHomePage.fxml"));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            controller.setUserInfo(ClientHomePageController.currentFullName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
