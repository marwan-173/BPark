package clientGui;
import common.ClientRequest;  // ✅ תקין מכל צד


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import client.ClientUI;

import java.util.ArrayList;

public class ClientPickUpController {

    @FXML
    private TextField txtParkingCode;

    @FXML
    private Label lblStatus;

    @FXML
    private Button btnBack;

    @FXML
    private Button btnSubmitPickUp;

    @FXML
    private Button btnClearFields;

    @FXML
    private ComboBox<Integer> comboExtension;

    private String finalCode;
    private String codeType;

    @FXML
    public void initialize() {
        comboExtension.setItems(FXCollections.observableArrayList(0,1, 2, 3, 4,5,6,7,8));
        comboExtension.getSelectionModel().selectFirst(); // ברירת מחדל 0
    }

    @FXML
    void getBackBtn(ActionEvent event) {
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

    @FXML
    void clearFieldsBtn() {
        txtParkingCode.clear();
        lblStatus.setText("");
        comboExtension.getSelectionModel().clearSelection();
    }

    @FXML
    void submitPickUpBtn() {
        String parkingCode = txtParkingCode.getText().trim();
        if (parkingCode.isEmpty()) {
            lblStatus.setText("Please enter your parking code.");
            return;
        }

        Integer extension = comboExtension.getValue();
        if (extension == null) {
            extension = 0;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.PICK_UP_CAR.name());
        request.add(finalCode);
        request.add(codeType);
        request.add(parkingCode);
        request.add(String.valueOf(extension));

        ClientUI.ParkClient.setPickUpController(this);
        ClientUI.ParkClient.request(request);
    }
    @FXML
    void handleForgotCode(ActionEvent event) {
        lblStatus.setStyle("-fx-text-fill: blue;");
        lblStatus.setText("⏳ Request is being processed...");

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.SEND_CODE_TO_EMAIL.name());
        request.add(finalCode); // מזהה מנוי
        request.add(codeType);  // "quick", "uname", "regular"

        ClientUI.ParkClient.setPickUpController(this);
        ClientUI.ParkClient.request(request);
    }



    public void inputResponse(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("Success:")) {
                lblStatus.setStyle("-fx-text-fill: green;");
                lblStatus.setText("✅ " + message.substring(8).trim());
            } else if (message.startsWith("Error:")) {
                lblStatus.setStyle("-fx-text-fill: red;");
                lblStatus.setText("❌ " + message.substring(6).trim());
            } else {
                lblStatus.setStyle("-fx-text-fill: orange;");
                lblStatus.setText("⚠️ " + message.trim());
            }
        });
    }


    public void handleServerResponse(String msg) {
        lblStatus.setText(msg);
    }

    public void setSubscriberCode(String code, String type) {
        this.finalCode = code;
        this.codeType = type;
    }
}
