package at.sunplugged.multicontacttable.database.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.sunplugged.multicontacttable.database.services.SettingsService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Singleton
public class SettingsServiceImpl implements SettingsService {

	private static final Logger LOG = LoggerFactory.getLogger(SettingsServiceImpl.class);
	
	private static final String PROPERTIES_FILE = "settings.properties";

	private Properties properties;

	private Map<String, StringProperty> settings = new HashMap<>();

	public SettingsServiceImpl() {
		try {
			loadProperties();
		} catch (IOException e) {
			LOG.error("IOException while loading properties on class creation.", e);
		}
	}

	private void loadProperties() throws IOException {
		File propertiesFile = new File(PROPERTIES_FILE);
		if (propertiesFile.exists() == false) {
			propertiesFile.createNewFile();
			setDefaultPropertiesFile();
		}
		try (FileInputStream fi = new FileInputStream(PROPERTIES_FILE)) {
			if (properties == null) {
				properties = new Properties();
			}
			properties.load(fi);

			properties.entrySet().forEach(entry -> {
				if (settings.containsKey(entry.getKey()) == false) {
					SimpleStringProperty property = new SimpleStringProperty();
					property.addListener((observable, oldValue, newValue) -> {
						try {
							saveProperties();
						} catch (IOException e) {
							LOG.error("IOException while saving properties", e);
						}
					});
					settings.put((String) entry.getKey(), property);
				}
				settings.get(entry.getKey()).set((String) entry.getValue());
			});
		}

	}

	private void setDefaultPropertiesFile() throws IOException {
		if (properties == null) {
			properties = new Properties();
			insertDefaultSettings();
			saveProperties();
		}
	}


	private void saveProperties() throws IOException {
		settings.entrySet().forEach(entry -> {
			properties.put(entry.getKey(), String.valueOf(entry.getValue().getValue()));
		});
		try (FileOutputStream fo = new FileOutputStream(PROPERTIES_FILE)) {
			properties.store(fo, "");
		}
	}

	private void insertDefaultSettings() {
		settings.put(DATABASE_FILE, new SimpleStringProperty(
				"C:\\Users\\jasch\\SunpluggedJob\\Multicontakttable\\VOC1200Labor\\save_old\\processdaten.mdb"));
	}

	@Override
	public StringProperty get(String key) {
		return (StringProperty) settings.get(key);
	}

	@Override
	public String getAsString(String key) {
		return settings.get(key).getValue();
	}

}
