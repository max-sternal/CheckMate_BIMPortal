#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  GetPromptRequestSchema,
  ListPromptsRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  ReadResourceRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import {
  DATA_RESOURCES,
  INSTRUCTIONS,
  LOINS_VIEW,
  MCP_PROMPTS,
  MCP_VERSION,
  MODEL_JSON_URI,
  MODEL_VIEW,
  REPORT_JSON_URI,
  REPORT_VIEW,
  TOOL_DEFINITIONS,
  UI_EXTENSION,
  UI_MIME,
  VIEW_RESOURCES,
  type ToolDef,
} from './catalog.js';
import { CheckMateTools } from './tools.js';

export {
  CheckMateTools,
  DATA_RESOURCES,
  INSTRUCTIONS,
  LOINS_VIEW,
  MCP_PROMPTS,
  MCP_VERSION,
  MODEL_JSON_URI,
  MODEL_VIEW,
  REPORT_JSON_URI,
  REPORT_VIEW,
  TOOL_DEFINITIONS,
  UI_EXTENSION,
  UI_MIME,
  VIEW_RESOURCES,
  type ToolDef,
};

export const MAX_ANSWER = 50_000;
export const VIEW_EXAMPLES = 10;

export function shorten(answer: string, max = MAX_ANSWER): string {
  if (answer.length <= max) return answer;
  const lastBreak = answer.lastIndexOf('\n');
  const last =
    lastBreak < 0 || answer.length - lastBreak > Math.floor(max / 10)
      ? ''
      : answer.slice(lastBreak);
  const note =
    `\n... gekürzt, ${answer.length} Zeichen insgesamt. Weniger 'examples' wählen oder element_report für einzelne` +
    ' Elemente nutzen.';
  const budget = Math.max(0, max - last.length - note.length);
  const end = answer.lastIndexOf('\n', budget);
  return answer.slice(0, end > 0 ? end : budget) + note + last;
}

const __dirname = dirname(fileURLToPath(import.meta.url));

function readViewHtml(fileName: string): string {
  return readFileSync(resolve(__dirname, '../views', fileName), 'utf-8');
}

function asString(args: Record<string, unknown>, key: string): string | null {
  const v = args[key];
  return v === undefined || v === null ? null : String(v);
}

function asStrings(args: Record<string, unknown>, key: string): string[] {
  const v = args[key];
  if (Array.isArray(v)) return v.map(String);
  if (typeof v === 'string' && v.trim()) return [v.trim()];
  return [];
}

function asInt(args: Record<string, unknown>, key: string, fallback: number): number {
  const v = args[key];
  if (typeof v === 'number' && Number.isFinite(v)) return Math.trunc(v);
  if (typeof v === 'string') {
    const parsed = Number.parseInt(v.trim(), 10);
    if (Number.isFinite(parsed)) return parsed;
  }
  return fallback;
}

function asBool(args: Record<string, unknown>, key: string, fallback = false): boolean {
  const v = args[key];
  if (typeof v === 'boolean') return v;
  if (typeof v === 'string') return v.toLowerCase() === 'true';
  return fallback;
}

