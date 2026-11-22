#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  ReadResourceRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { CheckMateTools } from './tools.js';

export { CheckMateTools } from './tools.js';

export const UI_EXTENSION = 'io.modelcontextprotocol/ui';
export const UI_MIME = 'text/html;profile=mcp-app';

export const REPORT_VIEW = 'ui://checkmate/report';
export const MODEL_VIEW = 'ui://checkmate/model';
export const LOINS_VIEW = 'ui://checkmate/loins';

export const MAX_ANSWER = 50_000;
export const VIEW_EXAMPLES = 10;

export const INSTRUCTIONS =
  'CheckMate 0.4 (IFClite + TypeScript) checks IFC building models against the requirements of the German BIM Portal' +
  ' (LOINs and AIA projects, delivered as IDS) and .rules.json rule sets, with 3D visualization and AIA/LOIN element highlighting.' +
  ' Typical order: describe_model or load_model; find_loins, or IDs the user gives; describe_requirements to see what a' +
  ' LOIN asks for; check_model; element_report for single elements; export_bcf for BCF issue export. Reports and messages' +
  " are in German. 'nicht prüfbar' means the specification selected no element - that" +
  ' is not a pass. Models name properties differently than the portal: load_mapping shows' +
  " or changes the JSON mapping that translates them (reports say 'als ...' when it was used).";

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
  const viewPath = resolve(__dirname, '../views', fileName);
  return readFileSync(viewPath, 'utf-8');
}

export interface ToolDef {
  name: string;
  title: string;
  description: string;
  inputSchema: Record<string, unknown>;
  annotations: {
    title: string;
    readOnlyHint: boolean;
    destructiveHint: boolean;
    idempotentHint: boolean;
    openWorldHint: boolean;
  };
  _meta?: Record<string, unknown>;
}

const VIEWS_BY_TOOL: Record<string, string> = {
  check_model: REPORT_VIEW,
  describe_model: MODEL_VIEW,
  load_model: MODEL_VIEW,
  find_loins: LOINS_VIEW,
};

function buildToolDef(opts: {
  name: string;
  title: string;
  description: string;
  properties?: Record<string, unknown>;
  required?: string[];
  readOnly?: boolean;
  openWorld?: boolean;
  appOnlyView?: string;
}): ToolDef {
  const readOnly = opts.readOnly ?? false;
  const openWorld = opts.openWorld ?? false;
  const def: ToolDef = {
    name: opts.name,
    title: opts.title,
    description: opts.description,
    inputSchema: {
      type: 'object',
      properties: opts.properties ?? {},
      required: opts.required ?? [],
    },
    annotations: {
      title: opts.title,
      readOnlyHint: readOnly,
      destructiveHint: false,
      idempotentHint: readOnly,
      openWorldHint: openWorld,
    },
  };

  if (opts.appOnlyView) {
    def._meta = {
      ui: {
        resourceUri: opts.appOnlyView,
        visibility: ['app'],
      },
    };
  } else if (VIEWS_BY_TOOL[opts.name]) {
    const uri = VIEWS_BY_TOOL[opts.name];
    def._meta = {
      ui: { resourceUri: uri },
      'ui/resourceUri': uri,
    };
  }
  return def;
}

