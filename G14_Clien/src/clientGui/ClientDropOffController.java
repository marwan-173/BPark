package clientGui;

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

import java.awt.Color;
import java.util.ArrayList;
import common.ClientRequest;
public class ClientDropOffController {

    @FXML
    private TextField txtConfirmationCode;

    @FXML
    private Label lblStatus;

    @FXML
    private Button btnBack;

    @FXML
    private Button btnSubmitDropOff;

    @FXML
    private Button btnClearFields;

    @FXML
    private ComboBox<Integer> comboExtension;

    private String finalCode;
    private String codeType;

    @FXML
    public void initialize() {
        comboExtension.setItems(FXCollections.observableArrayList(0, 1, 2, 3, 4));
        comboExtension.getSelectionModel().selectFirst(); // אפשר לבחור כברירת מחדל 0
    }

    @FXML
    void getBackBtn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscribeHomePage.fxml"));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            controller.setUserInfo(ClientHomePageController.currentFullName); // ✅ הצגת שם המנוי בחזרה

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
        txtConfirmationCode.clear();
        lblStatus.setText("");
        comboExtension.getSelectionModel().clearSelection();
    }

    @FXML
    void dropOffCar() {
        String confirmationCode = txtConfirmationCode.getText().trim();
        if (confirmationCode.isEmpty()) {
            confirmationCode = "0"; // אין קוד
        }

        Integer extension = comboExtension.getValue();
        if (extension == null) {
            extension = 0;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.DROP_OFF_CAR.name());
        request.add(finalCode);
        request.add(codeType);
        request.add(confirmationCode);
        request.add(String.valueOf(extension));

        ClientUI.ParkClient.setDropOffController(this);
        ClientUI.ParkClient.request(request);
    }

    public void inputResponse(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("Success:")) {
                lblStatus.setText(message);
                lblStatus.setStyle("-fx-text-fill: green;");
            } else if (message.startsWith("No reservation")) {
                lblStatus.setText("No matching reservation found for the confirmation code.");
                lblStatus.setStyle("-fx-text-fill: red;");
            } else if (message.startsWith("Error:")) {
                lblStatus.setText("Error: " + message.substring(6));
                lblStatus.setStyle("-fx-text-fill: red;");
            } else {
                lblStatus.setText(message);
                lblStatus.setStyle("-fx-text-fill: black;");
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