export function createCheckMateMcpServer(tools = new CheckMateTools()): Server {
  const server = new Server(
    { name: 'checkmate', version: MCP_VERSION },
    {
      capabilities: {
        tools: {},
        resources: {},
        prompts: {},
        extensions: { [UI_EXTENSION]: {} },
      } as Record<string, unknown>,
      instructions: INSTRUCTIONS,
    }
  );

  // 1. Tools (10 model-facing + 5 app-only)
  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [...TOOL_DEFINITIONS],
  }));

  // 2. Resources (3 interactive 3D HTML MCP App views + 2 JSON data resources)
  server.setRequestHandler(ListResourcesRequestSchema, async () => ({
    resources: [
      ...VIEW_RESOURCES.map((r) => ({
        uri: r.uri,
        name: r.name,
        title: r.title,
        description: r.description,
        mimeType: r.mimeType,
        _meta: { ui: { prefersBorder: true } },
      })),
      ...DATA_RESOURCES.map((r) => ({
        uri: r.uri,
        name: r.name,
        title: r.title,
        description: r.description,
        mimeType: r.mimeType,
      })),
    ],
  }));

  server.setRequestHandler(ReadResourceRequestSchema, async (req) => {
    const uri = req.params.uri;
    const view = VIEW_RESOURCES.find((r) => r.uri === uri);
    if (view) {
      return {
        contents: [
          {
            uri: view.uri,
            mimeType: view.mimeType,
            text: readViewHtml(view.file),
            _meta: { ui: { prefersBorder: true } },
          },
        ],
      };
    }
    if (uri === MODEL_JSON_URI) {
      return {
        contents: [{ uri, mimeType: 'application/json', text: await tools.currentModelJson() }],
      };
    }
    if (uri === REPORT_JSON_URI) {
      return {
        contents: [{ uri, mimeType: 'application/json', text: tools.lastReportJson() }],
      };
    }
    throw new Error(`Unbekannte Ressource: ${uri}`);
  });

  // 3. Guided Prompts (check-loin, explain-element)
  server.setRequestHandler(ListPromptsRequestSchema, async () => ({
    prompts: [...MCP_PROMPTS],
  }));

  server.setRequestHandler(GetPromptRequestSchema, async (req) => {
    const { name, arguments: pArgs } = req.params;
    if (name === 'check-loin') {
      const discipline = pArgs?.discipline?.trim();
      const phase = pArgs?.phase?.trim();
      const filters = [
        discipline ? `Fachmodell '${discipline}'` : '',
        phase ? `Leistungsphase '${phase}'` : '',
      ]
        .filter(Boolean)
        .join(', ');
      return {
        description: MCP_PROMPTS[0].description,
        messages: [
          {
            role: 'user',
            content: {
              type: 'text',
              text:
                `Beschreibe zuerst das geladene IFC-Modell mit describe_model, suche dann mit find_loins passende LOINs im BIM-Portal${filters ? ` (${filters})` : ''}, ` +
                'und prüfe die besten Treffer mit check_model. Erläutere anschließend das Ergebnis auf Deutsch.',
            },
          },
        ],
      };
    }
    if (name === 'explain-element') {
      const element = pArgs?.element?.trim() || '';
      return {
        description: MCP_PROMPTS[1].description,
        messages: [
          {
            role: 'user',
            content: {
              type: 'text',
              text: `Rufe element_report für '${element}' auf und erkläre verständlich auf Deutsch, welche Anforderungen erfüllt sind, welche abweichen und welche IFC-Merkmale das Bauteil trägt.`,
            },
          },
        ],
      };
    }
    throw new Error(`Unbekannter Prompt: ${name}`);
  });

  // 4. Tool Execution
  server.setRequestHandler(CallToolRequestSchema, async (req) => {
    const name = req.params.name;
    const args = (req.params.arguments ?? {}) as Record<string, unknown>;

    const respond = (text: string, structured?: Record<string, unknown>) => {
      const res: Record<string, unknown> = {
        content: [{ type: 'text', text: shorten(text, MAX_ANSWER) }],
        isError: false,
      };
      if (structured && Object.keys(structured).length > 0) {
        res.structuredContent = structured;
      }
      return res;
    };

    try {
      switch (name) {
        case 'load_model': {
          const text = await tools.loadModel(asString(args, 'source') ?? '');
          return respond(text, await tools.modelData());
        }
        case 'describe_model': {
          return respond(await tools.describeModel(), await tools.modelData());
        }
        case 'list_elements': {
          return respond(
            await tools.listElements(
              asString(args, 'entity'),
              asString(args, 'name'),
              asInt(args, 'limit', 30)
            )
          );
        }
        case 'find_loins': {
          const filter = {
            discipline: asString(args, 'discipline'),
            phase: asString(args, 'phase'),
            purpose: asString(args, 'purpose'),
          };
          const search = asString(args, 'search');
          const limit = asInt(args, 'limit', 20);
          const text = await tools.findLoins(
            search,
            filter,
            limit,
            asBool(args, 'include_unfitting', false)
          );
          const data = await tools.loinsData(search, filter, Math.max(limit, 50), true);
          return respond(text, data);
        }
        case 'describe_requirements': {
          return respond(await tools.describeRequirements(asString(args, 'id') ?? ''));
        }
        case 'check_model': {
          const text = await tools.check(
            asStrings(args, 'ids'),
            asStrings(args, 'ids_files'),
            asInt(args, 'examples', 3),
            asStrings(args, 'rules_files')
          );
          return respond(text, await tools.reportData(VIEW_EXAMPLES));
        }
        case 'element_report': {
          return respond(await tools.elementReport(asString(args, 'element') ?? ''));
        }
        case 'load_mapping': {
          return respond(tools.mapping(asString(args, 'source')));
        }
        case 'export_bcf': {
          return respond(await tools.exportBcfFile(asString(args, 'output') ?? 'checkmate.bcfzip'));
        }
        case 'server_status': {
          return respond(tools.status());
        }
        case 'app_report_page': {
          return respond(
            'Weitere Befunde der Prüfung (nur für die Ansicht).',
            tools.reportPage(
              asInt(args, 'run', -1),
              asInt(args, 'spec', -1),
              asInt(args, 'offset', 0),
              asInt(args, 'limit', 25)
            )
          );
        }
        case 'app_element_page': {
          return respond(
            'Elemente des Modells (nur für die Ansicht).',
            await tools.elementPage(
              asString(args, 'entity'),
              asString(args, 'name'),
              asInt(args, 'offset', 0),
              asInt(args, 'limit', 50)
            )
          );
        }
        case 'app_set_filter': {
          return respond(
            'Gefilterte LOINs (nur für die Ansicht).',
            await tools.loinsData(
              asString(args, 'search'),
              {
                discipline: asString(args, 'discipline'),
                phase: asString(args, 'phase'),
                purpose: asString(args, 'purpose'),
              },
              asInt(args, 'limit', 50),
              asBool(args, 'include_unfitting', true)
            )
          );
        }
        case 'app_check_selected': {
          const text = await tools.check(asStrings(args, 'ids'), [], 3);
          return respond(text, await tools.reportData(VIEW_EXAMPLES));
        }
        case 'app_element_mesh': {
          const ids = asStrings(args, 'element_ids');
          const data = await tools.elementMeshes(ids.length > 0 ? ids : undefined);
          return respond(
            `3D-Geometrie für ${data.totalElementsWithGeometry} Bauteile geladen.`,
            data as unknown as Record<string, unknown>
          );
        }
        default:
          throw new Error(`Unbekanntes Werkzeug: ${name}`);
      }
    } catch (err) {
      return {
        content: [{ type: 'text', text: (err as Error).message || String(err) }],
        isError: true,
      };
    }
  });

  return server;
}

const isMain =
  typeof process !== 'undefined' &&
  process.argv[1] &&
  (process.argv[1].endsWith('mcp/src/index.ts') ||
    process.argv[1].endsWith('checkmate-mcp') ||
    process.argv[1].endsWith('checkmate-mcp.mjs'));

if (isMain) {
  console.log = (...args: unknown[]) => console.error(...args);
  const server = createCheckMateMcpServer();
  const transport = new StdioServerTransport();
  server.connect(transport).catch((err) => {
    console.error('MCP server error:', err);
    process.exit(1);
  });
}
