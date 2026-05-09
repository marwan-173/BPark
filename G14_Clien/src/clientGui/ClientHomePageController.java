package clientGui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import common.ClientRequest;
public class ClientHomePageController {

    public static String currentFullName = ""; // ✅ שדה סטטי שמכיל את שם המנוי הנוכחי

    private String finalCode;
    private String codeType;

    @FXML
    private Label lblServerMessage;

    @FXML
    private Label lblUserInfo;

    @FXML
    void initialize() {
        lblUserInfo.setText("👋 Welcome, " + currentFullName); // מוצג מיד בעת טעינת המסך
    }

    @FXML
    void reserveParking(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ClientReservationUi.fxml"));
            Parent root = loader.load();
            ClientReservationController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            Stage stage = (Stage) lblUserInfo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reserve Parking");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void dropOffCar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ClientDropOff.fxml"));
            Parent root = loader.load();
            ClientDropOffController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            Stage stage = (Stage) lblUserInfo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Drop Off Car");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void viewParkingHistory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ParkingDisplay.fxml"));
            Parent root = loader.load();
            ClientParkingDisplayController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Parking History");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void viewReservationHistory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ClientReservationHistory.fxml"));
            Parent root = loader.load();
            ClientReservationHistoryController controller = loader.getController();
            controller.loadData(finalCode, codeType);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reservation History");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void pickUpCar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ClientPickUp.fxml"));
            Parent root = loader.load();
            ClientPickUpController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            Stage stage = (Stage) lblUserInfo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pick Up Car");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void updateDetails(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientUpdateDetails.fxml"));
            Parent root = loader.load();
            ClientUpdateDetailsController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            Stage stage = (Stage) lblUserInfo.getScene().getWindow();
            stage.setTitle("Update Details");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    void cancelReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/CancelReservation.fxml"));
            Parent root = loader.load();
            CancelReservationController controller = loader.getController(); // ⬅️ שינוי כאן
            controller.setSubscriberCode(finalCode, codeType);
            Stage stage = (Stage) lblUserInfo.getScene().getWindow();
            stage.setTitle("Cancel Reservation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


	@FXML
	private void viewProfile(ActionEvent event) {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ClientProfile.fxml"));
	        Parent root = loader.load();

	        ClientProfileController controller = loader.getController();
	        controller.setSubscriberCode(finalCode, codeType); // אם צריך לפי מפתח התחברות

	        Stage stage = (Stage) lblUserInfo.getScene().getWindow();
	        stage.setScene(new Scene(root));
	        stage.setTitle("Client Profile");
	        stage.show();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

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


    public void setSubscriberCode(String code, String type) {
        this.finalCode = code;
        this.codeType = type;
    }

    public void setUserInfo(String name) {
        lblUserInfo.setText("👋 Welcome, " + name); // 🟢 מאפשר להציג שם בכל שלב
    }

    public void setServerStatus(String message) {
        lblServerMessage.setText(message);
    }
}
