package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class Reservation implements Serializable {
	private int reservationId;
	private int subscriberNum;
	private int parkingSpace;
	private Timestamp reservationStart;
	private Timestamp reservationEnd;
	private int confirmationCode;
	private String status;
	private Timestamp createdAt;

	public Reservation(int reservationId, int subscriberNum, int parkingSpace, Timestamp reservationStart,
			Timestamp reservationEnd, int confirmationCode, String status, Timestamp createdAt) {
		this.reservationId = reservationId;
		this.subscriberNum = subscriberNum;
		this.parkingSpace = parkingSpace;
		this.reservationStart = reservationStart;
		this.reservationEnd = reservationEnd;
		this.confirmationCode = confirmationCode;
		this.status = status;
		this.createdAt = createdAt;
	}

	public int getReservationId() {
		return reservationId;
	}

	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	public int getSubscriberNum() {
		return subscriberNum;
	}

	public void setSubscriberNum(int subscriberNum) {
		this.subscriberNum = subscriberNum;
	}

	public int getParkingSpace() {
		return parkingSpace;
	}

	public void setParkingSpace(int parkingSpace) {
		this.parkingSpace = parkingSpace;
	}

	public Timestamp getReservationStart() {
		return reservationStart;
	}

	public void setReservationStart(Timestamp reservationStart) {
		this.reservationStart = reservationStart;
	}

	public Timestamp getReservationEnd() {
		return reservationEnd;
	}

	public void setReservationEnd(Timestamp reservationEnd) {
		this.reservationEnd = reservationEnd;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
}
