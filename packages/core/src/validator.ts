import { readFileSync } from 'node:fs';
import {
  parseIDS,
  validateIDS,
  checkFacet,
  formatConstraint,
  createTranslationService,
  type IDSDocument,
  type IDSSpecification,
  type IDSFacet,
  type IDSPropertyFacet,
  type IDSConstraint,
  type IFCDataAccessor,
  type PropertyValueResult,
  type PropertySetInfo,
  type IDSValidationReport,
} from '@ifc-lite/ids';
import {
  idsToRuleSet,
  ruleSetToIds,
  runRuleSet,
  type RuleSetFile,
  type EvaluatorModel,
} from '@ifc-lite/rules';
import {
  createBCFFromIDSReport,
  writeBCF,
  type IDSBCFExportOptions,
} from '@ifc-lite/bcf';
import {
  BimPortalClient,
  extractPortalId,
  unpackIdsBytes,
  type CatalogueEntry,
  type SimpleAiaProjectPublicDto,
} from '@checkmate/bimportal';
import {
  compareSpelling,
  createPropertyKey,
  describeSpellingDiff,
  formatPropertyKey,
  PropertyMapping,
  type PropertyKey,
} from './mapping.js';
import { LoadedModel, type ModelElement, type ModelInfo } from './model.js';

/**
 * Decorator around IFClite's `IFCDataAccessor` that resolves property aliases
 * from a JSON `PropertyMapping` after checking the requested IDS address.
 * Every alias hit is recorded in `aliasTraces` so the report explicitly states
 * `"als Pset_Objektinformation/_IDEbene3"`.
 */
export class MappedDataAccessor implements IFCDataAccessor {
  readonly base: IFCDataAccessor;
  readonly mapping: PropertyMapping;
  readonly queryableIds?: readonly number[];
  readonly aliasTraces = new Map<string, PropertyKey>();

  constructor(
    base: IFCDataAccessor,
    mapping: PropertyMapping,
    queryableIds?: readonly number[]
  ) {
    this.base = base;
    this.mapping = mapping;
    this.queryableIds = queryableIds;
  }

  private traceKey(expressId: number, pset: string, prop: string): string {
    return `${expressId}:${pset}/${prop}`;
  }

  usedAlias(expressId: number, requested: PropertyKey): PropertyKey | null {
    return (
      this.aliasTraces.get(
        this.traceKey(expressId, requested.propertySet, requested.baseName)
      ) ?? null
    );
  }

  getEntityType(expressId: number): string | undefined {
    return this.base.getEntityType(expressId);
  }
  getEntityName(expressId: number): string | undefined {
    return this.base.getEntityName(expressId);
  }
  getGlobalId(expressId: number): string | undefined {
    return this.base.getGlobalId(expressId);
  }
  getDescription(expressId: number): string | undefined {
    return this.base.getDescription(expressId);
  }
  getObjectType(expressId: number): string | undefined {
    return this.base.getObjectType(expressId);
  }
  getPredefinedTypeRaw(expressId: number): string | undefined {
    return this.base.getPredefinedTypeRaw?.(expressId);
  }
  getAttributeNames(expressId: number): string[] {
    return this.base.getAttributeNames?.(expressId) ?? [];
  }
  getAttributeXsdTypes(expressId: number, attrName: string): readonly string[] | undefined {
    return this.base.getAttributeXsdTypes?.(expressId, attrName);
  }
  getSchemaVersion(): string | undefined {
    return this.base.getSchemaVersion?.();
  }
  getTypeEntityType(expressId: number): string | undefined {
    return this.base.getTypeEntityType?.(expressId);
  }
  getEntitiesByType(typeName: string): number[] {
    return this.base.getEntitiesByType(typeName);
  }
  getAllEntityIds(): number[] {
    return this.queryableIds ? [...this.queryableIds] : this.base.getAllEntityIds();
  }

  getPropertyValue(
    expressId: number,
    propertySetName: string,
    propertyName: string
  ): PropertyValueResult | undefined {
    const requested = createPropertyKey(propertySetName, propertyName);
    const candidates = this.mapping.candidates(requested);
    for (const candidate of candidates) {
      const hit = this.base.getPropertyValue(
        expressId,
        candidate.propertySet,
        candidate.baseName
      );
      if (hit !== undefined && hit.value !== null && hit.value !== undefined && String(hit.value).trim() !== '') {
        if (this.mapping.isAlias(requested, candidate)) {
          this.aliasTraces.set(
            this.traceKey(expressId, requested.propertySet, requested.baseName),
            candidate
          );
        }
        return {
          ...hit,
          propertySetName: requested.propertySet,
          propertyName: requested.baseName,
        };
      }
    }
    return this.base.getPropertyValue(expressId, propertySetName, propertyName);
  }

