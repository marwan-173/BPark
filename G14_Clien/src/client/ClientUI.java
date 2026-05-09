package client;

import clientGui.ClientConnectController;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientUI extends Application {
   ClientConnectController clientConnectController;
   public static BParkClient ParkClient;

   public static void main(String[] args) {
      launch(args);
   }

   public void start(Stage primaryStage) throws Exception {
      this.clientConnectController = new ClientConnectController();
      this.clientConnectController.start(primaryStage);
   }
}