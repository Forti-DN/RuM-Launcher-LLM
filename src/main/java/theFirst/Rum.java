package theFirst;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.common.AbstractController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import util.FileUtils;

public class Rum extends Application {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		logger.debug("Starting RuM");

		String fxmlPath = "pages/common/RootLayout.fxml";
		FXMLLoader fxmlLoader = new FXMLLoader(Rum.class.getClassLoader().getResource(fxmlPath));
		Parent parent = fxmlLoader.load();
		logger.debug("Root fxml loaded");
		((AbstractController) fxmlLoader.getController()).setStage(primaryStage);
		Scene scene = new Scene(parent);
		scene.getStylesheets().add("main.css");
		logger.debug("Main css loaded");

		primaryStage.setTitle("RuM");
		primaryStage.getIcons().add(new Image(Rum.class.getClassLoader().getResourceAsStream("images/pickaxe.png")));
		primaryStage.setScene(scene);
		//Setting minimum window size to 720p
		primaryStage.setMinWidth(1280);
		primaryStage.setMinHeight(720);
		primaryStage.setMaximized(true);
		primaryStage.show();
		logger.debug("Primary stage ready and showing");

		primaryStage.setOnCloseRequest(t -> {
			// Store recent inventory elements 
			FileUtils.saveSavedElementsDataToFile();
			
			logger.info("RuM closed");
			Platform.exit();
			System.exit(0);
		});

		logger.info("RuM started");
	}
}
