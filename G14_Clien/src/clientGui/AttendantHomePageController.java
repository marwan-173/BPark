package clientGui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import common.ClientRequest;

public class AttendantHomePageController {

    @FXML
    private Button btnViewSubscribers;

    @FXML
    private Button btnViewParkedCars;

    @FXML
    private Button btnRegisterSubscriber;

    @FXML
    private Button btnLogout;

    /** כפתור לצפייה במנויים */
    @FXML
    private void handleViewSubscribers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ViewSubscribe.fxml"));
            Parent root = loader.load();

            // קבלת הקונטרולר והפעלת טעינת המנויים
            ViewSubscribeController controller = loader.getController();
            controller.loadSubscribers("", "");  // אם אתה לא מסנן – אפשר לשלוח מחרוזות ריקות

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Subscribers List");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** כפתור לצפייה ברכבים חונים */
    @FXML
    private void handleViewParkedCars(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ViewParking.fxml"));
            Parent root = loader.load();

            // קבלת הקונטרולר וטעינת הנתונים
            clientGui.ViewParkingController controller = loader.getController();
            controller.loadParkingRecords(); // קריאה לשרת

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("View Parked Cars");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** כפתור לרישום מנוי */
    @FXML
    void handleRegisterSubscriber(ActionEvent event) {
        loadScreen("RegisterSubscriber.fxml", "Register New Subscriber");
    }

    /** כפתור יציאה */
    @FXML
    private void getExitBtn(ActionEvent event) {
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
    /** פונקציה משותפת לטעינת מסכים */
    private void loadScreen(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
