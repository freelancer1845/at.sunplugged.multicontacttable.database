package at.sunplugged.multicontacttable.database.services.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.Cursor.Savepoint;

import at.sunplugged.multicontacttable.database.model.Result;
import at.sunplugged.multicontacttable.database.services.DatabaseService;
import at.sunplugged.multicontacttable.database.services.SettingsService;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

@Singleton
public class DatabaseServiceImpl implements DatabaseService {

	private static Logger LOG = LoggerFactory.getLogger(DatabaseServiceImpl.class);
	
	private Savepoint savepoint;

	private long currentPosition;
	
	private ObservableList<Result> resultList;

	private SettingsService settingsService;
	
	private StringProperty databaseFileProperty;
	
	@Inject
	public DatabaseServiceImpl(SettingsService settingsSerivce) {
		resultList = FXCollections.observableArrayList();
		this.settingsService = settingsSerivce;
		this.databaseFileProperty = settingsService.get(SettingsService.DATABASE_FILE);
		registerDatabaseFileWatcher();
	}

	private void registerDatabaseFileWatcher() {
		LOG.debug("Registering File Watcher");
		try {
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Path databaseFilePath = Paths.get(databaseFileProperty.get());
			Path databaseFileDirectory = databaseFilePath.getParent();
			
			databaseFileDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
			
			LOG.debug("WatchService registered to: " + databaseFileDirectory.toString());
			
			Task<Object> databasechangeTask = new Task<Object>() {
				
				@Override
				protected Object call() throws Exception {
					LOG.debug("DatabaseChangeTask Idle...");
					updateMessage("Idle...");
					while (true) {
						WatchKey key;
						try {
							key = watchService.take();
						} catch (InterruptedException e) {
							LOG.debug("WatchService.take interrupted.", e);
							return null;
						}
						
						LOG.debug("WatchEvent taken.");
						for (WatchEvent<?> event : key.pollEvents()) {
							WatchEvent.Kind<?> kind = event.kind();
							
							if (kind == StandardWatchEventKinds.OVERFLOW) {
								continue;
							}
							
							WatchEvent<Path> ev = (WatchEvent<Path>) event;
							Path filename = ev.context();
							LOG.debug("Watch event on: " + filename.toString());
							if (filename.equals(Paths.get(databaseFileProperty.get()).getFileName())) {
								LOG.debug("Watch event equals database to watch for.");
								try (Database db = openDatabase();){
								
									Table logModuleTable = db.getTable("LogModul");
									LOG.debug(String.format("Current Position: %d new RowCount: %d", logModuleTable.getRowCount(), getCurrentPosition()));
									logModuleTable.forEach(row -> LOG.debug("Row: " + row.getInt("ID")));
									logModuleTable.forEach(row -> {
										int id = row.getInt("ID");
										if (resultList.stream().anyMatch(res -> res.getId() == id) == false) {
											LOG.debug("New Result Found: " + row.toString());
											Result res = new Result(DatabaseServiceImpl.this);
											res.setId(row.getInt("ID"));
											res.setModule(row.getString("Modul"));
											resultList.add(res);
											res.load();
										}
									});
									
//									if (logModuleTable.getRowCount() > getCurrentPosition()) {
//										LOG.debug("New Data found... updating values");
//										updateMessage("Loading new data...");
//										long maxWork = logModuleTable.getRowCount()-getCurrentPosition();
//										long start = getCurrentPosition();
//										Cursor cr = CursorBuilder.createCursor(logModuleTable);
//										cr.moveNextRows((int) getCurrentPosition());
//										while(cr.moveToNextRow() == true) {
//											updateProgress(start++, maxWork);
//											Row row = cr.getCurrentRow();
//											Result res = new Result(DatabaseServiceImpl.this);
//											res.setId(row.getInt("ID"));
//											res.setModule(row.getString("Modul"));
//											resultList.add(res);
//										}
//										setCurrentPosition(logModuleTable.getRowCount());
//									} else {
//										LOG.debug("No new data found.");
//									}
								} catch (IOException e) {
									LOG.error("Error while updating Database on File Change event.", e);
									return null;
								}
							}
							updateMessage("Idle...");
							LOG.debug("Watch service going back to idle state.");
						}
						key.reset();
					}
				}
			};
			databasechangeTask.setOnFailed(new EventHandler<WorkerStateEvent>(){

				@Override
				public void handle(WorkerStateEvent arg0) {
					LOG.error("Database changetask failed...", databasechangeTask.getException());
				}
				
				
			});
			Thread updateThread = new Thread(databasechangeTask);
			updateThread.setDaemon(true);
			updateThread.setName("Database Updater Thread");
			updateThread.start();
			
		} catch (IOException e) {
			LOG.error("Unkown IOException in Database Watcher...", e);
			
		}
		
	}

