#!/usr/bin/env node
import { writeFileSync } from 'node:fs';
import {
  applyCatalogueFilter,
  BimPortalClient,
  extractPortalId,
  type CatalogueFilter,
} from '@checkmate/bimportal';
import {
  CheckService,
  describeFit,
  EXIT_NOT_READ,
  EXIT_USAGE,
  LoadedModel,
  ModelFit,
  PropertyMapping,
  ReportJson,
  ReportText,
  type CheckSource,
} from '@checkmate/core';

export const VERSION = '0.4.1-beta';
const EXAMPLES = 8;
const DEFAULT_SEARCH = 'Kappe';

export const USAGE =
  'Aufruf: checkmate [--model <IFC-Datei oder Link>] [--loin <LOIN-ID oder Link>]' +
  ' [--aia <AIA-Projekt-ID oder Link>] [--ids <IDS-Datei oder ZIP>] [--rules <Rules-JSON-Datei>]' +
  ' [--mapping <JSON-Datei|none|default>] [--suggest]' +
  ' [--discipline <Fachmodell>] [--phase <Leistungsphase>] [--purpose <Anwendungsfall>]' +
  ' [--json <Datei>] [--bcf <Datei>] [--full] [--help] [--version] [Suchbegriff ...]\n' +
  '--loin, --aia, --ids und --rules dürfen mehrfach stehen.';

export const HELP_TEXT = `${USAGE}

Beispiele:
  checkmate --model resources/models/mini-hochbau-bundesbau.ifc --ids resources/fixtures/ids/portal/mini-hochbau-bundesbau.zip --mapping none
  checkmate --model resources/models/mini-infrastruktur-bruecke.ifc --ids resources/fixtures/ids/portal/mini-infrastruktur-bruecke.zip --mapping none
  checkmate --ids resources/fixtures/ids/portal/ueberbau-kappe-lph8-9.ids --mapping default
  checkmate --suggest "Kappe" --discipline "Ingenieurbau"`;

export interface CliArguments {
  model: string | null;
  portalIds: string[];
  idsFiles: string[];
  rulesFiles: string[];
  mapping: string | null;
  search: string | null;
  suggest: boolean;
  filter: CatalogueFilter;
  json: string | null;
  bcf: string | null;
  full: boolean;
  help: boolean;
  version: boolean;
}

export function parseCliArguments(args: readonly string[]): CliArguments {
  let model: string | null = null;
  const portalIds: string[] = [];
  const idsFiles: string[] = [];
  const rulesFiles: string[] = [];
  let mapping: string | null = null;
  const words: string[] = [];
  let suggest = false;
  let full = false;
  let help = false;
  let version = false;
  let discipline: string | null = null;
  let phase: string | null = null;
  let purpose: string | null = null;
  let json: string | null = null;
  let bcf: string | null = null;

  const value = (idx: number, missing: string): string => {
    if (idx >= args.length || args[idx].startsWith('--')) {
      throw new Error(missing);
    }
    return args[idx];
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    switch (arg) {
      case '--help':
      case '-h':
        help = true;
        break;
      case '--version':
      case '-v':
        version = true;
        break;
      case '--suggest':
        suggest = true;
        break;
      case '--full':
        full = true;
        break;
      case '--json':
        json = value(++i, '--json braucht eine Datei.');
        break;
      case '--bcf':
        bcf = value(++i, '--bcf braucht eine Datei.');
        break;
      case '--mapping':
        mapping = value(++i, '--mapping braucht eine JSON-Datei, none oder default.');
        break;
      case '--discipline':
        discipline = value(++i, '--discipline braucht ein Fachmodell.');
        break;
      case '--phase':
        phase = value(++i, '--phase braucht eine Leistungsphase.');
        break;
      case '--purpose':
        purpose = value(++i, '--purpose braucht einen Anwendungsfall.');
        break;
      case '--model':
        model = value(++i, '--model braucht eine IFC-Datei oder einen Link.');
        break;
      case '--loin':
        portalIds.push(value(++i, '--loin braucht eine LOIN-ID oder einen Link.'));
        break;
      case '--aia':
        portalIds.push(value(++i, '--aia braucht eine AIA-Projekt-ID oder einen Link.'));
        break;
      case '--ids':
        idsFiles.push(value(++i, '--ids braucht eine IDS-Datei.'));
        break;
      case '--rules':
        rulesFiles.push(value(++i, '--rules braucht eine .rules.json-Datei.'));
        break;
      default:
        if (arg.startsWith('--')) {
          throw new Error(`Unbekannte Option ${arg}`);
        }
        words.push(arg);
    }
  }

  let search: string | null = null;
  if (words.length > 0) {
    const text = words.join(' ');
    if (extractPortalId(text)) {
      portalIds.push(text);
    } else {
      search = text;
    }
  } else if (suggest) {
    search = '';
  } else if (!help && !version && portalIds.length === 0 && idsFiles.length === 0 && rulesFiles.length === 0) {
    search = DEFAULT_SEARCH;
  }

  if (suggest && json !== null) {
    throw new Error('--json gilt für Prüfungen, --suggest prüft nichts.');
  }

  return {
    model,
    portalIds,
    idsFiles,
    rulesFiles,
    mapping,
    search,
    suggest,
    filter: { discipline, phase, purpose },
    json,
    bcf,
    full,
    help,
    version,
  };
}

