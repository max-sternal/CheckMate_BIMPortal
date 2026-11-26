#!/usr/bin/env node
/**
 * Reproducible IFC4 STEP generator for CheckMate's two compact BIM Portal example models:
 *
 * 1. `resources/models/mini-hochbau-bundesbau.ifc` (~135 STEP lines)
 *    A minimal single-storey building pavilion (1 IfcBuilding, 1 IfcBuildingStorey,
 *    1 IfcSlab floor, 2 IfcWall exterior walls, 1 IfcRoof flat roof) with 3D SweptSolid
 *    geometry and exact PropertySets for 5 live BIM Portal LOINs ("Beispiel AIA Bundesbauten"):
 *      - Gebäude       (a3ce341f-d275-4553-911b-00cce2e489c6)
 *      - Geschosse     (c5403b95-3517-4412-b860-66de2d7bafdd)
 *      - Geschossdecken(c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca)
 *      - Wand, massiv  (e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b)
 *      - Dach          (608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e)
 *
 * 2. `resources/models/mini-infrastruktur-bruecke.ifc` (~140 STEP lines)
 *    A minimal single-span road bridge (2 IfcFooting foundations, 2 IfcColumn pier shafts,
 *    1 IfcSlab bridge deck, 2 IfcBeam bridge copings / Kappen) with 3D SweptSolid geometry
 *    and exact PropertySets (`Klassifizierung` + `Bauteilinformation`) for 6 live BIM Portal
 *    LOINs ("Pflegestelle Straße / Ingenieurbau"):
 *      - Gruendung > Einzelfundament: LPH 1-9 (5d35c1cf-4c24-560f-ad17-506851e78011)
 *      - Pfeiler > Pfeilerschaft: LPH 1-9     (0785d1cb-857b-553d-9b1b-a28a2636e063)
 *      - Ueberbau > Fahrbahnplatte: LPH 1-9   (f5884e9e-f149-57cb-aa08-3b1462626e30)
 *      - Ueberbau > Fahrbahnplatte: LPH 3-9   (81586d48-c9eb-5ec7-88b6-b2259a6352f9)
 *      - Ueberbau > Fahrbahnplatte: LPH 8-9   (d4705d1e-d5b2-5728-8f15-8de87b5d626e)
 *      - Ueberbau > Kappe: LPH 8-9            (be959b9d-daef-5315-9aba-f60b6ad54ccb)
 *
 * Run with:
 *   node --import tsx scripts/generate-mini-models.ts
 */

import { createHash } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const IFC_B64 = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$';

/** Deterministic 22-character IFC GlobalId derived from a readable seed string. */
function ifcGuid(seed: string): string {
  const bytes = createHash('sha256').update(`checkmate:${seed}`).digest().subarray(0, 16);
  let out = '';
  // First 2 bits (0..3)
  out += IFC_B64[(bytes[0] >> 6) & 0x03];
  let bitPos = 2;
  for (let i = 0; i < 21; i++) {
    const byteIdx = Math.floor(bitPos / 8);
    const bitOffset = bitPos % 8;
    const combined =
      ((bytes[byteIdx] ?? 0) << 16) |
      ((bytes[byteIdx + 1] ?? 0) << 8) |
      (bytes[byteIdx + 2] ?? 0);
    const val = (combined >> (18 - bitOffset)) & 0x3f;
    out += IFC_B64[val];
    bitPos += 6;
  }
  return out;
}

function stepNum(n: number): string {
  return Number.isInteger(n) ? `${n}.` : `${n}`;
}

function stepStr(s: string): string {
  // Encode German umlauts & non-ASCII characters via ISO-10303-21 \X2\...\X0\ hex escaping
  let out = "'";
  for (const ch of s) {
    const code = ch.codePointAt(0)!;
    if (ch === "'") {
      out += "''";
    } else if (ch === '\\') {
      out += '\\\\';
    } else if (code >= 0x20 && code <= 0x7e) {
      out += ch;
    } else {
      out += `\\X2\\${code.toString(16).toUpperCase().padStart(4, '0')}\\X0\\`;
    }
  }
  out += "'";
  return out;
}

