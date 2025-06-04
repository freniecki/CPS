package cps.fx;

import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.io.IOException;

public class CPSApp extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CPSApp.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
        stage.setTitle("CPS: signal visualizer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
