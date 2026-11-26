/**
 * Declarative MCP catalog for CheckMate 0.4.1-beta:
 * - 10 model-facing tools (for AI assistants & users)
 * - 5 app-only tools (`visibility: ["app"]`) for interactive 3D MCP App views
 * - 3 interactive 3D HTML/WebGL MCP App resources (`ui://checkmate/*`)
 * - 2 JSON data resources (`checkmate://model/current`, `checkmate://report/last.json`)
 * - 2 guided MCP prompts (`check-loin`, `explain-element`)
 */

export const MCP_VERSION = '0.4.1-beta';

export const UI_EXTENSION = 'io.modelcontextprotocol/ui';
export const UI_MIME = 'text/html;profile=mcp-app';

export const REPORT_VIEW = 'ui://checkmate/report';
export const MODEL_VIEW = 'ui://checkmate/model';
export const LOINS_VIEW = 'ui://checkmate/loins';

export const MODEL_JSON_URI = 'checkmate://model/current';
export const REPORT_JSON_URI = 'checkmate://report/last.json';

export const INSTRUCTIONS =
  'CheckMate 0.4.1-beta (IFClite + TypeScript) checks IFC building and infrastructure models against the' +
  ' requirements of the German BIM Portal (LOINs and AIA projects, delivered as IDS) and .rules.json rule sets,' +
  ' with interactive 3D visualization and AIA/LOIN element highlighting.' +
  ' Typical order: describe_model or load_model; find_loins, or IDs the user gives; describe_requirements to see what a' +
  ' LOIN asks for; check_model; element_report for single elements; export_bcf for BCF issue export. Reports and messages' +
  " are in German. 'nicht prüfbar' means the specification selected no element - that is not a pass." +
  ' Models may name properties differently than the portal: load_mapping shows or changes the JSON mapping' +
  " that translates them (reports say 'als ...' when it was used).";

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

