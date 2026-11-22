---
title: Connecting Clients (Claude, OpenCode, Ollama & Strata)
description: Step-by-step setup of the CheckMate MCP server for Claude Desktop, Claude Code, OpenCode, Ollama (Mistral Cloud / local Qwen), and Strata.
---

CheckMate builds on **open standards** so authorities, engineering firms, and developers can freely choose their AI client and language model (open cloud or 100% local).

## Step 0: Build the MCP Server (`0.3.1`)

From `bim-portal/Java_client/` (in `0.3.1`, requires the APSTEX JARs in `hackathon-template/lib/` and Java 21):

```bash
./gradlew :hackathon-template:installMcp
```

This creates the standalone launcher at:
- **Linux / macOS:** `bim-portal/Java_client/hackathon-template/build/install/checkmate-mcp/bin/checkmate-mcp`
- **Windows:** `bim-portal\Java_client\hackathon-template\build\install\checkmate-mcp\bin\checkmate-mcp.bat`

---

## 1. Claude Desktop & Claude Code *(verified in `0.3.1`)*

### Claude Code
Run from the repository root:

```bash
# Linux / macOS
claude mcp add checkmate -- "/absolute/path/to/checkmate-mcp"

# Windows
claude mcp add checkmate -- cmd /c "C:\path\to\checkmate-mcp.bat"
```

Verify with `claude mcp list`.

### Claude Desktop
Open *Settings → Developer → Edit Config* (`claude_desktop_config.json`) and add the server (on Windows, explicitly pass `JAVA_HOME` in `env` because Claude Desktop does not always forward it):

```json
{
  "mcpServers": {
    "checkmate": {
      "command": "cmd",
      "args": ["/c", "C:\\path\\to\\checkmate-mcp.bat"],
      "env": {
        "JAVA_HOME": "C:\\path\\to\\jdk-21",
        "CHECKMATE_IFC": "C:\\path\\to\\model.ifc"
      }
    }
  }
}
```

---

## 2. OpenCode + Ollama (Open Cloud Models & Local)

With [**OpenCode**](https://opencode.ai/), you can run CheckMate completely open-source with either an **open cloud model** (e.g. **Mistral via Ollama Cloud**) or a **local Qwen model via Ollama**.

### Register CheckMate in `~/.config/opencode/opencode.json`

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "checkmate": {
      "type": "local",
      "command": ["/absolute/path/to/checkmate-mcp"],
      "environment": {
        "JAVA_HOME": "/path/to/jdk-21"
      },
      "enabled": true
    }
  }
}
```

### Option A: Open Cloud Model (e.g., Mistral via Ollama Cloud)
Ideal on standard office laptops without a dedicated GPU:
1. Create an Ollama Cloud API key and choose **Ollama Cloud** in OpenCode (or start via `ollama launch opencode`).
2. Select a tool-calling open model such as **Mistral** and ask:
   *"Describe the loaded IFC model and find fitting LOINs for LPH 5 civil engineering."*

### Option B: 100% Local With Ollama + Qwen
For full data sovereignty (no data leaves your workstation):
1. Pull a tool-calling Qwen model locally (e.g., `qwen3.6` / `qwen3.8` depending on VRAM/RAM) with a sufficiently large context window (`num_ctx` ≥ 32K–64K).
2. Launch via `ollama launch opencode` — or use a lightweight MCP host such as `ollmcp` (`pip install mcp-client-for-ollama`).

:::tip[Tip for smaller local models]
When working with smaller context windows, keep the `examples` argument on `check_model` small (default `3`) and drill into single elements via `element_report`. With **`0.3.2`** (`structuredContent`), tool accuracy for smaller models improves further.
:::

---

## 3. Local Consumer Hardware With Strata + Qwen

To run large Mixture-of-Experts models (such as **Qwen 3.8-Flash-Next 125B MoE**) locally on consumer GPU/workstation hardware (e.g., 12–16 GB VRAM + 32–64 GB RAM + NVMe SSD), [**Strata**](https://github.com/Niko1221/Strata) provides a specialized open-source inference engine:
- Strata distributes experts across GPU VRAM, system RAM, and SSD, exposing **local OpenAI- and Anthropic-compatible APIs** with tool calling.
- OpenCode (or compatible MCP hosts) can point directly to the local Strata endpoint and drive CheckMate completely offline via MCP.

---

## 4. Antigravity (Target: Native MCP App)

Once **Streamable HTTP** and the **MCP Apps Extension (`ui://`)** land (`0.3.2` / `0.4` *CheckMate Lite*), CheckMate connects directly into **Antigravity** — delivering an interactive compliance table and (in `0.4`) an embedded 3D WebGPU IFC viewer right inside the conversation.
