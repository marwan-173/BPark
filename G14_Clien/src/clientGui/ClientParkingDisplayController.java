package clientGui;

import client.ClientUI;
import common.Parking;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.util.ArrayList;
import common.ClientRequest;
public class ClientParkingDisplayController {

    @FXML private TableView<Parking> parkingTable;
    @FXML private TableColumn<Parking, Integer> colParkingCode;
    @FXML private TableColumn<Parking, Integer> colParkingSpace;
    @FXML private TableColumn<Parking, Timestamp> colParkingDate;
    @FXML private TableColumn<Parking, Timestamp> colRetrievalTime;
    @FXML private TableColumn<Parking, Integer> colConfirmationCode;
    @FXML private TableColumn<Parking, Integer> colSubscriberNum;
    @FXML private TableColumn<Parking, Integer> colExtensionCount;
    @FXML private TableColumn<Parking, Integer> colMaxTime;
    @FXML private Button btnBack;

    private String finalCode;
    private String codeType;

    private final ObservableList<Parking> parkingData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("📋 ParkingDisplayController initialized");
        colParkingCode.setCellValueFactory(new PropertyValueFactory<>("parkingCode"));
        colParkingSpace.setCellValueFactory(new PropertyValueFactory<>("parkingSpace"));
        colParkingDate.setCellValueFactory(new PropertyValueFactory<>("parkingDate"));
        colRetrievalTime.setCellValueFactory(new PropertyValueFactory<>("retrievalTime"));
        colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colSubscriberNum.setCellValueFactory(new PropertyValueFactory<>("subscriberNum"));
        colExtensionCount.setCellValueFactory(new PropertyValueFactory<>("extensionCount"));
        colMaxTime.setCellValueFactory(new PropertyValueFactory<>("maxTimeMinutes"));

        parkingTable.setItems(parkingData);
    }

    public void setSubscriberCode(String code, String type) {
        this.finalCode = code;
        this.codeType = type;

        System.out.println("📨 Sending request to display parkings for: " + code + ", type: " + type);

        if (ClientUI.ParkClient != null) {
            ArrayList<String> request = new ArrayList<>();
            request.add(ClientRequest.DisplayParkings.name());
            request.add(finalCode);
            request.add(codeType);

            ClientUI.ParkClient.setParkingDisplayController(this);
            ClientUI.ParkClient.requestFromServer(request);
        } else {
            System.out.println("❌ ClientUI.ParkClient is null!");
        }
    }

    public void displayParkingResponse(String response) {
        System.out.println("✅ Received response from server:\n" + response);

        Platform.runLater(() -> {
            parkingData.clear();

            String[] lines = response.split("\\n");
            for (String line : lines) {
                try {
                    System.out.println("🔄 Parsing line: " + line);
                    String[] details = line.split(",\\s*");
                    if (details.length >= 8) {
                        int parkingCode = Integer.parseInt(details[0].split(":", 2)[1].trim());
                        int space = Integer.parseInt(details[1].split(":", 2)[1].trim());
                        Timestamp parkingDate = Timestamp.valueOf(details[2].split(":", 2)[1].trim());

                        String retrievalTimeStr = details[3].split(":", 2)[1].trim();
                        Timestamp retrievalTime = retrievalTimeStr.equals("null") ? null : Timestamp.valueOf(retrievalTimeStr);

                        int confirmationCode = Integer.parseInt(details[4].split(":", 2)[1].trim());
                        int subscriberNum = Integer.parseInt(details[5].split(":", 2)[1].trim());
                        int extensionCount = Integer.parseInt(details[6].split(":", 2)[1].trim());
                        int maxTime = Integer.parseInt(details[7].split(":", 2)[1].trim());

                        Parking p = new Parking(parkingCode, space, parkingDate, retrievalTime,
                                confirmationCode, subscriberNum, extensionCount, maxTime);
                        parkingData.add(p);
                        System.out.println("✅ Added parking record to table: " + p);
                    } else {
                        System.out.println("⚠️ Skipped line due to insufficient data: " + line);
                    }
                } catch (Exception e) {
                    System.out.println("❗ Error parsing parking record: " + line);
                    e.printStackTrace();
                }
            }

            System.out.println("📦 Total records loaded into table: " + parkingData.size());
        });
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscribeHomePage.fxml"));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            controller.setUserInfo(ClientHomePageController.currentFullName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Home Page");
            stage.show();
        } catch (Exception e) {
            System.out.println("❗ Error loading back to homepage");
            e.printStackTrace();
        }
    }
}