  getPropertySets(expressId: number): PropertySetInfo[] {
    const baseSets = this.base.getPropertySets(expressId);
    if (this.mapping.aliases.size === 0) return baseSets;

    const cloned: PropertySetInfo[] = baseSets.map((s) => ({
      name: s.name,
      properties: [...s.properties],
    }));

    const hasExact = (psetName: string, propName: string): boolean => {
      const pset = cloned.find((s) => s.name === psetName);
      if (!pset) return false;
      const p = pset.properties.find((pr) => pr.name === propName);
      return p !== undefined && p.value !== null && p.value !== undefined && String(p.value).trim() !== '';
    };

    for (const [fromStr, targets] of this.mapping.aliases.entries()) {
      const slash = fromStr.indexOf('/');
      const reqKey = createPropertyKey(fromStr.slice(0, slash), fromStr.slice(slash + 1));
      if (hasExact(reqKey.propertySet, reqKey.baseName)) continue;

      for (const target of targets) {
        const srcSet = baseSets.find((s) => s.name === target.propertySet);
        const srcProp = srcSet?.properties.find((p) => p.name === target.baseName);
        if (
          srcProp &&
          srcProp.value !== null &&
          srcProp.value !== undefined &&
          String(srcProp.value).trim() !== ''
        ) {
          let destSet = cloned.find((s) => s.name === reqKey.propertySet);
          if (!destSet) {
            destSet = { name: reqKey.propertySet, properties: [] };
            cloned.push(destSet);
          }
          destSet.properties.push({
            ...srcProp,
            name: reqKey.baseName,
          });
          this.aliasTraces.set(
            this.traceKey(expressId, reqKey.propertySet, reqKey.baseName),
            target
          );
          break;
        }
      }
    }
    return cloned;
  }

  getClassifications(expressId: number) {
    return this.base.getClassifications(expressId);
  }
  getMaterials(expressId: number) {
    return this.base.getMaterials(expressId);
  }
  getParent(expressId: number, relationType: Parameters<IFCDataAccessor['getParent']>[1]) {
    return this.base.getParent(expressId, relationType);
  }
  getAncestors(expressId: number, relationType: Parameters<IFCDataAccessor['getParent']>[1]) {
    return this.base.getAncestors?.(expressId, relationType) ?? [];
  }
  getAttribute(expressId: number, attributeName: string) {
    return this.base.getAttribute(expressId, attributeName);
  }
}

export type FindingStatus =
  | 'FULFILLED'
  | 'MISSING'
  | 'WRONG_VALUE'
  | 'WRONG_TYPE'
  | 'PROHIBITED';

export function findingStatusLabel(status: FindingStatus): string {
  switch (status) {
    case 'FULFILLED':
      return 'erfüllt';
    case 'MISSING':
      return 'Merkmal fehlt';
    case 'WRONG_VALUE':
      return 'Wert weicht ab';
    case 'WRONG_TYPE':
      return 'Datentyp weicht ab';
    case 'PROHIBITED':
      return 'verbotenes Merkmal vorhanden';
  }
}

export function isFindingOk(status: FindingStatus): boolean {
  return status === 'FULFILLED';
}

export type Verdict = 'PASSED' | 'FAILED' | 'PARTLY_EVALUATED' | 'NOT_APPLICABLE';

export function verdictLabel(verdict: Verdict): string {
  switch (verdict) {
    case 'PASSED':
      return 'erfüllt';
    case 'FAILED':
      return 'nicht erfüllt';
    case 'PARTLY_EVALUATED':
      return 'erfüllt (soweit ausgewertet)';
    case 'NOT_APPLICABLE':
      return 'nicht prüfbar';
  }
}

export interface Finding {
  readonly elementId: string;
  readonly expressId: number;
  readonly elementLabel: string;
  readonly requirement: string;
  readonly status: FindingStatus;
  readonly detail: string;
  line(): string;
  requirementLine(): string;
}

export function createFinding(
  elementId: string,
  expressId: number,
  elementLabel: string,
  requirement: string,
  status: FindingStatus,
  detail: string
): Finding {
  return {
    elementId,
    expressId,
    elementLabel,
    requirement,
    status,
    detail,
    line() {
      return `[${findingStatusLabel(status)}] ${elementLabel} - ${requirement} - ${detail}`;
    },
    requirementLine() {
      return `[${findingStatusLabel(status)}] ${requirement} - ${detail}`;
    },
  };
}

export interface FacetMatch {
  readonly description: string;
  readonly matchedCount: number;
  readonly note?: string;
  line(): string;
}

export interface NearMiss {
  readonly elementId: string;
  readonly elementLabel: string;
  readonly matchedFacets: number;
  readonly totalFacets: number;
  readonly actualValues: string[];
  line(): string;
}

export interface Diagnosis {
  readonly facets: readonly FacetMatch[];
  readonly nearMisses: readonly NearMiss[];
  lines(): string[];
}

export interface SpecCheckResult {
  readonly specificationName: string;
  readonly specificationLabel: string;
  readonly ifcVersions: readonly string[];
  readonly examinedElements: number;
  readonly applicableElements: number;
  readonly passingElements: number;
  readonly failingElements: number;
  readonly verdict: Verdict;
  readonly verdictLabel: string;
  readonly findings: readonly Finding[];
  readonly diagnosis: Diagnosis;
  readonly hints: readonly string[];
  summary(): string;
  overviewLine(): string;
}

