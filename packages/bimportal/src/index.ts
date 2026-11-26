import { unzipSync } from 'fflate';

export const DEFAULT_BIM_PORTAL_BASE_URL = 'https://via.bund.de/bim';

export interface LoinPurposePublicDto {
  name?: string;
  alternativeIdentifier?: string;
}

export interface SimpleLoinPublicDto {
  id?: string;
  guid: string;
  name: string;
  description?: string;
  version?: string;
  organisation?: string;
  objectTypes?: string[];
  domainSpecificModelTypes?: string[];
  constructionTypes?: string[];
  projectPhases?: string[];
  purposes?: Array<string | LoinPurposePublicDto | null | undefined>;
}

export interface SimpleAiaProjectPublicDto {
  id?: string;
  guid: string;
  name: string;
  description?: string;
  version?: string;
  organisation?: string;
}

export interface SimpleDomainModelPublicDto {
  id?: string;
  guid: string;
  name: string;
  description?: string;
  version?: string;
  organisation?: string;
}

export interface SimpleAiaTemplatePublicDto {
  id?: string;
  guid: string;
  name: string;
  description?: string;
  version?: string;
  organisation?: string;
}

export interface SimpleContextInfoPublicDto {
  id?: string;
  guid: string;
  name: string;
  description?: string;
  version?: string;
  organisation?: string;
}

export interface CatalogueEntry {
  guid: string;
  name: string;
  version: string;
  organisation: string;
  objectTypes: string[];
  disciplines: string[];
  constructionTypes: string[];
  phases: string[];
  purposes: string[];
}

export interface CatalogueFilter {
  discipline?: string | null;
  phase?: string | null;
  purpose?: string | null;
}

export interface PortalSearchRequest {
  searchString?: string;
  pageNumber?: number;
  includeDeprecated?: boolean;
  organisationGuids?: string[];
}

export interface BimPortalClientOptions {
  baseUrl?: string;
  username?: string;
  password?: string;
  fetchImpl?: typeof fetch;
}

export class RequirementsError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'RequirementsError';
  }
}

const UUID_REGEX =
  /(?<![0-9a-fA-F])[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?![0-9a-fA-F])/g;

/**
 * Extracts a single UUID from text (such as a BIM Portal link or GUID string).
 * Throws RequirementsError if multiple distinct UUIDs are present.
 */
export function extractPortalId(text: string | null | undefined): string | null {
  if (!text) return null;
  const matches = text.match(UUID_REGEX) ?? [];
  const unique = Array.from(new Set(matches.map((m) => m.toLowerCase())));
  if (unique.length > 1) {
    throw new RequirementsError(
      `Der Text enthält mehrere IDs (${unique.join(', ')}). Bitte nur eine.`
    );
  }
  return unique[0] ?? null;
}

function cleanStrings(values?: (string | null | undefined)[] | null): string[] {
  if (!values) return [];
  return values
    .filter((v): v is string => typeof v === 'string')
    .map((v) => v.trim())
    .filter((v) => v.length > 0);
}

function cleanPurposes(
  values?: Array<string | LoinPurposePublicDto | null | undefined> | null
): string[] {
  if (!values) return [];
  const out: string[] = [];
  for (const v of values) {
    if (typeof v === 'string') {
      const t = v.trim();
      if (t) out.push(t);
    } else if (v && typeof v === 'object') {
      const t = (v.name ?? v.alternativeIdentifier ?? '').trim();
      if (t) out.push(t);
    }
  }
  return out;
}

export function toCatalogueEntry(dto: SimpleLoinPublicDto): CatalogueEntry {
  return {
    guid: dto.guid.toLowerCase(),
    name: dto.name ?? '',
    version: dto.version ?? '',
    organisation: dto.organisation ?? '',
    objectTypes: cleanStrings(dto.objectTypes),
    disciplines: cleanStrings(dto.domainSpecificModelTypes),
    constructionTypes: cleanStrings(dto.constructionTypes),
    phases: cleanStrings(dto.projectPhases),
    purposes: cleanPurposes(dto.purposes),
  };
}

export function applyCatalogueFilter(
  entries: readonly CatalogueEntry[],
  filter?: CatalogueFilter
): CatalogueEntry[] {
  if (!filter) return [...entries];
  const matches = (list: string[], needle?: string | null) => {
    if (!needle || !needle.trim()) return true;
    const part = needle.trim().toLowerCase();
    return list.some((item) => item.toLowerCase().includes(part));
  };
  return entries.filter(
    (e) =>
      matches(e.disciplines, filter.discipline) &&
      matches(e.phases, filter.phase) &&
      matches(e.purposes, filter.purpose)
  );
}

export interface ExtractedIdsEntry {
  fileName: string;
  xml: string;
  isCombinedAia: boolean;
}

/**
 * Unpacks a ZIP archive of IDS files (e.g. from an AIA project or Domain Model export)
 * or wraps a single IDS XML byte array. Combined `AIA_*.ids` entries come first.
 */
