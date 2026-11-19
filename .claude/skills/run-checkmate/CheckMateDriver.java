// CheckMate GUI driver: starts the real test.fx.App inside this JVM and drives its window.
// Launch it through drive.sh (same directory) - that compiles the module and supplies the
// classpath. Commands: see help() below or SKILL.md. Driver output lines start with ">> ";
// everything else on stdout is the app's own logging.

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

public class CheckMateDriver {

  static final int DEFAULT_TIMEOUT_S = 180;

  static volatile Throwable launchError;
  static Stage stage;
  static boolean interactive;
  static boolean quitting;

  /**
   * Counts down when the window closes. Closing the last window also shuts the FX toolkit down
   * (implicit exit), after which Platform.runLater silently never runs - so fx() must not wait.
   */
  static final java.util.concurrent.CountDownLatch closed = new java.util.concurrent.CountDownLatch(1);

  public static void main(String[] args) throws Exception {
    interactive = args.length == 0;

    Thread launcher =
        new Thread(
            () -> {
              try {
                Application.launch(test.fx.App.class);
              } catch (Throwable t) {
                launchError = t;
              }
            },
            "checkmate-launcher");
    launcher.setDaemon(true);
    launcher.start();

    stage = awaitStage(DEFAULT_TIMEOUT_S);
    fx(
        () -> {
          stage.showingProperty().addListener((obs, was, showing) -> {
            if (!showing) closed.countDown();
          });
          return null;
        });
    say(
        "window up: '"
            + stage.getTitle()
            + "' "
            + (int) stage.getWidth()
            + "x"
            + (int) stage.getHeight());

    int failures = 0;
    if (interactive) {
      say("ready - one command per line, 'help' lists them, EOF or 'quit' ends");
      BufferedReader in =
          new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
      String line;
      while (!quitting && (line = in.readLine()) != null) {
        if (!run(line)) {
          failures++;
        }
      }
    } else {
      for (String command : args) {
        if (!run(command)) {
          failures++;
          break; // later commands usually depend on this one
        }
        if (quitting) {
          break;
        }
      }
    }
    say(failures == 0 ? "done" : "done with " + failures + " failure(s)");
    Platform.exit();
    System.exit(failures == 0 ? 0 : 1); // the portal client leaves non-daemon threads behind
  }

  // ---------------------------------------------------------------- commands