export interface CheckRun {
  readonly name: string;
  readonly results: readonly SpecCheckResult[];
  readonly error: string | null;
  readonly rawIdsReport?: IDSValidationReport;
}

export const EXIT_PASSED = 0;
export const EXIT_NOT_READ = 1;
export const EXIT_USAGE = 2;
export const EXIT_FAILED = 3;
export const EXIT_NOT_APPLICABLE = 4;

export class CheckReport {
  readonly checkedAt: Date;
  readonly model: ModelInfo;
  readonly runs: readonly CheckRun[];

  constructor(checkedAt: Date, model: ModelInfo, runs: readonly CheckRun[]) {
    this.checkedAt = checkedAt;
    this.model = model;
    this.runs = runs;
  }

  results(): SpecCheckResult[] {
    return this.runs.flatMap((r) => r.results);
  }

  exitCode(): number {
    if (this.runs.some((r) => r.error !== null)) return EXIT_NOT_READ;
    const all = this.results();
    if (all.length === 0) return EXIT_NOT_APPLICABLE;
    if (all.some((r) => r.verdict === 'FAILED')) return EXIT_FAILED;
    if (all.every((r) => r.verdict === 'NOT_APPLICABLE')) return EXIT_NOT_APPLICABLE;
    return EXIT_PASSED;
  }

  verdict(): string {
    const all = this.results();
    const unread = this.runs.filter((r) => r.error !== null).length;
    if (all.length === 0) {
      return unread === 0 ? 'nichts geprüft' : `nichts geprüft; ${unread} nicht lesbar`;
    }
    const counts = new Map<Verdict, number>();
    for (const r of all) {
      counts.set(r.verdict, (counts.get(r.verdict) ?? 0) + 1);
    }
    const order: Verdict[] = ['PASSED', 'PARTLY_EVALUATED', 'FAILED', 'NOT_APPLICABLE'];
    const parts: string[] = [];
    for (const v of order) {
      const c = counts.get(v);
      if (c) parts.push(`${c} ${verdictLabel(v)}`);
    }
    const base = `${all.length} Spezifikation(en) für ${this.model.rawSchema}: ${parts.join(', ')}`;
    return unread === 0 ? base : `${base}; ${unread} nicht lesbar`;
  }

  header(): string[] {
    const pad = (n: number) => String(n).padStart(2, '0');
    const d = this.checkedAt;
    const ts = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    return [
      `CheckMate-Prüfung vom ${ts}`,
      `Modell: ${this.model.name} - Schema ${this.model.rawSchema}, ${this.model.elements} Objekte`,
    ];
  }
}

export type CheckSource =
  | { kind: 'portal'; idOrLink: string }
  | { kind: 'idsFile'; path: string }
  | { kind: 'idsXml'; name: string; xml: string }
  | { kind: 'rulesFile'; path: string }
  | { kind: 'rulesJson'; name: string; ruleSet: RuleSetFile };

function constraintToString(c: IDSConstraint | undefined): string {
  if (!c) return '*';
  if (typeof c === 'string') return `'${c}'`;
  if (c.type === 'simpleValue') return `'${c.value}'`;
  return formatConstraint(c);
}

function facetString(c: IDSConstraint | string | undefined): string {
  if (!c) return '';
  if (typeof c === 'string') return c;
  if (c.type === 'simpleValue') return c.value;
  return formatConstraint(c);
}

function describeFacet(facet: IDSFacet): string {
  switch (facet.type) {
    case 'entity': {
      const name = constraintToString(facet.name);
      return facet.predefinedType
        ? `Entity ${name} (PredefinedType ${constraintToString(facet.predefinedType)})`
        : `Entity ${name}`;
    }
    case 'property': {
      const pset = facetString(facet.propertySet);
      const base = facetString(facet.baseName);
      const val = facet.value ? ` = ${constraintToString(facet.value)}` : '';
      const dt = facet.dataType ? ` (${facetString(facet.dataType)})` : '';
      return `${pset}/${base}${val}${dt}`;
    }
    case 'attribute': {
      const name = facetString(facet.name);
      const val = facet.value ? ` = ${constraintToString(facet.value)}` : '';
      return `Attribut ${name}${val}`;
    }
    case 'classification':
      return `Klassifikation ${facet.system ? constraintToString(facet.system) : ''} ${facet.value ? constraintToString(facet.value) : ''}`.trim();
    case 'material':
      return `Material ${facet.value ? constraintToString(facet.value) : 'vorhanden'}`;
    case 'partOf':
      return `Teil von (${facet.relation})`;
  }
}

const RELATED_STRING_TYPES = new Set([
  'IFCLABEL',
  'IFCTEXT',
  'IFCIDENTIFIER',
  'IFCDESCRIPTION',
]);