export function unpackIdsBytes(bytes: Uint8Array, sourceName: string): ExtractedIdsEntry[] {
  const isZip =
    bytes.length >= 4 &&
    bytes[0] === 0x50 &&
    bytes[1] === 0x4b &&
    bytes[2] === 0x03 &&
    bytes[3] === 0x04;

  const decoder = new TextDecoder('utf-8');
  if (!isZip) {
    const xml = decoder.decode(bytes);
    if (!xml.includes('<ids') && !xml.includes(':ids')) {
      throw new RequirementsError(`${sourceName} ist keine gültige IDS-Datei.`);
    }
    return [{ fileName: sourceName, xml, isCombinedAia: false }];
  }

  let unzipped: Record<string, Uint8Array>;
  try {
    unzipped = unzipSync(bytes);
  } catch (err) {
    throw new RequirementsError(
      `${sourceName} ist kein lesbares ZIP-Archiv: ${(err as Error).message}`
    );
  }

  const combined: ExtractedIdsEntry[] = [];
  const others: ExtractedIdsEntry[] = [];

  for (const [entryPath, content] of Object.entries(unzipped)) {
    const slash = Math.max(entryPath.lastIndexOf('/'), entryPath.lastIndexOf('\\'));
    const baseName = entryPath.slice(slash + 1);
    if (!baseName.toLowerCase().endsWith('.ids') || content.length === 0) {
      continue;
    }
    const xml = decoder.decode(content);
    const entry: ExtractedIdsEntry = {
      fileName: entryPath,
      xml,
      isCombinedAia: baseName.startsWith('AIA_'),
    };
    if (entry.isCombinedAia) {
      combined.push(entry);
    } else {
      others.push(entry);
    }
  }

  const all = [...combined, ...others];
  if (all.length === 0) {
    throw new RequirementsError(`${sourceName} enthält keine IDS-Datei.`);
  }
  return all;
}

const PAGE_SIZE = 1000;
const MAX_PAGES = 20;

export class BimPortalClient {
  private readonly baseUrl: string;
  private readonly username?: string;
  private readonly password?: string;
  private readonly fetchImpl: typeof fetch;
  private token: string | null = null;

  constructor(options: BimPortalClientOptions = {}) {
    this.baseUrl = (
      options.baseUrl ??
      (typeof process !== 'undefined' ? process.env.BIM_PORTAL_BASE_URL : undefined) ??
      DEFAULT_BIM_PORTAL_BASE_URL
    ).replace(/\/+$/, '');
    this.username =
      options.username ??
      (typeof process !== 'undefined' ? process.env.BIM_PORTAL_USERNAME : undefined);
    this.password =
      options.password ??
      (typeof process !== 'undefined' ? process.env.BIM_PORTAL_PASSWORD : undefined);
    this.fetchImpl = options.fetchImpl ?? globalThis.fetch.bind(globalThis);
  }

