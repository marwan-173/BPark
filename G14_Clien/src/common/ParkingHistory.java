package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class ParkingHistory implements Serializable {
    private int subscriberNum;
    private int parkingCode;
    private Timestamp parkingDate;
    private Timestamp parkingTime;
    private Timestamp retrievalTime;

    public ParkingHistory(int subscriberNum, int parkingCode, Timestamp parkingDate, Timestamp parkingTime, Timestamp retrievalTime) {
        this.subscriberNum = subscriberNum;
        this.parkingCode = parkingCode;
        this.parkingDate = parkingDate;
        this.parkingTime = parkingTime;
        this.retrievalTime = retrievalTime;
    }

    public int getSubscriberNum() { return subscriberNum; }
    public void setSubscriberNum(int subscriberNum) { this.subscriberNum = subscriberNum; }
    public int getParkingCode() { return parkingCode; }
    public void setParkingCode(int parkingCode) { this.parkingCode = parkingCode; }
    public Timestamp getParkingDate() { return parkingDate; }
    public void setParkingDate(Timestamp parkingDate) { this.parkingDate = parkingDate; }
    public Timestamp getParkingTime() { return parkingTime; }
    public void setParkingTime(Timestamp parkingTime) { this.parkingTime = parkingTime; }
    public Timestamp getRetrievalTime() { return retrievalTime; }
    public void setRetrievalTime(Timestamp retrievalTime) { this.retrievalTime = retrievalTime; }
}