function areRelatedTypes(
  a: IDSConstraint | string | undefined,
  b: IDSConstraint | string | undefined
): boolean {
  const sa = facetString(a);
  const sb = facetString(b);
  if (!sa || !sb) return false;
  const ua = sa.toUpperCase();
  const ub = sb.toUpperCase();
  if (ua === ub) return false;
  if (RELATED_STRING_TYPES.has(ua) && RELATED_STRING_TYPES.has(ub)) return true;
  return false;
}

/**
 * Diagnoses why a specification matched 0 elements (`nicht prüfbar`):
 * checks each applicability facet individually and finds the closest near-miss elements.
 */
function diagnoseZeroApplicability(
  spec: IDSSpecification,
  model: LoadedModel,
  accessor: MappedDataAccessor
): { diagnosis: Diagnosis; hints: string[] } {
  const facets = spec.applicability.facets;
  if (facets.length === 0) {
    return {
      diagnosis: { facets: [], nearMisses: [], lines: () => [] },
      hints: [],
    };
  }

  const translator = createTranslationService('de');
  const facetMatches: FacetMatch[] = [];
  const perElementMatched = new Map<number, number>();
  const hints: string[] = [];

  for (const facet of facets) {
    let matchedCount = 0;
    const relatedTypeHits: string[] = [];
    const spellingHits: string[] = [];

    for (const el of model.elements) {
      const res = checkFacet(facet, el.expressId, accessor, translator);
      if (res.passed) {
        matchedCount++;
        perElementMatched.set(el.expressId, (perElementMatched.get(el.expressId) ?? 0) + 1);
      } else if (
        facet.type === 'property' &&
        facet.propertySet.type === 'simpleValue' &&
        facet.baseName.type === 'simpleValue'
      ) {
        const reqKey = createPropertyKey(facet.propertySet.value, facet.baseName.value);
        const propVal = accessor.getPropertyValue(
          el.expressId,
          reqKey.propertySet,
          reqKey.baseName
        );
        const expectedDt = facetString(facet.dataType);
        const expectedVal = facetString(facet.value);
        if (propVal && propVal.value !== null && propVal.value !== undefined) {
          const valMatches =
            !expectedVal || String(propVal.value).trim() === expectedVal.trim();
          if (valMatches && expectedDt && areRelatedTypes(expectedDt, propVal.dataType)) {
            relatedTypeHits.push(`${propVal.dataType} statt ${expectedDt.toUpperCase()}`);
            // Value matched via alias or primary key, only dataType differed -> near miss candidate!
            perElementMatched.set(
              el.expressId,
              (perElementMatched.get(el.expressId) ?? 0) + 1
            );
          }
        } else {
          for (const p of el.properties) {
            const diff = compareSpelling(reqKey, p.key);
            if (diff === 'CASE_ONLY' || diff === 'SIMILAR') {
              spellingHits.push(`${formatPropertyKey(p.key)} (${describeSpellingDiff(diff)})`);
            }
          }
        }
      }
    }

    let note: string | undefined;
    if (matchedCount === 0 && relatedTypeHits.length > 0) {
      const sample = Array.from(new Set(relatedTypeHits)).slice(0, 2).join(', ');
      note = `Hinweis: ${relatedTypeHits.length} Element(e) mit verwandtem Datentyp (${sample})`;
      hints.push(note);
    } else if (matchedCount === 0 && spellingHits.length > 0) {
      const sample = Array.from(new Set(spellingHits)).slice(0, 2).join(', ');
      note = `Hinweis: ähnliche Schreibweise im Modell gefunden: ${sample}`;
      hints.push(note);
    }

    const desc = describeFacet(facet);
    facetMatches.push({
      description: desc,
      matchedCount,
      note,
      line() {
        return `   Facette ${desc}: ${matchedCount} von ${model.elements.length} Element(en)${note ? ` [${note}]` : ''}`;
      },
    });
  }

  // Collect near misses (elements that matched at least one facet, sorted by highest match count)
  const nearCandidates = [...perElementMatched.entries()]
    .filter(([, count]) => count > 0 && count < facets.length)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  const nearMisses: NearMiss[] = nearCandidates.map(([expressId, count]) => {
    const el = model.byExpressId.get(expressId)!;
    const label = `${el.entityName} '${el.name}' (${el.id})`;
    const actualValues: string[] = [];

    for (const facet of facets) {
      if (
        facet.type === 'property' &&
        facet.propertySet.type === 'simpleValue' &&
        facet.baseName.type === 'simpleValue'
      ) {
        const reqKey = createPropertyKey(facet.propertySet.value, facet.baseName.value);
        const pv = accessor.getPropertyValue(expressId, reqKey.propertySet, reqKey.baseName);
        const alias = accessor.usedAlias(expressId, reqKey);
        const aliasSuffix = alias ? ` (als ${formatPropertyKey(alias)})` : '';
        if (pv && pv.value !== null && pv.value !== undefined) {
          actualValues.push(
            `${formatPropertyKey(reqKey)}${aliasSuffix} = '${pv.value}' (${pv.dataType ?? 'unbekannt'})`
          );
        } else {
          actualValues.push(`${formatPropertyKey(reqKey)}: fehlt`);
        }
      }
    }

    return {
      elementId: el.id,
      elementLabel: label,
      matchedFacets: count,
      totalFacets: facets.length,
      actualValues,
      line() {
        return `   Fast-Treffer (${count}/${facets.length} Facetten): ${label} -> ${actualValues.join('; ')}`;
      },
    };
  });

  const diagnosis: Diagnosis = {
    facets: facetMatches,
    nearMisses,
    lines() {
      const out: string[] = [];
      if (facetMatches.length > 0) {
        out.push('   Diagnose der Anwendbarkeit (Einzelfacetten):');
        for (const f of facetMatches) out.push(f.line());
      }
      if (nearMisses.length > 0) {
        out.push('   Elemente mit den meisten passenden Facetten:');
        for (const nm of nearMisses) out.push(nm.line());
      }
      return out;
    },
  };

  return { diagnosis, hints };
}

