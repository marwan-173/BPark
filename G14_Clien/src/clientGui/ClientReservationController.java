package clientGui;

import client.ClientUI;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import common.ClientRequest;
public class ClientReservationController {

    @FXML private DatePicker datePickerReservationDate;
    @FXML private TextField txtTime;
    @FXML private Label lblStatus;

    private String finalCode;
    private String codeType;
    

    @FXML
    void clearAllFieldsBtn() {
        datePickerReservationDate.setValue(null);
        txtTime.clear();
        lblStatus.setText("");
    }

    @FXML
    void getBackBtn(ActionEvent event) {
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

    @FXML
    void submitReservationBtn(ActionEvent event) {
        lblStatus.setTextFill(Color.RED);

        if (datePickerReservationDate.getValue() == null || txtTime.getText().trim().isEmpty()) {
            lblStatus.setText("Please select date and time.");
            return;
        }

        LocalDateTime reservationDateTime;
        try {
            LocalDate date = datePickerReservationDate.getValue();
            String[] timeParts = txtTime.getText().trim().split(":");

            if (timeParts.length != 2) {
                lblStatus.setText("Invalid time format. Use HH:MM.");
                return;
            }

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            reservationDateTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
        } catch (Exception e) {
            lblStatus.setText("Error parsing time. Use format HH:MM (e.g. 14:30).");
            return;
        }

        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            lblStatus.setText("Reservation time must be in the future.");
            return;
        }

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.RESERVE_PARKING_AUTO.name());
        request.add(finalCode);
        request.add(codeType);
        request.add(reservationDateTime.toString());

        ClientUI.ParkClient.setReservationController(this);
        ClientUI.ParkClient.request(request);
    }

    public void setSubscriberCode(String code, String type) {
        this.finalCode = code;
        this.codeType = type;
    }

    public void inputResponse(String response) {
        Platform.runLater(() -> {
            lblStatus.setText(response);

            // הפיכת ההודעה לאותיות קטנות כדי לבדוק מילים כלליות
            String lower = response.toLowerCase();

            // אם ההודעה מכילה אחד מהביטויים שמעידים על הצלחה
            if (lower.contains("confirmationcode")) {
                lblStatus.setTextFill(Color.GREEN);
            } else {
                lblStatus.setTextFill(Color.RED);
            }
        });
    }

}
