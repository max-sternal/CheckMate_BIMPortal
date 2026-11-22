import { readFileSync } from 'node:fs';
import { basename, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  parseAuto,
  isQueryableObjectType,
  type IfcDataStore,
} from '@ifc-lite/parser';
import { GeometryProcessor, type GeometryResult, type MeshData } from '@ifc-lite/geometry';
import { createDataAccessor, narrowSchemaVersion } from '@ifc-lite/ids/bridge';
import type { IFCDataAccessor, IFCVersion } from '@ifc-lite/ids';
import {
  createPropertyKey,
  formatPropertyKey,
  looseNormalize,
  PropertyMapping,
  type PropertyKey,
} from './mapping.js';

export interface ModelProperty {
  readonly key: PropertyKey;
  readonly values: readonly string[];
  readonly dataType: string;
}

export interface ModelElement {
  readonly expressId: number;
  readonly id: string;
  readonly name: string;
  readonly entityName: string;
  readonly predefinedType: string | null;
  readonly userDefinedType: boolean;
  readonly properties: readonly ModelProperty[];
}

export interface ModelInfo {
  readonly name: string;
  readonly schema: IFCVersion;
  readonly rawSchema: string;
  readonly elements: number;
}

export interface ElementMesh3D {
  readonly expressId: number;
  readonly globalId: string;
  readonly entityName: string;
  readonly name: string;
  readonly positions: number[];
  readonly normals: number[];
  readonly indices: number[];
  readonly color: [number, number, number, number];
}

/**
 * Translates GitHub blob URLs (`https://github.com/<owner>/<repo>/blob/<ref>/path.ifc`)
 * to raw download URLs (`https://raw.githubusercontent.com/<owner>/<repo>/<ref>/path.ifc`).
 */
export function resolveModelUrl(urlOrPath: string): string {
  const trimmed = urlOrPath.trim();
  const ghBlob = /^https?:\/\/github\.com\/([^/]+)\/([^/]+)\/blob\/(.+)$/i.exec(trimmed);
  if (ghBlob) {
    return `https://raw.githubusercontent.com/${ghBlob[1]}/${ghBlob[2]}/${ghBlob[3]}`;
  }
  return trimmed;
}

export class LoadedModel {
  readonly name: string;
  readonly schema: IFCVersion;
  readonly rawSchema: string;
  readonly buffer: Uint8Array;
  readonly store: IfcDataStore;
  readonly baseAccessor: IFCDataAccessor;
  readonly elements: readonly ModelElement[];
  readonly byId: ReadonlyMap<string, ModelElement>;
  readonly byExpressId: ReadonlyMap<number, ModelElement>;
  private cachedGeometry: GeometryResult | null = null;

  constructor(
    name: string,
    rawSchema: string,
    buffer: Uint8Array,
    store: IfcDataStore,
    baseAccessor: IFCDataAccessor,
    elements: readonly ModelElement[]
  ) {
    this.name = name;
    this.rawSchema = rawSchema;
    this.schema = narrowSchemaVersion(rawSchema) ?? 'IFC4';
    this.buffer = buffer;
    this.store = store;
    this.baseAccessor = baseAccessor;
    this.elements = elements;
    const byId = new Map<string, ModelElement>();
    const byExpressId = new Map<number, ModelElement>();
    for (const el of elements) {
      if (el.id) byId.set(el.id, el);
      byExpressId.set(el.expressId, el);
    }
    this.byId = byId;
    this.byExpressId = byExpressId;
  }

  info(): ModelInfo {
    return {
      name: this.name,
      schema: this.schema,
      rawSchema: this.rawSchema,
      elements: this.elements.length,
    };
  }

  summary(): string {
    return `${this.name} - Schema ${this.rawSchema}, ${this.elements.length} Objekte`;
  }

  static bundledPath(): string {
    const dir = dirname(fileURLToPath(import.meta.url));
    return resolve(dir, '../../../resources/models/bruecke-tbw1-mit-merkmalen.ifc');
  }

