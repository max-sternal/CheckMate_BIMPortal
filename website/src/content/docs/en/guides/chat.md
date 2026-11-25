---
title: Chat in the interface
description: Start a check from inside CheckMate through a language model, with Anthropic or through a gateway.
---

The chat field on the right of the window starts a check through a language model, without an MCP client running alongside. It **complements the [MCP interface](/CheckMate_BIMPortal/en/mcp/overview/) rather than replacing it**: both paths call the same `CheckService`, only the caller differs.

This feature is being developed on branch `0.3.3` and is not part of `0.3.2`.

## Setup

The key is read from the environment or from `bim-portal/Java_client/.env`:

```text
ANTHROPIC_API_KEY=...
```

Then restart CheckMate; the input field is enabled once at startup. Without a key the field says so and stays inactive — the rest of the interface, the CLI and the MCP server are unaffected. `*.env` is excluded from version control; never commit credentials to the repository.

## Choosing a model and endpoint

| Variable | Purpose |
|---|---|
| `ANTHROPIC_API_KEY` | Key for the chosen provider; without it the chat stays inactive. |
| `CHECKMATE_LLM_MODEL` | Model name, default `claude-opus-5-5`. |
| `CHECKMATE_LLM_BASE_URL` | Optional alternative endpoint that speaks the same Messages format. |

When `CHECKMATE_LLM_BASE_URL` is set, the key travels as a bearer token instead of `x-api-key`. This makes models from several vendors available through one gateway and one key, for example [OpenRouter](https://openrouter.ai/):

```text
CHECKMATE_LLM_BASE_URL=https://openrouter.ai/api
CHECKMATE_LLM_MODEL=openrouter/free
```

The base URL carries **no trailing `/v1`**: the SDK appends `v1/messages` itself, so a value ending in `/v1` requests `.../v1/v1/messages` and returns a 404. Through a gateway the model name needs its vendor as a prefix, such as `anthropic/claude-sonnet-4`.

`openrouter/free` is that provider's free router. It picks a free model per request and filters for the capabilities the request needs, tool calling among them. Free models are rate-limited — around 50 requests a day, and a single question can use up to six — and the lineup changes often.

The first line in the chat names the configured model. If a different model answered, for instance because a router selected it, the answer ends with `[geantwortet hat: ...]`.

## What the chat triggers

The model has a single tool available: `check_model`, wired to the same check that the "Validate LOIN" button runs. An answer therefore fills the summary table, the report panel and the element tabs exactly as a check started from the interface does.

The LOIN is named by its ID or its portal link. Searching the catalogue is not a chat tool yet; the LOIN can be looked up in the list on the left of the window.

## Cost

Every question is billed per token, because the request originates from CheckMate itself rather than from an AI application that is already paid for. A question with the shortened report costs a few cents, with the full report more. A smaller model, or a cheap one obtained through a gateway, reduces that by one to three orders of magnitude. The [MCP](/CheckMate_BIMPortal/en/mcp/overview/) path does not incur this cost, because there the client provides the model.

## Limits

- The tool response is capped at 50,000 characters so that no complete report enters a request; the report panel still shows the full one.
- At most six tool calls per question.
- The call runs in the background and the window stays responsive. While the model works, there is no progress message.
- The question, the model and LOIN details and the shortened report are transmitted to the chosen provider, and with a gateway additionally through its operator. Assess confidentiality and data protection before use.
- Whether a model calls the tool correctly depends on the model. With free models this is not dependable: of three runs on 24 November 2026, one degenerated into a repeating table until the token limit was reached.

## Current state

On 24 November 2026 a question was answered end to end over OpenRouter using `openrouter/free`: the model called `check_model`, the check ran, the summary table and report panel filled, and the answer named the cause of the "nicht prüfbar" result correctly. A paid model has not been compared.

Code-level details are on branch `0.3.3` in `docs/WORKFLOW.md` (section 2.8) and `docs/STATUS.md`; the configuration is described in `bim-portal/Java_client/.env_example`.
