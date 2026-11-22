package test.fx;

import com.bimportal.hackathon.catalogue.CatalogueEntry;
import com.bimportal.hackathon.catalogue.CatalogueFilter;
import com.bimportal.hackathon.catalogue.LoinDescription;
import com.bimportal.hackathon.catalogue.ModelFit;
import com.bimportal.hackathon.catalogue.ModelFit.Ranked;
import com.bimportal.hackathon.check.CheckService;
import com.bimportal.hackathon.check.Source;
import com.bimportal.hackathon.ids.CheckResult;
import com.bimportal.hackathon.ids.CheckResult.Finding;
import com.bimportal.hackathon.ids.CheckResult.NearMiss;
import com.bimportal.hackathon.ids.CheckResult.Status;
import com.bimportal.hackathon.ids.CheckResult.Verdict;
import com.bimportal.hackathon.ids.IdsDocument;
import com.bimportal.hackathon.ids.IdsParser;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PortalRequirements;
import com.bimportal.hackathon.ids.RequirementsException;
import com.bimportal.hackathon.ifc.LoadedModel;
import com.bimportal.hackathon.ifc.ModelLoadException;
import com.bimportal.hackathon.report.CheckReport;
import com.bimportal.hackathon.report.CheckReport.Run;
import com.bimportal.hackathon.report.ReportText;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import test.fx.RequirementSource.Loaded;
import test.fx.RequirementSource.PortalLoin;
import test.fx.RequirementSource.PortalProject;

/**
 * The CheckMate window: requirements on the left, the checked elements in the middle, summary and
 * report below. It only collects input and shows results; the check runs in the core packages.
 */
public class ElementUI extends VBox {

  /**
   * Bound on element tabs, so a specification applying to the whole model stays navigable. The
   * elements beyond it are listed in one last tab.
   */
  private static final int MAX_ELEMENT_TABS = 20;

  private static final String ALL_DISCIPLINES = "alle Fachmodelle";
  private static final String ALL_PHASES = "alle Leistungsphasen";
  private static final String ALL_PURPOSES = "alle Anwendungsfälle";

  /**
   * The model the LOIN check runs against. Replaced as a whole when another one is loaded; a check
   * already running keeps the one it started with.
   */
  private LoadedModel model;

  /** What the model can be checked against: LOINs from a search, IDS files, LOINs by ID. */
  private final ListView<RequirementSource> loinView = new ListView<>();

  /** The LOINs of the last search - the whole catalogue for an empty term - with their metadata. */
  private List<CatalogueEntry> catalogue = List.of();

  /** The AIA projects of the last search. */
  private List<PortalProject> projects = List.of();

  /** How the catalogue's LOINs fit the model; computed again when another model is loaded. */
  private ModelFit fit;

  /**
   * Requirements, search and check, the same as for the CLI. It downloads each LOIN once: one that
   * was looked at is not downloaded again for its check.
   */
  private final CheckService service =
      new CheckService(BimPortalClientBuilder::buildAuthenticatedOrPublic);

  private final ComboBox<String> disciplineFilter = filterBox(ALL_DISCIPLINES);
  private final ComboBox<String> phaseFilter = filterBox(ALL_PHASES);
  private final ComboBox<String> purposeFilter = filterBox(ALL_PURPOSES);
  private final CheckBox showAll = new CheckBox("alle anzeigen, auch nicht passende");
  private final Label catalogueInfo = new Label();

  /** True while the filters' choices are replaced, which must not refresh the list each time. */
  private boolean offeringFilters;

  /**
   * Counts checks. A description still loading when a check starts is not shown over the check's
   * report.
   */
  private long checks;

  private final TabPane tabPane = new TabPane();

  private final TextArea reportArea = new TextArea();

  /**
   * One row per specification of the last check, and one for all of them: the verdicts at a glance.
   * Clicking a row narrows the report and the element tabs to it.
   */
  private final TableView<SummaryRow> summaryTable = new TableView<>();

  /** The last check, for the summary table's rows. */
  private Checked lastCheck;