  static async configured(): Promise<LoadedModel> {
    const envPath =
      typeof process !== 'undefined' ? process.env.CHECKMATE_MODEL : undefined;
    if (envPath && envPath.trim()) {
      return LoadedModel.read(envPath.trim());
    }
    return LoadedModel.read(LoadedModel.bundledPath());
  }

  static async read(source: string): Promise<LoadedModel> {
    const resolved = resolveModelUrl(source);
    if (/^https?:\/\//i.test(resolved)) {
      const res = await fetch(resolved);
      if (!res.ok) {
        throw new Error(`IFC-Link konnte nicht geladen werden (${resolved}: HTTP ${res.status})`);
      }
      const ab = await res.arrayBuffer();
      const urlPath = new URL(resolved).pathname;
      const name = basename(urlPath) || 'remote.ifc';
      return LoadedModel.fromBytes(new Uint8Array(ab), name);
    }

    let bytes: Buffer;
    try {
      bytes = readFileSync(resolved);
    } catch {
      throw new Error(`IFC-Datei nicht gefunden: ${source}`);
    }
    return LoadedModel.fromBytes(
      new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength),
      basename(resolved)
    );
  }

  static async fromBytes(bytes: Uint8Array, name = 'model.ifc'): Promise<LoadedModel> {
    const ab = bytes.buffer.slice(
      bytes.byteOffset,
      bytes.byteOffset + bytes.byteLength
    ) as ArrayBuffer;
    const parsed = await parseAuto(ab, { disableWorkerScan: true });
    if (parsed.format !== 'ifc') {
      throw new Error(`Format ${parsed.format} wird aktuell als STEP-Store erwartet.`);
    }
    const store = parsed.data;
    const baseAccessor = createDataAccessor(store);
    const rawSchema =
      baseAccessor.getSchemaVersion?.() ??
      (store as unknown as { schemaVersion?: string }).schemaVersion ??
      'IFC2X3';

    const queryableIds: number[] = [];
    for (const [typeName, ids] of store.entityIndex.byType.entries()) {
      if (isQueryableObjectType(typeName)) {
        queryableIds.push(...ids);
      }
    }
    queryableIds.sort((a, b) => a - b);

    const elements: ModelElement[] = [];

    for (const expressId of queryableIds) {
      const globalId = baseAccessor.getGlobalId(expressId);
      const entityName = baseAccessor.getEntityType(expressId);
      if (!globalId || !entityName) continue;

      const elName = baseAccessor.getEntityName(expressId) ?? '';
      const rawPredefined = baseAccessor.getPredefinedTypeRaw?.(expressId) ?? null;
      const objectType = baseAccessor.getObjectType(expressId) ?? null;
      const userDefinedType = rawPredefined === 'USERDEFINED';
      const predefinedType =
        userDefinedType
          ? objectType
          : rawPredefined && rawPredefined !== 'NOTDEFINED'
            ? rawPredefined
            : objectType;

      const psets = baseAccessor.getPropertySets(expressId);
      const props: ModelProperty[] = [];
      for (const pset of psets) {
        for (const prop of pset.properties) {
          const rawVals =
            prop.values && prop.values.length > 0
              ? prop.values
              : prop.value !== null && prop.value !== undefined
                ? [prop.value]
                : [];
          const strVals = rawVals
            .map((v) => String(v).trim())
            .filter((v) => v.length > 0);
          if (strVals.length === 0) continue;
          props.push({
            key: createPropertyKey(pset.name, prop.name),
            values: strVals,
            dataType: prop.dataType ?? 'IFCLABEL',
          });
        }
      }

      elements.push({
        expressId,
        id: globalId,
        name: elName,
        entityName,
        predefinedType,
        userDefinedType,
        properties: props,
      });
    }

    return new LoadedModel(name, rawSchema, bytes, store, baseAccessor, elements);
  }

  /**
   * Extracts 3D triangle meshes via @ifc-lite/geometry (Rust WASM kernel).
   * Cached per LoadedModel instance so multiple MCP App 3D requests are instant.
   */
  async extractGeometry(): Promise<GeometryResult> {
    if (this.cachedGeometry) {
      return this.cachedGeometry;
    }
    const processor = new GeometryProcessor();
    try {
      await processor.init();
      const result = await processor.process(this.buffer);
      this.cachedGeometry = result;
      return result;
    } finally {
      processor.dispose();
    }
  }

  /**
   * Returns serializable 3D meshes for all elements (or a filtered list of GlobalIds),
   * ready to be sent to an MCP App iframe or Web viewer.
   */
  async extractElementMeshes(globalIds?: readonly string[]): Promise<{
    totalElementsWithGeometry: number;
    meshes: ElementMesh3D[];
  }> {
    const geom = await this.extractGeometry();
    const filterSet =
      globalIds && globalIds.length > 0 ? new Set(globalIds) : null;

    const byExpress = new Map<number, MeshData[]>();
    for (const m of geom.meshes) {
      const list = byExpress.get(m.expressId) ?? [];
      list.push(m);
      byExpress.set(m.expressId, list);
    }

    const resultMeshes: ElementMesh3D[] = [];
    for (const [expressId, subMeshes] of byExpress.entries()) {
      const el = this.byExpressId.get(expressId);
      const gid = el?.id ?? this.baseAccessor.getGlobalId(expressId) ?? `#${expressId}`;
      if (filterSet && !filterSet.has(gid)) continue;

      for (const m of subMeshes) {
        if (!m.positions || m.positions.length === 0) continue;
        const orig = (m as unknown as { origin?: [number, number, number] }).origin ?? [0, 0, 0];
        const ox = orig[0] ?? 0;
        const oy = orig[1] ?? 0;
        const oz = orig[2] ?? 0;
        const worldPos = new Array<number>(m.positions.length);
        for (let i = 0; i < m.positions.length; i += 3) {
          worldPos[i] = m.positions[i] + ox;
          worldPos[i + 1] = m.positions[i + 1] + oy;
          worldPos[i + 2] = m.positions[i + 2] + oz;
        }
        resultMeshes.push({
          expressId,
          globalId: gid,
          entityName: el?.entityName ?? m.ifcType ?? 'IfcProduct',
          name: el?.name ?? '',
          positions: worldPos,
          normals: Array.from(m.normals),
          indices: Array.from(m.indices),
          color: m.color ?? [0.75, 0.78, 0.82, 1.0],
        });
      }
    }

    return {
      totalElementsWithGeometry: byExpress.size,
      meshes: resultMeshes,
    };
  }
}

