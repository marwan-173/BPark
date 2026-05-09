package clientGui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ParkingTimeReportController {

    @FXML
    private BarChart<String, Number> parkingChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

   
    @FXML
    public void initialize() {
        // ביטול טווח אוטומטי – חובה כדי לשלוט על ערכי הציר
        yAxis.setAutoRanging(false);

        // קביעת גבולות הציר – שים לב לשנות בהתאם לנתונים שאתה מקבל
        yAxis.setLowerBound(0);  // תמיד להתחיל מ־0
        yAxis.setUpperBound(10); // אפשר לשנות לפי הצורך, או לחשב דינמית
        yAxis.setTickUnit(1);    // שלבים שלמים בלבד
        yAxis.setMinorTickCount(0);

        // תצוגת מספרים כשלמים
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                return Integer.toString(object.intValue());
            }
        });
    }


    public void loadTimeReportFromServer(String response) {
        if (!response.startsWith("PARKING_USAGE_REPORT:")) {
            System.out.println("❌ Invalid report format. Got: " + response);
            return;
        }

        String data = response.substring("PARKING_USAGE_REPORT:".length());

        XYChart.Series<String, Number> parkingCountSeries = new XYChart.Series<>();
        parkingCountSeries.setName("Parking");

        XYChart.Series<String, Number> extensionSeries = new XYChart.Series<>();
        extensionSeries.setName("Extensions");

        XYChart.Series<String, Number> delaySeries = new XYChart.Series<>();
        delaySeries.setName("Delays");

        int maxY = 0; // כדי לקבוע את הגבול העליון של הציר

        for (String row : data.split(";")) {
            if (row.trim().isEmpty()) continue;

            String[] parts = row.split(",");
            if (parts.length != 4) {
                System.out.println("⚠️ Unexpected row format: " + row);
                continue;
            }

            String subscriberLabel = parts[0];
            try {
                int parkingCount = Integer.parseInt(parts[1]);
                int extensions = Integer.parseInt(parts[2]);
                int delays = Integer.parseInt(parts[3]);

                parkingCountSeries.getData().add(new XYChart.Data<>(subscriberLabel, parkingCount));
                extensionSeries.getData().add(new XYChart.Data<>(subscriberLabel, extensions));
                delaySeries.getData().add(new XYChart.Data<>(subscriberLabel, delays));

                // עדכון המקסימום
                maxY = Math.max(maxY, Math.max(parkingCount, Math.max(extensions, delays)));

            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid number in row: " + row);
            }
        }

        // נקה גרף קיים
        xAxis.getCategories().clear();
        parkingChart.getData().clear();

        // הוסף את הסדרות לגרף
        parkingChart.getData().addAll(parkingCountSeries, extensionSeries, delaySeries);

        // 🔁 עדכון גבולות הציר Y לפי המקסימום
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxY + 1); // תוספת קטנה כדי שלא יחתוך את העמודה
        yAxis.setTickUnit(1);
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
