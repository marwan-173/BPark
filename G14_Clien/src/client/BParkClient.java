package client;

import common.ChatIF;
import javafx.application.Platform;
import clientGui.SubscriberLoginController;
import clientGui.ViewParkingController;
import clientGui.ViewReportsController;
import clientGui.ViewSubscribeController;
import clientGui.AttendantLoginController;
import clientGui.BparkHomeController;
import clientGui.CancelReservationController;
import clientGui.ClientDropOffController;
import clientGui.ClientParkingDisplayController;
import clientGui.ClientPickUpController;
import clientGui.ClientProfileController;
import clientGui.ClientReservationController;
import clientGui.ClientReservationHistoryController;
import clientGui.ClientUpdateDetailsController;
import clientGui.ManagerLoginController;
import clientGui.RegisterSubscriberController;


import java.io.IOException;
import java.util.ArrayList;

import ocsf.client.AbstractClient;

public class BParkClient extends AbstractClient implements ChatIF {

	public static final int DEFAULT_PORT = 5555;
	private ChatIF clientUI = this;

	// קונטרולרים שמורים
	private SubscriberLoginController subscriberController;
	private AttendantLoginController attendantController;
	private ManagerLoginController managerController;
	private ClientDropOffController dropOffController;
	private ClientReservationController ReservationController;
	private ClientPickUpController PickUpController;
	private ClientParkingDisplayController ParkingDisplayController;
	private ClientReservationHistoryController reservationHistoryController;
	private ClientUpdateDetailsController UpdateDetailsController;
	private RegisterSubscriberController SubscriberController;
	private ViewSubscribeController ViewSubController;
	private ViewParkingController ViewParController;
	private BparkHomeController homeController;
	private ViewReportsController ViewRepoController;
	private ClientProfileController ProfileController;
	private CancelReservationController CancelController;
	
	
	
	public BParkClient(String host, int port) {
		super(host, port);
	}

	public void requestFromServer(Object request) {
		try {
			if (this.isConnected()) {
				this.sendToServer(request);
			} else {
				System.exit(1);
			}
		} catch (IOException e) {
			System.out.println("Error sending request to server: " + e.getMessage());
		}
	}


	@Override
    public void handleMessageFromServer(Object msg) {
        try {
            String message = msg.toString();
            System.out.println("📨 Message received from server: \n" + message);

            // ניתוח תגובה עבור ParkingDisplay
            if (message.contains("parkingCode") && ParkingDisplayController != null) {
                System.out.println("📥 Detected parking data, passing to ParkingDisplayController");
                ParkingDisplayController.displayParkingResponse(message);
                return;
            }
            if (ViewSubController != null) {
            	ViewSubController.displaySubscribersResponse(message);
            }
            if (ViewParController != null) {
            	ViewParController.displayParkingResponse(message);
            }
            
            
            if (reservationHistoryController != null) {
                reservationHistoryController.displayReservationResponse(message);
            }

            // שיגור לשאר הקונטרולרים
            if (subscriberController != null) {
                subscriberController.inputResponse(message);
            }

            if (attendantController != null) {
                attendantController.inputResponse(message);
            }

            if (managerController != null) {
                managerController.inputResponse(message);
            }

            if (ReservationController != null) {
                ReservationController.inputResponse(message);
            }

            if (dropOffController != null) {
                dropOffController.inputResponse(message);
            }

            if (PickUpController != null) {
                PickUpController.inputResponse(message);
            }
         
            if (UpdateDetailsController != null) {
            	UpdateDetailsController.displayResult(message);
            }

            if (SubscriberController != null) {
            	SubscriberController.displayResponse(message);
            }
            if (homeController != null) {
            	homeController.inputResponsechecked(message);
            }
            if (ViewRepoController != null) {
            	ViewRepoController.displayReportResponse(message);
            }
            if (ProfileController != null) {
            	ProfileController.displayProfileData(message);
            }
            if (CancelController != null) {
            	CancelController.inputResponse(message);
            }
            
            
          

        } catch (Exception e) {
            System.out.println("❗ Exception while handling server message:");
            e.printStackTrace();
        }
    }


	@Override
	public void display(String message) {
		System.out.print("> " + message);
	}

	public void quit() {
		try {
			this.closeConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	// Setters
	public void setSubscriberController(SubscriberLoginController controller) {
		this.subscriberController = controller;
	}

	public void setAttendantController(AttendantLoginController controller) {
		this.attendantController = controller;
	}

	public void setManagerController(ManagerLoginController controller) {
		this.managerController = controller;
	}

	public void setReservationController(ClientReservationController controller) {
		this.ReservationController = controller;
	}
	public void setDropOffController(ClientDropOffController controller) {
	    this.dropOffController = controller;
	}
	
	public void setPickUpController(ClientPickUpController controller) {
	    this.PickUpController = controller;
	}
	
	public void setParkingDisplayController(ClientParkingDisplayController controller) {
	    this.ParkingDisplayController = controller;
	}
	public void setUpdateDetailsController(ClientUpdateDetailsController controller) {
	    this.UpdateDetailsController = controller;
	}
	public void setSubscriberController(RegisterSubscriberController controller) {
	    this.SubscriberController = controller;
	}
	
	public void setViewSubController(ViewSubscribeController controller) {
	    this.ViewSubController = controller;
	}
	public void setViewParkingController(ViewParkingController controller) {
	    this.ViewParController = controller;
	}

	public void setHomeController(BparkHomeController controller) {
	    this.homeController = controller;
	}
	public void setReportController(ViewReportsController controller) {
	    this.ViewRepoController = controller;
	}
	public void setProfileController(ClientProfileController controller) {
	    this.ProfileController = controller;
	}
	public void setCancelReservationController(CancelReservationController controller) {
	    this.CancelController = controller;
	}
	
	
	
	
	
	
	
	// Getters (אם תצטרך)
	public SubscriberLoginController getSubscriberController() {
		return subscriberController;
	}

	public AttendantLoginController getAttendantController() {
		return attendantController;
	}

	public ManagerLoginController getManagerController() {
		return managerController;
	}

	public ClientReservationController getReservationController() {
		return ReservationController;
	}
	public void setReservationHistoryController(ClientReservationHistoryController controller) {
	    this.reservationHistoryController = controller;
	}

	
	public void request(ArrayList<String> msg) {
	    try {
	        sendToServer(msg);  // שולח את הבקשה לשרת
	    } catch (Exception e) {
	        System.out.println("Failed to send request to server: " + e.getMessage());
	    }
	}

}
