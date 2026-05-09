package clientGui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import common.Reservation;
import client.ClientUI;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import common.ClientRequest;
public class ClientReservationHistoryController {

    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, Integer> colSpace;
    @FXML private TableColumn<Reservation, Timestamp> colStart;
    @FXML private TableColumn<Reservation, Timestamp> colEnd;
    @FXML private TableColumn<Reservation, Integer> colCode;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, Timestamp> colCreatedAt;

    private final ObservableList<Reservation> reservationData = FXCollections.observableArrayList();

    private String finalCode;
    private String codeType;

    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colSpace.setCellValueFactory(new PropertyValueFactory<>("parkingSpace"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("reservationStart"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("reservationEnd"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Callback<TableColumn<Reservation, Timestamp>, TableCell<Reservation, Timestamp>> formatter =
            column -> new TableCell<Reservation, Timestamp>() {
                @Override
                protected void updateItem(Timestamp item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : sdf.format(item));
                }
            };

        colStart.setCellFactory(formatter);
        colEnd.setCellFactory(formatter);
        colCreatedAt.setCellFactory(formatter);

        reservationTable.setItems(reservationData);
    }

    public void loadData(String finalCode, String codeType) {
        this.finalCode = finalCode;
        this.codeType = codeType;

        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.DisplayReservations.name());
        request.add(finalCode);
        request.add(codeType);

        ClientUI.ParkClient.setReservationHistoryController(this);
        ClientUI.ParkClient.requestFromServer(request);
    }

    public void displayReservationResponse(String response) {
        Platform.runLater(() -> {
            reservationData.clear();
            for (String line : response.split("\\n")) {
                try {
                    String[] parts = line.split(",\\s*");
                    if (parts.length >= 7) {
                        int id = Integer.parseInt(parts[0].split(":", 2)[1].trim());
                        int space = Integer.parseInt(parts[1].split(":", 2)[1].trim());
                        String startStr = parts[2].split(":", 2)[1].trim();
                        String endStr = parts[3].split(":", 2)[1].trim();
                        int code = Integer.parseInt(parts[4].split(":", 2)[1].trim());
                        String status = parts[5].split(":", 2)[1].trim();
                        String createdStr = parts[6].split(":", 2)[1].trim();

                        Timestamp start = Timestamp.valueOf(startStr);
                        Timestamp end = Timestamp.valueOf(endStr);
                        Timestamp createdAt = Timestamp.valueOf(createdStr);

                        Reservation r = new Reservation(id, 0, space, start, end, code, status, createdAt);
                        reservationData.add(r);
                    }
                } catch (Exception e) {
                    System.out.println("❗ Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGui/SubscribeHomePage.fxml"));
            Parent root = loader.load();

            ClientHomePageController controller = loader.getController();
            controller.setSubscriberCode(finalCode, codeType);
            controller.setUserInfo(ClientHomePageController.currentFullName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}