type PropVal =
  | { kind: 'label'; value: string }
  | { kind: 'integer'; value: number }
  | { kind: 'boolean'; value: boolean };

class StepBuilder {
  private nextId = 1;
  private readonly lines: string[] = [];
  readonly fileName: string;
  readonly description: string;

  // Shared geometric primitives initialized in initHeaderAndContext()
  ownerHistory = '';
  origin3d = '';
  dirZ = '';
  dirX = '';
  axisWorld = '';
  axis2dCenter = '';
  bodySubContext = '';
  project = '';

  constructor(fileName: string, description: string, projectName: string) {
    this.fileName = fileName;
    this.description = description;
    this.initHeaderAndContext(projectName);
  }

  add(entityExpr: string): string {
    const ref = `#${this.nextId++}`;
    this.lines.push(`${ref}=${entityExpr};`);
    return ref;
  }

  private initHeaderAndContext(projectName: string): void {
    const person = this.add(`IFCPERSON($,${stepStr('Team CheckMate')},$,$,$,$,$,$)`);
    const org = this.add(`IFCORGANIZATION($,${stepStr('CheckMate BIM-Portal')},$,$,$)`);
    const pao = this.add(`IFCPERSONANDORGANIZATION(${person},${org},$)`);
    const app = this.add(
      `IFCAPPLICATION(${org},${stepStr('0.4.1-beta')},${stepStr('CheckMate IFC4 Generator')},${stepStr('checkmate-ifc')})`
    );
    this.ownerHistory = this.add(`IFCOWNERHISTORY(${pao},${app},$,.ADDED.,1764172800,$,$,1764172800)`);

    const uLen = this.add('IFCSIUNIT(*,.LENGTHUNIT.,$,.METRE.)');
    const uArea = this.add('IFCSIUNIT(*,.AREAUNIT.,$,.SQUARE_METRE.)');
    const uVol = this.add('IFCSIUNIT(*,.VOLUMEUNIT.,$,.CUBIC_METRE.)');
    const units = this.add(`IFCUNITASSIGNMENT((${uLen},${uArea},${uVol}))`);

    this.origin3d = this.add('IFCCARTESIANPOINT((0.,0.,0.))');
    this.dirZ = this.add('IFCDIRECTION((0.,0.,1.))');
    this.dirX = this.add('IFCDIRECTION((1.,0.,0.))');
    this.axisWorld = this.add(`IFCAXIS2PLACEMENT3D(${this.origin3d},${this.dirZ},${this.dirX})`);

    const origin2d = this.add('IFCCARTESIANPOINT((0.,0.))');
    const dir2dX = this.add('IFCDIRECTION((1.,0.))');
    this.axis2dCenter = this.add(`IFCAXIS2PLACEMENT2D(${origin2d},${dir2dX})`);

    const ctx = this.add(`IFCGEOMETRICREPRESENTATIONCONTEXT($,'Model',3,1.E-05,${this.axisWorld},$)`);
    this.bodySubContext = this.add(
      `IFCGEOMETRICREPRESENTATIONSUBCONTEXT('Body','Model',*,*,*,*,${ctx},$,.MODEL_VIEW.,$)`
    );

    this.project = this.add(
      `IFCPROJECT('${ifcGuid(`${this.fileName}:project`)}',${this.ownerHistory},${stepStr(projectName)},${stepStr(this.description)},$,$,$,(${ctx}),${units})`
    );
  }

  localPlacement(parentPlacement: string | null, x: number, y: number, z: number): string {
    const pt = this.add(`IFCCARTESIANPOINT((${stepNum(x)},${stepNum(y)},${stepNum(z)}))`);
    const ax = this.add(`IFCAXIS2PLACEMENT3D(${pt},${this.dirZ},${this.dirX})`);
    return this.add(`IFCLOCALPLACEMENT(${parentPlacement ?? '$'},${ax})`);
  }

