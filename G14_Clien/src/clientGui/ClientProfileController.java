package clientGui;

import client.ClientUI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.util.ArrayList;
import common.ClientRequest;
public class ClientProfileController {

    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lblPassword;
    @FXML private Label lblSubscriptionCode;
    @FXML private Label lblQuickAccessCode;
    @FXML private Label lblRegistrationDate;
    @FXML private Label lblDelayCount;
    @FXML private Label lblStatus;

    private String subscriberCode;
    private String codeType;

    public void setSubscriberCode(String subscriberCode, String codeType) {
        this.subscriberCode = subscriberCode;
        this.codeType = codeType;

        System.out.println("🔽 Sending profile request:");
        System.out.println("   ➤ subscriberCode: " + subscriberCode);
        System.out.println("   ➤ codeType: " + codeType);

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.GET_CLIENT_PROFILE.name());
        request.add(subscriberCode);
        request.add(codeType);

        if (ClientUI.ParkClient == null) {
            System.out.println("❌ ClientUI.ParkClient is null!");
            return;
        }

        ClientUI.ParkClient.setProfileController(this);
        ClientUI.ParkClient.requestFromServer(request);
    }

    public void displayProfileData(String response) {
        System.out.println("✅ Profile data received: " + response);

        if (!response.startsWith("CLIENT_PROFILE:")) {
            System.out.println("❌ Invalid prefix in response.");
            return;
        }

        String data = response.substring("CLIENT_PROFILE:".length());
        String[] parts = data.split(",");
        System.out.println("📦 Parsed fields count: " + parts.length);

        if (parts.length < 9) {
            System.out.println("❌ Not enough fields in profile data.");
            return;
        }

        Platform.runLater(() -> {
            lblUsername.setText(parts[0]); 
            lblEmail.setText(parts[2]); 
            lblPhone.setText(parts[3]); 
            lblPassword.setText(parts[1]); 
            lblSubscriptionCode.setText(parts[4]);
            lblQuickAccessCode.setText(parts[5]);

            // ✂️ חיתוך התאריך לשם הצגה רק של yyyy-MM-dd
            String rawDate = parts[6];
            String formattedDate = rawDate.contains(" ") ? rawDate.split(" ")[0] : rawDate;
            lblRegistrationDate.setText(formattedDate);

            lblDelayCount.setText(parts[7]);
            lblStatus.setText(parts[8]);
        });

    }


    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscribeHomePage.fxml"));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(subscriberCode, codeType);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Home Page");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
