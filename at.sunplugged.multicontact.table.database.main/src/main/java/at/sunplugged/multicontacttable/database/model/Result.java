package at.sunplugged.multicontacttable.database.model;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;

import at.sunplugged.multicontacttable.database.di.InjectorContext;
import at.sunplugged.multicontacttable.database.services.DatabaseService;
import at.sunplugged.multicontacttable.database.services.DatabaseService.Messreihe;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

public class Result {
	
	private static Logger LOG = LoggerFactory.getLogger(Result.class);

	private IntegerProperty id;

	private StringProperty module;

	private ObjectProperty<Date> date;

	private ObservableList<Integer> messreihen;

	private Map<Integer, ObservableList<Integer>> segmenteMap;

	private Map<Integer, ObservableList<Integer>> vocValuesMap;

	private Map<Integer, ObservableList<Integer>> rValuesMap;


	private boolean loaded = false;

	private DatabaseService databaseService;

	public Result(DatabaseService databaseService) {
		id = new SimpleIntegerProperty(-1);
		module = new SimpleStringProperty("Default Module");
		date = new SimpleObjectProperty<Date>(Date.from(Instant.now()));
		messreihen = FXCollections.observableArrayList();
		segmenteMap = new HashMap<>();
		vocValuesMap = new HashMap<>();
		rValuesMap = new HashMap<>();

		this.databaseService = databaseService;

	}

	public int getId() {
		return id.get();
	}

	public IntegerProperty getIdProperty() {
		return id;
	}

	public void setId(int id) {
		this.id.set(id);
	}

	public String getModule() {
		return module.get();
	}

	public StringProperty getModuleProperty() {
		return module;
	}

	public void setModule(String module) {
		this.module.set(module);
	}

	public Date getDate() {
		return date.get();
	}

	public ObjectProperty<Date> getDateProperty() {
		return date;
	}

	public void setDate(Date value) {
		this.date.set(value);
	}

	public ObservableList<Integer> getVocValues(Integer messreihe) {
		if (loaded == false) {
			load();
		}
		return vocValuesMap.get(messreihe);
	}


	public ObservableList<Integer> getRValues(Integer messreihe) {
		if (loaded == false) {
			load();
		}
		return rValuesMap.get(messreihe);
	}


	public ObservableList<Integer> getSegemente(Integer messreihe) {
		if (loaded == false) {
			load();
		}
		return segmenteMap.get(messreihe);
	}

	public ObservableList<Integer> getMessreihen() {
		if (loaded == false) {
			load();
		}
		return messreihen;
	}

	public void load() {
		
		Task<Boolean> resultLoadingTask = new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception {
				
				updateMessage("Getting values for " + Result.this.getId());
				try {
					List<Messreihe> messreihen = databaseService.getValues(Result.this);
					messreihen.forEach(reihe -> {
						Result.this.messreihen.add(reihe.getIndex());
						segmenteMap.put(reihe.getIndex(), FXCollections.observableArrayList());
						vocValuesMap.put(reihe.getIndex(), FXCollections.observableArrayList());
						rValuesMap.put(reihe.getIndex(), FXCollections.observableArrayList());
						
						if(reihe.getSegmente().get(0) != null) {
							segmenteMap.get(reihe.getIndex()).addAll(reihe.getSegmente());
						}
						if (reihe.getVValues().get(0) != null) {
							vocValuesMap.get(reihe.getIndex()).addAll(reihe.getVValues());
						} 
						if (reihe.getRValues().get(0) != null) {
							rValuesMap.get(reihe.getIndex()).addAll(reihe.getRValues());
						}
					});
					loaded = true;
				} catch (IOException e) {
					LOG.error("IOException while loading result.", e);
				}
				return loaded;
			}
			
			
			
		};
		resultLoadingTask.run();
//		Executors.newSingleThreadExecutor().execute(resultLoadingTask);
	}

}
