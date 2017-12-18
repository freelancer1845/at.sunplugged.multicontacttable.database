package at.sunplugged.multicontacttable.database.services;

import javafx.beans.property.StringProperty;

public interface SettingsService {
	
	public static final String DATABASE_FILE = "database.file";
	
	public StringProperty get(String key);
	
	
	public String getAsString(String key);
	
	
}
