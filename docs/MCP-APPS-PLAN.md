# CheckMate as MCP App: plan (0.3.2 and 0.4)

Status 2026-11-22. Sources: MCP Apps spec 2026-01-26 (SEP-1865, `io.modelcontextprotocol/ui`),
MCP Java SDK 2.2.0, Antigravity docs "MCP Apps", Claude MCP Apps docs.

## 1. Standards: where CheckMate stands

| Item | State |
|---|---|
| MCP spec 2026-07-28 (latest) | Stateless, no handshake, `server/discover`. Java SDK 2.2.0 has a *modern* server (`io.modelcontextprotocol.modern`), but its API "may still change until 3.0", and extensions are not yet supported in the modern client. |
| MCP spec 2025-11-25 | Handled by the *legacy* server in the same SDK. This is what Claude Desktop, Claude Code and Antigravity talk today. |
| Java SDK | 0.3.2 had `2.1.0`, raised to `2.2.0` (same legacy API, bug fixes, SEP-2133 `extensions` on capabilities). |
| Jackson | SDK 2.2.0 is built for 2.22; the project pins 2.15.2 (STATUS #17). Raise the pin together with the MCP bump when tests pass. |

**Decision:** stay on the legacy server (stdio) for MCP Apps. Add the modern server later as a
second entry point (Streamable HTTP) once hosts negotiate 2026-07-28 and the modern API is stable.

## 2. What an MCP App is

A tool (`_meta.ui.resourceUri = "ui://checkmate/..."`) plus a resource of MIME type
`text/html;profile=mcp-app` that the host renders in a sandboxed iframe. The view gets
`ui/notifications/tool-input` / `tool-result` and may call tools (`tools/call`), send messages
(`ui/message`) and update the model context (`ui/update-model-context`). Tools with
`_meta.ui.visibility = ["app"]` are callable by the view only. Default CSP blocks all network:
the HTML must be a single self-contained file.

## 3. Host matrix

| Host | Renders UI? | Notes |
|---|---|---|
| Claude Desktop | yes | local stdio server via `claude_desktop_config.json` works. |
| Claude Code | **no** | calls the tool as text. Needs a good text answer (`content`). |
| Antigravity App (v2.34+) | yes | when the client advertises `extensions["io.modelcontextprotocol/ui"]`; plugin with `mcp_config.json`, `agy plugin validate --strict`, `agy plugin install`. Extra entry points `com.google.antigravity/ui` (side panel, full page, file viewer). |
| Antigravity CLI / IDE | no | plain text tools. |

Rule: every tool keeps its full German text answer and adds `structuredContent` for the view.
The server checks the client's capabilities in `initialize` and only attaches `_meta.ui` when
`io.modelcontextprotocol/ui` is advertised.

## 4. Server work (both branches)

1. `McpServer.sync(...).capabilities(...)`: declare `extensions: { "io.modelcontextprotocol/ui": {} }`.
2. `resources/read` for `ui://checkmate/*`, MIME `text/html;profile=mcp-app`, from classpath
   `src/main/resources/mcpapp/*.html` (single file, no external origins, so no `csp`).
3. Tool results carry `structuredContent` (JSON) next to the text. `CheckMateTools` returns
   a small result record instead of a `String`; the text is rendered from it as today.
4. App-only tools (`visibility: ["app"]`): `app_element_page` (paged elements), `app_set_filter`.
5. Tests: `tools/list` has `_meta.ui`, `resources/read` returns the HTML, a client without the
   extension gets no `_meta.ui`.

## 5. Views (UI), by priority

1. **Check report** (`check_model`): specifications as a list with verdict colour
   (erfüllt / nicht erfüllt / nicht prüfbar), unmet examples expandable, click on an element calls
   `element_report`. Replaces the 50,000-character text for people.
2. **LOIN picker** (`find_loins`): table with filters (discipline, phase, purpose), select several,
   button "Check" calls `check_model` with the chosen IDs. Needs only metadata, as decided 2026-11-21.
3. **Model overview** (`describe_model`): schema, count, entity histogram, element list with search.
4. Later: 3D (IFC geometry) only when the reader provides geometry; out of scope for 0.3.2.

Build: plain HTML/CSS/JS, no framework, one file per view, tiny postMessage client
(`ui/initialize`, `ui/notifications/initialized`) written by hand to avoid a Node build in a Java
project. Use host style variables (`--color-*`, `--font-sans`) with fallbacks.

## 6. Branches

- **0.3.2** (pure-Java IFC reader, no Apstex): all of section 4 and the three views. This is the
  portable branch: only Java 21 needed, MCP server runs everywhere.
- **0.4** (based on 0.3.1 with Apstex): do **not** duplicate. Port the `mcp` package changes by
  cherry-pick; the tools sit on `CheckService`, which both branches share. Open point: 0.4 still
  has `IfcModelElements` in `test.fx`, so conflicts are expected in `CheckMateTools` imports only.
  Check with `git merge-base` before cherry-picking (common base is `0.3.1`'s `c7062b5`).

## 7. Packaging per host

- Claude Desktop: `installMcp`, entry in `claude_desktop_config.json` (`JAVA_HOME` in `env`, see WORKFLOW 2.7).
- Claude Code: `claude mcp add checkmate -- <path>/checkmate-mcp`. Text only.
- Antigravity: plugin folder with `mcp_config.json` (`${PLUGIN_ROOT}` for the start script),
  validate with `agy plugin validate <dir> --strict`, install with `agy plugin install <dir>`.
  Ship from `build/install/checkmate-mcp` through a `installAntigravityPlugin` Gradle task.

## 8. Steps

- [x] SDK 2.1.0 to 2.2.0 (this branch).
- [x] Jackson 2.15.2 to 2.22.4, all tests green.
- [x] Capability `extensions` declared. (No per-client check: hosts without the extension ignore `_meta.ui` and use the text.)
- [x] `structuredContent` on `check_model`; [ ] on the other tools, with their views.
- [x] `ui://checkmate/report` view and resource, protocol tests (`CheckMateMcpServerTest`). Not yet seen in a real host.
- [ ] `ui://checkmate/loins` view, app-only tools.
- [ ] `ui://checkmate/model` view.
- [ ] Antigravity plugin task and Claude Desktop test (Windows).
- [ ] Cherry-pick to 0.4.
- [ ] Modern (2026-07-28) server over Streamable HTTP, when the SDK API is stable.

## 9. Risks

- Claude Desktop only renders UI if the server advertises the extension and the HTML is valid;
  failures are silent (see `anthropics/claude-ai-mcp#165`). Test with MCP Inspector / ext-apps `basic-host` first.
- Stdio: stdout is the protocol; the HTML resource is returned inline, nothing else may print.
- Large reports: paginate via app-only tools instead of sending 500k characters.