  public ElementUI(LoadedModel model) {
    super();
    this.model = model;
    this.fit = ModelFit.of(model.elements());
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
          String url = "https://via.bund.de/bim";
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
    loinSearchField.setPromptText("LOIN oder AIA-Projekt suchen, oder ID / Link - leer für alle");
    loinSearchField.setOnAction(e -> findLoins(loinSearchField.getText().trim()));

    // Only the label is shown; what the check needs travels with the item itself.
    loinView.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(RequirementSource item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
                // The list is narrow; the fit after the name is often cut off.
                setTooltip(empty || item == null ? null : new Tooltip(item.label()));
              }
            });
    loinView.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            validateSelected();
          }
        });
    // Several LOINs can be checked together (Ctrl/Shift); the one clicked last is described.
    loinView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    loinView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, previous, item) -> {
              if (item != null) {
                describe(item);
              }
            });
    VBox.setVgrow(loinView, Priority.ALWAYS);

    // Choosing from the catalogue works on its metadata alone; nothing is downloaded for it.
    for (ComboBox<String> filter : List.of(disciplineFilter, phaseFilter, purposeFilter)) {
      filter.setOnAction(
          e -> {
            if (!offeringFilters) {
              refreshList();
            }
          });
    }
    showAll.setOnAction(e -> refreshList());
    catalogueInfo.setWrapText(true);

    VBox loinBox =
        new VBox(
            6,
            new Label("LOINs"),
            loinSearchField,
            disciplineFilter,
            phaseFilter,
            purposeFilter,
            showAll,
            catalogueInfo,
            loinView);
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
    reportArea.setMinHeight(100);
    reportArea.setText(
        modelLine()
            + "\nLinks filtern: zuerst stehen die LOINs, deren Objekttyp im Modell vorkommt."
            + "\nEin Klick beschreibt eine LOIN, 'Validate LOIN' (oder Doppelklick) prüft die"
            + " ausgewählten (mehrere mit Strg/Umschalt)."
            + "\nEine ID im Suchfeld oder 'Load IDS' prüft sofort.");

    buildSummaryTable();
    VBox reportBox = new VBox(5, new Label("LOIN-PRÜFUNG"), summaryTable, reportArea);
    VBox.setVgrow(reportArea, Priority.ALWAYS);

    HBox tabPaneHBox = new HBox(loinBox, tabPane, rightBox);
    tabPaneHBox.setSpacing(10);
    // The elements above, the summary and the report below; the divider can be dragged, since
    // which of them needs the room depends on what is being looked at.
    SplitPane split = new SplitPane(tabPaneHBox, reportBox);
    split.setOrientation(Orientation.VERTICAL);
    split.setDividerPositions(0.55);
    VBox.setVgrow(split, Priority.ALWAYS);
    this.getChildren().addAll(topBox, split);
    this.setSpacing(10);
    this.setFillWidth(true);

    // Method to update tabs based on filter
    Consumer<String> updateTabs =
        (filter) -> {
          tabPane.getTabs().clear();
          model.elements().stream()
              .filter(e -> e.entityName().equalsIgnoreCase(filter))
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
   * What the search field asks for: a text holding an ID checks that LOIN or AIA project, any other
   * text is a search term.
   */
  private void findLoins(String text) {
    Optional<UUID> id;
    try {
      id = PortalRequirements.idIn(text);
    } catch (RequirementsException e) {
      reportArea.setText(e.getMessage());
      return;
    }
    if (id.isPresent()) {
      loadById(id.get());
    } else {
      searchLoins(text);
    }
  }

  /**
   * Look AIA projects and LOINs up in the BIM Portal and offer them for checking, projects first:
   * there are few, and each stands for many LOINs.
   *
   * <p>An empty term lists the whole published catalogue, which is the overview to start from.
   * Requirements read from a file or fetched by ID stay at the top of the list.
   */
  private void searchLoins(String term) {
    boolean all = term.isEmpty();
    reportArea.setText(all ? "Lade alle LOINs ..." : "Suche LOINs zu '" + term + "' ...");

    inBackground(
        () -> service.search(term),
        found -> {
          catalogue = found.loins();
          projects = found.projects().stream().map(PortalProject::of).toList();
          offerFilterValues();
          refreshList();
          reportArea.setText(
              modelLine()
                  + "\n"
                  + found.loins().size()
                  + " LOIN(s) und "
                  + found.projects().size()
                  + " AIA-Projekt(e)"
                  + (all ? " im Portal" : " zu '" + term + "'")
                  + " - filtern, anklicken für eine Beschreibung, auswählen und prüfen.");
        });
  }

  /**
   * Fill the list: what was read by ID or from a file, then the AIA projects, then the catalogue's
   * LOINs that pass the filters. LOINs whose object type occurs in the model come first; the rest
   * only on request, or when none fits.
   */
  private void refreshList() {
    CatalogueFilter filter =
        new CatalogueFilter(chosen(disciplineFilter), chosen(phaseFilter), chosen(purposeFilter));
    List<CatalogueEntry> filtered = filter.apply(catalogue);
    List<Ranked> ranked = fit.rank(filtered);
    long fitting = ranked.stream().filter(r -> r.fit().fits()).count();
    boolean everything = showAll.isSelected() || fitting == 0;

    List<RequirementSource> items = new ArrayList<>();
    loinView.getItems().stream().filter(Loaded.class::isInstance).forEach(items::add);
    items.addAll(projects);
    ranked.stream()
        .filter(r -> everything || r.fit().fits())
        .map(r -> new PortalLoin(r.entry(), r.fit()))
        .forEach(items::add);
    loinView.setItems(FXCollections.observableArrayList(items));

    catalogueInfo.setText(
        catalogue.size()
            + " LOIN(s)"
            + (filter.isAny() ? "" : ", " + filtered.size() + " nach Filter")
            + ", "
            + fitting
            + " passend zum Modell"
            + (fitting == 0 && !filtered.isEmpty() ? " - darum alle angezeigt" : ""));
  }

  /** Offer the values the loaded LOINs carry, keeping what was chosen where it still exists. */
  private void offerFilterValues() {
    offeringFilters = true;
    try {
      offer(disciplineFilter, ALL_DISCIPLINES, CatalogueEntry::disciplines);
      offer(phaseFilter, ALL_PHASES, CatalogueEntry::phases);
      offer(purposeFilter, ALL_PURPOSES, CatalogueEntry::purposes);
    } finally {
      offeringFilters = false;
    }
  }

  private void offer(
      ComboBox<String> filter, String all, Function<CatalogueEntry, List<String>> dimension) {
    String chosen = filter.getValue();
    List<String> values = new ArrayList<>();
    values.add(all);
    values.addAll(CatalogueFilter.values(catalogue, dimension));
    filter.setItems(FXCollections.observableArrayList(values));
    filter.setValue(values.contains(chosen) ? chosen : all);
  }

  private static ComboBox<String> filterBox(String all) {
    ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList(all));
    filter.setValue(all);
    filter.setMaxWidth(Double.MAX_VALUE);
    return filter;
  }

  /** The value chosen in a filter; null for its "alle ..." entry. */
  private static String chosen(ComboBox<String> filter) {
    String value = filter.getValue();
    return value == null || value.startsWith("alle ") ? null : value;
  }

  /**
   * Show what a list entry is about in the report panel. A LOIN or project is downloaded for this
   * once; its check uses the same download.
   */
  private void describe(RequirementSource source) {
    LoadedModel against = model;
    long checksBefore = checks;
    switch (source) {
      case Loaded loaded ->
          show(LoinDescription.describe(loaded.document(), against.schema(), against.elements()));
      case PortalLoin loin -> {
        IdsDocument known = service.known(loin.source()).orElse(null);
        show(describe(loin, known, against));
        if (known == null) {
          inBackground(
              () -> requirements(loin),
              document -> {
                if (checks == checksBefore
                    && loinView.getSelectionModel().getSelectedItem() == loin) {
                  show(describe(loin, document, against));
                }
              });
        }
      }
      case PortalProject project -> {
        reportArea.setText(project.label() + "\nAnforderungen werden geladen ...");
        inBackground(
            () -> requirements(project),
            document -> {
              if (checks == checksBefore
                  && loinView.getSelectionModel().getSelectedItem() == project) {
                show(LoinDescription.describe(document, against.schema(), against.elements()));
              }
            });
      }
    }
  }

  /** A catalogue LOIN's description; the requirements part once they are downloaded. */
  private static List<String> describe(
      PortalLoin loin, IdsDocument requirements, LoadedModel against) {
    return LoinDescription.describe(
        loin.entry(), loin.fit(), requirements, against.schema(), against.elements());
  }

  private void show(List<String> lines) {
    reportArea.setText(String.join("\n", lines));
  }

  /** The requirements of a list entry, downloaded once. Called off the FX thread. */
  private IdsDocument requirements(RequirementSource source) {
    return service.requirements(source.source());
  }

  /** Fetch a LOIN or an AIA project by its ID and check the model against it. */
  private void loadById(UUID id) {
    reportArea.setText("Lade " + id + " aus dem BIM Portal ...");
    inBackground(
        () -> service.requirements(new Source.PortalId(id.toString())),
        document -> offerAndCheck(new Loaded(document)));
  }

  /** Let the user pick an IDS file on this machine, then check the model against it. */
  private void chooseIdsFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("IDS-Datei öffnen");
    chooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter(
                "IDS-Dateien und AIA-Projekte (ZIP)", "*.ids", "*.xml", "*.zip"),
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
   * them. Reading the same file, LOIN or project again replaces its entry.
   */
  private void offerAndCheck(Loaded source) {
    loinView
        .getItems()
        .removeIf(item -> item instanceof Loaded loaded && loaded.label().equals(source.label()));
    loinView.getItems().add(0, source);
    loinView.getSelectionModel().clearAndSelect(0);
    loinView.scrollTo(0);
    validateSelected();
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
          fit = ModelFit.of(loaded.elements());
          tabPane.getTabs().clear();
          // The summary belongs to the model checked before.
          lastCheck = null;
          summaryTable.getItems().clear();
          summaryTable.setVisible(false);
          summaryTable.setManaged(false);
          refreshList();
          reportArea.setText(
              modelLine()
                  + "\nDie LOINs links sind neu nach Passung zu diesem Modell sortiert."
                  + "\nAuswählen und 'Validate LOIN' drücken (oder doppelklicken).");
        });
  }

  /**
   * Check the model against every selected entry; show the report and the elements it is about.
   */
  private void validateSelected() {
    List<RequirementSource> selected = List.copyOf(loinView.getSelectionModel().getSelectedItems());
    if (selected.isEmpty()) {
      reportArea.setText("Keine LOIN ausgewählt.");
      return;
    }
    checks++;
    LoadedModel checked = model;
    reportArea.setText(
        "Prüfe "
            + checked.name()
            + " gegen "
            + (selected.size() == 1
                ? "'" + selected.get(0).label() + "'"
                : selected.size() + " ausgewählte Einträge")
            + " ...");

    OffsetDateTime checkedAt = OffsetDateTime.now();
    inBackground(
        () -> check(selected, checked, checkedAt),
        outcome -> {
          lastCheck = outcome;
          showSummary(outcome);
        });
  }

  /**
   * A check: the report, the text it reads as, and the model it ran against.
   *
   * @param text the whole report: header, overview and every specification's details
   */
  private record Checked(CheckReport report, String text, LoadedModel model) {}

  /**
   * One row of the summary table.
   *
   * @param results the specifications the row stands for: one, or all for the total row
   * @param requirements what the specification belongs to, for its report; null for the total row
   */
  private record SummaryRow(
      Verdict verdict,
      String verdictLabel,
      String name,
      String applicable,
      String passing,
      String failing,
      List<CheckResult> results,
      String requirements) {

    static SummaryRow of(CheckResult result, String requirements) {
      return new SummaryRow(
          result.verdict(),
          result.verdict().label(),
          result.specification().label(),
          result.applicableElements() + " von " + result.examinedElements(),
          String.valueOf(result.passingElements()),
          String.valueOf(result.failingElements()),
          List.of(result),
          requirements);
    }

    /** The row for every specification of a check. */
    static SummaryRow total(CheckReport report) {
      List<CheckResult> results = report.results();
      Verdict worst =
          results.stream().map(CheckResult::verdict).anyMatch(v -> v == Verdict.FAILED)
              ? Verdict.FAILED
              : results.stream().allMatch(r -> r.verdict() == Verdict.PASSED)
                  ? Verdict.PASSED
                  : Verdict.NOT_APPLICABLE;
      return new SummaryRow(
          worst,
          "Gesamt",
          report.verdict(),
          results.stream().mapToInt(CheckResult::applicableElements).sum() + " Treffer",
          String.valueOf(results.stream().mapToLong(CheckResult::passingElements).sum()),
          String.valueOf(results.stream().mapToLong(CheckResult::failingElements).sum()),
          results,
          null);
    }
  }

  private void buildSummaryTable() {
    TableColumn<SummaryRow, String> verdict = column("Ergebnis", SummaryRow::verdictLabel, 170);
    verdict.setCellFactory(
        c ->
            new TableCell<>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                SummaryRow row = empty ? null : getTableRow().getItem();
                setText(empty ? null : item);
                setStyle(row == null ? "" : verdictStyle(row.verdict()));
              }
            });
    summaryTable
        .getColumns()
        .addAll(
            List.of(
                verdict,
                column("LOIN / Spezifikation", SummaryRow::name, 560),
                column("anwendbar", SummaryRow::applicable, 120),
                column("Elemente ohne Mängel", SummaryRow::passing, 150),
                column("Elemente mit Mängeln", SummaryRow::failing, 150)));
    summaryTable.setPrefHeight(140);
    summaryTable.setMinHeight(90);
    summaryTable.setPlaceholder(new Label("Noch nichts geprüft."));
    summaryTable
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, previous, row) -> {
              if (row != null && lastCheck != null) {
                showRow(row);
              }
            });
    // Hidden until there is something to show, so the report keeps its room.
    summaryTable.setVisible(false);
    summaryTable.setManaged(false);
  }

  private static TableColumn<SummaryRow, String> column(
      String title, Function<SummaryRow, String> value, double width) {
    TableColumn<SummaryRow, String> column = new TableColumn<>(title);
    column.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
    column.setPrefWidth(width);
    column.setSortable(false);
    return column;
  }

  private static String verdictStyle(Verdict verdict) {
    return switch (verdict) {
      case PASSED -> "-fx-background-color: #c8e6c9;";
      case FAILED -> "-fx-background-color: #ffcdd2;";
      case PARTLY_EVALUATED -> "-fx-background-color: #fff9c4;";
      case NOT_APPLICABLE -> "-fx-background-color: #e0e0e0;";
    };
  }

  /** Fill the summary table after a check and show the whole report. */
  private void showSummary(Checked check) {
    List<SummaryRow> rows = new ArrayList<>();
    List<CheckResult> results = check.report().results();
    if (results.size() > 1) {
      rows.add(SummaryRow.total(check.report()));
    }
    for (Run run : check.report().runs()) {
      run.results().forEach(result -> rows.add(SummaryRow.of(result, run.name())));
    }
    summaryTable.setItems(FXCollections.observableArrayList(rows));
    summaryTable.setVisible(!rows.isEmpty());
    summaryTable.setManaged(!rows.isEmpty());

    reportArea.setText(check.text());
    tabPane.getTabs().clear();
    if (!results.isEmpty()) {
      showCheckedElements(results, check.model());
    }
  }

  /** Narrow the report and the tabs to one row: one specification, or all of them. */
  private void showRow(SummaryRow row) {
    if (row.requirements() == null) {
      reportArea.setText(lastCheck.text());
    } else {
      show(ReportText.single(lastCheck.report(), row.requirements(), row.results().get(0)));
    }
    showCheckedElements(row.results(), lastCheck.model());
  }

  /** Run the selected entries against the model. Called off the FX thread. */
  private Checked check(
      List<RequirementSource> sources, LoadedModel against, OffsetDateTime checkedAt) {
    CheckReport report =
        service.check(
            sources.stream().map(RequirementSource::source).toList(),
            against.info(),
            against.elements(),
            checkedAt);
    return new Checked(report, String.join("\n", ReportText.full(report)), against);
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

    ElementLines byElement = new ElementLines(several);
    for (CheckResult result : results) {
      for (Finding finding : result.findings()) {
        List<String> lines = byElement.of(finding.elementId(), result);
        lines.add(finding.requirementLine());
      }
    }
    if (byElement.lines().isEmpty()) {
      for (CheckResult result : results) {
        for (NearMiss nearMiss : result.diagnosis().nearMisses()) {
          List<String> lines = byElement.of(nearMiss.elementId(), result);
          lines.add(
              "[Wert passt bei " + nearMiss.matchedFacets() + " von " + nearMiss.totalFacets()
                  + " Facetten] nicht ausgewählt");
          nearMiss.divergences().forEach(divergence -> lines.add("[Abweichung] " + divergence));
        }
      }
    }

    List<String> beyondLimit = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : byElement.lines().entrySet()) {
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
   * The lines of each element's tab, being built. With several specifications, each one's lines
   * start with its name, so the reader can tell which LOIN said what. Told apart by the result, not
   * by the name: an AIA project may hold two LOINs of the same name.
   */
  private static final class ElementLines {

    private final boolean several;
    private final Map<String, List<String>> lines = new LinkedHashMap<>();
    private final Map<String, CheckResult> lastResult = new HashMap<>();

    ElementLines(boolean several) {
      this.several = several;
    }

    /** The element's lines, with the result's heading added when the result changes. */
    List<String> of(String elementId, CheckResult result) {
      List<String> elementLines = lines.computeIfAbsent(elementId, id -> new ArrayList<>());
      if (several && lastResult.put(elementId, result) != result) {
        elementLines.add(result.specification().name() + ":");
      }
      return elementLines;
    }

    Map<String, List<String>> lines() {
      return lines;
    }
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

  /** Create a Tab for a given IFC element, displaying its attributes and grouped properties. */
  public Tab createObjectTab(ModelElement element) {
    List<String> lines = new ArrayList<>();
    lines.add("Entity: " + element.entityName());
    lines.add("GlobalId: " + element.id());
    if (element.name() != null && !element.name().isBlank()) {
      lines.add("Name: " + element.name());
    }
    if (element.predefinedType() != null && !element.predefinedType().isBlank()) {
      lines.add("PredefinedType: " + element.predefinedType());
    }
    if (element.typeEntityName() != null && !element.typeEntityName().isBlank()) {
      lines.add("Type: " + element.typeEntityName());
    }

    Map<String, List<ModelProperty>> bySet = new LinkedHashMap<>();
    for (ModelProperty property : element.properties()) {
      bySet
          .computeIfAbsent(property.key().propertySet(), key -> new ArrayList<>())
          .add(property);
    }
    if (!bySet.isEmpty()) {
      lines.add("");
      for (Map.Entry<String, List<ModelProperty>> entry : bySet.entrySet()) {
        String setName = entry.getKey().isEmpty() ? "(ohne)" : entry.getKey();
        lines.add("Property Set: " + setName);
        for (ModelProperty property : entry.getValue()) {
          String value = property.value();
          String dataType = property.dataType();
          StringBuilder line =
              new StringBuilder("    - ")
                  .append(property.key().baseName())
                  .append(": ")
                  .append(value == null ? "" : value);
          if (dataType != null && !dataType.isBlank()) {
            line.append(" (").append(dataType).append(")");
          }
          lines.add(line.toString());
        }
      }
    }

    ListView<String> tabListView = new ListView<>(FXCollections.observableArrayList(lines));
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

    return new Tab(tabTitle(element, element.id()), tabVBox);
  }
}
