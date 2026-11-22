import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  CheckService,
  createPropertyKey,
  LoadedModel,
  ModelFit,
  PropertyMapping,
  ReportText,
} from '../src/index.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const RESOURCES = resolve(__dirname, '../../../resources');

describe('@checkmate/core (IFClite + CheckMate engine)', () => {
  it('loads the bundled BIM.Hamburg mapping and resolves aliases in order', () => {
    const mapping = PropertyMapping.bundled();
    expect(mapping.name).toBe('BIM.Hamburg Brücken');
    const cands = mapping.candidates(createPropertyKey('Klassifizierung', 'Objekttyp'));
    expect(cands).toEqual([
      { propertySet: 'Klassifizierung', baseName: 'Objekttyp' },
      { propertySet: 'Pset_Objektinformation', baseName: '_IDEbene3' },
    ]);
  });

  it('parses the bundled bridge IFC model via @ifc-lite/parser and ranks LOINs with ModelFit', async () => {
    const model = await LoadedModel.configured();
    expect(model.rawSchema).toBe('IFC2X3');
    expect(model.elements.length).toBeGreaterThan(200);

    const fit = ModelFit.of(model.elements);
    const kappeFit = fit.fit({
      name: 'Ueberbau > Kappe',
      objectTypes: ['Kappe'],
    });
    expect(kappeFit.kind).toBe('CLASSIFICATION');
    expect(kappeFit.elements).toBeGreaterThan(0);
  });

  it('extracts 3D meshes from the bridge model via @ifc-lite/geometry WASM', async () => {
    const model = await LoadedModel.configured();
    const kappe = model.elements.find((e) => e.name.includes('Kappe'));
    expect(kappe).toBeDefined();
    const meshData = await model.extractElementMeshes(kappe ? [kappe.id] : undefined);
    expect(meshData.totalElementsWithGeometry).toBeGreaterThan(0);
    expect(meshData.meshes.length).toBeGreaterThan(0);
    expect(meshData.meshes[0].positions.length).toBeGreaterThan(0);
  });

  it('runs IDS validation with MappedDataAccessor and produces zero-applicability near-miss diagnosis', async () => {
    const model = await LoadedModel.configured();
    const service = new CheckService();
    const kappeIdsPath = resolve(RESOURCES, 'fixtures/ids/portal/ueberbau-kappe-lph8-9.ids');

    const report = await service.check(
      [{ kind: 'idsFile', path: kappeIdsPath }],
      model
    );

    expect(report.runs[0].error).toBeNull();
    expect(report.results().length).toBe(1);
    const res = report.results()[0];
    // On the bundled bridge model, Ueberbau > Kappe has 0 applicable elements (Kappe vs Ueberbau / IFCTEXT vs IFCLABEL),
    // so CheckMate produces NOT_APPLICABLE (Exit-Code 4) with a detailed facet & near-miss diagnosis.
    expect(res.verdict).toBe('NOT_APPLICABLE');
    expect(report.exitCode()).toBe(4);
    expect(res.diagnosis.facets.length).toBeGreaterThan(0);
    expect(res.diagnosis.nearMisses.length).toBeGreaterThan(0);

    const fullText = ReportText.full(report).join('\n');
    expect(fullText).toContain('nicht prüfbar');
    expect(fullText).toContain('Diagnose der Anwendbarkeit');
    expect(fullText).toContain('als Pset_Objektinformation/_IDEbene');
  });

  it('validates custom IDS XML where mapped alias satisfies applicability and exports BCF on failure', async () => {
    const model = await LoadedModel.configured();
    const service = new CheckService();

    // Custom IDS matching the bridge's actual _IDEbene3='Kappe' via Klassifizierung/Objekttyp alias,
    // but requiring a missing property 'Bauteilinformation/Feldnummer_Test' so it fails and exports BCF.
    const customIds = `<?xml version="1.0" encoding="UTF-8"?>
<ids:ids xmlns:ids="http://standards.buildingsmart.org/IDS" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <ids:info><ids:title>Kappe Alias Test</ids:title></ids:info>
  <ids:specifications>
    <ids:specification name="Kappe Prüfung" ifcVersion="IFC2X3">
      <ids:applicability minOccurs="0" maxOccurs="unbounded">
        <ids:property>
          <ids:propertySet><ids:simpleValue>Klassifizierung</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>Objekttyp</ids:simpleValue></ids:baseName>
          <ids:value><ids:simpleValue>Kappe</ids:simpleValue></ids:value>
        </ids:property>
      </ids:applicability>
      <ids:requirements>
        <ids:property cardinality="required">
          <ids:propertySet><ids:simpleValue>Bauteilinformation</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>NichtVorhandenesMerkmal</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:requirements>
    </ids:specification>
  </ids:specifications>
</ids:ids>`;

    const report = await service.check(
      [{ kind: 'idsXml', name: 'Kappe Alias Test', xml: customIds }],
      model
    );

    const res = report.results()[0];
    expect(res.applicableElements).toBeGreaterThan(0);
    expect(res.verdict).toBe('FAILED');
    expect(report.exitCode()).toBe(3);

    const bcfBytes = await service.exportBcf(report);
    expect(bcfBytes.byteLength).toBeGreaterThan(100);
  });
});
