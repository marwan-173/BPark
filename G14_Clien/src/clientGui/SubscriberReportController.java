package clientGui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class SubscriberReportController {

    @FXML
    private BarChart<String, Number> subscriberChart;

    /**
     * טוען את הנתונים מהשרת ומעדכן את הגרף בהתאם.
     * פורמט חדש: SUBSCRIBER_DURATION_REPORT:John Doe,400,350;Jane Smith,240,220;
     */
    public void loadSubscriberData(String response) {
        if (!response.startsWith("SUBSCRIBER_DURATION_REPORT:")) {
            System.err.println("❌ Invalid report format");
            return;
        }

        String dataPart = response.substring("SUBSCRIBER_DURATION_REPORT:".length());
        String[] entries = dataPart.split(";");

        XYChart.Series<String, Number> allocatedSeries = new XYChart.Series<>();
        allocatedSeries.setName("Allocated Time (min)");

        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual Time (min)");

        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            String[] parts = entry.split(",");
            if (parts.length != 3) continue;

            String fullName = parts[0];
            int allocated, actual;

            try {
                allocated = Integer.parseInt(parts[1]);
                actual = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Skipping invalid entry: " + entry);
                continue;
            }

            allocatedSeries.getData().add(new XYChart.Data<>(fullName, allocated));
            actualSeries.getData().add(new XYChart.Data<>(fullName, actual));
        }

        subscriberChart.getData().clear();
        subscriberChart.getData().addAll(allocatedSeries, actualSeries);
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ViewReports.fxml"));
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
