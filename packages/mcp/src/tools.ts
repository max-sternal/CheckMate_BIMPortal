import { writeFileSync } from 'node:fs';
import {
  applyCatalogueFilter,
  BimPortalClient,
  extractPortalId,
  type CatalogueEntry,
  type CatalogueFilter,
  type SimpleAiaProjectPublicDto,
} from '@checkmate/bimportal';
import {
  CheckReport,
  CheckService,
  describeFit,
  formatPropertyKey,
  isFindingOk,
  LoadedModel,
  ModelFit,
  PropertyMapping,
  ReportText,
  type CheckSource,
  type ElementMesh3D,
  type SpecCheckResult,
} from '@checkmate/core';

export interface Element3DStatus {
  readonly globalId: string;
  readonly expressId: number;
  readonly entityName: string;
  readonly name: string;
  readonly status: 'PASSED' | 'FAILED' | 'UNCHECKED';
  readonly findings: string[];
}

export class CheckMateTools {
  readonly service: CheckService;
  private readonly modelReader: (source: string) => Promise<LoadedModel>;
  private readonly defaultModel: () => Promise<LoadedModel>;

  private currentModel: LoadedModel | null = null;
  private lastReport: CheckReport | null = null;
  private lastFoundLoins: CatalogueEntry[] = [];
  private lastFoundProjects: SimpleAiaProjectPublicDto[] = [];
  private lastSearchTerm = '';
  private lastFilter: CatalogueFilter = {};

  constructor(
    service = new CheckService(new BimPortalClient()),
    modelReader: (source: string) => Promise<LoadedModel> = (s) => LoadedModel.read(s),
    defaultModel: () => Promise<LoadedModel> = () => LoadedModel.configured()
  ) {
    this.service = service;
    this.modelReader = modelReader;
    this.defaultModel = defaultModel;
  }

  async model(): Promise<LoadedModel> {
    if (!this.currentModel) {
      this.currentModel = await this.defaultModel();
    }
    return this.currentModel;
  }

  async loadModel(source: string): Promise<string> {
    this.currentModel = await this.modelReader(source);
    this.lastReport = null;
    return this.describeModel();
  }

  async describeModel(): Promise<string> {
    const current = await this.model();
    const entities = new Map<string, number>();
    for (const e of current.elements) {
      entities.set(e.entityName, (entities.get(e.entityName) ?? 0) + 1);
    }
    const sorted = [...entities.entries()].sort((a, b) => b[1] - a[1]);
    const lines: string[] = [`Modell: ${current.summary()}`, 'Entities:'];
    for (const [name, count] of sorted.slice(0, 15)) {
      lines.push(`   ${name}: ${count}`);
    }
    return lines.join('\n');
  }

  async modelData(): Promise<Record<string, unknown>> {
    const current = await this.model();
    const entities = new Map<string, number>();
    for (const e of current.elements) {
      entities.set(e.entityName, (entities.get(e.entityName) ?? 0) + 1);
    }
    const sorted = [...entities.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([name, count]) => ({ name, count }));

    return {
      name: current.name,
      summary: current.summary(),
      schema: current.rawSchema,
      elements: current.elements.length,
      entities: sorted,
      elementStatuses: this.computeElementStatuses(current),
    };
  }

  async listElements(entity?: string | null, name?: string | null, limit = 30): Promise<string> {
    const current = await this.model();
    const entFilter = entity?.trim().toLowerCase() ?? '';
    const nameFilter = name?.trim().toLowerCase() ?? '';
    const found = current.elements.filter(
      (e) =>
        (!entFilter || e.entityName.toLowerCase() === entFilter) &&
        (!nameFilter || e.name.toLowerCase().includes(nameFilter))
    );
    const lines: string[] = [
      `${found.length} Element(e)${found.length > limit ? `, die ersten ${limit}` : ''}`,
    ];
    for (const e of found.slice(0, limit)) {
      lines.push(
        `${e.id} | ${e.entityName} | ${e.name}${e.predefinedType ? ` | ${e.predefinedType}` : ''}`
      );
    }
    return lines.join('\n');
  }

  async elementPage(
    entity?: string | null,
    name?: string | null,
    offset = 0,
    limit = 50
  ): Promise<Record<string, unknown>> {
    const current = await this.model();
    const entFilter = entity?.trim().toLowerCase() ?? '';
    const nameFilter = name?.trim().toLowerCase() ?? '';
    const statusMap = new Map(
      this.computeElementStatuses(current).map((s) => [s.globalId, s.status])
    );

    const found = current.elements.filter(
      (e) =>
        (!entFilter || e.entityName.toLowerCase() === entFilter) &&
        (!nameFilter || e.name.toLowerCase().includes(nameFilter))
    );
    const safeOffset = Math.max(offset, 0);
    const safeLimit = Math.max(limit, 0);

    return {
      total: found.length,
      offset: safeOffset,
      elements: found.slice(safeOffset, safeOffset + safeLimit).map((e) => ({
        id: e.id,
        expressId: e.expressId,
        entity: e.entityName,
        name: e.name,
        predefinedType: e.predefinedType,
        status: statusMap.get(e.id) ?? 'UNCHECKED',
      })),
    };
  }

