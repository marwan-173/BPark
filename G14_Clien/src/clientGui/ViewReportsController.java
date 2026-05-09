package clientGui;

import client.ClientUI;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.application.Platform;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import common.ClientRequest;
public class ViewReportsController {

    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<String> yearCombo;
    @FXML private Label errorLabel;

    private String selectedReportType = null;

    /** מאותחל אוטומטית בעת טעינה */
    public void initialize() {
        reportTypeCombo.getItems().addAll("Parking Report", "Subscriber Report");
        monthCombo.getItems().addAll("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");

        int currentYear = Year.now().getValue();
        for (int y = currentYear; y >= currentYear - 5; y--) {
            yearCombo.getItems().add(String.valueOf(y));
        }
    }

    /** בעת לחיצה על כפתור דוח */
    @FXML
    private void handleGenerateReport(ActionEvent event) {
        String type = reportTypeCombo.getValue();
        String month = monthCombo.getValue();
        String year = yearCombo.getValue();

        if (type == null || month == null || year == null) {
            errorLabel.setText("Please select report type, month, and year.");
            return;
        }

        selectedReportType = type; // שמור את סוג הדוח להצגה

        ArrayList<String> request = new ArrayList<>();
        if (type.equals("Parking Report")) {
        	request.add(ClientRequest.GET_PARKING_REPORT.name());
        } else if (type.equals("Subscriber Report")) {
        	request.add(ClientRequest.GET_SUBSCRIBER_REPORT.name());
        } else {
            errorLabel.setText("Unknown report type selected.");
            return;
        }

        request.add(month);
        request.add(year);

        // שליחה לשרת ושמירת קונטרולר
        ClientUI.ParkClient.setReportController(this);
        ClientUI.ParkClient.request(request);

        errorLabel.setText("📤 Report request sent...");
    }
    
    @FXML
    void Exit(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }


    public void displayReportResponse(String response) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader;
                Parent root;
                Stage stage = new Stage();

                if (response.startsWith("PARKING_USAGE_REPORT:")) {
                    loader = new FXMLLoader(getClass().getResource("/clientGui/ParkingTimeReport.fxml"));
                    root = loader.load();

                    ParkingTimeReportController controller = loader.getController();
                    controller.loadTimeReportFromServer(response);

                    stage.setTitle("Parking Report");

                } else if (response.startsWith("SUBSCRIBER_DURATION_REPORT:")) {
                    loader = new FXMLLoader(getClass().getResource("/clientGui/SubscriberReport.fxml"));
                    root = loader.load();

                    SubscriberReportController controller = loader.getController();
                    controller.loadSubscriberData(response);

                    stage.setTitle("Subscriber Report");

                } else {
                    errorLabel.setText("⚠ " + response); // למשל: ERROR: Report available only after month ends.
                    return;
                }

                stage.setScene(new Scene(root));
                stage.show();

                ((Stage) reportTypeCombo.getScene().getWindow()).close();

            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("⚠ Failed to load report view.");
            }
        });
    }




    /** כפתור חזרה */
    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ManagerHomePage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reports Menu");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
