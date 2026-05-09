package common;

import java.io.Serializable;

public class ParkingSpace implements Serializable {
	private int spaceNumber;
	private String location;
	private boolean isOccupied;

	public ParkingSpace(int spaceNumber, String location, boolean isOccupied) {
		this.spaceNumber = spaceNumber;
		this.location = location;
		this.isOccupied = isOccupied;
	}

	public int getSpaceNumber() {
		return spaceNumber;
	}

	public void setSpaceNumber(int spaceNumber) {
		this.spaceNumber = spaceNumber;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public boolean isOccupied() {
		return isOccupied;
	}

	public void setOccupied(boolean occupied) {
		isOccupied = occupied;
	}
}