  async findLoins(
    search?: string | null,
    filter: CatalogueFilter = {},
    limit = 20,
    includeUnfitting = false
  ): Promise<string> {
    const term = search ?? '';
    const found = await this.service.search(term);
    this.lastSearchTerm = term;
    this.lastFilter = filter;
    this.lastFoundProjects = found.projects;
    this.lastFoundLoins = found.loins;

    const current = await this.model();
    const filtered = applyCatalogueFilter(found.loins, filter);
    const ranked = ModelFit.of(current.elements).rank(filtered);
    const fittingCount = ranked.filter((r) => r.fit.kind !== 'NONE').length;

    const lines: string[] = [
      `${found.loins.length} LOIN(s), ${filtered.length} nach Filter, ${fittingCount} passend zum Modell; ${found.projects.length} AIA-Projekt(e)`,
    ];
    for (const p of found.projects) {
      lines.push(`AIA-Projekt | ${p.guid} | ${p.name}`);
    }
    for (const r of ranked
      .filter((r) => includeUnfitting || fittingCount === 0 || r.fit.kind !== 'NONE')
      .slice(0, limit)) {
      lines.push(
        `LOIN | ${r.entry.guid} | ${r.entry.name} | ${
          r.fit.kind !== 'NONE' ? describeFit(r.fit) : 'passt nicht erkennbar'
        }`
      );
    }
    return lines.join('\n');
  }

  async loinsData(
    search?: string | null,
    filter?: CatalogueFilter,
    limit = 50,
    includeUnfitting = true
  ): Promise<Record<string, unknown>> {
    if (search !== undefined && search !== null && search !== this.lastSearchTerm) {
      const found = await this.service.search(search);
      this.lastSearchTerm = search;
      this.lastFoundProjects = found.projects;
      this.lastFoundLoins = found.loins;
    }
    const activeFilter = filter ?? this.lastFilter;
    this.lastFilter = activeFilter;

    const current = await this.model();
    const filtered = applyCatalogueFilter(this.lastFoundLoins, activeFilter);
    const ranked = ModelFit.of(current.elements).rank(filtered);
    const fittingCount = ranked.filter((r) => r.fit.kind !== 'NONE').length;

    // Collect distinct disciplines, phases, purposes for filter dropdowns in the MCP App
    const disciplines = new Set<string>();
    const phases = new Set<string>();
    const purposes = new Set<string>();
    for (const l of this.lastFoundLoins) {
      for (const d of l.disciplines) disciplines.add(d);
      for (const ph of l.phases) phases.add(ph);
      for (const pu of l.purposes) purposes.add(pu);
    }

    return {
      modelSummary: current.summary(),
      search: this.lastSearchTerm,
      filter: activeFilter,
      totalLoins: this.lastFoundLoins.length,
      filteredCount: filtered.length,
      fittingCount,
      availableFilters: {
        disciplines: [...disciplines].sort(),
        phases: [...phases].sort(),
        purposes: [...purposes].sort(),
      },
      projects: this.lastFoundProjects.map((p) => ({
        guid: p.guid,
        name: p.name,
        organisation: p.organisation ?? '',
      })),
      loins: ranked
        .filter((r) => includeUnfitting || fittingCount === 0 || r.fit.kind !== 'NONE')
        .slice(0, limit)
        .map((r) => ({
          guid: r.entry.guid,
          name: r.entry.name,
          version: r.entry.version,
          organisation: r.entry.organisation,
          objectTypes: r.entry.objectTypes,
          disciplines: r.entry.disciplines,
          phases: r.entry.phases,
          purposes: r.entry.purposes,
          fits: r.fit.kind !== 'NONE',
          fitKind: r.fit.kind,
          fitElements: r.fit.elements,
          fitDescription:
            r.fit.kind !== 'NONE' ? describeFit(r.fit) : 'passt nicht erkennbar',
        })),
    };
  }

  async describeRequirements(id: string): Promise<string> {
    const { name, document } = await this.service.loadIdsDocument({
      kind: 'portal',
      idOrLink: id,
    });
    const current = await this.model();
    const run = await this.service.validateIdsDocument(name, document, current);

    const lines: string[] = [
      `Anforderungen: ${name}`,
      `Spezifikationen für ${current.rawSchema}: ${run.results.length}`,
    ];
    for (const res of run.results) {
      lines.push(
        `  - ${res.specificationLabel}: ${res.applicableElements} von ${res.examinedElements} Elementen im Modell anwendbar`
      );
    }
    return lines.join('\n');
  }

