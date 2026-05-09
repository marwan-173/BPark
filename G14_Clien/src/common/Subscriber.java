package common;

import java.io.Serializable;
import java.sql.Date;

public class Subscriber implements Serializable {
	public enum Status {
		active, inactive
	}

	private int subscriberNum;
	private String subscriptionCode;
	private String quickAccessCode;
	private Date registrationDate;
	private int delayCount;
	private Status status;

	public Subscriber(int subscriberNum, String subscriptionCode, String quickAccessCode,
	                  Date registrationDate, int delayCount, Status status) {
		this.subscriberNum = subscriberNum;
		this.subscriptionCode = subscriptionCode;
		this.quickAccessCode = quickAccessCode;
		this.registrationDate = registrationDate;
		this.delayCount = delayCount;
		this.status = status;
	}

	public int getSubscriberNum() {
		return subscriberNum;
	}

	public void setSubscriberNum(int subscriberNum) {
		this.subscriberNum = subscriberNum;
	}

	public String getSubscriptionCode() {
		return subscriptionCode;
	}

	public void setSubscriptionCode(String subscriptionCode) {
		this.subscriptionCode = subscriptionCode;
	}

	public String getQuickAccessCode() {
		return quickAccessCode;
	}

	public void setQuickAccessCode(String quickAccessCode) {
		this.quickAccessCode = quickAccessCode;
	}

	public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public int getDelayCount() {
		return delayCount;
	}

	public void setDelayCount(int delayCount) {
		this.delayCount = delayCount;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}