  static boolean run(String line) {
    line = line.strip();
    if (line.isEmpty() || line.startsWith("#")) {
      return true;
    }
    int space = line.indexOf(' ');
    String cmd = (space < 0 ? line : line.substring(0, space)).toLowerCase(Locale.ROOT);
    String arg = space < 0 ? "" : line.substring(space + 1).strip();
    say("$ " + line);
    try {
      switch (cmd) {
        case "help" -> help();
        case "idle" -> idle(arg.isEmpty() ? DEFAULT_TIMEOUT_S : Integer.parseInt(arg));
        case "wait" -> waitForReport(arg);
        case "report" -> block(fx(() -> reportArea().getText()));
        case "search" -> search(arg);
        case "loins" -> loins(arg);
        case "select" -> select(arg);
        case "click" -> click(arg);
        case "tabs" -> tabs();
        case "tab" -> tab(arg);
        case "dump" -> dump();
        case "size" -> size(arg);
        case "ss" -> screenshot(arg.isEmpty() ? "hackathon-template/build/checkmate.png" : arg);
        case "sleep" -> Thread.sleep(Long.parseLong(arg));
        case "hold" -> hold();
        case "quit", "exit" -> quitting = true;
        default -> throw new IllegalArgumentException("unknown command '" + cmd + "' - try 'help'");
      }
      return true;
    } catch (Exception e) {
      say("FAILED: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
      if (!interactive) {
        try {
          screenshot("hackathon-template/build/checkmate-failure.png");
        } catch (Exception ignored) {
          // the failure itself is what matters
        }
      }
      return false;
    }
  }

  static void help() {
    block(
        String.join(
            "\n",
            "idle [sec]        wait until the report panel no longer ends in '...' (search/check done)",
            "wait <regex>      wait until the report panel text matches <regex>",
            "report            print the report panel (LOIN-PRUEFUNG, bottom)",
            "search <term>     server-side LOIN search (empty = whole catalogue); follow with idle",
            "loins [n|text]    list the first n LOINs (default 20) or those containing text, with index",
            "select <i|text>   select a LOIN by index or by name substring",
            "click <label>     fire a button: 'Validate LOIN', 'Filter', ... (BIM Portal opens a browser)",
            "tabs              list the element tabs in the middle pane",
            "tab <i|text>      select an element tab and print its lines",
            "dump              print the control tree (type, text, prompt)",
            "size <w> <h>      resize the window (the default 600x900 clips the layout)",
            "ss [file.png]     scene to PNG (default hackathon-template/build/checkmate.png)",
            "sleep <ms>        pause",
            "hold              hand over: block until a human closes the window",
            "quit              close the app"));
  }

  static void idle(int seconds) throws Exception {
    long start = System.nanoTime();
    poll(
        seconds,
        () -> !fx(() -> reportArea().getText()).strip().endsWith("..."),
        "report still busy");
    say("idle after " + (System.nanoTime() - start) / 1_000_000 + " ms");
  }

  static void waitForReport(String regex) throws Exception {
    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
    poll(
        DEFAULT_TIMEOUT_S,
        () -> pattern.matcher(fx(() -> reportArea().getText())).find(),
        "report never matched /" + regex + "/");
    say("matched /" + regex + "/");
  }

  static void search(String term) throws Exception {
    fx(
        () -> {
          TextField field =
              find(
                  TextField.class,
                  f -> f.getPromptText() != null && f.getPromptText().startsWith("LOIN"),
                  "LOIN search field (prompt 'LOIN-Suchbegriff...')");
          field.setText(term);
          field.fireEvent(new ActionEvent()); // same as pressing Enter
          return null;
        });
  }

  static void loins(String arg) throws Exception {
    fx(
        () -> {
          List<?> items = loinList().getItems();
          say(items.size() + " LOIN(s) in the list");
          boolean byCount = arg.isEmpty() || arg.matches("\\d+");
          int limit = arg.isEmpty() ? 20 : byCount ? Integer.parseInt(arg) : Integer.MAX_VALUE;
          int shown = 0;
          for (int i = 0; i < items.size() && shown < limit; i++) {
            String name = name(items.get(i));
            if (byCount || name.toLowerCase(Locale.ROOT).contains(arg.toLowerCase(Locale.ROOT))) {
              say(String.format("%5d  %s", i, name));
              shown++;
            }
          }
          return null;
        });
  }

  static void select(String arg) throws Exception {
    fx(
        () -> {
          ListView<?> list = loinList();
          int index = indexOf(list.getItems(), arg);
          list.getSelectionModel().select(index);
          list.scrollTo(index);
          say("selected " + index + ": " + name(list.getItems().get(index)));
          return null;
        });
  }

  static void click(String label) throws Exception {
    fx(
        () -> {
          List<Button> buttons = all(Button.class);
          for (Button b : buttons) {
            if (label.equalsIgnoreCase(b.getText())) {
              b.fire();
              return null;
            }
          }
          throw new IllegalStateException(
              "no button '" + label + "'; there are: " + buttons.stream().map(Button::getText).toList());
        });
  }

  static void tabs() throws Exception {
    fx(
        () -> {
          TabPane pane = find(TabPane.class, p -> true, "element TabPane");
          say(pane.getTabs().size() + " tab(s)");
          for (int i = 0; i < pane.getTabs().size(); i++) {
            Tab t = pane.getTabs().get(i);
            boolean selected = pane.getSelectionModel().getSelectedIndex() == i;
            say(String.format("%5d %s %s", i, selected ? "*" : " ", t.getText()));
          }
          return null;
        });
  }

  static void tab(String arg) throws Exception {
    fx(
        () -> {
          TabPane pane = find(TabPane.class, p -> true, "element TabPane");
          List<String> titles = pane.getTabs().stream().map(Tab::getText).toList();
          int index = indexOf(titles, arg);
          Tab tab = pane.getTabs().get(index);
          pane.getSelectionModel().select(index);
          say("tab " + index + ": " + tab.getText());
          List<ListView<?>> lists = new ArrayList<>();
          walk(tab.getContent(), n -> {
            if (n instanceof ListView<?> lv) lists.add(lv);
          });
          for (ListView<?> lv : lists) {
            lv.getItems().forEach(item -> say("  | " + item));
          }
          return null;
        });
  }

  static void dump() throws Exception {
    fx(
        () -> {
          dump(stage.getScene().getRoot(), 0);
          return null;
        });
  }

  static void dump(Node node, int depth) {
    StringBuilder line = new StringBuilder("  ".repeat(depth)).append(node.getClass().getSimpleName());
    if (node.getId() != null) line.append(" #").append(node.getId());
    if (node instanceof Labeled l && l.getText() != null) line.append(" '").append(l.getText()).append("'");
    if (node instanceof TextInputControl t) {
      if (t.getPromptText() != null) line.append(" prompt='").append(t.getPromptText()).append("'");
      line.append(" chars=").append(t.getText() == null ? 0 : t.getText().length());
      if (!t.isEditable()) line.append(" read-only");
    }
    if (node instanceof ListView<?> lv) line.append(" items=").append(lv.getItems().size());
    if (node instanceof TabPane tp) line.append(" tabs=").append(tp.getTabs().stream().map(Tab::getText).toList());
    if (!node.isVisible()) line.append(" (hidden)");
    say(line.toString());
    // A control's skin internals (cells, scroll bars) are noise; containers are not.
    boolean leaf = node instanceof Control && !(node instanceof javafx.scene.control.ScrollPane);
    if (!leaf && node instanceof Parent p) {
      for (Node child : p.getChildrenUnmodifiable()) dump(child, depth + 1);
    }
  }

  static void size(String arg) throws Exception {
    String[] wh = arg.split("[\\sx]+");
    double w = Double.parseDouble(wh[0]);
    double h = Double.parseDouble(wh[1]);
    fx(
        () -> {
          stage.setWidth(w);
          stage.setHeight(h);
          return null;
        });
    Thread.sleep(300); // let a layout pulse run before anything snapshots
    say("window " + (int) w + "x" + (int) h);
  }

  static void screenshot(String path) throws Exception {
    WritableImage image = fx(() -> stage.getScene().snapshot(null));
    int w = (int) image.getWidth();
    int h = (int) image.getHeight();
    int[] argb = new int[w * h];
    image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    out.setRGB(0, 0, w, h, argb, 0, w);
    File file = new File(path).getAbsoluteFile();
    if (file.getParentFile() != null) file.getParentFile().mkdirs();
    ImageIO.write(out, "png", file);
    say("screenshot " + file + " (" + w + "x" + h + ")");
  }

  static void hold() throws Exception {
    say("holding - close the window to continue");
    closed.await();
    say("window closed");
    quitting = true;
  }

  // ---------------------------------------------------------------- lookup

  /** The read-only text area at the bottom - the only non-editable TextArea in the window. */
  static TextArea reportArea() {
    return find(TextArea.class, t -> !t.isEditable(), "report panel (read-only TextArea)");
  }

  /** The ListView next to the 'LOINs' label. */
  static ListView<?> loinList() {
    for (Label label : all(Label.class)) {
      if ("LOINs".equals(label.getText()) && label.getParent() != null) {
        for (Node sibling : label.getParent().getChildrenUnmodifiable()) {
          if (sibling instanceof ListView<?> lv) return lv;
        }
      }
    }
    throw new IllegalStateException("LOIN list not found (no ListView next to a 'LOINs' label)");
  }

  static <T> T find(Class<T> type, Predicate<T> test, String what) {
    for (T candidate : all(type)) if (test.test(candidate)) return candidate;
    throw new IllegalStateException(what + " not found");
  }

  static <T> List<T> all(Class<T> type) {
    List<T> found = new ArrayList<>();
    walk(stage.getScene().getRoot(), n -> {
      if (type.isInstance(n)) found.add(type.cast(n));
    });
    return found;
  }

  static void walk(Node node, Consumer<Node> visit) {
    if (node == null) return;
    visit.accept(node);
    if (node instanceof Parent p) for (Node child : p.getChildrenUnmodifiable()) walk(child, visit);
  }

  /** An index given as a number, or the first item whose name contains the text. */
  static int indexOf(List<?> items, String arg) {
    if (arg.matches("\\d+")) {
      int i = Integer.parseInt(arg);
      if (i >= items.size()) throw new IllegalArgumentException(i + " is out of range, " + items.size() + " item(s)");
      return i;
    }
    String needle = arg.toLowerCase(Locale.ROOT);
    for (int i = 0; i < items.size(); i++) {
      if (name(items.get(i)).toLowerCase(Locale.ROOT).contains(needle)) return i;
    }
    throw new IllegalArgumentException("nothing contains '" + arg + "' among " + items.size() + " item(s)");
  }

  /** LOIN DTOs show their name in the list; anything else its toString. */
  static String name(Object item) {
    try {
      Object name = item.getClass().getMethod("getName").invoke(item);
      return String.valueOf(name);
    } catch (ReflectiveOperationException e) {
      return String.valueOf(item);
    }
  }

  // ---------------------------------------------------------------- plumbing

  static Stage awaitStage(int seconds) throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
    while (System.nanoTime() < end) {
      if (launchError != null) throw new IllegalStateException("app failed to start", launchError);
      try {
        Stage s =
            fx(() ->
                Window.getWindows().stream()
                    .filter(w -> w instanceof Stage && w.isShowing())
                    .map(w -> (Stage) w)
                    .findFirst()
                    .orElse(null));
        if (s != null) return s;
      } catch (IllegalStateException toolkitNotStartedYet) {
        // Platform.runLater refuses until Application.launch has started the toolkit
      }
      Thread.sleep(250);
    }
    throw new IllegalStateException("no window after " + seconds + " s");
  }

  interface Check {
    boolean ok() throws Exception;
  }

  static void poll(int seconds, Check check, String timeoutMessage) throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
    while (!check.ok()) {
      if (System.nanoTime() > end) throw new IllegalStateException(timeoutMessage + " after " + seconds + " s");
      Thread.sleep(250);
    }
  }

  /** Run on the FX thread and wait for the result - every scene-graph access goes through here. */
  static <T> T fx(Callable<T> work) throws Exception {
    if (Platform.isFxApplicationThread()) return work.call();
    if (closed.getCount() == 0) throw new IllegalStateException("the window was closed");
    CompletableFuture<T> result = new CompletableFuture<>();
    Platform.runLater(
        () -> {
          try {
            result.complete(work.call());
          } catch (Throwable t) {
            result.completeExceptionally(t);
          }
        });
    try {
      return result.get(60, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof Exception cause) throw cause;
      throw e;
    }
  }

  static void say(String line) {
    System.out.println(">> " + line);
  }

  static void block(String text) {
    for (String line : text.split("\n", -1)) say("| " + line);
  }
}
