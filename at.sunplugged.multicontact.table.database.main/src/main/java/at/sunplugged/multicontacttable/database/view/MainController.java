package at.sunplugged.multicontacttable.database.view;

import java.util.Date;

import javax.inject.Inject;
import javax.swing.event.DocumentEvent.EventType;

import at.sunplugged.multicontacttable.database.model.Result;
import at.sunplugged.multicontacttable.database.services.DatabaseService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MainController {

	@FXML
	private TableView<Result> resultTable;
	@FXML
	private TableColumn<Result, Number> idColumn;
	@FXML
	private TableColumn<Result, String> moduleNameColumn;
	@FXML
	private TableColumn<Result, Date> dateColumn;
	
	@FXML
	private XYChart<Integer, Integer> vChart;
	@FXML
	private XYChart<Integer, Integer> rChart;
	
	@FXML
	private MenuItem menuClose;
	
	@Inject
	private DatabaseService databaseService;
	
	@FXML
	private void initialize() {
		System.out.println("Initilized");
		
		idColumn.setCellValueFactory(cellData -> cellData.getValue().getIdProperty());
		moduleNameColumn.setCellValueFactory(cellData -> cellData.getValue().getModuleProperty());
		dateColumn.setCellValueFactory(cellData -> cellData.getValue().getDateProperty());
		
		SortedList<Result> dataSorted = new SortedList<>(databaseService.getResultOberservableList());
		dataSorted.setComparator((res1, res2) -> Integer.compare( res2.getId(),res1.getId()));
		dataSorted.comparatorProperty().bind(resultTable.comparatorProperty());
		resultTable.setItems(dataSorted);
		menuClose.setOnAction(event -> Platform.exit());
		
		resultTable.getSelectionModel().selectedItemProperty().addListener((oberservable, oldValue, newValue) -> {
			vChart.getData().clear();
			rChart.getData().clear();
			newValue.getMessreihen().forEach(index -> {
				XYChart.Series<Integer, Integer> seriesV = new XYChart.Series<Integer, Integer>();
				XYChart.Series<Integer, Integer> seriesR = new XYChart.Series<Integer, Integer>();
				for (int i = 0; i < newValue.getSegemente(index).size(); i++) {
					if (newValue.getVocValues(index).size() > i) {
						seriesV.getData().add(new XYChart.Data<>(newValue.getSegemente(index).get(i), newValue.getVocValues(index).get(i)));
					}
					if (newValue.getRValues(index).size() > i) {
						seriesR.getData().add(new XYChart.Data<>(newValue.getSegemente(index).get(i), newValue.getRValues(index).get(i)));
					}
				}
				if (seriesV.getData().isEmpty() == false) {
					seriesV.setName(String.format("Messreihe: %d", index));
				}
				if (seriesR.getData().isEmpty() == false) {
					seriesR.setName(String.format("Messreihe: %d", index));
				}
				vChart.getData().add(seriesV);
				rChart.getData().add(seriesR);
				
			});
		
		});
		
	}
	

	
}