export type FitKind = 'CLASSIFICATION' | 'NAME' | 'NONE';

export interface Fit {
  readonly kind: FitKind;
  readonly elements: number;
  readonly objectType: string | null;
  readonly inGroup: number;
  readonly group: string | null;
}

export function describeFit(fit: Fit): string {
  if (fit.kind === 'NONE') return 'kein Element mit diesem Objekttyp';
  const base =
    fit.kind === 'CLASSIFICATION'
      ? `${fit.elements} Element(e) mit '${fit.objectType}' in der Klassifizierung`
      : `${fit.elements} Element(e) mit '${fit.objectType}' im Namen`;
  return fit.inGroup > 0 && fit.group
    ? `${base}, ${fit.inGroup} davon in Gruppe '${fit.group}'`
    : base;
}

const MIN_WORD = 4;
const CLASSIFYING_PROP = /^(objekttyp|objektklasse|objektgruppe|_?idebene\d+)$/i;
const OBJECT_TYPE_PARTS = /\s+-\s+|,|\//;
const CAMEL_PARTS = /[A-ZÄÖÜ]?[a-zäöüß]+|[A-ZÄÖÜ]+(?![a-zäöüß])/g;

export class ModelFit {
  private readonly classified: Map<string, Set<string>>;
  private readonly named: Map<string, Set<string>>;

  constructor(classified: Map<string, Set<string>>, named: Map<string, Set<string>>) {
    this.classified = classified;
    this.named = named;
  }

