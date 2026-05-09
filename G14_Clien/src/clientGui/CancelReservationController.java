package clientGui;

import client.ClientUI;
import common.ClientRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;

public class CancelReservationController {

    @FXML
    private TextField txtReservationNumber;

    @FXML
    private Label lblStatus;

    private String finalCode;
    private String codeType;

    @FXML
    void handleCancelReservation(ActionEvent event) {
        lblStatus.setTextFill(Color.RED);

        String reservationIdStr = txtReservationNumber.getText().trim();
        if (reservationIdStr.isEmpty()) {
            lblStatus.setText("Please enter reservation number.");
            return;
        }

        int reservationId;
        try {
            reservationId = Integer.parseInt(reservationIdStr);
        } catch (NumberFormatException e) {
            lblStatus.setText("Reservation ID must be a valid number.");
            return;
        }

        // יצירת בקשה עם קוד מנוי וסוג זיהוי
        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.CancelReservation.name());
        request.add(finalCode);      // קוד זיהוי מנוי
        request.add(codeType);       // סוג קוד (regular / quick / uname)
        request.add(String.valueOf(reservationId)); // מזהה הזמנה

        ClientUI.ParkClient.setCancelReservationController(this);
        ClientUI.ParkClient.requestFromServer(request); // שליחת הבקשה לשרת
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscribeHomePage.fxml"));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSubscriberCode(String code, String type) {
        this.finalCode = code;
        this.codeType = type;
    }

    public void inputResponse(String response) {
        Platform.runLater(() -> {
            lblStatus.setText(response);
            lblStatus.setTextFill(response.contains("successfully") ? Color.GREEN : Color.RED);
        });
    }
}
