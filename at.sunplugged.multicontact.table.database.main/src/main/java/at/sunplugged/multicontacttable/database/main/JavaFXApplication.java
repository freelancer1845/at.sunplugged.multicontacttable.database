package at.sunplugged.multicontacttable.database.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import at.sunplugged.multicontacttable.database.di.InjectorContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JavaFXApplication extends Application {

	private static Logger LOG = LoggerFactory.getLogger(JavaFXApplication.class);
	
	private static Injector injector;

	
	@Override
	public void start(Stage primaryStage) {
		injector = Guice.createInjector(new InjectorContext());
		
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setControllerFactory(instaniatedClass -> injector.getInstance(instaniatedClass));
		
		try (InputStream fxmlInputStream = ClassLoader.getSystemResourceAsStream("at/sunplugged/multicontacttable/database/view/Rootanchor.fxml")) {
			Parent parent = fxmlLoader.load(fxmlInputStream);
			primaryStage.setScene(new Scene(parent));
			primaryStage.setTitle("JavaFX 8 Multicontacttable Visualizer");
			primaryStage.show();
			
		} catch (IOException e) {
			LOG.error("IO Exception reading root fxml file.", e);
		}
	
	}

	public static void main(String[] args) {
		launch(args);
	}
}
