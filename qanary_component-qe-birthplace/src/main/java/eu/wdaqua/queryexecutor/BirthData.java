package eu.wdaqua.queryexecutor;

import java.net.URI;
import java.util.String;
import net.minidev.json.JSONObject;

public class NamedEntity {
	private final String name;
	private final String birthplace;
	private final String birthdate; 

	// TODO: less information allowed?
	public NamedEntity(String name, String birthplace, String birthdate) {
		this.name = name;
		this.birthplace = birthplace;
		this.birthdate = birthdate;
	}

	public void setBirthplace(String birthplace) {
		this.birthplace = birthplace;
	}
	
	public String getBirthplace() {
		return this.birthplace;
	}

	public String getBirthdate() {
		return this.birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}

	public JSONObject getAsJson() {
		JSONObject json = new JSONObject();
		json.put("birthplace", birthplace);
		json.put("birthdate", birthdate);
		json.put("name", name);
		return json;
	}
}