  async loginIfConfigured(): Promise<void> {
    if (this.token || !this.username || !this.password) return;
    const res = await this.fetchImpl(`${this.baseUrl}/infrastruktur/api/v1/public/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ mail: this.username, password: this.password }),
    });
    if (res.ok) {
      const data = (await res.json()) as { token?: string };
      if (data.token) {
        this.token = data.token;
      }
    }
  }

  private async headers(accept = 'application/json'): Promise<Record<string, string>> {
    await this.loginIfConfigured();
    const h: Record<string, string> = { Accept: accept };
    if (this.token) {
      h.Authorization = `Bearer ${this.token}`;
    }
    return h;
  }

  private async searchPaged<T>(path: string, term = ''): Promise<T[]> {
    const all: T[] = [];
    const trimmed = term.trim();
    const h = await this.headers('application/json');
    h['Content-Type'] = 'application/json';

    for (let page = 0; page < MAX_PAGES; page++) {
      const body: PortalSearchRequest = { pageNumber: page };
      if (trimmed) body.searchString = trimmed;
      const res = await this.fetchImpl(`${this.baseUrl}${path}`, {
        method: 'POST',
        headers: h,
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        throw new RequirementsError(`BIM-Portal-Anfrage fehlgeschlagen (${path}: HTTP ${res.status})`);
      }
      const batch = (await res.json()) as T[];
      if (!Array.isArray(batch) || batch.length === 0) break;
      all.push(...batch);
      if (batch.length < PAGE_SIZE) break;
    }
    return all;
  }

  async searchLoins(term = ''): Promise<SimpleLoinPublicDto[]> {
    return this.searchPaged<SimpleLoinPublicDto>('/aia/api/v1/public/loin', term);
  }

  async searchCatalogue(term = ''): Promise<CatalogueEntry[]> {
    const dtos = await this.searchLoins(term);
    return dtos.map(toCatalogueEntry);
  }

  async searchProjects(term = ''): Promise<SimpleAiaProjectPublicDto[]> {
    return this.searchPaged<SimpleAiaProjectPublicDto>('/aia/api/v1/public/aiaProject', term);
  }

  async searchDomainModels(term = ''): Promise<SimpleDomainModelPublicDto[]> {
    return this.searchPaged<SimpleDomainModelPublicDto>('/aia/api/v1/public/domainSpecificModel', term);
  }

  async searchTemplates(term = ''): Promise<SimpleAiaTemplatePublicDto[]> {
    return this.searchPaged<SimpleAiaTemplatePublicDto>('/aia/api/v1/public/aiaTemplate', term);
  }

  async searchContextInfos(term = ''): Promise<SimpleContextInfoPublicDto[]> {
    return this.searchPaged<SimpleContextInfoPublicDto>('/aia/api/v1/public/contextInfo', term);
  }

  private async downloadOptionalBytes(path: string): Promise<Uint8Array | null> {
    const h = await this.headers('*/*');
    const res = await this.fetchImpl(`${this.baseUrl}${path}`, {
      method: 'GET',
      headers: h,
    });
    if (res.status === 404 || res.status === 400 || !res.ok) {
      return null;
    }
    const buf = await res.arrayBuffer();
    const bytes = new Uint8Array(buf);
    return bytes.length > 0 ? bytes : null;
  }

  async exportLoinIds(guid: string): Promise<Uint8Array | null> {
    return this.downloadOptionalBytes(`/aia/api/v1/public/loin/${encodeURIComponent(guid)}/IDS`);
  }

  async exportLoinXml(guid: string): Promise<Uint8Array | null> {
    return this.downloadOptionalBytes(`/aia/api/v1/public/loin/${encodeURIComponent(guid)}/loinXML`);
  }

  async exportProjectIds(guid: string): Promise<Uint8Array | null> {
    return this.downloadOptionalBytes(`/aia/api/v1/public/aiaProject/${encodeURIComponent(guid)}/IDS`);
  }

  async exportDomainModelIds(guid: string): Promise<Uint8Array | null> {
    return this.downloadOptionalBytes(
      `/aia/api/v1/public/domainSpecificModel/${encodeURIComponent(guid)}/IDS`
    );
  }

  /**
   * Resolves any portal ID or link (LOIN GUID, AIA project GUID, Domain Model GUID, or version ID)
   * into its unpacked IDS XML files.
   */
  async fetchRequirementsById(idOrLink: string): Promise<{
    guid: string;
    sourceLabel: string;
    entries: ExtractedIdsEntry[];
  }> {
    const id = extractPortalId(idOrLink);
    if (!id) {
      throw new RequirementsError(`Keine ID in '${idOrLink.trim()}'.`);
    }

    // 1. Direct LOIN IDS download
    const loinBytes = await this.exportLoinIds(id);
    if (loinBytes) {
      return {
        guid: id,
        sourceLabel: `BIM Portal, LOIN ${id}`,
        entries: unpackIdsBytes(loinBytes, `BIM Portal, LOIN ${id}`),
      };
    }

    // 2. Direct AIA Project IDS ZIP download
    const projectBytes = await this.exportProjectIds(id);
    if (projectBytes) {
      return {
        guid: id,
        sourceLabel: `BIM Portal, AIA-Projekt ${id}`,
        entries: unpackIdsBytes(projectBytes, `BIM Portal, AIA-Projekt ${id}`),
      };
    }

    // 3. Direct Domain Model IDS download
    const domainBytes = await this.exportDomainModelIds(id);
    if (domainBytes) {
      return {
        guid: id,
        sourceLabel: `BIM Portal, Fachmodell ${id}`,
        entries: unpackIdsBytes(domainBytes, `BIM Portal, Fachmodell ${id}`),
      };
    }

    // 4. Fallback: version ID lookup in projects or LOIN catalogue
    const projects = await this.searchProjects('');
    const matchedProject = projects.find(
      (p) => p.id?.toLowerCase() === id || p.guid?.toLowerCase() === id
    );
    if (matchedProject) {
      const bytes = await this.exportProjectIds(matchedProject.guid);
      if (!bytes) {
        throw new RequirementsError(
          `Das BIM Portal bietet für das AIA-Projekt keine IDS-Datei zum Herunterladen (${matchedProject.guid}).`
        );
      }
      const label = `BIM Portal, AIA-Projekt '${matchedProject.name}' (${matchedProject.guid})`;
      return {
        guid: matchedProject.guid,
        sourceLabel: label,
        entries: unpackIdsBytes(bytes, label),
      };
    }

    const loins = await this.searchLoins('');
    const matchedLoin = loins.find(
      (l) => l.id?.toLowerCase() === id || l.guid?.toLowerCase() === id
    );
    if (matchedLoin) {
      const bytes = await this.exportLoinIds(matchedLoin.guid);
      if (!bytes) {
        throw new RequirementsError(
          `Das BIM Portal bietet für die LOIN keine IDS-Datei zum Herunterladen (${matchedLoin.guid}).`
        );
      }
      const label = `BIM Portal, LOIN '${matchedLoin.name}' (${matchedLoin.guid})`;
      return {
        guid: matchedLoin.guid,
        sourceLabel: label,
        entries: unpackIdsBytes(bytes, label),
      };
    }

    throw new RequirementsError(
      `Im BIM Portal wurde weder eine LOIN noch ein AIA-Projekt mit der ID ${id} gefunden.`
    );
  }
}