function extractLoinGuid(identifier?: string): string | null {
  return extractPortalId(identifier ?? null);
}

function specIfcVersions(spec: IDSSpecification): string[] {
  const anySpec = spec as unknown as {
    ifcVersions?: string[];
    ifcVersion?: string[];
  };
  return anySpec.ifcVersions ?? anySpec.ifcVersion ?? [];
}

/**
 * Deduplicates specifications across multiple IDS documents in an AIA project ZIP:
 * keeps the first specification per `(LOIN GUID, IFCVersion)`.
 */
export function mergeAndDeduplicateIdsDocuments(
  entries: ReadonlyArray<{ fileName: string; xml: string; isCombinedAia?: boolean }>,
  sourceLabel: string
): IDSDocument {
  const docs = entries.map((e) => ({
    entry: e,
    doc: parseIDS(e.xml),
  }));

  const seenLoinSchema = new Set<string>();
  const specifications: IDSSpecification[] = [];

  for (const { doc } of docs) {
    for (const spec of doc.specifications) {
      const guid = extractLoinGuid(spec.identifier);
      if (!guid) {
        specifications.push(spec);
        continue;
      }
      const versions = specIfcVersions(spec);
      const effectiveVersions = versions.length > 0 ? versions : ['ALL'];
      const newVersions = effectiveVersions.filter((v) => {
        const key = `${guid} ${v.toUpperCase()}`;
        if (seenLoinSchema.has(key)) return false;
        seenLoinSchema.add(key);
        return true;
      });
      if (newVersions.length > 0) {
        specifications.push({
          ...spec,
          ifcVersions: newVersions as unknown as IDSSpecification['ifcVersions'],
        });
      }
    }
  }

  const primary = docs[0]?.doc;
  return {
    info: {
      title: primary?.info.title || sourceLabel,
      version: primary?.info.version,
      description: primary?.info.description,
      author: primary?.info.author,
      date: primary?.info.date,
      purpose: primary?.info.purpose,
      milestone: primary?.info.milestone,
    },
    specifications,
  };
}

export class CheckService {
  readonly portal: BimPortalClient;
  private mapping: PropertyMapping;
  private readonly cache = new Map<string, IDSDocument>();

  constructor(
    portal = new BimPortalClient(),
    initialMapping = PropertyMapping.resolve(
      typeof process !== 'undefined' ? process.env.CHECKMATE_MAPPING : undefined
    )
  ) {
    this.portal = portal;
    this.mapping = initialMapping;
  }

  currentMapping(): PropertyMapping {
    return this.mapping;
  }

  useMapping(mapping: PropertyMapping): void {
    this.mapping = mapping;
  }

  async search(term = ''): Promise<{
    projects: SimpleAiaProjectPublicDto[];
    loins: CatalogueEntry[];
  }> {
    const [projects, loins] = await Promise.all([
      this.portal.searchProjects(term),
      this.portal.searchCatalogue(term),
    ]);
    return { projects, loins };
  }

