package edu.berkeley.cellscope.cscore;

import java.sql.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/* Stores information about a picture */
@DatabaseTable(tableName = "CSPictures")
public class CSPicture {

	@DatabaseField(id = true)
	private String filepath;
	
	@DatabaseField
	private String name;

	@DatabaseField
	private String tags;
	
	@DatabaseField
	private int patientID;
	
	@DatabaseField
	private int userID;
	
	@DatabaseField
	private String annotations;
	
	@DatabaseField
	private String location;
	
	@DatabaseField
	private Date timestamp;
	
	public CSPicture() {
		
	}

	public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public int getPatientID() {
		return patientID;
	}

	public void setPatientID(int patientID) {
		this.patientID = patientID;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	
}
