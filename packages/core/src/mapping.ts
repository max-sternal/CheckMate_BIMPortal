export interface PropertyKey {
  readonly propertySet: string;
  readonly baseName: string;
}

export function createPropertyKey(propertySet: string, baseName: string): PropertyKey {
  return {
    propertySet: (propertySet ?? '').trim(),
    baseName: (baseName ?? '').trim(),
  };
}

export function formatPropertyKey(key: PropertyKey): string {
  return `${key.propertySet}/${key.baseName}`;
}

export function matchesPropertyKey(a: PropertyKey, b: PropertyKey): boolean {
  return a.propertySet === b.propertySet && a.baseName === b.baseName;
}

export type SpellingDiff = 'SAME' | 'CASE_ONLY' | 'SIMILAR' | 'DIFFERENT';

/**
 * Normalizes a string by expanding German umlauts/eszett, lowercasing,
 * and removing non-alphanumeric characters (spaces, hyphens, underscores, dots).
 */
export function looseNormalize(text: string | null | undefined): string {
  if (!text) return '';
  return text
    .toLowerCase()
    .replace(/ä/g, 'ae')
    .replace(/ö/g, 'oe')
    .replace(/ü/g, 'ue')
    .replace(/ß/g, 'ss')
    .replace(/[^a-z0-9]+/g, '');
}

export function compareSpelling(requested: PropertyKey, candidate: PropertyKey): SpellingDiff {
  if (matchesPropertyKey(requested, candidate)) {
    return 'SAME';
  }
  if (
    requested.propertySet.toLowerCase() === candidate.propertySet.toLowerCase() &&
    requested.baseName.toLowerCase() === candidate.baseName.toLowerCase()
  ) {
    return 'CASE_ONLY';
  }
  const reqPset = looseNormalize(requested.propertySet);
  const candPset = looseNormalize(candidate.propertySet);
  const reqName = looseNormalize(requested.baseName);
  const candName = looseNormalize(candidate.baseName);
  if (reqPset && reqName && reqPset === candPset && reqName === candName) {
    return 'SIMILAR';
  }
  return 'DIFFERENT';
}

export function describeSpellingDiff(diff: SpellingDiff): string {
  switch (diff) {
    case 'SAME':
      return '';
    case 'CASE_ONLY':
      return 'nur Groß-/Kleinschreibung weicht ab';
    case 'SIMILAR':
      return 'abweichende Schreibweise (Trennzeichen/Umlaute)';
    case 'DIFFERENT':
      return 'anderer Name';
  }
}

export interface PropertyMappingJson {
  name?: string;
  aliases: Array<{
    from: string;
    to: string[];
  }>;
}

export const BUNDLED_BIM_HAMBURG_MAPPING_JSON: PropertyMappingJson = {
  name: 'BIM.Hamburg Brücken',
  aliases: [
    { from: 'Klassifizierung/Objektgruppe', to: ['Pset_Objektinformation/_IDEbene1'] },
    { from: 'Klassifizierung/Objektklasse', to: ['Pset_Objektinformation/_IDEbene2'] },
    { from: 'Klassifizierung/Objekttyp', to: ['Pset_Objektinformation/_IDEbene3'] },
  ],
};

function readLocalTextSync(filePath: string): string {
  const req =
    typeof process !== 'undefined' &&
    typeof process.getBuiltinModule === 'function'
      ? (process.getBuiltinModule('node:fs') as { readFileSync: (p: string, enc: string) => string } | undefined)
      : undefined;
  if (req && typeof req.readFileSync === 'function') {
    return req.readFileSync(filePath, 'utf-8');
  }
  throw new Error(`Lokaler Dateizugriff wird im Browser nicht unterstützt (${filePath})`);
}

export class PropertyMapping {
  readonly name: string;
  readonly aliases: ReadonlyMap<string, readonly PropertyKey[]>;
  private readonly rawEntries: ReadonlyArray<{ from: PropertyKey; to: readonly PropertyKey[] }>;

  static readonly NONE = new PropertyMapping('keine', []);

