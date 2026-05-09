package server;

import java.io.IOException;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import common.ClientRequest;
import jakarta.mail.MessagingException;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import serverGui.ServerDisplayController;




public class BParkServer extends AbstractServer {
	public static final int DEFAULT_PORT = 5555;
	private static final String DB_URL = "jdbc:mysql://localhost:3306/?serverTimezone=Israel";
	private BiConsumer<String, String> clientDetailsCallback;
	private Runnable clientDisconnectionCallback;
	
	private static Connection conn = null;

	public BParkServer(int port, String dbName, String dbUser, String dbPassword) {
		super(port);
		try {
			String fullDBUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=Asia/Jerusalem";
			conn = DriverManager.getConnection(fullDBUrl, dbUser, dbPassword);
			System.out.println("Connected to database successfully.");
			startReservationCleaner(); // 🔁 התחלת מנגנון ניקוי הזמנות
		} catch (SQLException e) {
			System.out.println("Database connection failed: " + e.getMessage());
		}
	}
	
	

//***********************************************************************************************

	private void startReservationCleaner() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			try {
				cancelExpiredReservations();
			} catch (Exception e) {
				System.err.println("❌ Reservation cleaner error: " + e.getMessage());
			}
		}, 0, 10, TimeUnit.MINUTES);
	}

	private void cancelExpiredReservations() {
		String updateSql = """
				    UPDATE bpark.reservation r
				    SET r.status = 'cancelled'
				    WHERE r.status = 'active'
				      AND r.reservation_start <= ?
				      AND NOT EXISTS (
				          SELECT 1 FROM bpark.parking p
				          WHERE p.confirmation_code = r.confirmation_code
				            AND p.parking_date IS NOT NULL
				      )
				""";

		String selectCancelledSql = """
				    SELECT r.reservation_id
				    FROM bpark.reservation r
				    WHERE r.status = 'cancelled'
				      AND r.reservation_start <= ?
				      AND NOT EXISTS (
				          SELECT 1 FROM bpark.parking p
				          WHERE p.confirmation_code = r.confirmation_code
				            AND p.parking_date IS NOT NULL
				      )
				""";

		try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
				PreparedStatement selectStmt = conn.prepareStatement(selectCancelledSql)) {

			LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
			Timestamp cutoffTimestamp = Timestamp.valueOf(cutoffTime);

			// Execute update
			updateStmt.setTimestamp(1, cutoffTimestamp);
			int updated = updateStmt.executeUpdate();

			// Fetch and print IDs of cancelled reservations
			selectStmt.setTimestamp(1, cutoffTimestamp);
			try (ResultSet rs = selectStmt.executeQuery()) {
				//System.out.println("🔔 הזמנות שבוטלו:");
				while (rs.next()) {
					int reservationId = rs.getInt("reservation_id");
					System.out.println("❌ Reservation ID cancelled: " + reservationId);
				}
			}

			if (updated > 0) {
				//System.out.println("✅ סך הכל בוטלו: " + updated + " הזמנות.");
			} else {
				//System.out.println("ℹ אין הזמנות לבטל בשלב זה.");
			}

		} catch (SQLException e) {
			System.err.println("❌ SQL Error in reservation cleaner: " + e.getMessage());
		}
	}
	//*****************************************************************************
	
	
	
	
	//**********************************************************************************

	public void setClientDetailsCallback(BiConsumer<String, String> callback) {
		this.clientDetailsCallback = callback;
	}

	public void setClientDisconnectionCallback(Runnable callback) {
		this.clientDisconnectionCallback = callback;
	}

	private void sendConnectivityDetailsToClient(ConnectionToClient client) {
		try {
			String clientInfo = "IP Address: " + client.getInetAddress().getHostAddress() + ", Host Name: "
					+ client.getInetAddress().getHostName();
			client.sendToClient(clientInfo);
		} catch (IOException e) {
			System.out.println("Error sending connectivity details: " + e.getMessage());
		}
	}

	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		ServerDisplayController.getInstance().updateClientDisconnectionDetails(client);
		super.clientDisconnected(client);
		if (clientDisconnectionCallback != null)
			clientDisconnectionCallback.run();
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		super.clientConnected(client);
		if (clientDetailsCallback != null) {
			clientDetailsCallback.accept(client.getInetAddress().getHostName(),
					client.getInetAddress().getHostAddress());
		}
	}

	private boolean isNullOrLiteralNull(String value) {
		return value == null || value.equalsIgnoreCase("null");
	}

	// ******************************************************************

	// Server-side method
	private void displayParking(ArrayList<String> list, ConnectionToClient client) {
	    try {
	        String code = list.get(1);
	        String mode = list.get(2);

	        System.out.println("🔍 displayParking called with code = " + code + ", mode = " + mode);

	        // שלב 1: מציאת subscriber_num לפי סוג התחברות
	        String sqlFindSubscriber;
	        switch (mode.toLowerCase()) {
	            case "quick":
	                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE quick_access_code = ?";
	                break;
	            case "uname":
	                sqlFindSubscriber = """
	                    SELECT s.subscriber_num
	                    FROM bpark.subscriber s
	                    JOIN bpark.user u ON s.subscriber_num = u.id
	                    WHERE u.username = ?
	                """;
	                break;
	            default: // regular
	                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE subscription_code = ?";
	        }

	        int subscriberNum;
	        try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
	            stmt.setString(1, code);
	            try (ResultSet rs = stmt.executeQuery()) {
	                if (rs.next()) {
	                    subscriberNum = rs.getInt("subscriber_num");
	                    System.out.println("✅ Found subscriber_num = " + subscriberNum);
	                } else {
	                    System.out.println("❌ Subscriber not found for code: " + code);
	                    client.sendToClient("Error: Subscriber not found");
	                    return;
	                }
	            }
	        }

			// שלב 2: שליפת רשומות חניה
			StringBuilder response = new StringBuilder();
			String query = "SELECT * FROM bpark.parking WHERE subscriber_num = ?";

			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setInt(1, subscriberNum);
				try (ResultSet rs = stmt.executeQuery()) {
					boolean found = false;
					while (rs.next()) {
						found = true;
						String line = String.format(
								"parkingCode: %d, parkingSpace: %d, parkingDate: %s, retrievalTime: %s, confirmationCode: %d, subscriberNum: %d, extensionCount: %d, maxTimeMinutes: %d\n",
								rs.getInt("parking_code"), rs.getInt("parking_space"), rs.getTimestamp("parking_date"),
								rs.getTimestamp("retrieval_time"), rs.getInt("confirmation_code"),
								rs.getInt("subscriber_num"), rs.getInt("extension_count"),
								rs.getInt("max_time_minutes"));
						response.append(line);
						System.out.println("\uD83D\uDCC4 " + line.trim());
					}

					if (!found) {
						System.out.println("\u2139 No parking records found for subscriberNum: " + subscriberNum);
						client.sendToClient("No parking records found for subscriber.");
						return;
					}
				}
			}

			client.sendToClient(response.toString());

		} catch (Exception e) {
			System.out.println("\u2757 Exception in displayParking:");
			e.printStackTrace();
			try {
				client.sendToClient("Error: " + e.getMessage());
			} catch (IOException ex) {
				System.out.println("\u2757 Failed to send error to client:");
				ex.printStackTrace();
			}
		}
	}

	// *********************************
	private void checkSubscriberInDB(ArrayList<String> list, ConnectionToClient client) {
		String code = list.get(1); // יכול להיות subscriptionCode, quickAccessCode, או username
		String codeType = list.get(2); // "regular" / "quick" / "uname"

		String query = null;

		switch (codeType) {
		case "regular":
			query = "SELECT s.status, u.first_name, u.last_name "
					+ "FROM bpark.subscriber s JOIN bpark.user u ON s.subscriber_num = u.id "
					+ "WHERE s.subscription_code = ?";
			break;

		case "quick":
			query = "SELECT s.status, u.first_name, u.last_name "
					+ "FROM bpark.subscriber s JOIN bpark.user u ON s.subscriber_num = u.id "
					+ "WHERE s.quick_access_code = ?";
			break;

		case "uname":
			query = "SELECT s.status, u.first_name, u.last_name "
					+ "FROM bpark.subscriber s JOIN bpark.user u ON s.subscriber_num = u.id " + "WHERE u.username = ?";
			break;

		default:
			try {
				client.sendToClient("ERROR: Invalid code type");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, code);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String status = rs.getString("status");
				String fullName = rs.getString("first_name") + " " + rs.getString("last_name");

				if ("active".equalsIgnoreCase(status)) {
					client.sendToClient("SUBSCRIBER_FOUND:" + fullName);
				} else {
					client.sendToClient("SUBSCRIBER_INACTIVE");
				}
			} else {
				client.sendToClient("SUBSCRIBER_NOT_FOUND");
			}

		} catch (Exception e) {
			try {
				client.sendToClient("ERROR: " + e.getMessage());
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// **********************************************************************************************

	// ===================== UPDATED SERVER-SIDE HANDLER =====================
	// File: server/BParkServer.java (updated for automatic reservation)

	private void reserveParkingAuto(ArrayList<String> list, ConnectionToClient client) {
		try {

			String code = list.get(1);
			String mode = (list.size() > 2 && list.get(2) != null && !list.get(2).trim().isEmpty())
					? list.get(2).trim().toLowerCase()
					: "regular";

			LocalDateTime reservationStart = LocalDateTime.parse(list.get(3));

			// 1. בדיקת טווח תקף (24 שעות עד 7 ימים)
			LocalDateTime now = LocalDateTime.now();
			if (Duration.between(now, reservationStart).toHours() < 24
					|| Duration.between(now, reservationStart).toDays() > 7) {
				client.sendToClient("Error: Reservation must be at least 24 hours in advance and within 7 days.");
				return;
			}

			// 2. מציאת מספר מנוי מהקוד לפי השיטה
			String sqlFindSubscriber;
			switch (mode.toLowerCase()) {
			case "quick":
				sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE quick_access_code = ?";
				break;
			case "uname":
				sqlFindSubscriber = """
						    SELECT s.subscriber_num
						    FROM bpark.subscriber s
						    JOIN bpark.user u ON s.subscriber_num = u.id
						    WHERE u.username = ?
						""";
				break;
			default:
				sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE subscription_code = ?";
			}

			int subscriberNum = -1;
			try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
				stmt.setString(1, code);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						subscriberNum = rs.getInt("subscriber_num");
					} else {
						client.sendToClient("Error: Subscriber not found.");
						return;
					}
				}
			}

			// 3. בדיקת אחוז הזמנות קיימות באותו טווח זמן
			LocalDateTime reservationEnd = reservationStart.plusHours(4);
			int totalSpots = 0;
			int conflictingReservations = 0;

			try (Statement stmt = conn.createStatement()) {
				ResultSet rsTotal = stmt.executeQuery("SELECT COUNT(*) FROM bpark.parking_space");
				if (rsTotal.next())
					totalSpots = rsTotal.getInt(1);
			}

			try (PreparedStatement stmt = conn
					.prepareStatement("SELECT COUNT(*) FROM bpark.reservation WHERE status = 'active' AND "
							+ "((? BETWEEN reservation_start AND reservation_end) OR "
							+ "(? BETWEEN reservation_start AND reservation_end) OR "
							+ "(reservation_start BETWEEN ? AND ?) OR " + "(reservation_end BETWEEN ? AND ?))")) {
				stmt.setTimestamp(1, Timestamp.valueOf(reservationStart));
				stmt.setTimestamp(2, Timestamp.valueOf(reservationEnd));
				stmt.setTimestamp(3, Timestamp.valueOf(reservationStart));
				stmt.setTimestamp(4, Timestamp.valueOf(reservationEnd));
				stmt.setTimestamp(5, Timestamp.valueOf(reservationStart));
				stmt.setTimestamp(6, Timestamp.valueOf(reservationEnd));

				ResultSet rs = stmt.executeQuery();
				if (rs.next())
					conflictingReservations = rs.getInt(1);
			}

			double ratio = (double) conflictingReservations / totalSpots;
			if (ratio >= 0.6) {
				client.sendToClient("Error: Reservation denied. Too many bookings at this time.");
				return;
			}

			// 4. חיפוש מקום פנוי שלא שמור בטווח הזמן
			String findFreeSpaceSql = """
					    SELECT space_number FROM bpark.parking_space
					    WHERE space_number NOT IN (
					        SELECT parking_space FROM bpark.reservation
					        WHERE status = 'active' AND (
					            (? BETWEEN reservation_start AND reservation_end) OR
					            (? BETWEEN reservation_start AND reservation_end) OR
					            (reservation_start BETWEEN ? AND ?) OR
					            (reservation_end BETWEEN ? AND ?)
					        )
					    )
					    LIMIT 1
					""";

			int spaceNum = -1;
			try (PreparedStatement stmt = conn.prepareStatement(findFreeSpaceSql)) {
				stmt.setTimestamp(1, Timestamp.valueOf(reservationStart));
				stmt.setTimestamp(2, Timestamp.valueOf(reservationEnd));
				stmt.setTimestamp(3, Timestamp.valueOf(reservationStart));
				stmt.setTimestamp(4, Timestamp.valueOf(reservationEnd));
				stmt.setTimestamp(5, Timestamp.valueOf(reservationStart));
				stmt.setTimestamp(6, Timestamp.valueOf(reservationEnd));

				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					spaceNum = rs.getInt("space_number");
				} else {
					client.sendToClient("Error: No free spots available for the selected time.");
					return;
				}
			}

			// 5. הכנסת ההזמנה
			int confirmationCode = (int) (Math.random() * 9000) + 1000;
			try (PreparedStatement insertStmt = conn.prepareStatement(
					"INSERT INTO bpark.reservation (subscriber_num, parking_space, reservation_start, confirmation_code) "
							+ "VALUES (?, ?, ?, ?)")) {
				insertStmt.setInt(1, subscriberNum);
				insertStmt.setInt(2, spaceNum);
				insertStmt.setTimestamp(3, Timestamp.valueOf(reservationStart));
				insertStmt.setInt(4, confirmationCode);

				int rows = insertStmt.executeUpdate();
				if (rows == 0) {
					client.sendToClient("Error: Failed to insert reservation.");
					return;
				}
			}

			client.sendToClient( "your confirmationCode: " + confirmationCode);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.sendToClient("Error: " + e.getMessage());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	// **************************************************************************
	private void dropOffCar(ArrayList<String> list, ConnectionToClient client) {
		try {
			System.out.println("🔧 dropOffCar called with: " + list);

			if (list.size() < 5) {
				client.sendToClient(
						"Error: Missing parameters. Expected format: [DROP_OFF_CAR, subscriptionCode, codeType, confirmationCode, extensionHours]");
				return;
			}

			String code = list.get(1); // יכול להיות subscriptionCode / quickCode / username
			String mode = list.get(2); // regular / quick / uname
			String confirmation = list.get(3); // confirmationCode
			int extensionHours = Integer.parseInt(list.get(4));

			conn.setAutoCommit(false);

			int spaceNum = -1;
			int subscriberNum = -1;
			LocalDateTime now = LocalDateTime.now();
			int baseMinutes = 240;
			int maxTimeMinutes = baseMinutes + (extensionHours * 60);
			LocalDateTime dropStart = now;
			LocalDateTime dropEnd = now.plusMinutes(maxTimeMinutes);
			int confirmation_Code = (int) (Math.random() * 9000) + 1000;
            int flag = 0 ;
			// 1. מציאת מספר מנוי לפי שיטת התחברות
			String sqlFindSubscriber;

			switch (mode.toLowerCase()) {
			case "quick":
				sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE quick_access_code = ?";
				break;
			case "uname":
				sqlFindSubscriber = """
						    SELECT s.subscriber_num
						    FROM bpark.subscriber s
						    JOIN bpark.user u ON s.subscriber_num = u.id
						    WHERE u.username = ?
						""";
				break;
			default: // regular
				sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE subscription_code = ?";
			}

			try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
				stmt.setString(1, code);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						subscriberNum = rs.getInt("subscriber_num");
						System.out.println("👤 Found subscriber_num = " + subscriberNum);
					} else {
						client.sendToClient("Error: Subscriber not found.");
						conn.rollback();
						return;
					}
				}
			}


			if (!confirmation.equals("0")) { 
				 flag = 1;
				//מקרה עם קוד הזמנה
				try (PreparedStatement stmt = conn
						.prepareStatement("SELECT parking_space, reservation_start FROM bpark.reservation "
								+ "WHERE confirmation_code = ? AND subscriber_num = ?")) {
					stmt.setInt(1, Integer.parseInt(confirmation));
					stmt.setInt(2, subscriberNum);
					try (ResultSet rs = stmt.executeQuery()) {
						if (rs.next()) {
							Timestamp reservationStartTS = rs.getTimestamp("reservation_start");
							spaceNum = rs.getInt("parking_space");
							LocalDateTime reservationStart = reservationStartTS.toLocalDateTime();

							if (now.isBefore(reservationStart) || now.isAfter(reservationStart.plusMinutes(15))) {

								client.sendToClient(
										"Error: Arrival time invalid. You must arrive within 15 minutes of the reservation start.");
								conn.rollback();
								return;
							}

							// בדיקה אם הארכה מתנגשת עם הזמנה עתידית
							if (extensionHours > 0) {
								try (PreparedStatement check = conn
										.prepareStatement("SELECT 1 FROM bpark.reservation WHERE parking_space = ? AND "
												+ "status = 'active' AND reservation_start < ? AND reservation_end > ?")) {
									check.setInt(1, spaceNum);
									check.setTimestamp(2, Timestamp.valueOf(dropEnd));
									check.setTimestamp(3, Timestamp.valueOf(dropStart));
									try (ResultSet clash = check.executeQuery()) {
										if (clash.next()) {
											System.out.println(
													"⚠ Extension conflicts with future reservation. Dropping extension.");
											extensionHours = 0;
											maxTimeMinutes = baseMinutes;
											dropEnd = now.plusMinutes(maxTimeMinutes);
										}
									}
								}
							}
						} else {
							client.sendToClient("Error: No reservation found for confirmation code.");
							conn.rollback();
							return;
						}
					}
				}
			} else {
				// מקרה ללא קוד הזמנה → חיפוש מקום שלא תפוס ולא שמור בטווח הארכה
				
				String findSpot = "SELECT ps.space_number FROM bpark.parking_space ps "
						+ "WHERE ps.is_occupied = FALSE AND ps.space_number NOT IN ("
						+ "SELECT r.parking_space FROM bpark.reservation r "
						+ "WHERE r.status = 'active' AND r.reservation_start < ? AND r.reservation_end > ?) "
						+ "LIMIT 1";
				try (PreparedStatement stmt = conn.prepareStatement(findSpot)) {
					stmt.setTimestamp(1, Timestamp.valueOf(dropEnd));
					stmt.setTimestamp(2, Timestamp.valueOf(dropStart));
					try (ResultSet rs = stmt.executeQuery()) {
						if (rs.next()) {
							spaceNum = rs.getInt("space_number");
							System.out.println("✅ Assigned free spot with no conflict: " + spaceNum);
						} else {
							client.sendToClient("Error: No parking spot available for requested extension period.");
							conn.rollback();
							return;
						}
					}
				}
			}

			// עדכון המקום לתפוס
			try (PreparedStatement updateStmt = conn
					.prepareStatement("UPDATE bpark.parking_space SET is_occupied = TRUE WHERE space_number = ?")) {
				updateStmt.setInt(1, spaceNum);
				if (updateStmt.executeUpdate() == 0) {
					client.sendToClient("Error: Failed to update parking space.");
					conn.rollback();
					return;
				}
			}

			// הכנסת לרשומת parking
			try (PreparedStatement insertParking = conn.prepareStatement(
					"INSERT INTO bpark.parking (parking_space, parking_date, confirmation_code, subscriber_num, extension_count, max_time_minutes) "
							+ "VALUES (?, ?, ?, ?, ?, ?)")) {
				insertParking.setInt(1, spaceNum);
				insertParking.setTimestamp(2, Timestamp.valueOf(now));
				insertParking.setInt(3, confirmation.equals("0") ? confirmation_Code : Integer.parseInt(confirmation));
				insertParking.setInt(4, subscriberNum);
				insertParking.setInt(5, extensionHours);
				insertParking.setInt(6, maxTimeMinutes);

				insertParking.executeUpdate();
				System.out.println("📋 Inserted to bpark.parking: spot=" + spaceNum + ", extension=" + extensionHours);
			}
			
			
			int thecode = confirmation_Code ;
			if (!conn.getAutoCommit())
				conn.commit();
			if (flag==0) {
			client.sendToClient("Success: Car dropped off " + " your Code: " + thecode);
			 }
			if (flag==1) {
				client.sendToClient("Success: drop off");
				 }
		} catch (Exception e) {
			try {
				if (conn != null && !conn.getAutoCommit())
					conn.rollback();
				client.sendToClient("Error: " + e.getMessage());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// **********************************************************************
	// Server-side method for PICK_UP_CAR
	private void pickUpCar(ArrayList<String> list, ConnectionToClient client) {
	    try {
	        System.out.println("🚗 pickUpCar called with: " + list);

	        if (list.size() < 5) {
	            client.sendToClient("Error: Missing parameters. Expected: [PICK_UP_CAR, code, codeType, parkingCode, extension]");
	            return;
	        }

	        String code = list.get(1);
	        String mode = list.get(2); // regular / quick / uname
	        String parkingCodeStr = list.get(3);
	        int requestedExtension = Integer.parseInt(list.get(4));

	        conn.setAutoCommit(false);
	        int subscriberNum = -1;
	        String subscriberEmail = "";

	        String sqlFindSubscriber;
	        switch (mode.toLowerCase()) {
	            case "quick" -> sqlFindSubscriber = """
	                SELECT s.subscriber_num, u.email
	                FROM bpark.subscriber s
	                JOIN bpark.user u ON s.subscriber_num = u.id
	                WHERE s.quick_access_code = ?
	            """;
	            case "uname" -> sqlFindSubscriber = """
	                SELECT s.subscriber_num, u.email
	                FROM bpark.subscriber s
	                JOIN bpark.user u ON s.subscriber_num = u.id
	                WHERE u.username = ?
	            """;
	            default -> sqlFindSubscriber = """
	                SELECT s.subscriber_num, u.email
	                FROM bpark.subscriber s
	                JOIN bpark.user u ON s.subscriber_num = u.id
	                WHERE s.subscription_code = ?
	            """;
	        }


	        try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
	            stmt.setString(1, code);
	            try (ResultSet rs = stmt.executeQuery()) {
	                if (rs.next()) {
	                    subscriberNum = rs.getInt("subscriber_num");
	                    subscriberEmail = rs.getString("email");
	                    System.out.println("👤 Found subscriber_num = " + subscriberNum);
	                } else {
	                    client.sendToClient("Error: Subscriber not found.");
	                    conn.rollback();
	                    return;
	                }
	            }
	        }

	        // שלב 2: מציאת חניה פעילה
	        String getParkingQuery = "SELECT * FROM bpark.parking WHERE confirmation_code = ? AND subscriber_num = ? AND retrieval_time IS NULL";
	        PreparedStatement parkStmt = conn.prepareStatement(getParkingQuery);
	        parkStmt.setInt(1, Integer.parseInt(parkingCodeStr));
	        parkStmt.setInt(2, subscriberNum);
	        ResultSet parkRs = parkStmt.executeQuery();

	        if (!parkRs.next()) {
	            client.sendToClient("Error: No active parking record found.");
	            conn.rollback();
	            return;
	        }

	        Timestamp parkingDate = parkRs.getTimestamp("parking_date");
	        int maxMinutes = parkRs.getInt("max_time_minutes");
	        int currentExtension = parkRs.getInt("extension_count");
	        int parkingCode = parkRs.getInt("parking_code");
	        int parkingSpace = parkRs.getInt("parking_space");

	        Timestamp now = new Timestamp(System.currentTimeMillis());
	        Timestamp expectedRetrievalTime = new Timestamp(parkingDate.getTime() + maxMinutes * 60L * 1000);

	        boolean wasLate = false;
	        int delayCount = 0;

	        // שלב 3: בדיקת איחור
	        if (now.after(expectedRetrievalTime)) {
	            wasLate = true;
	            long lateMillis = now.getTime() - expectedRetrievalTime.getTime();
	            int requiredExtension = (int) Math.ceil(lateMillis / (60.0 * 60 * 1000));

	            if (requestedExtension < requiredExtension) {
	                client.sendToClient("Error: You are late by " + requiredExtension + "h. Please extend at least that much.");
	                conn.rollback();
	                return;
	            }

	            // עדכון מונה איחורים
	            PreparedStatement delayStmt = conn.prepareStatement("UPDATE bpark.subscriber SET delay_count = delay_count + 1 WHERE subscriber_num = ?");
	            delayStmt.setInt(1, subscriberNum);
	            delayStmt.executeUpdate();

	            // שליפת מונה עדכני
	            ResultSet delayRs = conn.createStatement().executeQuery("SELECT delay_count FROM bpark.subscriber WHERE subscriber_num = " + subscriberNum);
	            if (delayRs.next()) {
	                delayCount = delayRs.getInt(1);
	                if (delayCount >= 3) {
	                    conn.createStatement().executeUpdate("UPDATE bpark.subscriber SET status = 'inactive' WHERE subscriber_num = " + subscriberNum);
	                    System.out.println("⚠ Subscriber " + subscriberNum + " set to inactive due to delays.");
	                }
	            }

	            // עדכון זמן חניה
	            int newMaxTime = maxMinutes + (requestedExtension * 60);
	            PreparedStatement maxTimeStmt = conn.prepareStatement("UPDATE bpark.parking SET max_time_minutes = ? WHERE parking_code = ?");
	            maxTimeStmt.setInt(1, newMaxTime);
	            maxTimeStmt.setInt(2, parkingCode);
	            maxTimeStmt.executeUpdate();

	            System.out.println("🔁 Extended max_time_minutes to: " + newMaxTime);

	            // שליחת מייל אזהרה
	            String warningText = "You were late to pick up your car. This is warning number " + delayCount + ". Please avoid delays.";
	            EmailSender.sendLateWarning(subscriberEmail, warningText);

	            // הוספת הודעה למערכת
	            PreparedStatement msgStmt = conn.prepareStatement("""
	                INSERT INTO bpark.message (msg_to, msg_type, message)
	                VALUES (?, 'Late Alert', ?)
	            """);
	            msgStmt.setInt(1, subscriberNum);
	            msgStmt.setString(2, warningText);
	            msgStmt.executeUpdate();
	        }

	        // שלב 4: עדכון קבלת רכב
	        PreparedStatement updateStmt = conn.prepareStatement(
	            "UPDATE bpark.parking SET retrieval_time = ?, extension_count = extension_count + ? WHERE parking_code = ?"
	        );
	        updateStmt.setTimestamp(1, now);
	        updateStmt.setInt(2, requestedExtension);
	        updateStmt.setInt(3, parkingCode);
	        updateStmt.executeUpdate();

	        // שלב 5: שחרור מקום
	        PreparedStatement freeStmt = conn.prepareStatement("UPDATE bpark.parking_space SET is_occupied = FALSE WHERE space_number = ?");
	        freeStmt.setInt(1, parkingSpace);
	        freeStmt.executeUpdate();

	        conn.commit();
	        System.out.println("✅ Car pickup complete. Spot released: " + parkingSpace);
	        client.sendToClient("Success: Car pick-up confirmed. Retrieval time recorded." + (wasLate ? " You were late and received warning #" + delayCount : ""));

	    } catch (Exception e) {
	        e.printStackTrace();
	        try {
	            client.sendToClient("Error: " + e.getMessage());
	            conn.rollback();
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }
	    }
	}


	// **********************************************************************

	private void displayReservations(ArrayList<String> list, ConnectionToClient client) {
	    try {
	        String code = list.get(1);
	        String mode = list.get(2);

	        System.out.println("📥 displayReservations called with code = " + code + ", mode = " + mode);

	        // 1. מציאת מספר מנוי לפי שיטת התחברות
	        String sqlFindSubscriber;
	        switch (mode.toLowerCase()) {
	            case "quick":
	                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE quick_access_code = ?";
	                break;
	            case "uname":
	                sqlFindSubscriber = """
	                    SELECT s.subscriber_num
	                    FROM bpark.subscriber s
	                    JOIN bpark.user u ON s.subscriber_num = u.id
	                    WHERE u.username = ?
	                """;
	                break;
	            default: // regular
	                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE subscription_code = ?";
	        }

	        int subscriberNum;
	        try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
	            stmt.setString(1, code);
	            try (ResultSet rs = stmt.executeQuery()) {
	                if (rs.next()) {
	                    subscriberNum = rs.getInt("subscriber_num");
	                    System.out.println("✅ Found subscriber_num = " + subscriberNum);
	                } else {
	                    client.sendToClient("Error: Subscriber not found");
	                    return;
	                }
	            }
	        }

			StringBuilder response = new StringBuilder();
			String query = "SELECT * FROM bpark.reservation WHERE subscriber_num = ?";

			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setInt(1, subscriberNum);
				try (ResultSet rs = stmt.executeQuery()) {
					boolean found = false;
					while (rs.next()) {
						found = true;
						String line = String.format(
								"reservationId: %d, parkingSpace: %d, startTime: %s, endTime: %s, confirmationCode: %d, status: %s, createdAt: %s\n",
								rs.getInt("reservation_id"), rs.getInt("parking_space"),
								rs.getTimestamp("reservation_start").toString(),
								rs.getTimestamp("reservation_end").toString(), rs.getInt("confirmation_code"),
								rs.getString("status"), rs.getTimestamp("created_at").toString());
						response.append(line);
						System.out.println("📄 " + line.trim());
					}
					if (!found) {
						client.sendToClient("No reservation records found for subscriber.");
						return;
					}
				}
			}

			client.sendToClient(response.toString());

		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.sendToClient("Error: " + e.getMessage());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	// **********************************************************************

	private void UpadateDetails(ArrayList<String> list, ConnectionToClient client) {
	    try {
	        String code = list.get(1);
	        String mode = list.get(2);
	        String email = list.get(3);
	        String phone = list.get(4);

	        System.out.println("📥 UpdateDetails called with code = " + code + ", mode = " + mode);

	        // שלב 1: מציאת מספר מנוי לפי שיטת התחברות
	        String sqlFindSubscriber;
	        switch (mode.toLowerCase()) {
	            case "quick":
	                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE quick_access_code = ?";
	                break;
	            case "uname":
	                sqlFindSubscriber = """
	                    SELECT s.subscriber_num
	                    FROM bpark.subscriber s
	                    JOIN bpark.user u ON s.subscriber_num = u.id
	                    WHERE u.username = ?
	                """;
	                break;
	            default: // regular
	                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE subscription_code = ?";
	        }

	        int subscriberNum;
	        try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
	            stmt.setString(1, code);
	            try (ResultSet rs = stmt.executeQuery()) {
	                if (rs.next()) {
	                    subscriberNum = rs.getInt("subscriber_num");
	                    System.out.println("✅ Found subscriber_num = " + subscriberNum);
	                } else {
	                    client.sendToClient("Error: Subscriber not found.");
	                    return;
	                }
	            }
	        }


			// שלב 2: בניית שאילתת UPDATE דינמית לטבלת user
			StringBuilder updateSql = new StringBuilder("UPDATE bpark.user SET ");
			ArrayList<Object> params = new ArrayList<>();

			if (!"null".equalsIgnoreCase(email)) {
				updateSql.append("email = ?, ");
				params.add(email);
			}
			if (!"null".equalsIgnoreCase(phone)) {
				updateSql.append("phone_number = ?, ");
				params.add(phone);
			}

			// אם אין מה לעדכן
			if (params.isEmpty()) {
				client.sendToClient("No fields to update.");
				return;
			}

			// הסרת פסיק אחרון והוספת WHERE
			updateSql.setLength(updateSql.length() - 2);
			updateSql.append(" WHERE id = ?");
			params.add(subscriberNum); // id בטבלת user

			// שלב 3: ביצוע העדכון
			try (PreparedStatement stmt = conn.prepareStatement(updateSql.toString())) {
				for (int i = 0; i < params.size(); i++) {
					stmt.setObject(i + 1, params.get(i));
				}

				int rows = stmt.executeUpdate();
				if (rows > 0) {
					client.sendToClient("Details updated successfully.");
					System.out.println("✅ Updated user.id = " + subscriberNum);
				} else {
					client.sendToClient("Update failed.");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.sendToClient("Error: " + e.getMessage());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	// **********************************************************************
	private void RegisterSubscriber(ArrayList<String> list, ConnectionToClient client) {
		try {
			String firstName = list.get(1);
			String lastName = list.get(2);
			String email = list.get(3);
			String phone = list.get(4);
			String username = list.get(5);
			String password = list.get(6);

			// בדיקה אם המשתמש כבר קיים
			String checkQuery = "SELECT * FROM bpark.user WHERE username = ? OR email = ?";
			try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
				checkStmt.setString(1, username);
				checkStmt.setString(2, email);
				try (ResultSet rs = checkStmt.executeQuery()) {
					if (rs.next()) {
						client.sendToClient("ERROR: Username or Email already exists.");
						return;
					}
				}
			}

			// הכנסת משתמש חדש
			String insertUser = "INSERT INTO bpark.user (username, password, first_name, last_name, email, phone_number, user_type) VALUES (?, ?, ?, ?, ?, ?, 'subscriber')";
			int generatedUserId;

			try (PreparedStatement userStmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
				userStmt.setString(1, username);
				userStmt.setString(2, password);
				userStmt.setString(3, firstName);
				userStmt.setString(4, lastName);
				userStmt.setString(5, email);
				userStmt.setString(6, phone);

				int affectedRows = userStmt.executeUpdate();
				if (affectedRows == 0) {
					client.sendToClient("ERROR: Failed to insert user.");
					return;
				}

				try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						generatedUserId = generatedKeys.getInt(1);
					} else {
						client.sendToClient("ERROR: Failed to get user ID.");
						return;
					}
				}
			}

			// יצירת קוד מנוי וקוד גישה מהירה
			String subscriptionCode = "S" + generatedUserId;
			String quickAccessCode = "Q" + (int) (Math.random() * 1000000);

			String insertSubscriber = "INSERT INTO bpark.subscriber (subscriber_num, subscription_code, quick_access_code) VALUES (?, ?, ?)";
			try (PreparedStatement subStmt = conn.prepareStatement(insertSubscriber)) {
				subStmt.setInt(1, generatedUserId);
				subStmt.setString(2, subscriptionCode);
				subStmt.setString(3, quickAccessCode);
				subStmt.executeUpdate();
			}

			client.sendToClient("REGISTER_SUCCESS      Subscription Code: " + subscriptionCode + "      Quick Access Code: "
					+ quickAccessCode);
			System.out.println("✅ New subscriber registered: " + username);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.sendToClient("ERROR: " + e.getMessage());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

//*****************************************************************************************
	
	//*****************************************************************************************

		
		private void DisplaySubscribers(ArrayList<String> list, ConnectionToClient client) {
		    try {
		        // אם אתה משתמש בפרמטרים – תוכל להפעיל עליהם סינון עתידי
		        // String subscriptionCode = list.get(1);
		        // String codeType = list.get(2);
			                
		        String query = """
		                SELECT u.id, u.username, u.first_name, u.last_name, u.email, u.phone_number,
		                       s.subscription_code, s.quick_access_code, s.registration_date,
		                       s.delay_count, s.status
		                FROM bpark.user u
		                JOIN bpark.subscriber s ON u.id = s.subscriber_num
		                """;

		        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
		            ResultSet rs = pstmt.executeQuery();

		            StringBuilder response = new StringBuilder("DisplaySubscribersResponse\n");

		            while (rs.next()) {
		                response.append("id: ").append(rs.getInt("id")).append(", ");
		                response.append("username: ").append(rs.getString("username")).append(", ");
		                response.append("firstName: ").append(rs.getString("first_name")).append(", ");
		                response.append("lastName: ").append(rs.getString("last_name")).append(", ");
		                response.append("email: ").append(rs.getString("email")).append(", ");
		                response.append("phone: ").append(rs.getString("phone_number")).append(", ");
		                response.append("subscriptionCode: ").append(rs.getString("subscription_code")).append(", ");
		                response.append("quickAccessCode: ").append(rs.getString("quick_access_code")).append(", ");
		                response.append("registrationDate: ").append(rs.getDate("registration_date")).append(", ");
		                response.append("delayCount: ").append(rs.getInt("delay_count")).append(", ");
		                response.append("status: ").append(rs.getString("status")).append("\n");
		            }

		            client.sendToClient(response.toString());

		        } catch (Exception e) {
		            client.sendToClient("DisplaySubscribersResponse\nError: Failed to retrieve data from database.");
		            e.printStackTrace();
		        }

		    } catch (Exception e) {
		        System.out.println("❗ Error in DisplaySubscribers:");
		        e.printStackTrace();
		    }
		}
	//*****************************************************************************************

	//*****************************************************************************************

		private void AttendantPark(ArrayList<String> request, ConnectionToClient client) {
		    StringBuilder response = new StringBuilder("DisplayParkingResponse\n");

		    try {
		        PreparedStatement stmt = conn.prepareStatement(
		            "SELECT parking_code, parking_space, parking_date, retrieval_time, " +
		            "confirmation_code, subscriber_num, extension_count, max_time_minutes " +
		            "FROM bpark.parking"
		        );
		        ResultSet rs = stmt.executeQuery();

		        while (rs.next()) {
		            int code = rs.getInt("parking_code");
		            int space = rs.getInt("parking_space");
		            Timestamp date = rs.getTimestamp("parking_date");
		            Timestamp retrieval = rs.getTimestamp("retrieval_time");
		            int confirm = rs.getInt("confirmation_code");
		            int subNum = rs.getInt("subscriber_num");
		            int extensions = rs.getInt("extension_count");
		            int max = rs.getInt("max_time_minutes");

		            response.append(String.format(
		                "parkingCode: %d, parkingSpace: %d, parkingDate: %s, retrievalTime: %s, " +
		                "confirmationCode: %d, subscriberNum: %d, extensionCount: %d, maxTimeMinutes: %d\n",
		                code, space, date.toString(),
		                (retrieval != null ? retrieval.toString() : "null"),
		                confirm, subNum, extensions, max
		            ));
		        }

		        rs.close();
		        stmt.close();

		        client.sendToClient(response.toString());

		    } catch (Exception e) {
		        System.out.println("❗ Error in AttendantPark:");
		        e.printStackTrace();
		        try {
		            client.sendToClient("DisplayParkingResponse\nERROR");
		        } catch (IOException ioException) {
		            ioException.printStackTrace();
		        }
		    }
		}

		
		
	//*****************************************************************************************
		
		private void AttendantLogIn(ArrayList<String> list, ConnectionToClient client) {
		    try {
		        String username = list.get(1);
		        String password = list.get(2);

		        String query = "SELECT * FROM bpark.user WHERE username = ? AND user_type = 'attendant'";
		        try (PreparedStatement stmt = conn.prepareStatement(query)) {
		            stmt.setString(1, username);

		            try (ResultSet rs = stmt.executeQuery()) {
		                if (!rs.next()) {
		                    client.sendToClient("ERROR: USER_NOT_FOUND");
		                    System.out.println("❌ Attendant not found: " + username);
		                } else {
		                    String realPassword = rs.getString("password");
		                    if (realPassword.equals(password)) {
		                        client.sendToClient("ATTENDANT_FOUND");
		                        System.out.println("✅ Attendant login success for: " + username);
		                    } else {
		                        client.sendToClient("ERROR: WRONG_PASSWORD");
		                        System.out.println("❌ Wrong password for: " + username);
		                    }
		                }
		            }
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		        try {
		            client.sendToClient("ERROR: " + e.getMessage());
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    }
		}

	//*************************************************************************************

		
		private void ManagerLogIn(ArrayList<String> list, ConnectionToClient client) {
		    try {
		        String username = list.get(1);
		        String password = list.get(2);

		        String query = "SELECT * FROM bpark.user WHERE username = ? AND user_type = 'manager'";
		        try (PreparedStatement stmt = conn.prepareStatement(query)) {
		            stmt.setString(1, username);

		            try (ResultSet rs = stmt.executeQuery()) {
		                if (!rs.next()) {
		                    client.sendToClient("ERROR: USER_NOT_FOUND");
		                    System.out.println("❌ Manager not found: " + username);
		                } else {
		                    String realPassword = rs.getString("password");
		                    if (realPassword.equals(password)) {
		                        client.sendToClient("MANAGER_FOUND");
		                        System.out.println("✅ Manager login success for: " + username);
		                    } else {
		                        client.sendToClient("ERROR: WRONG_PASSWORD");
		                        System.out.println("❌ Wrong password for manager: " + username);
		                    }
		                }
		            }
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		        try {
		            client.sendToClient("ERROR: " + e.getMessage());
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    }
		}

		//*************************************************************************************

		private void FreeSpots(ArrayList<String> list, ConnectionToClient client) {
		    try {
		        String query = "SELECT COUNT(*) FROM bpark.parking_space WHERE is_occupied = FALSE";

		        try (PreparedStatement stmt = conn.prepareStatement(query)) {
		            try (ResultSet rs = stmt.executeQuery()) {
		                if (rs.next()) {
		                    int freeSpots = rs.getInt(1);
		                    client.sendToClient("FREE_SPOTS:" + freeSpots);
		                    System.out.println("✅ Free spots query success. Available: " + freeSpots);
		                } else {
		                    client.sendToClient("ERROR: Failed to retrieve free spots.");
		                }
		            }
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		        try {
		            client.sendToClient("ERROR: " + e.getMessage());
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    }
		}

		//*************************************************************************************

		private void ParkReport(ArrayList<String> request, ConnectionToClient client) {
		    String month = request.get(1); // למשל: "06"
		    String year = request.get(2);  // למשל: "2025"

		    // בדיקה אם החודש כבר הסתיים
		    YearMonth requestedMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
		    YearMonth currentMonth = YearMonth.now();

		    if (!requestedMonth.isBefore(currentMonth)) {
		        try {
		            client.sendToClient("ERROR: Report available only after month ends.");
		            return;
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }

		    String sql = """
		        SELECT p.subscriber_num, COUNT(*) AS parking_count,
		               SUM(p.extension_count) AS total_extensions,
		               s.delay_count
		        FROM bpark.parking p
		        JOIN bpark.subscriber s ON p.subscriber_num = s.subscriber_num
		        WHERE p.retrieval_time IS NOT NULL
		          AND MONTH(p.parking_date) = ?
		          AND YEAR(p.parking_date) = ?
		        GROUP BY p.subscriber_num
		    """;

		    try (PreparedStatement ps = conn.prepareStatement(sql)) {
		        ps.setInt(1, Integer.parseInt(month));
		        ps.setInt(2, Integer.parseInt(year));

		        ResultSet rs = ps.executeQuery();
		        StringBuilder response = new StringBuilder("PARKING_USAGE_REPORT:");

		        while (rs.next()) {
		            int subscriber = rs.getInt("subscriber_num");
		            int count = rs.getInt("parking_count");
		            int extensions = rs.getInt("total_extensions");
		            int delays = rs.getInt("delay_count");

		            response.append("S").append(subscriber).append(",")
		                    .append(count).append(",")        // מספר פעמים שחנה
		                    .append(extensions).append(",")   // מספר הארכות
		                    .append(delays).append(";");      // מספר איחורים
		        }

		        if (response.toString().equals("PARKING_USAGE_REPORT:")) {
		            client.sendToClient("ERROR: No parking records found for this month.");
		        } else {
		            client.sendToClient(response.toString());
		        }

		    } catch (SQLException | IOException e) {
		        e.printStackTrace();
		        try {
		            client.sendToClient("ERROR: Failed to generate parking usage report.");
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    }
		}


		//*************************************************************************************

		private void SubscriberReport(ArrayList<String> request, ConnectionToClient client) {
		    String month = request.get(1); // "05"
		    String year = request.get(2);  // "2025"

		    YearMonth requestedMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
		    YearMonth currentMonth = YearMonth.now();

		    if (!requestedMonth.isBefore(currentMonth)) {
		        try {
		            client.sendToClient("ERROR: Report available only after month ends.");
		            return;
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }

		    String sql = """
		        SELECT s.subscriber_num, CONCAT(u.first_name, ' ', u.last_name) AS full_name,
		               p.parking_date, p.retrieval_time, p.max_time_minutes
		        FROM bpark.parking p
		        JOIN bpark.subscriber s ON p.subscriber_num = s.subscriber_num
		        JOIN bpark.user u ON u.id = s.subscriber_num
		        WHERE MONTH(p.parking_date) = ? AND YEAR(p.parking_date) = ? AND p.retrieval_time IS NOT NULL
		    """;

		    try (PreparedStatement ps = conn.prepareStatement(sql)) {
		        ps.setString(1, month);
		        ps.setString(2, year);

		        ResultSet rs = ps.executeQuery();
		        Map<String, int[]> dataMap = new HashMap<>(); // full_name → [allocated total, actual total]

		        while (rs.next()) {
		            String fullName = rs.getString("full_name");
		            Timestamp start = rs.getTimestamp("parking_date");
		            Timestamp end = rs.getTimestamp("retrieval_time");
		            int max = rs.getInt("max_time_minutes");

		            if (start == null || end == null) continue;

		            long actualMinutes = Duration.between(
		                    start.toLocalDateTime(),
		                    end.toLocalDateTime()
		            ).toMinutes();

		            dataMap.putIfAbsent(fullName, new int[]{0, 0});
		            dataMap.get(fullName)[0] += max;
		            dataMap.get(fullName)[1] += actualMinutes;
		        }

		        if (dataMap.isEmpty()) {
		            client.sendToClient("ERROR: No completed parking records found for this month.");
		            return;
		        }

		        StringBuilder response = new StringBuilder("SUBSCRIBER_DURATION_REPORT:");
		        for (Map.Entry<String, int[]> entry : dataMap.entrySet()) {
		            String name = entry.getKey();
		            int[] totals = entry.getValue();
		            response.append(name).append(",").append(totals[0]).append(",").append(totals[1]).append(";");
		        }

		        client.sendToClient(response.toString());

		    } catch (SQLException | IOException e) {
		        e.printStackTrace();
		        try {
		            client.sendToClient("ERROR: Failed to generate subscriber report.");
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    }
		}


		//*************************************************************************************

		private void ClientProfile(ArrayList<String> request, ConnectionToClient client) {
		    if (request.size() < 3) {
		        sendError(client, "Invalid profile request.");
		        return;
		    }

		    String code = request.get(1);
		    String mode = request.get(2);

		    System.out.println("📥 ClientProfile called with code = " + code + ", mode = " + mode);

		    // 1. בניית שאילתא לפי סוג התחברות
		    String sql;
		    switch (mode.toLowerCase()) {
		        case "quick" -> sql = """
		            SELECT u.username, u.password, u.email, u.phone_number,
		                   s.subscription_code, s.quick_access_code,
		                   s.registration_date, s.delay_count, s.status
		            FROM bpark.user u
		            JOIN bpark.subscriber s ON u.id = s.subscriber_num
		            WHERE s.quick_access_code = ?
		        """;
		        case "uname" -> sql = """
		            SELECT u.username, u.password, u.email, u.phone_number,
		                   s.subscription_code, s.quick_access_code,
		                   s.registration_date, s.delay_count, s.status
		            FROM bpark.user u
		            JOIN bpark.subscriber s ON u.id = s.subscriber_num
		            WHERE u.username = ?
		        """;
		        default -> sql = """
		            SELECT u.username, u.password, u.email, u.phone_number,
		                   s.subscription_code, s.quick_access_code,
		                   s.registration_date, s.delay_count, s.status
		            FROM bpark.user u
		            JOIN bpark.subscriber s ON u.id = s.subscriber_num
		            WHERE s.subscription_code = ?
		        """;
		    }

		    try (PreparedStatement ps = conn.prepareStatement(sql)) {
		        ps.setString(1, code);
		        ResultSet rs = ps.executeQuery();

		        if (rs.next()) {
		            StringBuilder sb = new StringBuilder("CLIENT_PROFILE:");
		            sb.append(rs.getString("username")).append(",");
		            sb.append(rs.getString("password")).append(",");
		            sb.append(rs.getString("email")).append(",");
		            sb.append(rs.getString("phone_number")).append(",");
		            sb.append(rs.getString("subscription_code")).append(",");
		            sb.append(rs.getString("quick_access_code")).append(",");
		            sb.append(rs.getTimestamp("registration_date")).append(",");
		            sb.append(rs.getInt("delay_count")).append(",");
		            sb.append(rs.getString("status"));

		            client.sendToClient(sb.toString());
		        } else {
		            sendError(client, "Subscriber not found.");
		        }

		    } catch (SQLException | IOException e) {
		        e.printStackTrace();
		        sendError(client, "Failed to retrieve client profile.");
		    }
		}
		
		private void sendError(ConnectionToClient client, String message) {
		    try {
		        client.sendToClient("ERROR: " + message);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		//*************************************************************************************
		private void sendCodeToEmail(ArrayList<String> request, ConnectionToClient client) {
		    if (request.size() < 3) {
		        sendError(client, "Invalid email request.");
		        return;
		    }

		    String code = request.get(1);
		    String mode = request.get(2).toLowerCase();

		    System.out.println("📥 sendCodeToEmail called with code = " + code + ", mode = " + mode);

		    String sql;
		    switch (mode) {
		        case "quick" -> sql = """
		            SELECT p.confirmation_code, u.email, u.id
		            FROM bpark.parking p
		            JOIN bpark.subscriber s ON p.subscriber_num = s.subscriber_num
		            JOIN bpark.user u ON s.subscriber_num = u.id
		            WHERE s.quick_access_code = ?
		            ORDER BY p.parking_date DESC
		            LIMIT 1
		        """;
		        case "uname" -> sql = """
		            SELECT p.confirmation_code, u.email, u.id
		            FROM bpark.parking p
		            JOIN bpark.subscriber s ON p.subscriber_num = s.subscriber_num
		            JOIN bpark.user u ON s.subscriber_num = u.id
		            WHERE u.username = ?
		            ORDER BY p.parking_date DESC
		            LIMIT 1
		        """;
		        default -> sql = """
		            SELECT p.confirmation_code, u.email, u.id
		            FROM bpark.parking p
		            JOIN bpark.subscriber s ON p.subscriber_num = s.subscriber_num
		            JOIN bpark.user u ON s.subscriber_num = u.id
		            WHERE s.subscription_code = ?
		            ORDER BY p.parking_date DESC
		            LIMIT 1
		        """;
		    }

		    try (PreparedStatement ps = conn.prepareStatement(sql)) {
		        ps.setString(1, code);
		        ResultSet rs = ps.executeQuery();

		        if (rs.next()) {
		            String confirmationCode = rs.getString("confirmation_code");
		            String email = rs.getString("email");
		            int userId = rs.getInt("id");

		            // שליחת מייל
		            EmailSender.sendRecoveryEmail(email, confirmationCode);

		            // הכנסת הודעה לטבלת message
		            String msg = "Your parking confirmation code is: " + confirmationCode;
		            PreparedStatement log = conn.prepareStatement("""
		                INSERT INTO bpark.message (msg_to, msg_type, message)
		                VALUES (?, 'Order Code', ?)
		            """);
		            log.setInt(1, userId);
		            log.setString(2, msg);
		            log.executeUpdate();

		            try {
		                client.sendToClient("Success: Email sent to " + email);
		            } catch (IOException ex) {
		                ex.printStackTrace();
		                sendError(client, "Email sent but client notification failed.");
		            }

		        } else {
		            sendError(client, "Subscriber not found.");
		        }

		    } catch (SQLException | MessagingException e) {
		        e.printStackTrace();
		        sendError(client, "Email delivery failed. Please try again.");
		    }
		}

	// *************************************************************************************

		private void CancelReservation(ArrayList<String> list, ConnectionToClient client) {
		    try {
		        String code = list.get(1);
		        String mode = list.get(2);
		        int reservationId = Integer.parseInt(list.get(3));

		        System.out.println("📥 CancelReservation called: code=" + code + ", mode=" + mode + ", reservationId=" + reservationId);

		        // שלב 1: מציאת מספר מנוי לפי שיטת התחברות
		        String sqlFindSubscriber;
		        switch (mode.toLowerCase()) {
		            case "quick":
		                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE quick_access_code = ?";
		                break;
		            case "uname":
		                sqlFindSubscriber = """
		                    SELECT s.subscriber_num
		                    FROM bpark.subscriber s
		                    JOIN bpark.user u ON s.subscriber_num = u.id
		                    WHERE u.username = ?
		                """;
		                break;
		            default:
		                sqlFindSubscriber = "SELECT subscriber_num FROM bpark.subscriber WHERE subscription_code = ?";
		        }

		        int subscriberNum;
		        try (PreparedStatement stmt = conn.prepareStatement(sqlFindSubscriber)) {
		            stmt.setString(1, code);
		            try (ResultSet rs = stmt.executeQuery()) {
		                if (rs.next()) {
		                    subscriberNum = rs.getInt("subscriber_num");
		                    System.out.println("✅ Found subscriber_num = " + subscriberNum);
		                } else {
		                    client.sendToClient("Error: Subscriber not found.");
		                    return;
		                }
		            }
		        }

		        // שלב 2: בדיקה אם ההזמנה קיימת + שייכת למנוי + מצב עדכני
		        String checkSql = "SELECT status FROM bpark.reservation WHERE reservation_id = ? AND subscriber_num = ?";
		        String currentStatus;
		        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
		            checkStmt.setInt(1, reservationId);
		            checkStmt.setInt(2, subscriberNum);
		            try (ResultSet rs = checkStmt.executeQuery()) {
		                if (!rs.next()) {
		                    client.sendToClient("Error: Reservation not found for this subscriber.");
		                    return;
		                }
		                currentStatus = rs.getString("status");
		            }
		        }

		        // אם כבר מבוטלת – לא נמשיך
		        if ("cancelled".equalsIgnoreCase(currentStatus)) {
		            client.sendToClient("Reservation is already cancelled.");
		            System.out.println("ℹ️ Reservation " + reservationId + " is already cancelled.");
		            return;
		        }

		        // שלב 3: עדכון הסטטוס
		        String updateSql = "UPDATE bpark.reservation SET status = 'cancelled' WHERE reservation_id = ?";
		        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
		            updateStmt.setInt(1, reservationId);
		            int rows = updateStmt.executeUpdate();

		            if (rows > 0) {
		                client.sendToClient("Reservation cancelled successfully.");
		                System.out.println("✅ Reservation " + reservationId + " marked as cancelled.");
		            } else {
		                client.sendToClient("Error: Failed to update reservation status.");
		            }
		        }

		    } catch (NumberFormatException e) {
		        try {
		            client.sendToClient("Error: Invalid reservation ID.");
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		        try {
		            client.sendToClient("Error: " + e.getMessage());
		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }
		    }
		}



	// *************************************************************************************

		@Override
		protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		    try {
		        if (msg instanceof ArrayList<?> list && !list.isEmpty()) {
		            ClientRequest request = ClientRequest.fromString(list.get(0).toString());

		            switch (request) { 
		                case RESERVE_PARKING_AUTO -> reserveParkingAuto((ArrayList<String>) list, client);
		                case CHECK_SUBSCRIBER -> checkSubscriberInDB((ArrayList<String>) list, client);
		                case DROP_OFF_CAR -> dropOffCar((ArrayList<String>) list, client);
		                case PICK_UP_CAR -> pickUpCar((ArrayList<String>) list, client);
		                case DisplayParkings -> displayParking((ArrayList<String>) list, client);
		                case DisplayReservations -> displayReservations((ArrayList<String>) list, client);
		                case UPDATE_DETAILS -> UpadateDetails((ArrayList<String>) list, client);
		                case ATTENDANT_LOGIN -> AttendantLogIn((ArrayList<String>) list, client);
		                case REGISTER_SUBSCRIBER -> RegisterSubscriber((ArrayList<String>) list, client);
		                case DisplaySubscribers -> DisplaySubscribers((ArrayList<String>) list, client);
		                case AttendantParking -> AttendantPark((ArrayList<String>) list, client);
		                case MANAGER_LOGIN -> ManagerLogIn((ArrayList<String>) list, client);
		                case GET_FREE_SPOTS -> FreeSpots((ArrayList<String>) list, client);
		                case GET_PARKING_REPORT -> ParkReport((ArrayList<String>) list, client);
		                case GET_SUBSCRIBER_REPORT -> SubscriberReport((ArrayList<String>) list, client);
		                case GET_CLIENT_PROFILE -> ClientProfile((ArrayList<String>) list, client);
		                case SEND_CODE_TO_EMAIL -> sendCodeToEmail((ArrayList<String>) list, client);
		                case CancelReservation -> CancelReservation((ArrayList<String>) list, client);
		                case DISCONNECT -> clientDisconnected(client);
		            }
		        }
		    } catch (Exception e) {
		        System.out.println("❌ Exception occurred: " + e.getMessage());
		        e.printStackTrace();
		    }
		}




	// *****************************************************************************************

	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception ignored) {
		}

		BParkServer server = new BParkServer(port, "bpark", "root", "Aa123456");
		try {
			server.listen();
		} catch (Exception e) {
			System.out.println("ERROR - Could not listen for clients!");
		}
	}
}