  async loadIdsDocument(source: CheckSource): Promise<{ name: string; document: IDSDocument }> {
    switch (source.kind) {
      case 'idsXml':
        return { name: source.name, document: parseIDS(source.xml) };
      case 'idsFile': {
        let bytes: Buffer;
        try {
          bytes = readFileSync(source.path);
        } catch {
          throw new Error(`IDS-Datei nicht gefunden: ${source.path}`);
        }
        const unpacked = unpackIdsBytes(
          new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength),
          source.path
        );
        const doc = mergeAndDeduplicateIdsDocuments(unpacked, source.path);
        return { name: doc.info.title || source.path, document: doc };
      }
      case 'portal': {
        const cached = this.cache.get(source.idOrLink);
        if (cached) {
          return { name: cached.info.title || source.idOrLink, document: cached };
        }
        const fetched = await this.portal.fetchRequirementsById(source.idOrLink);
        const doc = mergeAndDeduplicateIdsDocuments(fetched.entries, fetched.sourceLabel);
        this.cache.set(source.idOrLink, doc);
        this.cache.set(fetched.guid, doc);
        return { name: doc.info.title || fetched.sourceLabel, document: doc };
      }
      default:
        throw new Error(`Quelle ist kein IDS-Dokument: ${source.kind}`);
    }
  }

  /**
   * Validates a model against an IDSDocument using `@ifc-lite/ids` + CheckMate's
   * `MappedDataAccessor`, Near-Miss Diagnosis, and Spelling/Type hint enrichment.
   */
  async validateIdsDocument(
    name: string,
    document: IDSDocument,
    model: LoadedModel
  ): Promise<CheckRun> {
    const queryableIds = model.elements.map((e) => e.expressId);
    const mappedAccessor = new MappedDataAccessor(
      model.baseAccessor,
      this.mapping,
      queryableIds
    );
    const translator = createTranslationService('de');

    // Filter specifications applicable to the model's IFC schema version
    const matchingSpecs = document.specifications.filter((s) => {
      const vers = specIfcVersions(s);
      return (
        vers.length === 0 ||
        vers.some((v) => v.toUpperCase().startsWith(model.schema.toUpperCase()))
      );
    });

    const filteredDoc: IDSDocument = {
      ...document,
      specifications: matchingSpecs,
    };

    const rawReport = await validateIDS(
      filteredDoc,
      mappedAccessor,
      {
        modelId: model.name,
        schemaVersion: model.schema,
        entityCount: model.elements.length,
      },
      { translator, includePassingEntities: true }
    );

    const specResults: SpecCheckResult[] = [];

    for (const sr of rawReport.specificationResults) {
      const spec = sr.specification;
      const specVers = specIfcVersions(spec);
      const versions = specVers.length > 0 ? specVers.join(', ') : model.rawSchema;
      const phaseInfo = spec.milestone || document.info.milestone || '';
      const specLabel = `${spec.name} (${versions}${phaseInfo ? `, ${phaseInfo}` : ''})`;

      const findings: Finding[] = [];
      const specHints: string[] = [];

      for (const er of sr.entityResults) {
        const el = model.byExpressId.get(er.expressId);
        const elId = er.globalId ?? el?.id ?? `#${er.expressId}`;
        const elName = er.entityName ?? el?.name ?? '';
        const elType = er.entityType ?? el?.entityName ?? 'IfcProduct';
        const elementLabel = `${elName || elType} (${elId})`;

        for (const rr of er.requirementResults) {
          const facet = rr.requirement.facet;
          const reqDesc = describeFacet(facet);
          const reqPassed =
            (rr as unknown as { status?: string }).status !== undefined
              ? (rr as unknown as { status?: string }).status !== 'fail'
              : Boolean((rr as unknown as { passed?: boolean }).passed);
          let status: FindingStatus = reqPassed ? 'FULFILLED' : 'WRONG_VALUE';
          let detail = reqPassed
            ? rr.actualValue
              ? `'${rr.actualValue}'`
              : 'vorhanden'
            : rr.failureReason ?? 'Anforderung nicht erfüllt';

          if (!reqPassed && rr.failure) {
            const ft = rr.failure.type;
            if (ft.endsWith('_MISSING') || ft === 'PROPERTY_EMPTY') {
              status = 'MISSING';
            } else if (ft.includes('DATATYPE')) {
              status = 'WRONG_TYPE';
            } else if (ft.startsWith('PROHIBITED_')) {
              status = 'PROHIBITED';
            }
          }

          // Check if an alias from PropertyMapping was used or if a spelling hint exists
          if (
            facet.type === 'property' &&
            facet.propertySet.type === 'simpleValue' &&
            facet.baseName.type === 'simpleValue'
          ) {
            const reqKey = createPropertyKey(
              facet.propertySet.value,
              facet.baseName.value
            );
            const alias = mappedAccessor.usedAlias(er.expressId, reqKey);
            if (alias) {
              detail += ` (als ${formatPropertyKey(alias)})`;
            } else if (status === 'MISSING' && el) {
              for (const p of el.properties) {
                const diff = compareSpelling(reqKey, p.key);
                if (diff === 'CASE_ONLY' || diff === 'SIMILAR') {
                  const hint = `im Modell als ${formatPropertyKey(p.key)} (${describeSpellingDiff(diff)})`;
                  detail += ` [${hint}]`;
                  if (!specHints.includes(hint)) specHints.push(hint);
                }
              }
            }
          }

          findings.push(
            createFinding(elId, er.expressId, elementLabel, reqDesc, status, detail)
          );
        }
      }

      let diagnosis: Diagnosis = { facets: [], nearMisses: [], lines: () => [] };
      if (sr.applicableCount === 0) {
        const diagResult = diagnoseZeroApplicability(spec, model, mappedAccessor);
        diagnosis = diagResult.diagnosis;
        for (const h of diagResult.hints) {
          if (!specHints.includes(h)) specHints.push(h);
        }
      }

      const verdict: Verdict =
        sr.applicableCount === 0
          ? 'NOT_APPLICABLE'
          : sr.failedCount > 0 || sr.status === 'fail'
            ? 'FAILED'
            : 'PASSED';

      const vLabel = verdictLabel(verdict);
      const examined = model.elements.length;
      const applicable = sr.applicableCount;
      const passing = sr.passedCount;
      const failing = sr.failedCount;

      specResults.push({
        specificationName: spec.name,
        specificationLabel: specLabel,
        ifcVersions: specVers,
        examinedElements: examined,
        applicableElements: applicable,
        passingElements: passing,
        failingElements: failing,
        verdict,
        verdictLabel: vLabel,
        findings,
        diagnosis,
        hints: specHints,
        summary() {
          if (applicable === 0) {
            return `${specLabel}: 0 von ${examined} Elementen anwendbar`;
          }
          return `${specLabel}: ${applicable} von ${examined} Elementen anwendbar, ${passing} erfüllt, ${failing} nicht erfüllt`;
        },
        overviewLine() {
          return `[${vLabel}] ${this.summary()}`;
        },
      });
    }

    return {
      name,
      results: specResults,
      error: null,
      rawIdsReport: rawReport,
    };
  }

  /**
   * Validates a model against an `@ifc-lite/rules` `.rules.json` file.
   */
  async validateRuleSet(
    name: string,
    ruleSet: RuleSetFile,
    model: LoadedModel
  ): Promise<CheckRun> {
    const evaluatorModel: EvaluatorModel = {
      id: model.name,
      name: model.name,
      dataStore: model.store,
    };
    const report = await runRuleSet({
      ruleSet,
      models: [evaluatorModel],
    });

    const specResults: SpecCheckResult[] = report.specificationResults.map((sr) => {
      const specLabel = `${sr.specification.name} (.rules.json)`;
      const findings: Finding[] = [];
      for (const er of sr.entityResults) {
        const el = model.byExpressId.get(er.expressId);
        const elId = er.globalId ?? el?.id ?? `#${er.expressId}`;
        const elementLabel = `${er.entityName || er.entityType} (${elId})`;
        for (const rr of er.requirementResults) {
          const status: FindingStatus = rr.passed ? 'FULFILLED' : 'WRONG_VALUE';
          const detail = rr.passed
            ? rr.actualValue ?? 'erfüllt'
            : rr.failureReason ?? 'Regel nicht erfüllt';
          findings.push(
            createFinding(
              elId,
              er.expressId,
              elementLabel,
              rr.requirement.label,
              status,
              detail
            )
          );
        }
      }

      const verdict: Verdict =
        sr.applicableCount === 0
          ? 'NOT_APPLICABLE'
          : sr.failedCount > 0 || sr.status === 'fail'
            ? 'FAILED'
            : 'PASSED';
      const vLabel = verdictLabel(verdict);
      const examined = model.elements.length;

      return {
        specificationName: sr.specification.name,
        specificationLabel: specLabel,
        ifcVersions: [model.rawSchema],
        examinedElements: examined,
        applicableElements: sr.applicableCount,
        passingElements: sr.passedCount,
        failingElements: sr.failedCount,
        verdict,
        verdictLabel: vLabel,
        findings,
        diagnosis: { facets: [], nearMisses: [], lines: () => [] },
        hints: [],
        summary() {
          if (sr.applicableCount === 0) {
            return `${specLabel}: 0 von ${examined} Elementen anwendbar`;
          }
          return `${specLabel}: ${sr.applicableCount} von ${examined} Elementen anwendbar, ${sr.passedCount} erfüllt, ${sr.failedCount} nicht erfüllt`;
        },
        overviewLine() {
          return `[${vLabel}] ${this.summary()}`;
        },
      };
    });

    return {
      name,
      results: specResults,
      error: null,
    };
  }

  async runSource(source: CheckSource, model: LoadedModel): Promise<CheckRun> {
    try {
      if (source.kind === 'rulesJson') {
        return await this.validateRuleSet(source.name, source.ruleSet, model);
      }
      if (source.kind === 'rulesFile') {
        const raw = readFileSync(source.path, 'utf-8');
        const ruleSet = JSON.parse(raw) as RuleSetFile;
        return await this.validateRuleSet(ruleSet.name || source.path, ruleSet, model);
      }
      const { name, document } = await this.loadIdsDocument(source);
      return await this.validateIdsDocument(name, document, model);
    } catch (err) {
      const label =
        source.kind === 'portal'
          ? source.idOrLink
          : source.kind === 'idsFile' || source.kind === 'rulesFile'
            ? source.path
            : source.name;
      return {
        name: label,
        results: [],
        error: (err as Error).message,
      };
    }
  }

  async check(
    sources: readonly CheckSource[],
    model: LoadedModel,
    checkedAt = new Date()
  ): Promise<CheckReport> {
    const runs: CheckRun[] = [];
    for (const src of sources) {
      runs.push(await this.runSource(src, model));
    }
    return new CheckReport(checkedAt, model.info(), runs);
  }

  /**
   * Converts an IDS document to an `@ifc-lite/rules` RuleSetFile so users can
   * customize rules (e.g. case-insensitive property comparison or value rules).
   */
  convertIdsToRuleSet(document: IDSDocument) {
    return idsToRuleSet(document);
  }

  /**
   * Converts an `@ifc-lite/rules` RuleSetFile back to IDS 1.0 XML.
   */
  convertRuleSetToIds(ruleSet: RuleSetFile, model: LoadedModel) {
    const evaluatorModel: EvaluatorModel = {
      id: model.name,
      name: model.name,
      dataStore: model.store,
    };
    return ruleSetToIds(ruleSet, {
      ifcVersions: [model.schema],
      models: [evaluatorModel],
    });
  }

  /**
   * Exports all failed checks from a CheckReport into a BCF 2.1 `.bcfzip` byte array
   * using `@ifc-lite/bcf`.
   */
  async exportBcf(
    report: CheckReport,
    options: Partial<IDSBCFExportOptions> = {}
  ): Promise<Uint8Array> {
    const firstRaw = report.runs.find((r) => r.rawIdsReport)?.rawIdsReport;
    if (firstRaw) {
      const bcfProject = createBCFFromIDSReport(
        {
          title: firstRaw.source.document.info.title || `CheckMate ${report.model.name}`,
          description: firstRaw.source.document.info.description,
          specificationResults: firstRaw.specificationResults,
        },
        {
          author: options.author ?? 'checkmate@bimportal.de',
          projectName: options.projectName ?? `CheckMate ${report.model.name}`,
          bcfVersion: options.bcfVersion ?? '2.1',
          grouping: options.grouping ?? 'per-specification',
          includeSnapshots: false,
        }
      );
      const blob = await writeBCF(bcfProject);
      const ab = await blob.arrayBuffer();
      return new Uint8Array(ab);
    }
    throw new Error('Kein IDS-Prüfbericht für den BCF-Export vorhanden.');
  }
}

