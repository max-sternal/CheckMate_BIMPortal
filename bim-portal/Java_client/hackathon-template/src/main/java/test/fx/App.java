package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {

    IfcModel apModel = IfcResources.loadDefaultModel();

    ElementUI elementUI = new ElementUI(apModel);

    primaryStage.setTitle("<CheckMate>");
    primaryStage.setScene(new Scene(elementUI, 600, 900));
    primaryStage.show();
  }
}
