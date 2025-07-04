import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import view.MainView;

public class main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView.getRoot(), 800, 600);
        primaryStage.setTitle("Controlador PID - Temperatura");
        primaryStage.setScene(scene);
        primaryStage.show();
        mainView.startLoop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
