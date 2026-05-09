package clientGui;

import client.ClientUI;
import common.ClientRequest;
import common.Parking;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ViewParkingController {

    @FXML private TableView<Parking> parkingTable;
    @FXML private TableColumn<Parking, Integer> colCode;
    @FXML private TableColumn<Parking, Integer> colSpace;
    @FXML private TableColumn<Parking, Timestamp> colStart;
    @FXML private TableColumn<Parking, Timestamp> colEnd;
    @FXML private TableColumn<Parking, Integer> colConfirm;
    @FXML private TableColumn<Parking, Integer> colSubscriber;
    @FXML private TableColumn<Parking, Integer> colExtensions;
    @FXML private TableColumn<Parking, Integer> colMaxTime;

    @FXML private TextField searchSubscriberField; // שדה חיפוש מנוי

    private final ObservableList<Parking> parkingData = FXCollections.observableArrayList();
    private final ObservableList<Parking> originalData = FXCollections.observableArrayList(); // מקור כללי

    public void initialize() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("parkingCode"));
        colSpace.setCellValueFactory(new PropertyValueFactory<>("parkingSpace"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("parkingDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("retrievalTime"));
        colConfirm.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colSubscriber.setCellValueFactory(new PropertyValueFactory<>("subscriberNum"));
        colExtensions.setCellValueFactory(new PropertyValueFactory<>("extensionCount"));
        colMaxTime.setCellValueFactory(new PropertyValueFactory<>("maxTimeMinutes"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        colStart.setCellFactory(col -> new javafx.scene.control.TableCell<Parking, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : sdf.format(item));
            }
        });

        colEnd.setCellFactory(col -> new javafx.scene.control.TableCell<Parking, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : sdf.format(item));
            }
        });

        parkingTable.setItems(parkingData);
        loadParkingRecords();
    }

    public void loadParkingRecords() {
        ArrayList<String> request = new ArrayList<>();
        request.add(ClientRequest.AttendantParking.name());
        ClientUI.ParkClient.setViewParkingController(this);
        ClientUI.ParkClient.requestFromServer(request);
    }

    public void displayParkingResponse(String response) {
        Platform.runLater(() -> {
            parkingData.clear();
            originalData.clear();

            for (String line : response.split("\\n")) {
                try {
                    String[] parts = line.split(",\\s*");
                    if (parts.length >= 8) {
                        int code = Integer.parseInt(parts[0].split(":", 2)[1].trim());
                        int space = Integer.parseInt(parts[1].split(":", 2)[1].trim());
                        Timestamp date = Timestamp.valueOf(parts[2].split(":", 2)[1].trim());
                        String endStr = parts[3].split(":", 2)[1].trim();
                        Timestamp end = endStr.equalsIgnoreCase("null") ? null : Timestamp.valueOf(endStr);
                        int confirm = Integer.parseInt(parts[4].split(":", 2)[1].trim());
                        int subNum = Integer.parseInt(parts[5].split(":", 2)[1].trim());
                        int ext = Integer.parseInt(parts[6].split(":", 2)[1].trim());
                        int maxTime = Integer.parseInt(parts[7].split(":", 2)[1].trim());

                        Parking p = new Parking(code, space, date, end, confirm, subNum, ext, maxTime);
                        parkingData.add(p);
                        originalData.add(p);
                    }
                } catch (Exception e) {
                    System.out.println("❗ Error parsing parking line: " + line);
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleSearchBySubscriber(ActionEvent event) {
        String input = searchSubscriberField.getText().trim();
        if (input.isEmpty()) {
            parkingTable.setItems(originalData);
            return;
        }

        try {
            int searchId = Integer.parseInt(input);
            ObservableList<Parking> filtered = FXCollections.observableArrayList();

            for (Parking p : originalData) {
                if (p.getSubscriberNum() == searchId) {
                    filtered.add(p);
                }
            }

            parkingTable.setItems(filtered);
        } catch (NumberFormatException e) {
            System.out.println("⚠ מספר מנוי לא תקין: " + input);
        }
    }

    @FXML
    private void resetSearch(ActionEvent event) {
        searchSubscriberField.clear();
        parkingTable.setItems(originalData);
    }

    @FXML
    private void exitWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}