package test.fx;

import com.apstex.ifctoolbox.ifc.IfcBuildingElement;
import com.apstex.ifctoolbox.ifc.IfcPropertySet;
import com.apstex.ifctoolbox.ifc.IfcPropertySetDefinition;
import com.apstex.ifctoolbox.ifc.IfcRelDefines;
import com.apstex.step.core.ClassInterface;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.PortalLoins;
import com.bimportal.hackathon.ids.RequirementsException;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import test.fx.RequirementSource.Loaded;
import test.fx.RequirementSource.PortalLoin;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class ElementUI extends VBox {

  /**
   * Bound on element tabs, so a specification applying to the whole model stays navigable. The
   * elements beyond it are listed in one last tab.
   */
  private static final int MAX_ELEMENT_TABS = 20;

  /**
   * The model the LOIN check runs against. Replaced as a whole when another one is loaded; a check
   * already running keeps the one it started with.
   */
  private LoadedModel model;

  /** What the model can be checked against: LOINs from a search, IDS files, LOINs by ID. */
  private final ListView<RequirementSource> loinView = new ListView<>();

  private final TabPane tabPane = new TabPane();

  private final TextArea reportArea = new TextArea();

  public ElementUI(LoadedModel model) {
    super();
    this.model = model;
    this.setSpacing(10);
    this.setFillWidth(true);
    this.buildGUI();
  }

  /** Build the main GUI layout with filter input and tab pane for IFC objects. */
  private void buildGUI() {

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
    // Two ways to a model: a file on this machine, or a link (a GitHub file link works too).
    Button loadIfcButton = new Button("Load IFC");
    loadIfcButton.setOnAction(e -> chooseModelFile());
    loadIfcButton.setStyle(
        "-fx-font-size: 16px; -fx-background-color: #388e3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
    Button loadIfcLinkButton = new Button("Load IFC link");
    loadIfcLinkButton.setOnAction(e -> askModelLink());
    loadIfcLinkButton.setStyle(loadIfcButton.getStyle());
    // Requirements from a local IDS file, beside the LOINs from the portal.
    Button loadIdsButton = new Button("Load IDS");
    loadIdsButton.setOnAction(e -> chooseIdsFile());
    loadIdsButton.setStyle(loadIfcButton.getStyle());
    Button validateLoinButton = new Button("Validate LOIN");
    validateLoinButton.setOnAction(e -> validateSelected());

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
        new HBox(
            10,
            bimPortalButton,
            loadIfcButton,
            loadIfcLinkButton,
            loadIdsButton,
            validateLoinButton,
            connectMcpButton);
    buttonsBox.setSpacing(10);
    buttonsBox.setStyle("-fx-alignment: center;");

    VBox topBox = new VBox(10, filterBox, buttonsBox);

    HBox.setHgrow(tabPane, Priority.ALWAYS);
    // A fixed preferred width: by default it is the width of all tab headers, and twenty
    // element tabs would squeeze the LOIN list to nothing. Headers that don't fit go into the
    // tab pane's own menu.
    tabPane.setPrefWidth(600);
    tabPane.setMinWidth(200);

    /*--------------------------------------------------------------------*/
    // LOIN list: the search term is editable, because which LOINs exist for a term is
    // exactly what one wants to try out against a model. A LOIN ID, or a link holding one,
    // checks that LOIN directly - in a project the AIA names the LOINs, so there is no need
    // to search.
    TextField loinSearchField = new TextField();
    loinSearchField.setPromptText("LOIN suchen, oder LOIN-ID / Link - leer für alle");
    loinSearchField.setOnAction(e -> findLoins(loinSearchField.getText().trim()));

    // Only the label is shown; what the check needs travels with the item itself.
    loinView.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(RequirementSource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    loinView.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            validateSelected();
          }
        });
    VBox.setVgrow(loinView, Priority.ALWAYS);

    VBox loinBox = new VBox(10, new Label("LOINs"), loinSearchField, loinView);
    loinBox.setPrefWidth(320);
    loinBox.setMinWidth(280);
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
            + "\nin der Mitte stehen dann die Elemente, um die es geht, hier das Ergebnis."
            + "\nEine LOIN-ID im Suchfeld oder 'Load IDS' prüft sofort.");

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
          model.ifc().getObjects().stream()
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
   * What the search field asks for: a text holding a LOIN ID checks that LOIN, any other text is a
   * search term.
   */
  private void findLoins(String text) {
    Optional<UUID> id;
    try {
      id = PortalLoins.idIn(text);
    } catch (RequirementsException e) {
      reportArea.setText(e.getMessage());
      return;
    }
    if (id.isPresent()) {
      loadLoinById(id.get());
    } else {
      searchLoins(text);
    }
  }

  /**
   * Look LOINs up in the BIM Portal and offer them for checking.
   *
   * <p>An empty term lists the whole published catalogue, which is the overview to start from.
   * Requirements read from a file or fetched by ID stay at the top of the list.
   */
  private void searchLoins(String term) {
    boolean all = term.isEmpty();
    reportArea.setText(all ? "Lade alle LOINs ..." : "Suche LOINs zu '" + term + "' ...");

    inBackground(
        () -> withPortal(client -> PortalLoins.search(client, term)),
        loins -> {
          List<RequirementSource> items = new ArrayList<>();
          loinView.getItems().stream().filter(Loaded.class::isInstance).forEach(items::add);
          loins.stream().map(PortalLoin::of).forEach(items::add);
          loinView.setItems(FXCollections.observableArrayList(items));
          reportArea.setText(
              modelLine()
                  + "\n"
                  + loins.size()
                  + (all ? " LOIN(s) im Portal" : " LOIN(s) zu '" + term + "'")
                  + " - eine auswählen und prüfen.");
        });
  }

  /** Fetch one LOIN by its ID and check the model against it. */
  private void loadLoinById(UUID id) {
    reportArea.setText("Lade LOIN " + id + " aus dem BIM Portal ...");
    inBackground(
        () -> withPortal(client -> PortalLoins.requirements(client, id)),
        document -> offerAndCheck(new Loaded(document)));
  }

  /** Let the user pick an IDS file on this machine, then check the model against it. */
  private void chooseIdsFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("IDS-Datei öffnen");
    chooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("IDS-Dateien", "*.ids", "*.xml"),
            new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
    File file = chooser.showOpenDialog(getScene().getWindow());
    if (file != null) {
      reportArea.setText("Lese IDS " + file + " ...");
      inBackground(
          () -> IdsParser.read(file.toPath()), document -> offerAndCheck(new Loaded(document)));
    }
  }

  /**
   * Put requirements just read at the top of the list, select them and check the model against
   * them. Reading the same file or LOIN again replaces its entry.
   */
  private void offerAndCheck(Loaded source) {
    loinView
        .getItems()
        .removeIf(item -> item instanceof Loaded loaded && loaded.label().equals(source.label()));
    loinView.getItems().add(0, source);
    loinView.getSelectionModel().select(0);
    loinView.scrollTo(0);
    validateSelected();
  }

  /** Run something against the portal with a client of its own, logged out afterwards. */
  private static <T> T withPortal(Function<EnhancedBimPortalClient, T> work) {
    EnhancedBimPortalClient client = BimPortalClientBuilder.buildAuthenticatedOrPublic();
    try {
      return work.apply(client);
    } finally {
      client.logout();
    }
  }

  /** What the check is run against, for the top of the report panel. */
  private String modelLine() {
    return "Modell: " + model.summary();
  }

  /** Let the user pick an IFC file on this machine and load it. */
  private void chooseModelFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("IFC-Modell öffnen");
    chooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("IFC-Modelle", "*.ifc", "*.ifczip"),
            new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
    File file = chooser.showOpenDialog(getScene().getWindow());
    if (file != null) {
      loadModel(file.getPath());
    }
  }

  /** Ask for a link to an IFC file and load it. */
  private void askModelLink() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("<CheckMate>");
    dialog.setHeaderText("IFC-Modell aus einem Link laden (.ifc oder .ifczip)");
    dialog.setContentText("Link, z. B. zu einer Datei auf GitHub:");
    dialog.getEditor().setPrefColumnCount(50);
    dialog
        .showAndWait()
        .map(String::trim)
        .filter(link -> !link.isEmpty())
        .ifPresent(this::loadModel);
  }

  /**
   * Read a model from a path or a link and make it the one the checks run against.
   *
   * <p>In the background: a download or a large model would otherwise freeze the window. The
   * element tabs belong to the old model, so they go.
   */
  private void loadModel(String source) {
    reportArea.setText("Lade Modell " + source + " ...");
    inBackground(
        () -> LoadedModel.read(source),
        loaded -> {
          model = loaded;
          tabPane.getTabs().clear();
          reportArea.setText(
              modelLine()
                  + "\nLOIN links auswählen und 'Validate LOIN' drücken (oder doppelklicken).");
        });
  }

  /** Check the model against the selected entry; show the report and the elements it is about. */
  private void validateSelected() {
    RequirementSource selected = loinView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      reportArea.setText("Keine LOIN ausgewählt.");
      return;
    }
    LoadedModel checked = model;
    reportArea.setText("Prüfe " + checked.name() + " gegen '" + selected.label() + "' ...");

    inBackground(
        () -> check(selected, checked),
        outcome -> {
          reportArea.setText("Modell: " + checked.summary() + "\n" + outcome.report());
          if (!outcome.results().isEmpty()) {
            showCheckedElements(outcome.results(), checked);
          }
        });
  }

  /**
   * A check that ran, or the reason it could not.
   *
   * @param results one per specification checked; empty when the check did not get as far as a
   *     verdict
   */
  private record Checked(String report, List<CheckResult> results) {}

  /**
   * Run one entry of the list against the model.
   *
   * <p>Called off the FX thread: for a LOIN from a search this exports its IDS from the portal, and
   * every check walks every element.
   */
  private Checked check(RequirementSource source, LoadedModel against) {
    try {
      IdsDocument requirements =
          switch (source) {
            case Loaded loaded -> loaded.document();
            case PortalLoin loin ->
                withPortal(client -> PortalLoins.requirements(client, loin.guid()));
          };
      List<CheckResult> results = requirements.check(against.schema(), against.elements());

      List<String> lines = new ArrayList<>();
      lines.add("Anforderungen: " + requirements.describe());
      if (results.size() > 1) {
        lines.add(results.size() + " Spezifikationen für " + against.schema());
      }
      for (CheckResult result : results) {
        if (results.size() > 1) {
          lines.add("");
        }
        lines.addAll(result.report());
      }
      return new Checked(String.join("\n", lines), results);
    } catch (RuntimeException e) {
      // No IDS for a LOIN, or none for this schema, is an ordinary outcome here - it belongs in
      // the report rather than in a stack trace. RequirementsException messages name the source.
      String message =
          e instanceof RequirementsException ? e.getMessage() : source.label() + ": " + e;
      return new Checked(message, List.of());
    }
  }

  /**
   * Show the elements the report is about in the middle pane.
   *
   * <p>The checked elements when a specification applied, otherwise the ones that came closest - so
   * the pane always answers "which elements is the verdict below about", instead of standing on an
   * unrelated class filter. With several specifications, an element's tab holds what each of them
   * said about it.
   */
  private void showCheckedElements(List<CheckResult> results, LoadedModel checked) {
    tabPane.getTabs().clear();
    boolean several = results.size() > 1;

    Map<String, List<String>> byElement = new LinkedHashMap<>();
    for (CheckResult result : results) {
      for (Finding finding : result.findings()) {
        List<String> lines = linesOf(byElement, finding.elementId(), result, several);
        lines.add(finding.requirementLine());
      }
    }
    if (byElement.isEmpty()) {
      for (CheckResult result : results) {
        for (NearMiss nearMiss : result.diagnosis().nearMisses()) {
          List<String> lines = linesOf(byElement, nearMiss.elementId(), result, several);
          lines.add(
              "[Wert passt bei " + nearMiss.matchedFacets() + " von " + nearMiss.totalFacets()
                  + " Facetten] nicht ausgewählt");
          nearMiss.divergences().forEach(divergence -> lines.add("[Abweichung] " + divergence));
        }
      }
    }

    List<String> beyondLimit = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : byElement.entrySet()) {
      if (tabPane.getTabs().size() < MAX_ELEMENT_TABS) {
        tabPane.getTabs().add(elementTab(checked, entry.getKey(), entry.getValue()));
      } else {
        beyondLimit.add(overviewLine(checked, entry.getKey(), entry.getValue()));
      }
    }
    if (!beyondLimit.isEmpty()) {
      tabPane
          .getTabs()
          .add(
              new Tab(
                  "+" + beyondLimit.size() + " weitere",
                  new ListView<>(FXCollections.observableArrayList(beyondLimit))));
    }

    if (tabPane.getTabs().isEmpty()) {
      // Neither checked nor near: the notes and facet counts are the only thing left to show.
      List<String> lines = new ArrayList<>();
      for (CheckResult result : results) {
        if (several) {
          lines.add(result.specification().name() + ":");
        }
        result.notes().forEach(note -> lines.add("   " + note));
        lines.addAll(result.diagnosis().lines());
      }
      tabPane
          .getTabs()
          .add(new Tab("kein Treffer", new ListView<>(FXCollections.observableArrayList(lines))));
    }
  }

  /**
   * An element's lines in the tab being built. With several specifications, each one's lines start
   * with its name, so the reader can tell which LOIN said what.
   */
  private static List<String> linesOf(
      Map<String, List<String>> byElement, String elementId, CheckResult result, boolean several) {
    List<String> lines = byElement.computeIfAbsent(elementId, id -> new ArrayList<>());
    String heading = result.specification().name() + ":";
    if (several && !lines.contains(heading)) {
      lines.add(heading);
    }
    return lines;
  }

  /** One line for an element beyond the tab limit: its name and how many lines are not met. */
  private static String overviewLine(LoadedModel checked, String elementId, List<String> lines) {
    long open = lines.stream().filter(ElementUI::heldAgainst).count();
    String title = tabTitle(checked.elementsById().get(elementId), elementId);
    return open == 0 ? title + " - alles erfüllt" : title + " - " + open + " offen";
  }

  /** One tab per element: what the check said about it, then the merkmale it carries. */
  private Tab elementTab(LoadedModel checked, String elementId, List<String> verdict) {
    ModelElement element = checked.elementsById().get(elementId);

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
    if (met(line)) {
      return "-fx-background-color: #c8e6c9;";
    }
    return heldAgainst(line) ? "-fx-background-color: #ffcdd2;" : "";
  }

  private static boolean met(String line) {
    return line.startsWith("[" + Status.FULFILLED.label() + "]");
  }

  /** A finding that is not met, or a divergence of a near miss. */
  private static boolean heldAgainst(String line) {
    return line.startsWith("[") && !met(line);
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
   * <p>The LOIN search and the check talk to the portal, and loading a model may download one;
   * each would otherwise freeze the window for as long as it takes.
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
    task.setOnFailed(e -> reportArea.setText(failure(task.getException())));

    Thread worker = new Thread(task, "checkmate-worker");
    worker.setDaemon(true);
    worker.start();
  }

  /**
   * A failure for the report panel. A model or requirements that could not be loaded say why in
   * their own words; the model that was loaded before stays in use, and the panel says so.
   */
  private String failure(Throwable error) {
    if (error instanceof ModelLoadException load) {
      return load.getMessage() + "\n\nWeiterhin geladen: " + model.summary();
    }
    if (error instanceof RequirementsException requirements) {
      return requirements.getMessage();
    }
    return "Fehlgeschlagen: " + error;
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
