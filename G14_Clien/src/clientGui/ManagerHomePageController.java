package clientGui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import common.ClientRequest;
public class ManagerHomePageController {

    /** צפייה במנויים */
	public void handleViewSubscribers(ActionEvent event) {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ViewSubscribe.fxml"));
	        Parent root = loader.load();

	        clientGui.ViewSubscribeController controller = loader.getController();
	        controller.loadSubscribers("", "");  // אפשר לשלוח פרמטרים אם נדרש

	        Stage stage = new Stage();
	        stage.setScene(new Scene(root));
	        stage.setTitle("Subscribers View");
	        stage.show();

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	/** צפייה בחניות פעילות */
	public void handleViewParking(ActionEvent event) {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ViewParking.fxml"));
	        Parent root = loader.load();

	        clientGui.ViewParkingController controller = loader.getController();
	        controller.loadParkingRecords();

	        Stage stage = new Stage();
	        stage.setScene(new Scene(root));
	        stage.setTitle("Parking Overview");
	        stage.show();

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


    /** צפייה בדוחות */
    public void handleViewReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/ViewReports.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reports");
            stage.show();

        } catch (IOException e) {
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
}
