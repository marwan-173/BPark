package common;

import java.io.Serializable;
import java.sql.Date;

public class MonthlyReport implements Serializable {
	private int reportId;
	private String reportType;
	private Date generatedOn;
	private String data;

	public MonthlyReport(int reportId, String reportType, Date generatedOn, String data) {
		this.reportId = reportId;
		this.reportType = reportType;
		this.generatedOn = generatedOn;
		this.data = data;
	}

	public int getReportId() {
		return reportId;
	}

	public void setReportId(int reportId) {
		this.reportId = reportId;
	}

	public String getReportType() {
		return reportType;
	}

	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

	public Date getGeneratedOn() {
		return generatedOn;
	}

	public void setGeneratedOn(Date generatedOn) {
		this.generatedOn = generatedOn;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