  async check(
    ids: readonly string[] = [],
    idsFiles: readonly string[] = [],
    examples = 3,
    rulesFiles: readonly string[] = []
  ): Promise<string> {
    const sources: CheckSource[] = [];
    for (const f of idsFiles) {
      if (f && f.trim()) sources.push({ kind: 'idsFile', path: f.trim() });
    }
    for (const rf of rulesFiles) {
      if (rf && rf.trim()) sources.push({ kind: 'rulesFile', path: rf.trim() });
    }
    for (const id of ids) {
      if (id && id.trim()) sources.push({ kind: 'portal', idOrLink: id.trim() });
    }
    if (sources.length === 0) {
      throw new Error('Keine Anforderungen angegeben: ids, ids_files oder rules_files.');
    }

    const current = await this.model();
    this.lastReport = await this.service.check(sources, current, new Date());

    const lines: string[] = [];
    if (examples <= 0) {
      lines.push(...ReportText.full(this.lastReport));
    } else {
      lines.push(...this.lastReport.header(), '');
      for (const run of this.lastReport.runs) {
        lines.push(...ReportText.brief(run, examples));
      }
      lines.push(ReportText.verdictLine(this.lastReport));
    }
    return lines.join('\n');
  }

  computeElementStatuses(current: LoadedModel): Element3DStatus[] {
    const byId = new Map<string, { failed: boolean; passed: boolean; findings: string[] }>();

    if (this.lastReport) {
      for (const res of this.lastReport.results()) {
        for (const f of res.findings) {
          let entry = byId.get(f.elementId);
          if (!entry) {
            entry = { failed: false, passed: false, findings: [] };
            byId.set(f.elementId, entry);
          }
          if (isFindingOk(f.status)) {
            entry.passed = true;
          } else {
            entry.failed = true;
          }
          entry.findings.push(`${res.specificationLabel}: ${f.requirementLine()}`);
        }
      }
    }

    return current.elements.map((el) => {
      const st = byId.get(el.id);
      const status: 'PASSED' | 'FAILED' | 'UNCHECKED' = st
        ? st.failed
          ? 'FAILED'
          : 'PASSED'
        : 'UNCHECKED';
      return {
        globalId: el.id,
        expressId: el.expressId,
        entityName: el.entityName,
        name: el.name,
        status,
        findings: st?.findings ?? [],
      };
    });
  }

  async reportData(examples = 10): Promise<Record<string, unknown>> {
    if (!this.lastReport) return {};
    const current = await this.model();

    const runs = this.lastReport.runs.map((run) => ({
      name: run.name,
      error: run.error,
      specifications: run.results.map((result) => {
        const unmetAll = result.findings.filter((f) => !isFindingOk(f.status));
        const passedIds = Array.from(
          new Set(
            result.findings
              .filter((f) => isFindingOk(f.status))
              .map((f) => f.elementId)
          )
        );
        const failedIds = Array.from(
          new Set(unmetAll.map((f) => f.elementId))
        );

        return {
          label: result.specificationLabel,
          verdict: result.verdict,
          verdictLabel: result.verdictLabel,
          examined: result.examinedElements,
          applicable: result.applicableElements,
          failing: result.failingElements,
          passing: result.passingElements,
          summary: result.summary(),
          hints: result.hints,
          passedElementIds: passedIds,
          failedElementIds: failedIds,
          diagnosis: {
            facets: result.diagnosis.facets.map((f) => ({
              description: f.description,
              matchedCount: f.matchedCount,
              note: f.note,
              line: f.line(),
            })),
            nearMisses: result.diagnosis.nearMisses.map((nm) => ({
              elementId: nm.elementId,
              elementLabel: nm.elementLabel,
              matchedFacets: nm.matchedFacets,
              totalFacets: nm.totalFacets,
              actualValues: nm.actualValues,
              line: nm.line(),
            })),
          },
          unmetTotal: unmetAll.length,
          unmet: this.unmetPage(result, 0, examples),
        };
      }),
    }));

    return {
      header: this.lastReport.header(),
      verdict: ReportText.verdictLine(this.lastReport),
      mapping: this.service.currentMapping().name,
      runs,
      elementStatuses: this.computeElementStatuses(current),
    };
  }

  private unmetPage(
    result: SpecCheckResult,
    offset: number,
    limit: number
  ): Array<Record<string, unknown>> {
    return result.findings
      .filter((f) => !isFindingOk(f.status))
      .slice(Math.max(offset, 0), Math.max(offset, 0) + Math.max(limit, 0))
      .map((f) => ({
        elementId: f.elementId,
        expressId: f.expressId,
        element: f.elementLabel,
        line: f.requirementLine(),
      }));
  }