  /** Creates a 3D extruded rectangular box centered at (0,0) in XY and extruded +height in Z. */
  extrudedBoxShape(
    dimX: number,
    dimY: number,
    heightZ: number,
    rgb?: [number, number, number]
  ): string {
    const prof = this.add(
      `IFCRECTANGLEPROFILEDEF(.AREA.,$,${this.axis2dCenter},${stepNum(dimX)},${stepNum(dimY)})`
    );
    const solid = this.add(
      `IFCEXTRUDEDAREASOLID(${prof},${this.axisWorld},${this.dirZ},${stepNum(heightZ)})`
    );
    if (rgb) {
      const col = this.add(
        `IFCCOLOURRGB($,${rgb[0].toFixed(3)},${rgb[1].toFixed(3)},${rgb[2].toFixed(3)})`
      );
      const rendering = this.add(`IFCSURFACESTYLERENDERING(${col},0.,$,$,$,$,$,$,.NOTDEFINED.)`);
      const style = this.add(`IFCSURFACESTYLE($,.BOTH.,(${rendering}))`);
      this.add(`IFCSTYLEDITEM(${solid},(${style}),$)`);
    }
    const rep = this.add(
      `IFCSHAPEREPRESENTATION(${this.bodySubContext},'Body','SweptSolid',(${solid}))`
    );
    return this.add(`IFCPRODUCTDEFINITIONSHAPE($,$,(${rep}))`);
  }

  attachPropertySet(
    seed: string,
    targetRefs: string[],
    psetName: string,
    props: Record<string, PropVal>
  ): void {
    const propRefs: string[] = [];
    for (const [k, v] of Object.entries(props)) {
      let valExpr: string;
      if (v.kind === 'label') {
        valExpr = `IFCLABEL(${stepStr(v.value)})`;
      } else if (v.kind === 'integer') {
        valExpr = `IFCINTEGER(${Math.trunc(v.value)})`;
      } else {
        valExpr = `IFCBOOLEAN(${v.value ? '.T.' : '.F.'})`;
      }
      propRefs.push(this.add(`IFCPROPERTYSINGLEVALUE(${stepStr(k)},$,${valExpr},$)`));
    }
    const pset = this.add(
      `IFCPROPERTYSET('${ifcGuid(`${seed}:pset:${psetName}`)}',${this.ownerHistory},${stepStr(psetName)},$,(${propRefs.join(',')}))`
    );
    this.add(
      `IFCRELDEFINESBYPROPERTIES('${ifcGuid(`${seed}:rel:${psetName}`)}',${this.ownerHistory},$,$,(${targetRefs.join(',')}),${pset})`
    );
  }

  toStep(): string {
    return [
      'ISO-10303-21;',
      'HEADER;',
      "FILE_DESCRIPTION(('ViewDefinition [ReferenceView_V1.2]'),'2;1');",
      `FILE_NAME(${stepStr(this.fileName)},'2026-11-26T12:00:00',('Team CheckMate'),('CheckMate BIM-Portal'),'CheckMate 0.4.1-beta','CheckMate IFC4 Generator','Apache-2.0');`,
      "FILE_SCHEMA(('IFC4'));",
      'ENDSEC;',
      'DATA;',
      ...this.lines,
      'ENDSEC;',
      'END-ISO-10303-21;',
      '',
    ].join('\n');
  }
}

