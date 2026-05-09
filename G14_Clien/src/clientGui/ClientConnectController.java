package clientGui;

import client.BParkClient;
import client.ClientUI;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import common.ClientRequest;
public class ClientConnectController implements Initializable {
   private String server_connection_data;
   @FXML
   private Button btnConnect;
   @FXML
   private Label conStatus;
   @FXML
   private Button btnExit;
   @FXML
   private TextField txtId;

   public void start(Stage primaryStage) throws Exception {
      Parent root = (Parent)FXMLLoader.load(this.getClass().getResource("/clientGui/ClientConnect.fxml"));
      Scene scene = new Scene(root);
      primaryStage.setTitle("Client Connect");
      primaryStage.setScene(scene);
      primaryStage.show();
   }

   @FXML
   void getExitBtn(ActionEvent event) throws Exception {
      System.exit(0);
   }

   @FXML
   void getBtnConnect(ActionEvent event) throws Exception {
       this.server_connection_data = this.txtId.getText();

       try {
           ClientUI.ParkClient = new BParkClient(this.server_connection_data, 5555);
           ClientUI.ParkClient.openConnection();

           // טען את המסך בצורה תקנית
           FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/BparkHome.fxml"));
           Pane root = loader.load();

           Stage primaryStage = new Stage();
           Scene scene = new Scene(root);
           primaryStage.setTitle("BPARK - Home");
           primaryStage.setScene(scene);
           primaryStage.show();

           // סגור את חלון ההתחברות
           ((Node) event.getSource()).getScene().getWindow().hide();

       } catch (Exception e) {
           this.conStatus.setText("Connection To Server Failed");
           e.printStackTrace();
       }
   }

   public void initialize(URL location, ResourceBundle resources) {
      this.txtId.setText("localhost");
   }
}