function tool(opts: {
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
  // --- 10 Model-Facing Tools ---
  tool({
    name: 'load_model',
    title: 'Modell laden',
    description:
      'Load an IFC model (IFC2X3, IFC4, IFC4X3, .ifczip) from a local path or HTTP(S)/GitHub link. Replaces the active model.',
    properties: {
      source: { type: 'string', description: 'Path or link of the .ifc or .ifczip file' },
    },
    required: ['source'],
    openWorld: true,
  }),
  tool({
    name: 'describe_model',
    title: 'Modell beschreiben',
    description:
      'Describe the active IFC model: name, schema, element count, frequent IFC entities, and 3D overview.',
    readOnly: true,
  }),
  tool({
    name: 'list_elements',
    title: 'Elemente auflisten',
    description:
      "List elements as 'GlobalId | entity | name | PredefinedType', filtered by IFC entity and/or part of the name.",
    properties: {
      entity: { type: 'string', description: 'IFC entity, e.g. IfcWall or IfcBeam' },
      name: { type: 'string', description: 'Part of the element name' },
      limit: { type: 'integer', description: 'Maximum elements to list (default 30)' },
    },
    readOnly: true,
  }),
  tool({
    name: 'find_loins',
    title: 'LOINs finden',
    description:
      'Search LOINs and AIA projects in the German BIM Portal and rank LOINs that fit the active model first.',
    properties: {
      search: { type: 'string', description: 'Search term (empty for full catalogue)' },
      discipline: { type: 'string', description: 'Fachmodell, e.g. Ingenieurbau' },
      phase: { type: 'string', description: 'Leistungsphase, e.g. LPH 5' },
      purpose: { type: 'string', description: 'Anwendungsfall, e.g. Mengenermittlung' },
      limit: { type: 'integer', description: 'Maximum LOINs to list (default 20)' },
      include_unfitting: { type: 'boolean', description: 'Also list LOINs that do not fit the model' },
    },
    readOnly: true,
    openWorld: true,
  }),
  tool({
    name: 'describe_requirements',
    title: 'Anforderungen beschreiben',
    description:
      'Describe what a BIM Portal LOIN or AIA project selects and requires, and how many elements of the model match.',
    properties: {
      id: { type: 'string', description: 'GUID or link of a LOIN or AIA project' },
    },
    required: ['id'],
    readOnly: true,
    openWorld: true,
  }),
  tool({
    name: 'check_model',
    title: 'Modell prüfen',
    description:
      'Check the active model against BIM Portal LOIN/AIA IDs, local IDS/ZIP files, or .rules.json files, with 3D highlighting.',
    properties: {
      ids: {
        type: 'array',
        items: { type: 'string' },
        description: 'GUIDs or links of BIM Portal LOINs or AIA projects',
      },
      ids_files: {
        type: 'array',
        items: { type: 'string' },
        description: 'Local .ids or .zip file paths',
      },
      rules_files: {
        type: 'array',
        items: { type: 'string' },
        description: 'Local .rules.json file paths (@ifc-lite/rules)',
      },
      examples: {
        type: 'integer',
        description: 'Unmet findings per specification (default 3; 0 for full report)',
      },
    },
    openWorld: true,
  }),
  tool({
    name: 'element_report',
    title: 'Elementbericht',
    description:
      'Show check findings and all IFC properties for an element by GlobalId or part of its name.',
    properties: {
      element: { type: 'string', description: 'GlobalId or part of the element name' },
    },
    required: ['element'],
    readOnly: true,
  }),
  tool({
    name: 'load_mapping',
    title: 'Mapping laden',
    description:
      "Show or switch the JSON property mapping ('default' = BIM.Hamburg, 'none' = strict IDS, or path to JSON file).",
    properties: {
      source: {
        type: 'string',
        description: "Mapping JSON path, 'none', or 'default'; omit to inspect current mapping",
      },
    },
  }),
  tool({
    name: 'export_bcf',
    title: 'BCF exportieren',
    description:
      'Export failed findings of the last check_model run as a BCF 2.1 (.bcfzip) archive.',
    properties: {
      output: { type: 'string', description: 'Target .bcfzip file path' },
    },
    required: ['output'],
  }),
  tool({
    name: 'server_status',
    title: 'Status',
    description:
      'Show active IFC model, property mapping, and verdict of the last check.',
    readOnly: true,
  }),

  // --- 5 App-Only Tools (used by interactive 3D MCP App views) ---
  tool({
    name: 'app_report_page',
    title: 'Mehr Befunde laden',
    description: 'App-only: load paginated unmet findings of a specification.',
    properties: {
      run: { type: 'integer', description: 'Run index' },
      spec: { type: 'integer', description: 'Specification index' },
      offset: { type: 'integer', description: 'Findings offset' },
      limit: { type: 'integer', description: 'Page size (default 25)' },
    },
    required: ['run', 'spec'],
    readOnly: true,
    appOnlyView: REPORT_VIEW,
  }),
  tool({
    name: 'app_element_page',
    title: 'Elemente seitenweise',
    description: 'App-only: load a filtered page of model elements.',
    properties: {
      entity: { type: 'string', description: 'IFC entity' },
      name: { type: 'string', description: 'Part of element name' },
      offset: { type: 'integer', description: 'Offset' },
      limit: { type: 'integer', description: 'Page size (default 50)' },
    },
    readOnly: true,
    appOnlyView: MODEL_VIEW,
  }),
  tool({
    name: 'app_set_filter',
    title: 'LOIN-Filter setzen',
    description: 'App-only: filter and rank LOINs in the interactive LOIN view.',
    properties: {
      search: { type: 'string', description: 'Search term' },
      discipline: { type: 'string', description: 'Fachmodell' },
      phase: { type: 'string', description: 'Leistungsphase' },
      purpose: { type: 'string', description: 'Anwendungsfall' },
      limit: { type: 'integer', description: 'Limit (default 50)' },
      include_unfitting: { type: 'boolean', description: 'Include unfitting LOINs' },
    },
    readOnly: true,
    appOnlyView: LOINS_VIEW,
  }),
  tool({
    name: 'app_check_selected',
    title: 'Ausgewählte LOINs prüfen',
    description: 'App-only: run check_model on LOINs/projects selected in the UI.',
    properties: {
      ids: {
        type: 'array',
        items: { type: 'string' },
        description: 'Selected LOIN or AIA project GUIDs',
      },
    },
    required: ['ids'],
    appOnlyView: REPORT_VIEW,
  }),
  tool({
    name: 'app_element_mesh',
    title: '3D-Bauteilgeometrie laden',
    description: 'App-only: extract 3D triangle meshes and check status colors via @ifc-lite/geometry.',
    properties: {
      element_ids: {
        type: 'array',
        items: { type: 'string' },
        description: 'Optional GlobalIds filter; empty for all elements with geometry',
      },
    },
    readOnly: true,
    appOnlyView: REPORT_VIEW,
  }),
];

export const VIEW_RESOURCES = [
  {
    uri: REPORT_VIEW,
    name: 'checkmate_report',
    title: 'CheckMate — 3D Prüfbericht & AIA/LOIN Markierungen',
    description: 'Interaktive 3D-Ansicht des Prüfberichts von check_model mit Bauteil-Markierungen',
    mimeType: UI_MIME,
    file: 'report.html',
  },
  {
    uri: MODEL_VIEW,
    name: 'checkmate_model',
    title: 'CheckMate — 3D Modellübersicht',
    description: '3D-Modellübersicht mit Entities und Elementliste',
    mimeType: UI_MIME,
    file: 'model.html',
  },
  {
    uri: LOINS_VIEW,
    name: 'checkmate_loins',
    title: 'CheckMate — BIM-Portal LOIN & AIA Auswahl',
    description: 'Interaktive Auswahl und Filterung von LOINs und AIA-Projekten',
    mimeType: UI_MIME,
    file: 'loins.html',
  },
] as const;

export const DATA_RESOURCES = [
  {
    uri: MODEL_JSON_URI,
    name: 'checkmate_model_current',
    title: 'Aktuelles IFC-Modell (JSON)',
    description: 'Metadaten und Entity-Übersicht des aktuell geladenen IFC-Modells',
    mimeType: 'application/json',
  },
  {
    uri: REPORT_JSON_URI,
    name: 'checkmate_report_last',
    title: 'Letzter Prüfbericht (JSON)',
    description: 'Vollständiger JSON-Bericht der letzten Prüfung (check_model)',
    mimeType: 'application/json',
  },
] as const;

export const MCP_PROMPTS = [
  {
    name: 'check-loin',
    title: 'LOIN-Prüfung durchführen',
    description: 'Sucht passende LOINs im BIM-Portal für das geladene Modell und führt eine Prüfung aus.',
    arguments: [
      { name: 'discipline', description: 'Optionales Fachmodell, z. B. Ingenieurbau oder Hochbau', required: false },
      { name: 'phase', description: 'Optionale Leistungsphase, z. B. LPH 3 oder LPH 8', required: false },
    ],
  },
  {
    name: 'explain-element',
    title: 'Elementbefunde erklären',
    description: 'Zeigt und erläutert alle Prüfbefunde und IFC-Merkmale eines Bauteils.',
    arguments: [
      { name: 'element', description: 'GlobalId oder Namensteil des Bauteils', required: true },
    ],
  },
] as const;
