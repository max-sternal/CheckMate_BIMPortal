package test.fx;

import java.io.File;

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

		IfcModel apModel = loadIFCExample();

		ElementUI elementUI = new ElementUI(apModel);

		primaryStage.setTitle("<CheckMate>");
		primaryStage.setScene(new Scene(elementUI, 600, 900));
		primaryStage.show();
	}

	/**
	 * Load an example IFC model from a specified file path.
	 */
	private IfcModel loadIFCExample() {
		IfcModel apModel = new IfcModel();
		try {
			apModel.readStepFile(new File(
					"C:/Users/Max/Nextcloud/BIM_Deutschland_Hackathon/data/ifc/Brücke_BIM.HH_TBW1_01_P_ohneMerkmale.ifc"));
			// "C:/Users/Max/Nextcloud/BIM_Deutschland_Hackathon/data/ifc/IFC-Modell
			// Hochbauprojekt.ifc"));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return apModel;
	}

}
