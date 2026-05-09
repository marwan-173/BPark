package clientGui;

import client.ClientUI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import common.ClientRequest;

public class ViewSubscribeController {

	// 🟢 שדה קלט לחיפוש ID (כעת במקום הנכון)
	@FXML
	private TextField searchIdField;

	// מחלקת תצוגה
	public static class UserSubscriberViewModel {
		private final int id;
		private final String username;
		private final String firstName;
		private final String lastName;
		private final String email;
		private final String phone;
		private final String subscriptionCode;
		private final String quickAccessCode;
		private final String registrationDate;
		private final int delayCount;
		private final String status;

		public UserSubscriberViewModel(int id, String username, String firstName, String lastName, String email,
				String phone, String subscriptionCode, String quickAccessCode, String registrationDate, int delayCount,
				String status) {
			this.id = id;
			this.username = username;
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
			this.phone = phone;
			this.subscriptionCode = subscriptionCode;
			this.quickAccessCode = quickAccessCode;
			this.registrationDate = registrationDate;
			this.delayCount = delayCount;
			this.status = status;
		}

		public int getId() {
			return id;
		}

		public String getUsername() {
			return username;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public String getEmail() {
			return email;
		}

		public String getPhone() {
			return phone;
		}

		public String getSubscriptionCode() {
			return subscriptionCode;
		}

		public String getQuickAccessCode() {
			return quickAccessCode;
		}

		public String getRegistrationDate() {
			return registrationDate;
		}

		public int getDelayCount() {
			return delayCount;
		}

		public String getStatus() {
			return status;
		}
	}

	@FXML
	private TableView<UserSubscriberViewModel> subscriberTable;
	@FXML
	private TableColumn<UserSubscriberViewModel, Integer> colId;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colUsername;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colFirstName;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colLastName;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colEmail;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colPhone;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colSubscriptionCode;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colQuickCode;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colRegDate;
	@FXML
	private TableColumn<UserSubscriberViewModel, Integer> colDelays;
	@FXML
	private TableColumn<UserSubscriberViewModel, String> colStatus;

	private final ObservableList<UserSubscriberViewModel> subscriberData = FXCollections.observableArrayList();

	public void initialize() {
		colId.setCellValueFactory(new PropertyValueFactory<>("id"));
		colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
		colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
		colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
		colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
		colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
		colSubscriptionCode.setCellValueFactory(new PropertyValueFactory<>("subscriptionCode"));
		colQuickCode.setCellValueFactory(new PropertyValueFactory<>("quickAccessCode"));
		colRegDate.setCellValueFactory(new PropertyValueFactory<>("registrationDate"));
		colDelays.setCellValueFactory(new PropertyValueFactory<>("delayCount"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

		subscriberTable.setItems(subscriberData);
	}

	public void loadSubscribers(String subscriptionCode, String codeType) {
		ArrayList<String> request = new ArrayList<>();
		request.add(ClientRequest.DisplaySubscribers.name());
		ClientUI.ParkClient.setViewSubController(this);
		ClientUI.ParkClient.requestFromServer(request);
	}

	@FXML
	private void handleSearchById(ActionEvent event) {
		String input = searchIdField.getText().trim();

		if (input.isEmpty()) {
			subscriberTable.setItems(subscriberData); // הצג את כל הנתונים
			return;
		}

		try {
			int searchId = Integer.parseInt(input);
			ObservableList<UserSubscriberViewModel> filteredList = FXCollections.observableArrayList();

			for (UserSubscriberViewModel sub : subscriberData) {
				if (sub.getId() == searchId) {
					filteredList.add(sub);
					break;
				}
			}

			subscriberTable.setItems(filteredList);

		} catch (NumberFormatException e) {
			System.out.println("⚠ ערך לא חוקי לשדה ID: " + input);
		}
	}

	@FXML
	private void resetSearch(ActionEvent event) {
		searchIdField.clear();
		subscriberTable.setItems(subscriberData);
	}

	@FXML
	private void exitWindow(ActionEvent event) {
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		stage.close();
	}

	public void displaySubscribersResponse(String response) {
		Platform.runLater(() -> {
			subscriberData.clear();
			for (String line : response.split("\\n")) {
				try {
					String[] parts = line.split(",\\s*");
					if (parts.length >= 11) {
						int id = Integer.parseInt(parts[0].split(":", 2)[1].trim());
						String username = parts[1].split(":", 2)[1].trim();
						String firstName = parts[2].split(":", 2)[1].trim();
						String lastName = parts[3].split(":", 2)[1].trim();
						String email = parts[4].split(":", 2)[1].trim();
						String phone = parts[5].split(":", 2)[1].trim();
						String subCode = parts[6].split(":", 2)[1].trim();
						String quickCode = parts[7].split(":", 2)[1].trim();
						String regDate = parts[8].split(":", 2)[1].trim();
						int delays = Integer.parseInt(parts[9].split(":", 2)[1].trim());
						String status = parts[10].split(":", 2)[1].trim();

						UserSubscriberViewModel viewModel = new UserSubscriberViewModel(id, username, firstName,
								lastName, email, phone, subCode, quickCode, regDate, delays, status);
						subscriberData.add(viewModel);
					}
				} catch (Exception e) {
					System.out.println("❗ שגיאה בעת פיענוח שורה: " + line);
					e.printStackTrace();
				}
			}
		});
	}
}