package test.fx;

import java.util.Set;
import java.util.function.Consumer;

import com.apstex.ifctoolbox.ifc.IfcBuildingElement;
import com.apstex.ifctoolbox.ifc.IfcPropertySet;
import com.apstex.ifctoolbox.ifc.IfcPropertySetDefinition;
import com.apstex.ifctoolbox.ifc.IfcRelDefines;
import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import com.apstex.step.core.ClassInterface;
import com.bimportal.hackathon.examples.BasicExample;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ElementUI extends VBox {

	public ElementUI(IfcModel apModel) {
		super();
		this.setSpacing(10);
		this.setFillWidth(true);
		this.buildGUI(apModel);
	}

	/**
	 * Build the main GUI layout with filter input and tab pane for IFC objects.
	 */
	private void buildGUI(IfcModel apModel) {

		// UI Components:
		TextField filterField = new TextField();
		filterField.setPromptText("Enter class name filter");
		Button filterButton = new Button("Filter");

		// Create styled buttons
		Button bimPortalButton = new Button("BIM Portal");
		bimPortalButton.setOnAction(e -> {
			// Action to open BIM Portal URL
			String url = "https://bimportal.com"; // Replace with actual URL
			try {
				java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		bimPortalButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
		Button loadIfcButton = new Button("Load IFC");
		loadIfcButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: #388e3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
		Button validateLoinButton = new Button("Validate LOIN");
		validateLoinButton.setOnAction(e -> {
			// Action to open BIM Portal URL
			validateIFCandLOIN();
		});

		validateLoinButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: #fbc02d; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 20;");
		Button connectMcpButton = new Button("Connect MCP");
		connectMcpButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

		HBox filterBox = new HBox(10, filterField, filterButton);
		filterBox.setSpacing(10);
		filterBox.setStyle("-fx-alignment: center;");
		filterField.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
		filterButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

		HBox buttonsBox = new HBox(10, bimPortalButton, loadIfcButton,
				validateLoinButton, connectMcpButton);
		buttonsBox.setSpacing(10);
		buttonsBox.setStyle("-fx-alignment: center;");

		VBox topBox = new VBox(10, filterBox, buttonsBox);

		TabPane tabPane = new TabPane();

		HBox.setHgrow(tabPane, Priority.ALWAYS);

		/*--------------------------------------------------------------------*/
		// Initialize client
		EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

		client.searchProjects();

		ListView<String> loinView = new ListView<>(
				FXCollections.observableArrayList(
						BasicExample.loinSearch(client, "Schaft").split("\n")));

		/*--------------------------------------------------------------------*/
		// Initialize client

		loinView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
				} else {
					setText(item);
					if (item.contains("Pset")) {
						setStyle("-fx-background-color: red;");
					} else {
						setStyle("");
					}
				}
			}
		});
		HBox.setHgrow(loinView, Priority.ALWAYS);

		client.logout();
		System.out.println("✅ Basic example completed successfully!");

		// Create right VBox with two text areas and logo
		TextArea talkToCheckMateField = new TextArea();
		talkToCheckMateField.setPromptText(
				"Ask CheckMate about IFC, LOIN, or construction topics...");
		talkToCheckMateField.setText(
				"\n<CheckMate>\nWie kann ich dir helfen?\n\n\t\t<<<Nutzer>>>\nBitte Suche die LOIN für Stützen im Modell.\n\n\t\t<<<CheckMate>>>\nGerne! Ich habe die LOIN 'Schaft' gefunden.\n");
		talkToCheckMateField.setPrefRowCount(8);
		talkToCheckMateField.setMinHeight(120);
		talkToCheckMateField.setMaxHeight(Double.MAX_VALUE);
		TextArea responseField = new TextArea();
		responseField.setPromptText("Talk To CheckMate");
		responseField.setPrefRowCount(4);
		responseField.setMinHeight(60);
		responseField.setMaxHeight(Double.MAX_VALUE);

		ImageView logoView = null;
		try {
			Image logoImg = new Image("file:lib/CheckMate.png", 180, 180, true,
					true);
			logoView = new ImageView(logoImg);
			logoView.setPreserveRatio(true);
			logoView.setFitWidth(180);
		} catch (Exception e) {
			logoView = new ImageView(); // fallback empty
		}

		VBox rightBox = new VBox(10, new Label("CHAT HISTORY"),
				talkToCheckMateField, responseField, logoView);
		rightBox.setPrefWidth(220);
		rightBox.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5;");
		VBox.setVgrow(talkToCheckMateField, Priority.ALWAYS);
		VBox.setVgrow(responseField, Priority.ALWAYS);
		VBox.setVgrow(logoView, Priority.NEVER);
		rightBox.setFillWidth(true);

		HBox tabPaneHBox = new HBox(loinView, tabPane, rightBox);
		tabPaneHBox.setSpacing(10);
		VBox.setVgrow(tabPaneHBox, Priority.ALWAYS);
		this.getChildren().addAll(topBox, tabPaneHBox);
		this.setSpacing(10);
		this.setFillWidth(true);

		// Method to update tabs based on filter
		Consumer<String> updateTabs = (filter) -> {
			tabPane.getTabs().clear();
			apModel.getObjects().stream()
					.filter(obj -> obj.getClassName().equalsIgnoreCase(filter))
					.map(this::createObjectTab).forEach(tabPane.getTabs()::add);
		};

		// Initial load with a default filter
		updateTabs.accept("IFCcolumn");

		// Event Handlers
		filterField.setOnAction(e -> {
			String filter = filterField.getText().trim();
			if (!filter.isEmpty()) {
				updateTabs.accept(filter);
			}
		});
		filterButton.setOnAction(e -> {
			String filter = filterField.getText().trim();
			if (!filter.isEmpty()) {
				updateTabs.accept(filter);
			}
		});
	}

	private void validateIFCandLOIN() {
		// TODO Auto-generated method stub

	}

	/**
	 * Create a Tab for a given IFC object, displaying its properties and
	 * quantities.
	 */
	public Tab createObjectTab(ClassInterface obj) {
		String tabTitle = "#" + obj.getStepLineNumber();
		StringBuilder tabContent = new StringBuilder();

		if (obj instanceof IfcBuildingElement.Ifc2x3 be) {

			tabContent.append(PropertyUtils.appendElelemtInfo(be));
			for (IfcRelDefines.Ifc2x3 rel : be.getIsDefinedBy_Inverse()) {
				if (rel instanceof com.apstex.ifctoolbox.ifc.IfcRelDefinesByProperties.Ifc2x3 byProps) {
					// Hier hast du die IfcRelDefinesByProperties.Ifc2x3-Instanz
					// Du kannst z.B. PropertySets oder Quantities auslesen:
					tabContent.append(
							PropertyUtils.listPropertiesAndQuantities(byProps));
					// var psd = byProps.getRelatingPropertyDefinition();
					// Weiterverarbeitung...
				} else if (rel instanceof com.apstex.ifctoolbox.ifc.IfcRelDefinesByType.Ifc2x3 byType) {
					// tabContent.append("Defined by Type: ")
					// .append(byType.getRelatingType().getHasPropertySets().forEach(
					// p -> tabContent.append(appendPropertyInfo(p, "\t\t")));)
					// .append("\n");

					Set<IfcPropertySetDefinition.Ifc2x3> sets = byType
							.getRelatingType().getHasPropertySets();
					if (sets == null) {
					} else {
						if (sets.isEmpty() == false) {

							for (IfcPropertySetDefinition.Ifc2x3 set : sets) {

								tabContent.append("Property Set: ")
										.append(set.getName()).append("\n");

								if (set instanceof IfcPropertySet ps) {
									ps.getHasProperties().forEach(
											p -> tabContent.append(PropertyUtils
													.appendPropertyInfo(p,
															"\t\t")));
								}
							}
						}
					}

				}
			}

			// be.getIsDefinedBy_Inverse().forEach(rel -> tabContent
			// .append(PropertyUtils.listPropertiesAndQuantities(rel)));

		}
		ListView<String> tabListView = new ListView<>(FXCollections
				.observableArrayList(tabContent.toString().split("\n")));
		tabListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
				} else {
					setText(item);
					if (item.contains("- _")) {
						setStyle("-fx-background-color: lightgreen;");
					} else {
						setStyle("");
					}
				}
			}
		});
		VBox tabVBox = new VBox(tabListView);
		VBox.setVgrow(tabListView, Priority.ALWAYS);

		return new Tab(tabTitle, tabVBox);
	}

}