function buildMiniHochbau(): string {
  const b = new StepBuilder(
    'mini-hochbau-bundesbau.ifc',
    'Kompaktes Hochbau-Beispielmodell fuer 5 LOINs aus Beispiel AIA Bundesbauten (LPH 3)',
    'CheckMate Mini-Hochbau (AIA Bundesbauten)'
  );

  const sitePl = b.localPlacement(null, 0, 0, 0);
  const site = b.add(
    `IFCSITE('${ifcGuid('hb:site')}',${b.ownerHistory},${stepStr('Grundstueck Bundespavillon')},$,$,${sitePl},$,$,.ELEMENT.,$,$,0.,$,$)`
  );
  b.add(`IFCRELAGGREGATES('${ifcGuid('hb:agg:proj-site')}',${b.ownerHistory},$,$,${b.project},(${site}))`);

  const bldgGuid = ifcGuid('hb:building');
  const bldgPl = b.localPlacement(sitePl, 0, 0, 0);
  const bldg = b.add(
    `IFCBUILDING('${bldgGuid}',${b.ownerHistory},${stepStr('Pavillon Haus A')},$,$,${bldgPl},$,${stepStr('Pavillon Haus A')},.ELEMENT.,$,$,$)`
  );
  b.add(`IFCRELAGGREGATES('${ifcGuid('hb:agg:site-bldg')}',${b.ownerHistory},$,$,${site},(${bldg}))`);

  // 1. LOIN Gebäude (a3ce341f-d275-4553-911b-00cce2e489c6)
  b.attachPropertySet('hb:bldg', [bldg], 'Gebäude', {
    'Gebäude-Bezeichnung': { kind: 'label', value: 'Pavillon Haus A' },
    'Gebäude-ID': { kind: 'label', value: bldgGuid },
    'Bauwerkstyp': { kind: 'label', value: 'Verwaltungsbau' },
    'Gebäudeteil-ID': { kind: 'label', value: 'A' },
    'Gebäude-Nummer': { kind: 'label', value: '01' },
  });

  const storeyGuid = ifcGuid('hb:storey:eg');
  const storeyPl = b.localPlacement(bldgPl, 0, 0, 0);
  const storey = b.add(
    `IFCBUILDINGSTOREY('${storeyGuid}',${b.ownerHistory},${stepStr('00 Erdgeschoss')},$,$,${storeyPl},$,$,.ELEMENT.,0.)`
  );
  b.add(`IFCRELAGGREGATES('${ifcGuid('hb:agg:bldg-storey')}',${b.ownerHistory},$,$,${bldg},(${storey}))`);

  // 2. LOIN Geschosse (c5403b95-3517-4412-b860-66de2d7bafdd)
  b.attachPropertySet('hb:storey', [storey], 'Geschosse', {
    'Geschoss-Codierung': { kind: 'label', value: '00' },
    'Geschoss-ID': { kind: 'label', value: storeyGuid },
  });

  // 3. LOIN Geschossdecken (c9e1d502-6d9a-4da4-9bd1-1cd251cae3ca): 1 IfcSlab (6.0m x 5.0m x 0.25m)
  const slabShape = b.extrudedBoxShape(6.0, 5.0, 0.25, [0.72, 0.75, 0.78]);
  const slabPl = b.localPlacement(storeyPl, 0, 0, -0.25);
  const slab = b.add(
    `IFCSLAB('${ifcGuid('hb:slab:eg')}',${b.ownerHistory},${stepStr('Bodenplatte EG')},$,$,${slabPl},${slabShape},'BP-01',.USERDEFINED.)`
  );
  const slabType = b.add(
    `IFCSLABTYPE('${ifcGuid('hb:slabtype')}',${b.ownerHistory},${stepStr('Stahlbetonplatte 25cm')},$,$,$,$,$,$,.USERDEFINED.)`
  );
  b.add(`IFCRELDEFINESBYTYPE('${ifcGuid('hb:reltype:slab')}',${b.ownerHistory},$,$,(${slab}),${slabType})`);

  b.attachPropertySet('hb:slab', [slab], 'Geschossdecken', {
    'Name': { kind: 'label', value: 'Bodenplatte EG' },
    'Bauteiltyp': { kind: 'label', value: 'Bodenplatte' },
    'Material': { kind: 'label', value: 'Stahlbeton C30/37' },
    'Funktion, Lage': { kind: 'label', value: 'Bodenplatte, Erdgeschoss' },
    'Tragend': { kind: 'label', value: 'ja' },
    'Gebäude-ID': { kind: 'label', value: bldgGuid },
    'Geschoss-ID': { kind: 'label', value: storeyGuid },
    'Länge (m)': { kind: 'label', value: '6.00' },
    'Breite (m)': { kind: 'label', value: '5.00' },
    'Höhe (m)': { kind: 'label', value: '0.25' },
    'Fläche': { kind: 'label', value: '30.00' },
    'Volumen': { kind: 'label', value: '7.50' },
  });

  // 4. LOIN Wand, massiv (e84ce0d9-0ca7-4d11-a5be-eefbfcf8102b): 2 IfcWall (6.0m x 0.30m x 3.00m)
  const wallShape = b.extrudedBoxShape(6.0, 0.3, 3.0, [0.88, 0.85, 0.78]);
  const wallNorthPl = b.localPlacement(storeyPl, 0, 2.35, 0);
  const wallSouthPl = b.localPlacement(storeyPl, 0, -2.35, 0);
  const wallNorth = b.add(
    `IFCWALL('${ifcGuid('hb:wall:north')}',${b.ownerHistory},${stepStr('Aussenwand Nord')},$,$,${wallNorthPl},${wallShape},'AW-01',.USERDEFINED.)`
  );
  const wallSouth = b.add(
    `IFCWALL('${ifcGuid('hb:wall:south')}',${b.ownerHistory},${stepStr('Aussenwand Sued')},$,$,${wallSouthPl},${wallShape},'AW-02',.USERDEFINED.)`
  );
  const wallType = b.add(
    `IFCWALLTYPE('${ifcGuid('hb:walltype')}',${b.ownerHistory},${stepStr('Massivwand STB 30cm')},$,$,$,$,$,$,.USERDEFINED.)`
  );
  b.add(
    `IFCRELDEFINESBYTYPE('${ifcGuid('hb:reltype:wall')}',${b.ownerHistory},$,$,(${wallNorth},${wallSouth}),${wallType})`
  );

  for (const [key, wRef, wName] of [
    ['north', wallNorth, 'Aussenwand Nord'],
    ['south', wallSouth, 'Aussenwand Sued'],
  ] as const) {
    b.attachPropertySet(`hb:wall:${key}`, [wRef], 'Wand, massiv', {
      'Name': { kind: 'label', value: wName },
      'Bauteiltyp': { kind: 'label', value: 'Aussenwand massiv' },
      'Material': { kind: 'label', value: 'Stahlbeton C25/30' },
      'Gebäude-ID': { kind: 'label', value: bldgGuid },
      'Geschoss-ID': { kind: 'label', value: storeyGuid },
      'Länge (m)': { kind: 'label', value: '6.00' },
      'Tiefe (m)': { kind: 'label', value: '0.30' },
      'Höhe (m)': { kind: 'label', value: '3.00' },
      'Fläche': { kind: 'label', value: '18.00' },
      'Volumen': { kind: 'label', value: '5.40' },
    });
  }

  // 5. LOIN Dach (608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e): 1 IfcRoof (6.4m x 5.4m x 0.25m)
  const roofShape = b.extrudedBoxShape(6.4, 5.4, 0.25, [0.58, 0.25, 0.22]);
  const roofPl = b.localPlacement(storeyPl, 0, 0, 3.0);
  const roof = b.add(
    `IFCROOF('${ifcGuid('hb:roof')}',${b.ownerHistory},${stepStr('Flachdach Pavillon')},$,$,${roofPl},${roofShape},'DA-01',.USERDEFINED.)`
  );
  const roofType = b.add(
    `IFCROOFTYPE('${ifcGuid('hb:rooftype')}',${b.ownerHistory},${stepStr('Flachdach STB 25cm')},$,$,$,$,$,$,.USERDEFINED.)`
  );
  b.add(`IFCRELDEFINESBYTYPE('${ifcGuid('hb:reltype:roof')}',${b.ownerHistory},$,$,(${roof}),${roofType})`);

  b.attachPropertySet('hb:roof', [roof], 'Dach', {
    'Name': { kind: 'label', value: 'Flachdach Pavillon' },
    'Bauteiltyp': { kind: 'label', value: 'Flachdach' },
    'Material': { kind: 'label', value: 'Stahlbeton mit Gefaelledaemmung' },
    'Gebäude-ID': { kind: 'label', value: bldgGuid },
    'Geschoss-ID': { kind: 'label', value: storeyGuid },
    'Länge (m)': { kind: 'label', value: '6.40' },
    'Breite (m)': { kind: 'label', value: '5.40' },
    'Tiefe (m)': { kind: 'label', value: '0.25' },
    'Fläche': { kind: 'label', value: '34.56' },
    'Volumen': { kind: 'label', value: '8.64' },
  });

  b.add(
    `IFCRELCONTAINEDINSPATIALSTRUCTURE('${ifcGuid('hb:spatial')}',${b.ownerHistory},$,$,(${slab},${wallNorth},${wallSouth},${roof}),${storey})`
  );

  return b.toStep();
}