  constructor(
    name: string,
    entries: ReadonlyArray<{ from: PropertyKey; to: readonly PropertyKey[] }>
  ) {
    this.name = name;
    this.rawEntries = entries;
    const map = new Map<string, PropertyKey[]>();
    for (const entry of entries) {
      const k = formatPropertyKey(entry.from);
      const existing = map.get(k) ?? [];
      existing.push(...entry.to);
      map.set(k, existing);
    }
    this.aliases = map;
  }

  static parseAddress(address: unknown, where: string): PropertyKey {
    const text = typeof address === 'string' ? address.trim() : '';
    const slash = text.indexOf('/');
    if (slash <= 0 || slash === text.length - 1) {
      throw new Error(`${where}: Adresse 'PropertySet/Name' erwartet`);
    }
    return createPropertyKey(text.slice(0, slash), text.slice(slash + 1));
  }

  static parse(jsonText: string): PropertyMapping {
    let root: unknown;
    try {
      root = JSON.parse(jsonText);
    } catch (err) {
      throw new Error(`kein gültiges JSON: ${(err as Error).message}`);
    }
    if (!root || typeof root !== 'object' || !Array.isArray((root as PropertyMappingJson).aliases)) {
      throw new Error('erwartet {"aliases": [{"from": ..., "to": [...]}]}');
    }
    const typed = root as PropertyMappingJson;
    const merged = new Map<string, { from: PropertyKey; to: PropertyKey[] }>();

    typed.aliases.forEach((item, idx) => {
      const i = idx + 1;
      const from = PropertyMapping.parseAddress(item?.from, `aliases[${i}].from`);
      if (!Array.isArray(item?.to) || item.to.length === 0) {
        throw new Error(`aliases[${i}].to: Liste mit mindestens einer Adresse erwartet`);
      }
      const targets = item.to.map((t) => PropertyMapping.parseAddress(t, `aliases[${i}].to`));
      const key = formatPropertyKey(from);
      const existing = merged.get(key);
      if (existing) {
        existing.to.push(...targets);
      } else {
        merged.set(key, { from, to: targets });
      }
    });

    const name = typeof typed.name === 'string' && typed.name.trim() ? typed.name.trim() : 'ohne Namen';
    return new PropertyMapping(name, Array.from(merged.values()));
  }

  static loadFile(filePath: string): PropertyMapping {
    let content: string;
    try {
      content = readLocalTextSync(filePath);
    } catch (err) {
      throw new Error(`Mapping-Datei nicht lesbar: ${filePath} (${(err as Error).message})`);
    }
    try {
      return PropertyMapping.parse(content);
    } catch (err) {
      throw new Error(`${filePath}: ${(err as Error).message}`);
    }
  }

  static bundled(): PropertyMapping {
    return PropertyMapping.parse(JSON.stringify(BUNDLED_BIM_HAMBURG_MAPPING_JSON));
  }

  static resolve(named?: string | null): PropertyMapping {
    if (!named || !named.trim()) return PropertyMapping.bundled();
    const val = named.trim();
    const lower = val.toLowerCase();
    if (lower === 'none' || lower === 'keine') return PropertyMapping.NONE;
    if (lower === 'default' || lower === 'standard') return PropertyMapping.bundled();
    if (val.startsWith('{')) return PropertyMapping.parse(val);
    return PropertyMapping.loadFile(val);
  }

  toJson(): PropertyMappingJson {
    return {
      name: this.name,
      aliases: this.rawEntries.map((e) => ({
        from: formatPropertyKey(e.from),
        to: e.to.map(formatPropertyKey),
      })),
    };
  }

  /**
   * Returns all candidate addresses for a requested property address,
   * starting with the requested address itself.
   */
  candidates(requested: PropertyKey): PropertyKey[] {
    const list: PropertyKey[] = [requested];
    const targets = this.aliases.get(formatPropertyKey(requested));
    if (targets) {
      list.push(...targets);
    }
    return list;
  }

  isAlias(requested: PropertyKey, found: PropertyKey): boolean {
    return !matchesPropertyKey(requested, found);
  }

  describe(): string[] {
    const lines: string[] = [
      `Mapping '${this.name}': ${this.rawEntries.length} Alias(e)`,
    ];
    for (const entry of this.rawEntries) {
      const targets = entry.to.map(formatPropertyKey).join(', ');
      lines.push(`   ${formatPropertyKey(entry.from)} -> ${targets}`);
    }
    return lines;
  }
}