  reportPage(run: number, spec: number, offset = 0, limit = 25): Record<string, unknown> {
    if (!this.lastReport) {
      throw new Error('Noch keine Prüfung - zuerst check_model aufrufen.');
    }
    if (run < 0 || run >= this.lastReport.runs.length) {
      throw new Error(`Kein Lauf ${run} in der letzten Prüfung.`);
    }
    const results = this.lastReport.runs[run].results;
    if (spec < 0 || spec >= results.length) {
      throw new Error(`Keine Spezifikation ${spec} in Lauf ${run}.`);
    }
    const result = results[spec];
    const total = result.findings.filter((f) => !isFindingOk(f.status)).length;
    return {
      run,
      spec,
      offset: Math.max(offset, 0),
      total,
      unmet: this.unmetPage(result, offset, limit),
    };
  }

  async elementReport(element: string): Promise<string> {
    if (!element || !element.trim()) {
      throw new Error('Kein Element angegeben (GlobalId oder Namensteil).');
    }
    if (!this.lastReport) {
      throw new Error('Noch keine Prüfung - zuerst check_model aufrufen.');
    }
    const current = await this.model();
    const needle = element.trim();
    const lower = needle.toLowerCase();
    const matches = current.elements
      .filter((e) => e.id === needle || e.name.toLowerCase().includes(lower))
      .slice(0, 5);

    if (matches.length === 0) {
      return `Kein Element mit der GlobalId oder dem Namensteil '${element}'.`;
    }

    const lines: string[] = [];
    for (const match of matches) {
      lines.push(`${match.entityName} '${match.name}' (${match.id})`);
      let any = false;
      for (const result of this.lastReport.results()) {
        const findings = result.findings.filter((f) => f.elementId === match.id);
        if (findings.length > 0) {
          any = true;
          lines.push(`   ${result.specificationLabel}:`);
          for (const f of findings) {
            lines.push(`      ${f.requirementLine()}`);
          }
        }
      }
      if (!any) {
        lines.push('   von keiner geprüften Spezifikation erfasst');
      }
      lines.push(`   Merkmale: ${match.properties.length}`);
      for (const p of match.properties.slice(0, 40)) {
        lines.push(
          `      ${formatPropertyKey(p.key)} = ${p.values.join(', ')} (${p.dataType})`
        );
      }
    }
    return lines.join('\n');
  }

  /**
   * App-only 3D geometry provider: returns triangle meshes from `@ifc-lite/geometry`
   * annotated with each element's AIA / LOIN check status (`PASSED`, `FAILED`, `UNCHECKED`)
   * so the MCP App 3D canvas can render and highlight elements in 3D inside Antigravity / Claude Desktop.
   */
  async elementMeshes(globalIds?: readonly string[]): Promise<{
    modelName: string;
    schema: string;
    totalElementsWithGeometry: number;
    meshes: Array<ElementMesh3D & { status: 'PASSED' | 'FAILED' | 'UNCHECKED' }>;
  }> {
    const current = await this.model();
    const extracted = await current.extractElementMeshes(
      globalIds && globalIds.length > 0 ? globalIds : undefined
    );
    const statusMap = new Map(
      this.computeElementStatuses(current).map((s) => [s.globalId, s.status])
    );

    return {
      modelName: current.name,
      schema: current.rawSchema,
      totalElementsWithGeometry: extracted.totalElementsWithGeometry,
      meshes: extracted.meshes.map((m) => ({
        ...m,
        status: statusMap.get(m.globalId) ?? 'UNCHECKED',
      })),
    };
  }

  mapping(source?: string | null): string {
    if (source && source.trim()) {
      this.service.useMapping(PropertyMapping.resolve(source));
      this.lastReport = null;
    }
    return this.service.currentMapping().describe().join('\n');
  }

  async exportBcfFile(outputPath: string): Promise<string> {
    if (!this.lastReport) {
      throw new Error('Noch keine Prüfung - zuerst check_model aufrufen.');
    }
    const bytes = await this.service.exportBcf(this.lastReport);
    writeFileSync(outputPath, bytes);
    return `BCF-Archiv gespeichert: ${outputPath} (${bytes.byteLength} Bytes)`;
  }

  status(): string {
    const lines: string[] = [
      `Modell: ${
        this.currentModel
          ? this.currentModel.summary()
          : 'noch keines geladen (Standardmodell wird genutzt)'
      }`,
      `Mapping: ${this.service.currentMapping().name}`,
      `Letzte Prüfung: ${
        this.lastReport ? ReportText.verdictLine(this.lastReport) : 'keine'
      }`,
    ];
    return lines.join('\n');
  }
}
