package test.fx;

import com.apstex.ifctoolbox.ifc.IfcBuildingElement;
import com.apstex.ifctoolbox.ifc.IfcPropertySet;
import com.apstex.ifctoolbox.ifc.IfcPropertySetDefinition;
import com.apstex.ifctoolbox.ifc.IfcRelDefines;
import com.apstex.ifctoolbox.ifcmodel.IfcModel;
import com.apstex.step.core.ClassInterface;
import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.LoinIdsCheck;
import com.bimportal.hackathon.ids.ModelElement;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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

  /** Elements the portal puts on one page of LOIN search results; a shorter page is the last. */
  private static final int PORTAL_PAGE_SIZE = 1000;

  /** Bound on paging, so a portal always answering a full page cannot spin forever. */
  private static final int MAX_PAGES = 20;

  /** Bound on element tabs, so a specification applying to the whole model stays navigable. */
  private static final int MAX_ELEMENT_TABS = 20;

  /** The model as the LOIN check sees it - read once, it does not change while the GUI is open. */
  private final List<ModelElement> modelElements;

  /** The same elements by id, to get from a finding back to the element it is about. */
  private final Map<String, ModelElement> elementsById;

  private final String modelSchema;

  private final ListView<SimpleLoinPublicDto> loinView = new ListView<>();

  private final TabPane tabPane = new TabPane();

  private final TextArea reportArea = new TextArea();

  public ElementUI(IfcModel apModel) {
    super();
    this.modelElements = IfcModelElements.from(apModel);
    this.modelSchema = IfcModelElements.schemaOf(apModel);
    this.elementsById =
        modelElements.stream()
            .collect(
                Collectors.toMap(
                    ModelElement::id, element -> element, (first, duplicate) -> first));
    this.setSpacing(10);
    this.setFillWidth(true);
    this.buildGUI(apModel);
  }

  /** Build the main GUI layout with filter input and tab pane for IFC objects. */
  private void buildGUI(IfcModel apModel) {

    // UI Components:
    TextField filterField = new TextField();
    filterField.setPromptText("Enter class name filter");
    Button filterButton = new Button("Filter");

    // Create styled buttons
    Button bimPortalButton = new Button("BIM Portal");
    bimPortalButton.setOnAction(
        e -> {
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
    validateLoinButton.setOnAction(e -> validateSelectedLoin());

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

    HBox buttonsBox =
        new HBox(10, bimPortalButton, loadIfcButton, validateLoinButton, connectMcpButton);
    buttonsBox.setSpacing(10);
    buttonsBox.setStyle("-fx-alignment: center;");

    VBox topBox = new VBox(10, filterBox, buttonsBox);

    HBox.setHgrow(tabPane, Priority.ALWAYS);

    /*--------------------------------------------------------------------*/
    // LOIN list: the search term is editable, because which LOINs exist for a term is
    // exactly what one wants to try out against a model.
    TextField loinSearchField = new TextField();
    loinSearchField.setPromptText("LOIN-Suchbegriff - leer für alle");
    loinSearchField.setOnAction(e -> searchLoins(loinSearchField.getText().trim()));

    // Only the name is shown; the GUID the check needs travels with the item itself.
    loinView.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(SimpleLoinPublicDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
              }
            });
    loinView.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            validateSelectedLoin();
          }
        });
    VBox.setVgrow(loinView, Priority.ALWAYS);

    VBox loinBox = new VBox(10, new Label("LOINs"), loinSearchField, loinView);
    loinBox.setPrefWidth(320);
    HBox.setHgrow(loinBox, Priority.ALWAYS);

    /*--------------------------------------------------------------------*/

    // Create right VBox with two text areas and logo
    TextArea talkToCheckMateField = new TextArea();
    talkToCheckMateField.setPromptText("Ask CheckMate about IFC, LOIN, or construction topics...");
    talkToCheckMateField.setText(
        "\n<CheckMate>\nWie kann ich dir helfen?\n\n\t\t<<<Nutzer>>>\nBitte Suche die LOIN für Stützen im Modell.\n\n\t\t<<<CheckMate>>>\nGerne! Ich habe die LOIN 'Schaft' gefunden.\n");
    talkToCheckMateField.setPrefRowCount(8);
    talkToCheckMateField.setMinHeight(120);
    talkToCheckMateField.setMaxHeight(Double.MAX_VALUE);

    ImageView logoView = null;
    try {
      // Bundled resource, not a working-directory path: has to resolve inside a JAR too.
      Image logoImg =
          new Image(
              ElementUI.class.getResourceAsStream("/images/checkmate-logo.png"),
              180,
              180,
              true,
              true);
      logoView = new ImageView(logoImg);
      logoView.setPreserveRatio(true);
      logoView.setFitWidth(180);
    } catch (Exception e) {
      logoView = new ImageView(); // fallback empty
    }

    VBox rightBox = new VBox(10, new Label("CHAT HISTORY"), talkToCheckMateField, logoView);
    rightBox.setPrefWidth(220);
    rightBox.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5;");
    VBox.setVgrow(talkToCheckMateField, Priority.ALWAYS);
    VBox.setVgrow(logoView, Priority.NEVER);
    rightBox.setFillWidth(true);

    // The report is read, not written, and lines are pre-aligned - hence read-only and monospaced.
    reportArea.setEditable(false);
    reportArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
    reportArea.setPrefRowCount(10);
    reportArea.setMinHeight(140);
    reportArea.setText(
        modelLine()
            + "\nLOIN links auswählen und 'Validate LOIN' drücken (oder doppelklicken):"
            + "\nin der Mitte stehen dann die Elemente, um die es geht, hier das Ergebnis.");

    VBox reportBox = new VBox(5, new Label("LOIN-PRÜFUNG"), reportArea);
    VBox.setVgrow(reportArea, Priority.ALWAYS);

    HBox tabPaneHBox = new HBox(loinBox, tabPane, rightBox);
    tabPaneHBox.setSpacing(10);
    VBox.setVgrow(tabPaneHBox, Priority.ALWAYS);
    this.getChildren().addAll(topBox, tabPaneHBox, reportBox);
    this.setSpacing(10);
    this.setFillWidth(true);

    // Method to update tabs based on filter
    Consumer<String> updateTabs =
        (filter) -> {
          tabPane.getTabs().clear();
          apModel.getObjects().stream()
              .filter(obj -> obj.getClassName().equalsIgnoreCase(filter))
              .map(this::createObjectTab)
              .forEach(tabPane.getTabs()::add);
        };

    // Initial load with a default filter
    updateTabs.accept("IFCcolumn");

    // Event Handlers
    filterField.setOnAction(
        e -> {
          String filter = filterField.getText().trim();
          if (!filter.isEmpty()) {
            updateTabs.accept(filter);
          }
        });
    filterButton.setOnAction(
        e -> {
          String filter = filterField.getText().trim();
          if (!filter.isEmpty()) {
            updateTabs.accept(filter);
          }
        });

    searchLoins("");
  }

  /**
   * Look LOINs up in the BIM Portal and offer them for checking.
   *
   * <p>An empty term lists the whole published catalogue, which is the overview to start from. A
   * term is passed to the server rather than filtered here, because the matches for something like
   * "Schaft" sit outside the first page.
   */
  private void searchLoins(String term) {
    boolean all = term.isEmpty();
    reportArea.setText(all ? "Lade alle LOINs ..." : "Suche LOINs zu '" + term + "' ...");

    inBackground(
        () -> {
          EnhancedBimPortalClient client = BimPortalClientBuilder.buildAuthenticatedOrPublic();
          try {
            return fetchLoins(client, term);
          } finally {
            client.logout();
          }
        },
        loins -> {
          loinView.setItems(FXCollections.observableArrayList(loins));
          reportArea.setText(
              modelLine()
                  + "\n"
                  + loins.size()
                  + (all ? " LOIN(s) im Portal" : " LOIN(s) zu '" + term + "'")
                  + " - eine auswählen und prüfen.");
        });
  }

  /**
   * Every LOIN the portal answers with, paged.
   *
   * <p>A single request stops at {@value #PORTAL_PAGE_SIZE} results, filtered or not - a term like
   * "LPH" hits that limit just as an empty one does, and the list would silently end mid-catalogue.
   * So pages are read until one comes back short.
   */
  private static List<SimpleLoinPublicDto> fetchLoins(
      EnhancedBimPortalClient client, String term) {

    List<SimpleLoinPublicDto> all = new ArrayList<>();
    for (int page = 0; page < MAX_PAGES; page++) {
      LoinForPublicRequest request = new LoinForPublicRequest().pageNumber(page);
      if (!term.isEmpty()) {
        request.searchString(term);
      }

      List<SimpleLoinPublicDto> found = client.searchLoins(request);
      all.addAll(found);
      if (found.size() < PORTAL_PAGE_SIZE) {
        break;
      }
    }
    return all;
  }

  /** What the check is run against, for the top of the report panel. */
  private String modelLine() {
    return "Modell: Schema " + modelSchema + ", " + modelElements.size() + " Objekte";
  }

  /** Check the model against the selected LOIN, then show the report and the elements it is about. */
  private void validateSelectedLoin() {
    SimpleLoinPublicDto selected = loinView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      reportArea.setText("Keine LOIN ausgewählt.");
      return;
    }
    reportArea.setText("Prüfe Modell gegen '" + selected.getName() + "' ...");

    inBackground(
        () -> check(selected),
        checked -> {
          reportArea.setText(checked.report());
          if (checked.result() != null) {
            showCheckedElements(checked.result());
          }
        });
  }

  /**
   * A check that ran, or the reason it could not.
   *
   * @param result null when the check did not get as far as a verdict
   */
  private record Checked(String report, CheckResult result) {}

  /**
   * Run one LOIN against the model.
   *
   * <p>Called off the FX thread: this exports an IDS from the portal and walks every element.
   */
  private Checked check(SimpleLoinPublicDto loin) {
    EnhancedBimPortalClient client = BimPortalClientBuilder.buildAuthenticatedOrPublic();
    try {
      CheckResult result = LoinIdsCheck.check(client, loin.getGuid(), modelSchema, modelElements);
      return new Checked(String.join("\n", result.report()), result);
    } catch (Exception e) {
      // The portal not holding an IDS for a LOIN, or none for this schema, is an ordinary
      // outcome here - it belongs in the report rather than in a stack trace.
      return new Checked(loin.getName() + ": " + e.getMessage(), null);
    } finally {
      client.logout();
    }
  }

  /**
   * Show the elements the report is about in the middle pane.
   *
   * <p>The checked elements when the specification applied, otherwise the ones that came closest -
   * so the pane always answers "which elements is the verdict below about", instead of standing on
   * an unrelated class filter.
   */
  private void showCheckedElements(CheckResult result) {
    tabPane.getTabs().clear();

    if (result.notApplicable()) {
      for (NearMiss nearMiss : result.diagnosis().nearMisses()) {
        List<String> lines = new ArrayList<>();
        lines.add(
            "[" + nearMiss.matchedFacets() + " von " + nearMiss.totalFacets() + " Facetten] "
                + "nicht ausgewählt");
        nearMiss.divergences().forEach(divergence -> lines.add("[Abweichung] " + divergence));
        tabPane.getTabs().add(elementTab(nearMiss.elementId(), lines));
      }
    } else {
      Map<String, List<Finding>> byElement =
          result.findings().stream()
              .collect(
                  Collectors.groupingBy(
                      Finding::elementId, LinkedHashMap::new, Collectors.toList()));

      for (Map.Entry<String, List<Finding>> entry : byElement.entrySet()) {
        if (tabPane.getTabs().size() >= MAX_ELEMENT_TABS) {
          break;
        }
        tabPane
            .getTabs()
            .add(
                elementTab(
                    entry.getKey(),
                    entry.getValue().stream().map(Finding::requirementLine).toList()));
      }
    }

    if (tabPane.getTabs().isEmpty()) {
      // Neither checked nor near: the facet counts are the only thing left to show.
      tabPane
          .getTabs()
          .add(
              new Tab(
                  "kein Treffer",
                  new ListView<>(FXCollections.observableArrayList(result.diagnosis().lines()))));
    }
  }

  /** One tab per element: what the check said about it, then the merkmale it carries. */
  private Tab elementTab(String elementId, List<String> verdict) {
    ModelElement element = elementsById.get(elementId);

    List<String> lines = new ArrayList<>(verdict);
    if (element != null) {
      lines.add("");
      lines.add("Merkmale im Modell (" + element.entityName() + "):");
      element.properties().forEach(property -> lines.add("   " + property));
    }

    ListView<String> view = new ListView<>(FXCollections.observableArrayList(lines));
    view.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle(empty || item == null ? "" : styleFor(item));
              }
            });

    VBox box = new VBox(view);
    VBox.setVgrow(view, Priority.ALWAYS);
    return new Tab(tabTitle(element, elementId), box);
  }

  /** Met requirements green, anything the check held against the element red, the rest plain. */
  private static String styleFor(String line) {
    if (line.startsWith("[" + Status.FULFILLED.label() + "]")) {
      return "-fx-background-color: #c8e6c9;";
    }
    return line.startsWith("[") ? "-fx-background-color: #ffcdd2;" : "";
  }

  /**
   * A tab title short enough to read.
   *
   * <p>Authoring tools repeat the family name and append an id, so only the first segment of
   * {@code Kappe_Mitte_Fluegel:Kappe_Mitte_Fluegel:701260} carries information.
   */
  private static String tabTitle(ModelElement element, String elementId) {
    if (element == null) {
      return elementId;
    }
    String label = element.label();
    int separator = label.indexOf(':');
    return separator > 0 ? label.substring(0, separator) : label;
  }

  /**
   * Run work off the FX thread and hand the result back on it.
   *
   * <p>Both the LOIN search and the check talk to the portal, which would otherwise freeze the
   * window for as long as the request takes.
   */
  private <T> void inBackground(Callable<T> work, Consumer<T> onSuccess) {
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return work.call();
          }
        };
    task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
    task.setOnFailed(e -> reportArea.setText("Fehlgeschlagen: " + task.getException()));

    Thread worker = new Thread(task, "checkmate-worker");
    worker.setDaemon(true);
    worker.start();
  }

  /** Create a Tab for a given IFC object, displaying its properties and quantities. */
  public Tab createObjectTab(ClassInterface obj) {
    String tabTitle = "#" + obj.getStepLineNumber();
    StringBuilder tabContent = new StringBuilder();

    if (obj instanceof IfcBuildingElement.Ifc2x3 be) {

      tabContent.append(PropertyUtils.appendElelemtInfo(be));
      for (IfcRelDefines.Ifc2x3 rel : be.getIsDefinedBy_Inverse()) {
        if (rel instanceof com.apstex.ifctoolbox.ifc.IfcRelDefinesByProperties.Ifc2x3 byProps) {
          // Hier hast du die IfcRelDefinesByProperties.Ifc2x3-Instanz
          // Du kannst z.B. PropertySets oder Quantities auslesen:
          tabContent.append(PropertyUtils.listPropertiesAndQuantities(byProps));
          // var psd = byProps.getRelatingPropertyDefinition();
          // Weiterverarbeitung...
        } else if (rel instanceof com.apstex.ifctoolbox.ifc.IfcRelDefinesByType.Ifc2x3 byType) {
          // tabContent.append("Defined by Type: ")
          // .append(byType.getRelatingType().getHasPropertySets().forEach(
          // p -> tabContent.append(appendPropertyInfo(p, "\t\t")));)
          // .append("\n");

          Set<IfcPropertySetDefinition.Ifc2x3> sets = byType.getRelatingType().getHasPropertySets();
          if (sets == null) {
          } else {
            if (sets.isEmpty() == false) {

              for (IfcPropertySetDefinition.Ifc2x3 set : sets) {

                tabContent.append("Property Set: ").append(set.getName()).append("\n");

                if (set instanceof IfcPropertySet ps) {
                  ps.getHasProperties()
                      .forEach(p -> tabContent.append(PropertyUtils.appendPropertyInfo(p, "\t\t")));
                }
              }
            }
          }
        }
      }

      // be.getIsDefinedBy_Inverse().forEach(rel -> tabContent
      // .append(PropertyUtils.listPropertiesAndQuantities(rel)));

    }
    ListView<String> tabListView =
        new ListView<>(FXCollections.observableArrayList(tabContent.toString().split("\n")));
    tabListView.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<>() {
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
