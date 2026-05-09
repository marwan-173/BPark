package serverGui;

import java.net.InetAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ocsf.server.ConnectionToClient;
import server.BParkServer;
import server.ServerUI;

public class ServerDisplayController implements Initializable {
   private static ServerDisplayController instance;
   @FXML
   private Button connectBtn = null;
   @FXML
   private Button disconnectBtn = null;
   @FXML
   private Button closeBtn = null;
   @FXML
   private TextField port;
   @FXML
   private TextField DBname;
   @FXML
   private TextField DBuser;
   @FXML
   private TextField DBpassword;
   @FXML
   private Label Server_Configuration;
   @FXML
   private Label ip_label;
   @FXML
   private Label port_label;
   @FXML
   private Label DB_label;
   @FXML
   private Label DBuser_label;
   @FXML
   private Label DBpassword_label;
   @FXML
   private Label serverStatusLabel;
   @FXML
   private Label server_ipLabel;
   @FXML
   private Label ClientDetailsLabel;
   @FXML
   private Label HostNameLabel;
   @FXML
   private Label IPAddLabel;
   @FXML
   private TableView<ClientConnInfo> clientTable;
   @FXML
   private TableColumn<ClientConnInfo, String> hostNameColumn;
   @FXML
   private TableColumn<ClientConnInfo, String> ipAddressColumn;
   @FXML
   private TableColumn<ClientConnInfo, String> statusColumn;
   private static ObservableList<ClientConnInfo> clientData;

   public ServerDisplayController() {
      instance = this;
   }

   public static ServerDisplayController getInstance() {
      return instance;
   }

   public void initialize(URL url, ResourceBundle resourceBundle) {
      this.port.setText("5555");
      this.DBname.setText("bpark");
      this.DBuser.setText("root");
      this.DBpassword.setText("Enter database password");

      try {
         String ipAddress = InetAddress.getLocalHost().getHostAddress();
         this.server_ipLabel.setText(ipAddress);
         this.hostNameColumn.setCellValueFactory(new PropertyValueFactory("hostName"));
         this.ipAddressColumn.setCellValueFactory(new PropertyValueFactory("ipAddress"));
         this.statusColumn.setCellValueFactory(new PropertyValueFactory("status"));
         clientData = FXCollections.observableArrayList();
         this.clientTable.setItems(clientData);
      } catch (Exception var4) {
         this.server_ipLabel.setText("Unable to get IP address");
      }

   }

   public void start(Stage primaryStage) throws Exception {
      Parent root = (Parent)FXMLLoader.load(this.getClass().getResource("/serverGui/ServerDisplay.fxml"));
      Scene scene = new Scene(root);
      primaryStage.setTitle("Server menu");
      primaryStage.setScene(scene);
      primaryStage.show();
   }

   @FXML
   public void connect(ActionEvent event) {
      String dbName = this.DBname.getText();
      String dbUser = this.DBuser.getText();
      String dbPassword = this.DBpassword.getText();

      int portNumber;
      try {
         portNumber = Integer.parseInt(this.port.getText());
      } catch (NumberFormatException var7) {
         this.serverStatusLabel.setText("Invalid port number");
         this.serverStatusLabel.setTextFill(Color.RED);
         this.serverStatusLabel.setVisible(true);
         return;
      }

      BParkServer serverInstance = new BParkServer(portNumber, dbName, dbUser, dbPassword);
      serverInstance.setClientDetailsCallback(this::updateClientDetails);
      ServerUI.runServer(portNumber, serverInstance);
      this.serverStatusLabel.setText("Server is listening for connections on port " + portNumber);
      this.serverStatusLabel.setTextFill(Color.GREEN);
      this.serverStatusLabel.setVisible(true);
      this.disableButton(this.connectBtn, true);
      this.disableButton(this.disconnectBtn, false);
   }

   @FXML
   public void disconnect(ActionEvent event) {
      ServerUI.stopServer();
      this.serverStatusLabel.setText("Server disconnected");
      this.serverStatusLabel.setTextFill(Color.RED);
      this.serverStatusLabel.setVisible(true);
      this.disableButton(this.connectBtn, false);
      this.disableButton(this.disconnectBtn, true);
   }

   @FXML
   public void getcloseBtn(ActionEvent event) throws Exception {
      System.exit(0);
   }

   private void disableButton(Button button, boolean disable) {
      button.setDisable(disable);
      button.setOpacity(disable ? 0.5D : 1.0D);
   }

   public void updateClientDetails(String clientHostName, String clientIpAddress) {
      Platform.runLater(() -> {
         ClientConnInfo newClient = new ClientConnInfo(clientHostName, clientIpAddress, "connect");
         clientData.add(newClient);
      });
   }

   public void updateClientDisconnectionDetails(ConnectionToClient conn) {
      String[] details = conn.getInetAddress().toString().split("/");
      Platform.runLater(() -> {
         Iterator var3 = clientData.iterator();

         while(var3.hasNext()) {
            ClientConnInfo c = (ClientConnInfo)var3.next();
            if (c.getHostName().equals(details[0])) {
               c.setStatus("Disconnected");
               this.clientTable.refresh();
            }
         }

      });
   }
}