export class ReportText {
  static verdictLine(report: CheckReport): string {
    return `Ergebnis: ${report.verdict()} (Exit-Code ${report.exitCode()})`;
  }

  static brief(run: CheckRun, examples = 3): string[] {
    const lines: string[] = [];
    if (run.error) {
      lines.push(`Anforderungen: ${run.name} - Fehler: ${run.error}`);
      return lines;
    }
    lines.push(`Anforderungen: ${run.name}`);
    for (const res of run.results) {
      lines.push(`  ${res.overviewLine()}`);
      if (res.verdict === 'NOT_APPLICABLE') {
        lines.push(...res.diagnosis.lines());
      } else if (examples > 0) {
        const unmet = res.findings.filter((f) => !isFindingOk(f.status));
        for (const f of unmet.slice(0, examples)) {
          lines.push(`    ${f.line()}`);
        }
        if (unmet.length > examples) {
          lines.push(`    ... und ${unmet.length - examples} weitere Abweichung(en)`);
        }
      }
    }
    return lines;
  }

  static full(report: CheckReport): string[] {
    const lines: string[] = [...report.header(), ''];
    for (const run of report.runs) {
      if (run.error) {
        lines.push(`Anforderungen: ${run.name} - Fehler: ${run.error}`);
        continue;
      }
      lines.push(`Anforderungen: ${run.name}`);
      for (const res of run.results) {
        lines.push(`  ${res.overviewLine()}`);
        if (res.verdict === 'NOT_APPLICABLE') {
          lines.push(...res.diagnosis.lines());
        } else {
          for (const f of res.findings) {
            lines.push(`    ${f.line()}`);
          }
        }
      }
      lines.push('');
    }
    lines.push(ReportText.verdictLine(report));
    return lines;
  }
}

