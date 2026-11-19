# CheckMate status

What is being worked on and what is still open. One line per item. Reference code as
`file:line` where it helps.

## Open points

### GUI: `bim-portal/Java_client/hackathon-template/src/main/java/test/fx/`

1. No control has a fixed ID (`setId`). The `run-checkmate` driver finds controls by their visible text, so renaming a label or button in `ElementUI.java` silently breaks it. Fix both of these together:
   - Give the key controls IDs as constants in `ElementUI`.
   - Move the driver out of `.claude/skills/run-checkmate/` into `hackathon-template/src/test/java/com/bimportal/hackathon/devtools/CheckMateGuiDriver.java`, add a `runGuiDriver` Gradle task, and have the driver use those constants.

   The driver then compiles with the app, and `driver-classpath.init.gradle` is no longer needed.
2. "Load IFC" has no handler (`ElementUI.java:104`). The model can only be changed through `CHECKMATE_IFC` or `-Dcheckmate.ifc`.
3. "Connect MCP" has no handler (`ElementUI.java:112`). The LLM chat from the pitch is not implemented.
4. "BIM Portal" opens a placeholder URL, `https://bimportal.com` (`ElementUI.java:94`).
5. The default window size of 600x900 (`App.java:21`) cuts off the tab pane and the chat panel.

### LOIN check

6. Every portal LOIN is "nicht prüfbar" on the bundled bridge model (0 of 303 elements applicable). The model is classified after Objektkatalog `Ingenieurbau_V005`, whose level names differ from what the LOINs ask for. This needs a matching model or a classification mapping.
7. The LOIN search matches substrings, so "Schaft" also returns the "Landschaftsbau" LOINs. The GUI could rank or filter by the category path (`Ueberbau > Schaft`).

### Documentation

8. There is no README at the repo root.
9. `bim-portal/README.md` and `bim-portal/Java_client/README.md` are the hackathon starter kit's docs. They point to the `pb40development/bim-portal` repo and to a `bim-portal-client-suite` folder that doesn't exist.
10. `bim-portal/doku/README.md` is empty.
11. `.github/copilot-instructions.md` stops before CheckMate: it doesn't mention `ids/`, `test/fx`, the Apstex JARs or `runGui`.

### Repository

12. `bim-portal/.gitignore` ignores every `*.xml` and `*.ids` file, so IDS or LOIN-XML test files would be silently left out of git.

### Code structure

13. The CheckMate GUI (`App`, `ElementUI`, `LoinCheckCli`, ...) lives in the package `test.fx` in the main sources, so the product looks like test code. Rename it to `com.bimportal.hackathon.gui`. This touches `build.gradle.kts` (`runGui`, `runLoinCheck`, the `test/fx` exclusion), the `run-checkmate` skill and the READMEs. Coordinate with SOPHIEfree, who made the recent changes to this code.