  static of(elements: readonly ModelElement[]): ModelFit {
    const classified = new Map<string, Set<string>>();
    const named = new Map<string, Set<string>>();

    const add = (idx: Map<string, Set<string>>, word: string, id: string) => {
      if (!word) return;
      let set = idx.get(word);
      if (!set) {
        set = new Set();
        idx.set(word, set);
      }
      set.add(id);
    };

    for (const el of elements) {
      for (const prop of el.properties) {
        if (CLASSIFYING_PROP.test(prop.key.baseName)) {
          for (const val of prop.values) {
            add(classified, looseNormalize(val), el.id);
          }
        }
      }
      if (el.userDefinedType && el.predefinedType) {
        add(classified, looseNormalize(el.predefinedType), el.id);
      }
      for (const word of ModelFit.nameWords(el.name)) {
        add(named, word, el.id);
      }
    }

    return new ModelFit(classified, named);
  }

  static groupOf(loinName: string): string | null {
    const idx = loinName ? loinName.indexOf(' > ') : -1;
    return idx > 0 ? loinName.slice(0, idx).trim() : null;
  }

  static objectTypeWords(objectType: string): string[] {
    const words: string[] = [looseNormalize(objectType)];
    for (const part of objectType.split(OBJECT_TYPE_PARTS)) {
      const w = looseNormalize(part);
      if (w.length >= MIN_WORD && !words.includes(w)) {
        words.push(w);
      }
    }
    return words;
  }

  static nameWords(name: string): Set<string> {
    const words = new Set<string>();
    if (!name) return words;
    const colon = name.indexOf(':');
    const prefix = colon < 0 ? name : name.slice(0, colon);
    for (const segment of prefix.split(/[^a-zA-Z0-9äöüÄÖÜß]+/)) {
      const norm = looseNormalize(segment);
      if (norm.length >= MIN_WORD) words.add(norm);
      const camels = segment.match(CAMEL_PARTS) ?? [];
      for (const c of camels) {
        const cn = looseNormalize(c);
        if (cn.length >= MIN_WORD) words.add(cn);
      }
    }
    return words;
  }

  private elementsWith(words: string[], ...indexes: Map<string, Set<string>>[]): Set<string> {
    const out = new Set<string>();
    for (const idx of indexes) {
      for (const w of words) {
        const set = idx.get(w);
        if (set) {
          for (const id of set) out.add(id);
        }
      }
    }
    return out;
  }

  fit(entry: { name: string; objectTypes: readonly string[] }): Fit {
    const group = ModelFit.groupOf(entry.name);
    const groupElements = group
      ? this.elementsWith(ModelFit.objectTypeWords(group), this.classified, this.named)
      : new Set<string>();

    let best: Fit = { kind: 'NONE', elements: 0, objectType: null, inGroup: 0, group: null };

    for (const objectType of entry.objectTypes) {
      const words = ModelFit.objectTypeWords(objectType);
      let kind: FitKind = 'CLASSIFICATION';
      let matched = this.elementsWith(words, this.classified);
      if (matched.size === 0) {
        kind = 'NAME';
        matched = this.elementsWith(words, this.named);
      }
      if (matched.size === 0) continue;

      let inGroupCount = 0;
      for (const id of matched) {
        if (groupElements.has(id)) inGroupCount++;
      }

      const candidate: Fit = {
        kind,
        elements: matched.size,
        objectType,
        inGroup: inGroupCount,
        group,
      };
      if (ModelFit.compareStrength(candidate, best) < 0) {
        best = candidate;
      }
    }
    return best;
  }

  rank<T extends { name: string; objectTypes: readonly string[] }>(
    entries: readonly T[]
  ): Array<{ entry: T; fit: Fit }> {
    const ranked = entries.map((entry) => ({ entry, fit: this.fit(entry) }));
    ranked.sort((a, b) => {
      const cmp = ModelFit.compareStrength(a.fit, b.fit);
      if (cmp !== 0) return cmp;
      return a.entry.name.localeCompare(b.entry.name, 'de', { sensitivity: 'base' });
    });
    return ranked;
  }

  static compareStrength(a: Fit, b: Fit): number {
    const order: Record<FitKind, number> = { CLASSIFICATION: 0, NAME: 1, NONE: 2 };
    if (order[a.kind] !== order[b.kind]) return order[a.kind] - order[b.kind];
    if (a.inGroup !== b.inGroup) return b.inGroup - a.inGroup;
    return b.elements - a.elements;
  }
}