export const TOOL_DEFINITIONS: readonly ToolDef[] = [
  buildToolDef({
    name: 'load_model',
    title: 'Modell laden',
    description:
      'Load an IFC model (IFC2X3, IFC4, IFC4X3, .ifczip) from a local path or a link (http/https; a GitHub file link works as copied from the browser). It replaces the model in use.',
    properties: {
      source: { type: 'string', description: 'Path or link of the .ifc or .ifczip file' },
    },
    required: ['source'],
    readOnly: false,
    openWorld: true,
  }),
  buildToolDef({
    name: 'describe_model',
    title: 'Modell beschreiben',
    description:
      'The model in use: name, schema, number of elements, most frequent IFC entities, and 3D overview.',
    readOnly: true,
    openWorld: false,
  }),
  buildToolDef({
    name: 'list_elements',
    title: 'Elemente auflisten',
    description:
      "Elements of the model as 'GlobalId | entity | name | PredefinedType', filtered by IFC entity and/or a part of the name.",
    properties: {
      entity: { type: 'string', description: 'IFC entity, e.g. IfcWall' },
      name: { type: 'string', description: 'Part of the element name' },
      limit: { type: 'integer', description: 'Most elements to list, default 30' },
    },
    readOnly: true,
    openWorld: false,
  }),
  buildToolDef({
    name: 'find_loins',
    title: 'LOINs finden',
    description:
      'Find requirements in the German BIM Portal: AIA projects and LOINs (Level of Information Need). LOINs whose object type occurs in the model come first.',
    properties: {
      search: { type: 'string', description: 'Search term for the names; empty for the whole catalogue' },
      discipline: { type: 'string', description: 'Fachmodell, e.g. Ingenieurbau' },
      phase: { type: 'string', description: 'Leistungsphase, e.g. LPH 5' },
      purpose: { type: 'string', description: 'Anwendungsfall, e.g. Mengenermittlung' },
      limit: { type: 'integer', description: 'Most LOINs to list, default 20' },
      include_unfitting: { type: 'boolean', description: 'Also list LOINs that do not fit the model' },
    },
    readOnly: true,
    openWorld: true,
  }),
  buildToolDef({
    name: 'describe_requirements',
    title: 'Anforderungen beschreiben',
    description:
      'What a LOIN or AIA project from the portal requires, by its ID or a link holding it, and how many elements of the model it would apply to.',
    properties: {
      id: { type: 'string', description: 'ID of a LOIN or AIA project, or a link holding one' },
    },
    required: ['id'],
    readOnly: true,
    openWorld: true,
  }),
  buildToolDef({
    name: 'check_model',
    title: 'Modell prüfen',
    description:
      'Check the model against LOINs or AIA projects (portal IDs or links), local IDS files, and/or .rules.json files. Renders an interactive 3D MCP App view highlighting failed and passed elements.',
    properties: {
      ids: {
        type: 'array',
        items: { type: 'string' },
        description: 'IDs of LOINs or AIA projects, or links holding them',
      },
      ids_files: {
        type: 'array',
        items: { type: 'string' },
        description: 'Paths of local IDS files or ZIPs of them',
      },
      rules_files: {
        type: 'array',
        items: { type: 'string' },
        description: 'Paths of local .rules.json files (@ifc-lite/rules)',
      },
      examples: {
        type: 'integer',
        description: 'Unmet requirements to show per specification, default 3; 0 for the full report',
      },
    },
    readOnly: false,
    openWorld: true,
  }),
  buildToolDef({
    name: 'element_report',
    title: 'Elementbericht',
    description:
      'What the last check_model said about an element, found by GlobalId or a part of its name, with the properties it carries.',
    properties: {
      element: { type: 'string', description: 'GlobalId or part of the element name' },
    },
    required: ['element'],
    readOnly: true,
    openWorld: false,
  }),
  buildToolDef({
    name: 'app_report_page',
    title: 'Mehr Befunde laden',
    description:
      'For the check view only: more unmet findings of one specification of the last check.',
    properties: {
      run: { type: 'integer', description: 'Index of the run, from the check data' },
      spec: { type: 'integer', description: 'Index of the specification within the run' },
      offset: { type: 'integer', description: 'Findings to skip' },
      limit: { type: 'integer', description: 'Findings to return, default 25' },
    },
    required: ['run', 'spec'],
    readOnly: true,
    appOnlyView: REPORT_VIEW,
  }),
  buildToolDef({
    name: 'app_element_page',
    title: 'Elemente seitenweise',
    description:
      "For the model view only: one page of the model's elements, filtered by entity and part of the name.",
    properties: {
      entity: { type: 'string', description: 'IFC entity, e.g. IfcWall' },
      name: { type: 'string', description: 'Part of the element name' },
      offset: { type: 'integer', description: 'Elements to skip' },
      limit: { type: 'integer', description: 'Elements to return, default 50' },
    },
    readOnly: true,
    appOnlyView: MODEL_VIEW,
  }),
  buildToolDef({
    name: 'app_set_filter',
    title: 'LOIN-Filter setzen',
    description:
      'For the LOIN picker view only: filter and rank LOINs by discipline, phase, purpose and search term.',
    properties: {
      search: { type: 'string', description: 'Search term' },
      discipline: { type: 'string', description: 'Fachmodell' },
      phase: { type: 'string', description: 'Leistungsphase' },
      purpose: { type: 'string', description: 'Anwendungsfall' },
      limit: { type: 'integer', description: 'Limit, default 50' },
      include_unfitting: { type: 'boolean', description: 'Include unfitting LOINs' },
    },
    readOnly: true,
    appOnlyView: LOINS_VIEW,
  }),
  buildToolDef({
    name: 'app_check_selected',
    title: 'Ausgewählte LOINs prüfen',
    description:
      'For the LOIN picker view only: check the model against the LOINs/projects selected in the UI.',
    properties: {
      ids: {
        type: 'array',
        items: { type: 'string' },
        description: 'Selected LOIN or AIA project GUIDs',
      },
    },
    required: ['ids'],
    readOnly: false,
    appOnlyView: REPORT_VIEW,
  }),
  buildToolDef({
    name: 'app_element_mesh',
    title: '3D-Bauteilgeometrie laden',
    description:
      'For the 3D views only: extract triangle meshes and AIA/LOIN status colors via @ifc-lite/geometry.',
    properties: {
      element_ids: {
        type: 'array',
        items: { type: 'string' },
        description: 'Optional GlobalIds to filter; empty for all elements with geometry',
      },
    },
    readOnly: true,
    appOnlyView: REPORT_VIEW,
  }),
  buildToolDef({
    name: 'load_mapping',
    title: 'Mapping laden',
    description:
      "Which property addresses a model uses for the ones a LOIN asks for, as a JSON mapping. Without source, shows the mapping in use. 'none' switches aliases off, 'default' is the bundled BIM.Hamburg one.",
    properties: {
      source: {
        type: 'string',
        description: "Path of a mapping JSON file, 'none' or 'default'; empty to show it",
      },
    },
    readOnly: false,
    openWorld: false,
  }),
  buildToolDef({
    name: 'export_bcf',
    title: 'BCF exportieren',
    description:
      'Export the failed checks of the last check_model run as a BCF 2.1 (.bcfzip) archive via @ifc-lite/bcf.',
    properties: {
      output: { type: 'string', description: 'Target file path for the .bcfzip file' },
    },
    required: ['output'],
    readOnly: false,
    openWorld: false,
  }),
  buildToolDef({
    name: 'server_status',
    title: 'Status',
    description:
      'What this session holds: the model in use, the property mapping and the verdict of the last check.',
    readOnly: true,
    openWorld: false,
  }),
];

