package test.fx;

import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {

    IfcModel apModel;
    try {
      apModel = IfcResources.loadDefaultModel();
    } catch (ModelLoadException e) {
      // Without a model there is nothing to check; say why instead of showing an empty one.
      Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
      alert.setTitle("<CheckMate>");
      alert.setHeaderText("IFC-Modell konnte nicht geladen werden");
      alert.showAndWait();
      Platform.exit();
      return;
    }

    ElementUI elementUI = new ElementUI(apModel);

    primaryStage.setTitle("<CheckMate>");
    primaryStage.setScene(new Scene(elementUI, 600, 900));
    primaryStage.show();
  }
}
