package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class Parking implements Serializable {
	private int parkingCode;
	private int parkingSpace;
	private Timestamp parkingDate;
	private Timestamp retrievalTime;
	private int confirmationCode;
	private int subscriberNum;
	private int extensionCount;
	private int maxTimeMinutes;

	public Parking(int parkingCode, int parkingSpace, Timestamp parkingDate, Timestamp retrievalTime,
			int confirmationCode, int subscriberNum, int extensionCount, int maxTimeMinutes) {
		this.parkingCode = parkingCode;
		this.parkingSpace = parkingSpace;
		this.parkingDate = parkingDate;
		this.retrievalTime = retrievalTime;
		this.confirmationCode = confirmationCode;
		this.subscriberNum = subscriberNum;
		this.extensionCount = extensionCount;
		this.maxTimeMinutes = maxTimeMinutes;
	}

	public int getParkingCode() {
		return parkingCode;
	}

	public void setParkingCode(int parkingCode) {
		this.parkingCode = parkingCode;
	}

	public int getParkingSpace() {
		return parkingSpace;
	}

	public void setParkingSpace(int parkingSpace) {
		this.parkingSpace = parkingSpace;
	}

	public Timestamp getParkingDate() {
		return parkingDate;
	}

	public void setParkingDate(Timestamp parkingDate) {
		this.parkingDate = parkingDate;
	}

	public Timestamp getRetrievalTime() {
		return retrievalTime;
	}

	public void setRetrievalTime(Timestamp retrievalTime) {
		this.retrievalTime = retrievalTime;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public int getSubscriberNum() {
		return subscriberNum;
	}

	public void setSubscriberNum(int subscriberNum) {
		this.subscriberNum = subscriberNum;
	}

	public int getExtensionCount() {
		return extensionCount;
	}

	public void setExtensionCount(int extensionCount) {
		this.extensionCount = extensionCount;
	}

	public int getMaxTimeMinutes() {
		return maxTimeMinutes;
	}

	public void setMaxTimeMinutes(int maxTimeMinutes) {
		this.maxTimeMinutes = maxTimeMinutes;
	}
}