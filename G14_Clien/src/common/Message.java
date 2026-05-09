package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class Message implements Serializable {
	private int id;
	private int msgTo;
	private String msgType;
	private String message;
	private boolean isRead;
	private Timestamp sentAt;

	public Message(int id, int msgTo, String msgType, String message, boolean isRead, Timestamp sentAt) {
		this.id = id;
		this.msgTo = msgTo;
		this.msgType = msgType;
		this.message = message;
		this.isRead = isRead;
		this.sentAt = sentAt;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMsgTo() {
		return msgTo;
	}

	public void setMsgTo(int msgTo) {
		this.msgTo = msgTo;
	}

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isRead() {
		return isRead;
	}

	public void setRead(boolean read) {
		isRead = read;
	}

	public Timestamp getSentAt() {
		return sentAt;
	}

	public void setSentAt(Timestamp sentAt) {
		this.sentAt = sentAt;
	}
}