export async function runCli(
  args: readonly string[],
  out: (line: string) => void = console.log,
  err: (line: string) => void = console.error,
  portalClient = new BimPortalClient()
): Promise<number> {
  let parsed: CliArguments;
  try {
    parsed = parseCliArguments(args);
  } catch (e) {
    err((e as Error).message);
    err(USAGE);
    return EXIT_USAGE;
  }

  if (parsed.help) {
    out(`CheckMate ${VERSION}`);
    out(HELP_TEXT);
    return 0;
  }

  if (parsed.version) {
    out(`CheckMate ${VERSION}`);
    return 0;
  }

  let mapping: PropertyMapping;
  try {
    mapping = PropertyMapping.resolve(parsed.mapping);
  } catch (e) {
    err((e as Error).message);
    return EXIT_USAGE;
  }

  let loadedModel: LoadedModel;
  try {
    loadedModel = parsed.model
      ? await LoadedModel.read(parsed.model)
      : await LoadedModel.configured();
  } catch (e) {
    err((e as Error).message);
    return EXIT_NOT_READ;
  }

  const service = new CheckService(portalClient, mapping);

  if (parsed.suggest) {
    const found = await service.search(parsed.search ?? '');
    const filtered = applyCatalogueFilter(found.loins, parsed.filter);
    const ranked = ModelFit.of(loadedModel.elements).rank(filtered);
    const fitting = ranked.filter((r) => r.fit.kind !== 'NONE');

    out(`Modell: ${loadedModel.summary()}`);
    out(
      `${found.loins.length} LOIN(s), ${filtered.length} nach Filter, ${fitting.length} passend zum Modell; ${found.projects.length} AIA-Projekt(e)`
    );
    for (const p of found.projects) {
      out(`AIA-Projekt | ${p.guid} | ${p.name}`);
    }
    const list = fitting.length > 0 ? fitting : ranked;
    for (const r of list.slice(0, 30)) {
      out(`LOIN | ${r.entry.guid} | ${r.entry.name} | ${describeFit(r.fit)}`);
    }
    return 0;
  }

  const sources: CheckSource[] = [];
  for (const f of parsed.idsFiles) {
    sources.push({ kind: 'idsFile', path: f });
  }
  for (const r of parsed.rulesFiles) {
    sources.push({ kind: 'rulesFile', path: r });
  }
  for (const id of parsed.portalIds) {
    sources.push({ kind: 'portal', idOrLink: id });
  }

  if (parsed.search !== null && sources.length === 0) {
    const found = await service.search(parsed.search);
    const filtered = applyCatalogueFilter(found.loins, parsed.filter);
    const ranked = ModelFit.of(loadedModel.elements).rank(filtered);
    const fitting = ranked.filter((r) => r.fit.kind !== 'NONE');
    const chosen = (fitting.length > 0 ? fitting : ranked).slice(0, 5);
    for (const c of chosen) {
      sources.push({ kind: 'portal', idOrLink: c.entry.guid });
    }
  }

  const report = await service.check(sources, loadedModel);

  if (parsed.full) {
    for (const line of ReportText.full(report)) out(line);
  } else {
    for (const line of report.header()) out(line);
    out('');
    for (const run of report.runs) {
      for (const line of ReportText.brief(run, EXAMPLES, report.model.rawSchema)) out(line);
    }
    out(ReportText.verdictLine(report));
  }

  if (parsed.json) {
    writeFileSync(parsed.json, ReportJson.stringify(report), 'utf-8');
  }

  if (parsed.bcf) {
    const bcfBytes = await service.exportBcf(report);
    writeFileSync(parsed.bcf, bcfBytes);
  }

  return report.exitCode();
}

const isMain =
  typeof process !== 'undefined' &&
  process.argv[1] &&
  (process.argv[1].endsWith('cli/src/index.ts') ||
    process.argv[1].endsWith('checkmate') ||
    process.argv[1].endsWith('checkmate.mjs'));

if (isMain) {
  runCli(process.argv.slice(2)).then(
    (code) => process.exit(code),
    (err) => {
      console.error(err);
      process.exit(1);
    }
  );
}