export class ReportJson {
  static toObject(report: CheckReport): Record<string, unknown> {
    return {
      checkedAt: report.checkedAt.toISOString(),
      model: report.model,
      verdict: report.verdict(),
      exitCode: report.exitCode(),
      runs: report.runs.map((run) => ({
        name: run.name,
        error: run.error,
        specifications: run.results.map((r) => ({
          name: r.specificationName,
          label: r.specificationLabel,
          verdict: r.verdict,
          verdictLabel: r.verdictLabel,
          examinedElements: r.examinedElements,
          applicableElements: r.applicableElements,
          passingElements: r.passingElements,
          failingElements: r.failingElements,
          hints: r.hints,
          diagnosis: {
            facets: r.diagnosis.facets.map((f) => ({
              description: f.description,
              matchedCount: f.matchedCount,
              note: f.note,
            })),
            nearMisses: r.diagnosis.nearMisses.map((nm) => ({
              elementId: nm.elementId,
              elementLabel: nm.elementLabel,
              matchedFacets: nm.matchedFacets,
              totalFacets: nm.totalFacets,
              actualValues: nm.actualValues,
            })),
          },
          findings: r.findings.map((f) => ({
            elementId: f.elementId,
            expressId: f.expressId,
            elementLabel: f.elementLabel,
            requirement: f.requirement,
            status: f.status,
            statusLabel: findingStatusLabel(f.status),
            detail: f.detail,
          })),
        })),
      })),
    };
  }

  static stringify(report: CheckReport): string {
    return JSON.stringify(ReportJson.toObject(report), null, 2);
  }
}