function buildMiniInfrastruktur(): string {
  const b = new StepBuilder(
    'mini-infrastruktur-bruecke.ifc',
    'Kompaktes Infrastruktur-Beispielmodell (Einfeldbruecke) fuer 6 BIM-Portal-LOINs (Ingenieurbau)',
    'CheckMate Mini-Infrastruktur (Einfeldbruecke)'
  );

  const sitePl = b.localPlacement(null, 0, 0, 0);
  const site = b.add(
    `IFCSITE('${ifcGuid('ib:site')}',${b.ownerHistory},${stepStr('Trasse Einfeldbruecke BW 01')},$,$,${sitePl},$,$,.ELEMENT.,$,$,0.,$,$)`
  );
  b.add(`IFCRELAGGREGATES('${ifcGuid('ib:agg:proj-site')}',${b.ownerHistory},$,$,${b.project},(${site}))`);

  const structPl = b.localPlacement(sitePl, 0, 0, 0);
  const bridgeContainer = b.add(
    `IFCBUILDING('${ifcGuid('ib:bridge-container')}',${b.ownerHistory},${stepStr('Einfeldbruecke BW 01')},$,$,${structPl},$,${stepStr('Ingenieurbauwerk')},.ELEMENT.,$,$,$)`
  );
  b.add(
    `IFCRELAGGREGATES('${ifcGuid('ib:agg:site-bridge')}',${b.ownerHistory},$,$,${site},(${bridgeContainer}))`
  );
  const level = b.add(
    `IFCBUILDINGSTOREY('${ifcGuid('ib:level:00')}',${b.ownerHistory},${stepStr('Bauwerksebene 00')},$,$,${structPl},$,$,.ELEMENT.,0.)`
  );
  b.add(
    `IFCRELAGGREGATES('${ifcGuid('ib:agg:bridge-level')}',${b.ownerHistory},$,$,${bridgeContainer},(${level}))`
  );

  // 1. Gruendung > Einzelfundament (5d35c1cf-4c24-560f-ad17-506851e78011): 2x IfcFooting
  const footingShape = b.extrudedBoxShape(2.6, 4.8, 0.8, [0.55, 0.58, 0.62]);
  const footingWestPl = b.localPlacement(structPl, -4.5, 0, -0.8);
  const footingEastPl = b.localPlacement(structPl, 4.5, 0, -0.8);
  const footingWest = b.add(
    `IFCFOOTING('${ifcGuid('ib:footing:west')}',${b.ownerHistory},${stepStr('Einzelfundament West')},$,$,${footingWestPl},${footingShape},'FD-W',.PAD_FOOTING.)`
  );
  const footingEast = b.add(
    `IFCFOOTING('${ifcGuid('ib:footing:east')}',${b.ownerHistory},${stepStr('Einzelfundament Ost')},$,$,${footingEastPl},${footingShape},'FD-O',.PAD_FOOTING.)`
  );
  b.attachPropertySet('ib:footings', [footingWest, footingEast], 'Klassifizierung', {
    'Objektgruppe': { kind: 'label', value: 'Gruendung' },
    'Objektklasse': { kind: 'label', value: 'Fundament' },
    'Objekttyp': { kind: 'label', value: 'Einzelfundament' },
  });

  // 2. Pfeiler > Pfeilerschaft (0785d1cb-857b-553d-9b1b-a28a2636e063): 2x IfcColumn
  const pierShape = b.extrudedBoxShape(0.9, 3.6, 3.6, [0.68, 0.72, 0.76]);
  const pierWestPl = b.localPlacement(structPl, -4.5, 0, 0);
  const pierEastPl = b.localPlacement(structPl, 4.5, 0, 0);
  const pierWest = b.add(
    `IFCCOLUMN('${ifcGuid('ib:pier:west')}',${b.ownerHistory},${stepStr('Pfeilerschaft West')},$,$,${pierWestPl},${pierShape},'PF-W',.PILASTER.)`
  );
  const pierEast = b.add(
    `IFCCOLUMN('${ifcGuid('ib:pier:east')}',${b.ownerHistory},${stepStr('Pfeilerschaft Ost')},$,$,${pierEastPl},${pierShape},'PF-O',.PILASTER.)`
  );
  b.attachPropertySet('ib:piers', [pierWest, pierEast], 'Klassifizierung', {
    'Objektgruppe': { kind: 'label', value: 'Pfeiler' },
    'Objektklasse': { kind: 'label', value: 'Pfeiler' },
    'Objekttyp': { kind: 'label', value: 'Pfeilerschaft' },
  });

  // 3. Ueberbau > Fahrbahnplatte LPH 1-9 / 3-9 / 8-9: 1x IfcSlab
  const deckShape = b.extrudedBoxShape(12.0, 5.2, 0.45, [0.76, 0.79, 0.83]);
  const deckPl = b.localPlacement(structPl, 0, 0, 3.6);
  const deck = b.add(
    `IFCSLAB('${ifcGuid('ib:deck')}',${b.ownerHistory},${stepStr('Fahrbahnplatte Feld 1')},$,$,${deckPl},${deckShape},'FB-01',.BASESLAB.)`
  );
  b.attachPropertySet('ib:deck:klass', [deck], 'Klassifizierung', {
    'Objektgruppe': { kind: 'label', value: 'Ueberbau' },
    'Objektklasse': { kind: 'label', value: 'Platte' },
    'Objekttyp': { kind: 'label', value: 'Fahrbahnplatte' },
  });
  b.attachPropertySet('ib:deck:info', [deck], 'Bauteilinformation', {
    'Fertigteil': { kind: 'label', value: 'nein' },
    'Feldnummer': { kind: 'integer', value: 1 },
  });

  // 4. Ueberbau > Kappe LPH 1-9 & LPH 8-9 (be959b9d-daef-5315-9aba-f60b6ad54ccb): 2x IfcBeam
  const copingShape = b.extrudedBoxShape(12.0, 0.55, 0.25, [0.85, 0.42, 0.24]);
  const copingNorthPl = b.localPlacement(structPl, 0, 2.325, 4.05);
  const copingSouthPl = b.localPlacement(structPl, 0, -2.325, 4.05);
  const copingNorth = b.add(
    `IFCBEAM('${ifcGuid('ib:kappe:north')}',${b.ownerHistory},${stepStr('Kappe Nord')},$,$,${copingNorthPl},${copingShape},'KP-N',.BEAM.)`
  );
  const copingSouth = b.add(
    `IFCBEAM('${ifcGuid('ib:kappe:south')}',${b.ownerHistory},${stepStr('Kappe Sued')},$,$,${copingSouthPl},${copingShape},'KP-S',.BEAM.)`
  );
  b.attachPropertySet('ib:kappen:klass', [copingNorth, copingSouth], 'Klassifizierung', {
    'Objektgruppe': { kind: 'label', value: 'Ueberbau' },
    'Objektklasse': { kind: 'label', value: 'Kappe' },
    'Objekttyp': { kind: 'label', value: 'Kappe' },
  });
  b.attachPropertySet('ib:kappen:info', [copingNorth, copingSouth], 'Bauteilinformation', {
    'Feldnummer': { kind: 'integer', value: 1 },
  });

  b.add(
    `IFCRELCONTAINEDINSPATIALSTRUCTURE('${ifcGuid('ib:spatial')}',${b.ownerHistory},$,$,(${footingWest},${footingEast},${pierWest},${pierEast},${deck},${copingNorth},${copingSouth}),${level})`
  );

  return b.toStep();
}

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const modelsDir = resolve(rootDir, 'resources/models');
mkdirSync(modelsDir, { recursive: true });

const hochbauStep = buildMiniHochbau();
const infraStep = buildMiniInfrastruktur();

const hochbauPath = resolve(modelsDir, 'mini-hochbau-bundesbau.ifc');
const infraPath = resolve(modelsDir, 'mini-infrastruktur-bruecke.ifc');

writeFileSync(hochbauPath, hochbauStep, 'utf-8');
writeFileSync(infraPath, infraStep, 'utf-8');

console.log(
  `Wrote ${hochbauPath} (${hochbauStep.split('\n').length - 1} lines, ${Buffer.byteLength(hochbauStep)} bytes)`
);
console.log(
  `Wrote ${infraPath} (${infraStep.split('\n').length - 1} lines, ${Buffer.byteLength(infraStep)} bytes)`
);
