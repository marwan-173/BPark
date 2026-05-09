package clientGui;

import java.io.IOException;
import java.util.ArrayList;

import client.ClientUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import common.ClientRequest;
public class BparkHomeController {

    @FXML private Label lblServerMessage;
    @FXML private Button btnSubscriber;
    @FXML private Button btnAttendant;
    @FXML private Button btnManager;
    @FXML private Button btnExit;

    private String serverMessage;

    /** כפתור עבור מנוי */
    @FXML
    public void subscriberBtnClicked(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscriberLogin.fxml"));
        loadNextScene(loader, event, "Subscriber Login");

        // כאן תוכל להוסיף גישה לקונטרולר אם נדרש
        clientGui.SubscriberLoginController controller = loader.getController();
        if (ClientUI.ParkClient != null) {
            ClientUI.ParkClient.setSubscriberController(controller);
        } else {
            System.out.println("Not connected to server");
        }
    }

    /** כפתור עבור סדרן */
    @FXML
    public void attendantBtnClicked(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/AttendantLogin.fxml"));
        loadNextScene(loader, event, "Attendant Login");

        clientGui.AttendantLoginController controller = loader.getController();
        if (ClientUI.ParkClient != null) {
            ClientUI.ParkClient.setAttendantController(controller);
        } else {
            System.out.println("Not connected to server");
        }
    }

    /** כפתור עבור מנהל */
    @FXML
    public void managerBtnClicked(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ManagerLogin.fxml"));
        loadNextScene(loader, event, "Manager Login");

        clientGui.ManagerLoginController controller = loader.getController();
        if (ClientUI.ParkClient != null) {
            ClientUI.ParkClient.setManagerController(controller);
        } else {
            System.out.println("Not connected to server");
        }
    }
    
    @FXML
    private Label lblAvailableSpots;

@FXML
private void handleCheckAvailableSpots(ActionEvent event) {
    ArrayList<String> request = new ArrayList<>();
    request.add(ClientRequest.GET_FREE_SPOTS.name());
    ClientUI.ParkClient.setHomeController(this); // חשוב להוסיף את ה־controller הנוכחי לקליינט
    ClientUI.ParkClient.request(request);
}


    
    /** מקבל תשובה מהשרת ומציג בתווית */
    public void inputResponsechecked(String msg) {
        if (msg.startsWith("FREE_SPOTS:")) {
            String count = msg.split(":", 2)[1];
            Platform.runLater(() -> {
                lblAvailableSpots.setText("🅿 Available Spots: " + count);
                lblAvailableSpots.setVisible(true);
                lblAvailableSpots.setManaged(true);
            });
        }
    }

    /** הצגת הודעה מהשרת */
    public void inputResponse(String response) {
        this.serverMessage = response;
        Platform.runLater(() -> lblServerMessage.setText(serverMessage));
    }


    /** טעינת סצנה חדשה */
    private void loadNextScene(FXMLLoader loader, ActionEvent event, String title) throws Exception {
        ((Node) event.getSource()).getScene().getWindow().hide();
        Stage stage = new Stage();
        Pane root = loader.load();
        Scene scene = new Scene(root);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
    
    @FXML
    private void handleExit(ActionEvent event) {
        try {
            if (ClientUI.ParkClient != null) {
                ArrayList<String> disconnectRequest = new ArrayList<>();
                disconnectRequest.add(ClientRequest.DISCONNECT.name()); // ⬅️ שימוש ב־enum במקום "DisConnect"
                ClientUI.ParkClient.sendToServer(disconnectRequest);
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to send disconnect message: " + e.getMessage());
            e.printStackTrace();
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
        System.exit(0);
    }


    @FXML
    public void initialize() {
        if (ClientUI.ParkClient == null || !ClientUI.ParkClient.isConnected()) {
            System.out.println("Client not connected!");
        } else {
            System.out.println("Client is connected.");
        }
    }

}
