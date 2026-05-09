package clientGui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import client.ClientUI;

import java.io.IOException;
import java.util.ArrayList;
import common.ClientRequest;
public class SubscriberLoginController {

    @FXML
    private TextField txtSubscriptionCode;

    @FXML
    private TextField txtQuickAccessCode;

    @FXML
    private TextField txtUsername;

    @FXML
    private Label lblStatus;

    private String finalCode; // הקוד שיקבע לפי שיטת ההתחברות
    private String codeType;

    @FXML
    void handleLogin(ActionEvent event) {
        String subscriptionCode = txtSubscriptionCode.getText().trim();
        String quickCode = txtQuickAccessCode.getText().trim();
        String username = txtUsername.getText().trim();

        int filled = 0;
        if (!subscriptionCode.isEmpty()) filled++;
        if (!quickCode.isEmpty()) filled++;
        if (!username.isEmpty()) filled++;

        if (filled == 0) {
            lblStatus.setStyle("-fx-text-fill: red;");
            lblStatus.setText("❌ Please fill at least one field.");
            return;
        }

        if (filled > 1) {
            lblStatus.setStyle("-fx-text-fill: red;");
            lblStatus.setText("⚠ Please use only one method to log in.");
            return;
        }

        if (!quickCode.isEmpty()) {
            codeType = "quick";
            finalCode = quickCode;
        } else if (!username.isEmpty()) {
            codeType = "uname";
            finalCode = username;
        } else {
            codeType = "regular";
            finalCode = subscriptionCode;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.CHECK_SUBSCRIBER.name());
        request.add(finalCode);
        request.add(codeType);

        ClientUI.ParkClient.setSubscriberController(this);
        ClientUI.ParkClient.request(request);
    }

    @FXML
    void handleClear(ActionEvent event) {
        txtSubscriptionCode.clear();
        txtQuickAccessCode.clear();
        txtUsername.clear();
        lblStatus.setText("");
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
    

    public void inputResponse(String msg) {
        Platform.runLater(() -> {
            if (msg.startsWith("SUBSCRIBER_FOUND:")) {
                String fullName = msg.substring("SUBSCRIBER_FOUND:".length());
                lblStatus.setStyle("-fx-text-fill: green;");
                lblStatus.setText("✅ Welcome, " + fullName + "!");

                // 🟢 שמירת השם למשתנה סטטי עבור שימוש גלובלי
                ClientHomePageController.currentFullName = fullName;

                loadScene("/clientGui/SubscribeHomePage.fxml");
                return;
            }

            switch (msg) {
                case "SUBSCRIBER_NOT_FOUND":
                    lblStatus.setStyle("-fx-text-fill: red;");
                    lblStatus.setText("❌ Subscriber not found. Please check your code.");
                    break;
                case "SUBSCRIBER_INACTIVE":
                    lblStatus.setStyle("-fx-text-fill: red;");
                    lblStatus.setText("⛔ Your subscription is inactive. Please contact support.");
                    break;
                default:
                    if (msg.startsWith("ERROR:")) {
                        lblStatus.setStyle("-fx-text-fill: orange;");
                        lblStatus.setText("⚠ Server error: " + msg.substring(6));
                    } else {
                        lblStatus.setStyle("-fx-text-fill: orange;");
                        lblStatus.setText("⚠ Unexpected response: " + msg);
                    }
                    break;
            }
        });
    }
   

    private void loadScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType); // לא צריך fullName – כבר קיים בשדה סטטי

            Stage stage = (Stage) txtSubscriptionCode.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Dashboard");
            stage.show();
        } catch (IOException e) {
            lblStatus.setText("Error loading page: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