	@Override
	public ObservableList<Result> getResultOberservableList() {
		Executors.newSingleThreadExecutor().execute(() -> {
			loadNewValues();
		});
		return this.resultList;
	}
	
	private synchronized Database openDatabase() throws IOException {
		return DatabaseBuilder.open(new File(databaseFileProperty.get()));
	}



	@Override
	public void loadNewValues() {
		try (Database db = openDatabase()){

			Table logModuleTable = db.getTable("LogModul");
			logModuleTable.forEach(row -> {
				Result res = new Result(this);
				res.setId(row.getInt("ID"));
				res.setModule(row.getString("Modul"));
				resultList.add(res);
			});
			setCurrentPosition(logModuleTable.getRowCount());
			
//			Cursor logModuleCursor = CursorBuilder.createCursor(logModuleTable);
//			if (savepoint == null) {
//				logModuleCursor.afterLast();
//			} else {
//				logModuleCursor.restoreSavepoint(savepoint);
//			}
//			for (int i = 0; i < 100; i++) {
//				if (logModuleCursor.moveToPreviousRow() == true) {
//					Row row = logModuleCursor.getPreviousRow();
//					Result res = new Result();
//					res.setId(row.getInt("ID"));
//					res.setModule(row.getString("Modul"));
//					resultList.add(res);
//				}
//			}
//			savepoint = logModuleCursor.getSavepoint();
		} catch (IOException e) {
			LOG.error("IOException while loading new values...", e);
		}
	}

	
	private synchronized long getCurrentPosition() {
		return this.currentPosition;
	}
	
	private synchronized void setCurrentPosition(long position) {
		this.currentPosition = position;
	}
	
	@Override
	public List<Messreihe> getValues(Result result) throws IOException {

		List<Messreihe> messreihen = new ArrayList<>();
		try (Database db = openDatabase()) {
			Table logR = db.getTable("LogR");
			Table logV = db.getTable("LogV");

			Cursor logRCursor = CursorBuilder.createCursor(logR);
			Cursor logVCursor = CursorBuilder.createCursor(logV);

			MessreiheImpl currentMessreihe = new MessreiheImpl((short) -1);

			for (Row row : logRCursor.newIterable().addMatchPattern("ID", result.getId())) {
				if (currentMessreihe.getIndex() != new Integer(row.getShort("Messreihe"))) {
					currentMessreihe = (MessreiheImpl) messreihen.stream().filter(reihe -> reihe.getIndex().equals(new Integer(row.getShort("Messreihe"))))
							.findFirst().orElse(null);
					if (currentMessreihe == null) {
						currentMessreihe = new MessreiheImpl(row.getShort("Messreihe"));
						messreihen.add(currentMessreihe);
					}
				}
				currentMessreihe.addRValue(new Integer(row.getShort("Segment")), new Integer(row.getShort("MesswertR")));
			}
			for (Row row : logVCursor.newIterable().addMatchPattern("ID", result.getId())) {
				if (currentMessreihe.getIndex() != new Integer(row.getShort("Messreihe"))) {
					currentMessreihe = (MessreiheImpl) messreihen.stream().filter(reihe -> reihe.getIndex().equals(new Integer(row.getShort("Messreihe"))))
							.findFirst().orElse(null);
					if (currentMessreihe == null) {
						currentMessreihe = new MessreiheImpl(row.getShort("Messreihe"));
						messreihen.add(currentMessreihe);
					}
				}
				currentMessreihe.addVValue(new Integer(row.getShort("Segment")), new Integer(row.getShort("MesswertV")));
			}
		}
		return messreihen;
	}

	private final class MessreiheImpl implements DatabaseService.Messreihe {

		private Short index = -1;

		private Map<Integer, Integer[]> map = new HashMap<>();

		public MessreiheImpl(Short index) {
			this.index = index;
		}

		public void setIndex(Short index) {
			this.index = index;
		}

		public void addRValue(Integer segement, Integer rValue) {
			if (map.containsKey(segement) == false) {
				map.put(segement, new Integer[2]);
			}
			map.get(segement)[1] = rValue;
		}

		public void addVValue(Integer segment, Integer vValue) {
			if (map.containsKey(segment) == false) {
				map.put(segment, new Integer[2]);
			}
			map.get(segment)[0] = vValue;
		}

		@Override
		public Integer getIndex() {
			return new Integer(index);
		}

		@Override
		public List<Integer> getSegmente() {
			return new ArrayList<Integer>(map.keySet());
		}

		@Override
		public List<Integer> getRValues() {
			return map.entrySet().stream().map(entry -> entry.getValue()[1]).collect(Collectors.toList());
		}

		@Override
		public List<Integer> getVValues() {
			return map.entrySet().stream().map(entry -> entry.getValue()[0]).collect(Collectors.toList());
		}

	}

}
