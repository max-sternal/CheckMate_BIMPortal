package test.fx;

import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.ifc.ModelLoadException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Starts the CheckMate window: loads the model {@code CHECKMATE_IFC} (or {@code -Dcheckmate.ifc})
 * names, or the bundled bridge model, and opens {@link ElementUI}. Gradle task {@code runGui}.
 */
public class App extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {

    LoadedModel model;
    try {
      model = LoadedModel.configured();
    } catch (ModelLoadException configuredFailed) {
      // Say why, then carry on with the bundled model: "Load IFC" can still fetch the right one,
      // and the report panel names the model in use, so nothing passes for the requested model.
      showError(
          "Das eingestellte IFC-Modell konnte nicht geladen werden",
          configuredFailed.getMessage()
              + "\n\nCheckMate startet mit dem mitgelieferten Brückenmodell.");
      try {
        model = LoadedModel.bundled();
      } catch (ModelLoadException bundledFailed) {
        showError("IFC-Modell konnte nicht geladen werden", bundledFailed.getMessage());
        Platform.exit();
        return;
      }
    }

    ElementUI elementUI = new ElementUI(model);

    primaryStage.setTitle("<CheckMate>");
    // Wide enough for the button bar, the LOIN list, the element tabs and the chat panel.
    primaryStage.setScene(new Scene(elementUI, 1280, 900));
    primaryStage.show();
  }

  private static void showError(String header, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message);
    alert.setTitle("<CheckMate>");
    alert.setHeaderText(header);
    alert.showAndWait();
  }
}