export const VIEW_RESOURCES = [
  {
    uri: REPORT_VIEW,
    name: 'checkmate_report',
    title: 'CheckMate 0.4 — 3D Prüfbericht & AIA/LOIN Markierungen',
    description: 'Interaktive 3D-Ansicht des Prüfberichts von check_model mit Bauteil-Markierungen',
    mimeType: UI_MIME,
    file: 'report.html',
  },
  {
    uri: MODEL_VIEW,
    name: 'checkmate_model',
    title: 'CheckMate 0.4 — 3D Modellübersicht',
    description: '3D-Modellübersicht mit Entities und Elementliste',
    mimeType: UI_MIME,
    file: 'model.html',
  },
  {
    uri: LOINS_VIEW,
    name: 'checkmate_loins',
    title: 'CheckMate 0.4 — BIM-Portal LOIN & AIA Auswahl',
    description: 'Interaktive Auswahl und Filterung von LOINs und AIA-Projekten',
    mimeType: UI_MIME,
    file: 'loins.html',
  },
];

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
    {
      name: 'checkmate',
      version: '0.4.0',
    },
    {
      capabilities: {
        tools: {},
        resources: {},
        extensions: {
          [UI_EXTENSION]: {},
        },
      } as Record<string, unknown>,
      instructions: INSTRUCTIONS,
    }
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [...TOOL_DEFINITIONS],
  }));

  server.setRequestHandler(ListResourcesRequestSchema, async () => ({
    resources: VIEW_RESOURCES.map((r) => ({
      uri: r.uri,
      name: r.name,
      title: r.title,
      description: r.description,
      mimeType: r.mimeType,
      _meta: { ui: { prefersBorder: true } },
    })),
  }));

  server.setRequestHandler(ReadResourceRequestSchema, async (req) => {
    const found = VIEW_RESOURCES.find((r) => r.uri === req.params.uri);
    if (!found) {
      throw new Error(`Unbekannte Ressource: ${req.params.uri}`);
    }
    return {
      contents: [
        {
          uri: found.uri,
          mimeType: found.mimeType,
          text: readViewHtml(found.file),
          _meta: { ui: { prefersBorder: true } },
        },
      ],
    };
  });

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
          const src = asString(args, 'source') ?? '';
          const text = await tools.loadModel(src);
          const data = await tools.modelData();
          return respond(text, data);
        }
        case 'describe_model': {
          const text = await tools.describeModel();
          const data = await tools.modelData();
          return respond(text, data);
        }
        case 'list_elements': {
          const text = await tools.listElements(
            asString(args, 'entity'),
            asString(args, 'name'),
            asInt(args, 'limit', 30)
          );
          return respond(text);
        }
        case 'find_loins': {
          const filter = {
            discipline: asString(args, 'discipline'),
            phase: asString(args, 'phase'),
            purpose: asString(args, 'purpose'),
          };
          const search = asString(args, 'search');
          const limit = asInt(args, 'limit', 20);
          const includeUnfitting = asBool(args, 'include_unfitting', false);
          const text = await tools.findLoins(search, filter, limit, includeUnfitting);
          const data = await tools.loinsData(search, filter, Math.max(limit, 50), true);
          return respond(text, data);
        }
        case 'describe_requirements': {
          const id = asString(args, 'id') ?? '';
          return respond(await tools.describeRequirements(id));
        }
        case 'check_model': {
          const text = await tools.check(
            asStrings(args, 'ids'),
            asStrings(args, 'ids_files'),
            asInt(args, 'examples', 3),
            asStrings(args, 'rules_files')
          );
          const data = await tools.reportData(VIEW_EXAMPLES);
          return respond(text, data);
        }
        case 'element_report': {
          const el = asString(args, 'element') ?? '';
          return respond(await tools.elementReport(el));
        }
        case 'app_report_page': {
          const data = tools.reportPage(
            asInt(args, 'run', -1),
            asInt(args, 'spec', -1),
            asInt(args, 'offset', 0),
            asInt(args, 'limit', 25)
          );
          return respond('Weitere Befunde der Prüfung (nur für die Ansicht).', data);
        }
        case 'app_element_page': {
          const data = await tools.elementPage(
            asString(args, 'entity'),
            asString(args, 'name'),
            asInt(args, 'offset', 0),
            asInt(args, 'limit', 50)
          );
          return respond('Elemente des Modells (nur für die Ansicht).', data);
        }
        case 'app_set_filter': {
          const filter = {
            discipline: asString(args, 'discipline'),
            phase: asString(args, 'phase'),
            purpose: asString(args, 'purpose'),
          };
          const data = await tools.loinsData(
            asString(args, 'search'),
            filter,
            asInt(args, 'limit', 50),
            asBool(args, 'include_unfitting', true)
          );
          return respond('Gefilterte LOINs (nur für die Ansicht).', data);
        }
        case 'app_check_selected': {
          const text = await tools.check(asStrings(args, 'ids'), [], 3);
          const data = await tools.reportData(VIEW_EXAMPLES);
          return respond(text, data);
        }
        case 'app_element_mesh': {
          const ids = asStrings(args, 'element_ids');
          const data = await tools.elementMeshes(ids.length > 0 ? ids : undefined);
          return respond(
            `3D-Geometrie für ${data.totalElementsWithGeometry} Bauteile geladen.`,
            data as unknown as Record<string, unknown>
          );
        }
        case 'load_mapping': {
          return respond(tools.mapping(asString(args, 'source')));
        }
        case 'export_bcf': {
          const outPath = asString(args, 'output') ?? 'checkmate.bcfzip';
          return respond(await tools.exportBcfFile(outPath));
        }
        case 'server_status': {
          return respond(tools.status());
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
    process.argv[1].endsWith('checkmate-mcp'));

if (isMain) {
  // Redirect console.log to stderr so stdout carries only JSON-RPC protocol messages
  console.log = (...args: unknown[]) => console.error(...args);
  const server = createCheckMateMcpServer();
  const transport = new StdioServerTransport();
  server.connect(transport).catch((err) => {
    console.error('MCP server error:', err);
    process.exit(1);
  });
}
