import { GeometryProcessor, type MeshData } from '@ifc-lite/geometry';
import type { RuleSetFile } from '@ifc-lite/rules';
import {
  applyCatalogueFilter,
  BimPortalClient,
  extractPortalId,
  type CatalogueEntry,
  type CatalogueFilter,
  type SimpleAiaProjectPublicDto,
} from '@checkmate/bimportal';
import {
  BUNDLED_BIM_HAMBURG_MAPPING_JSON,
  CheckReport,
  CheckService,
  describeFit,
  formatPropertyKey,
  isFindingOk,
  LoadedModel,
  LoinDescription,
  ModelFit,
  PropertyMapping,
  ReportJson,
  ReportText,
  type CheckSource,
  type SpecCheckResult,
} from '@checkmate/core';

interface GpuMeshBuffer {
  pb: WebGLBuffer;
  nb: WebGLBuffer;
  ib: WebGLBuffer;
  count: number;
  indexType: number;
  expressId: number;
  globalId: string;
  name: string;
  entityType: string;
  status: 'PASSED' | 'FAILED' | 'UNCHECKED';
}

const DEMO_BRIDGE_IDS = `<?xml version="1.0" encoding="UTF-8"?>
<ids:ids xmlns:ids="http://standards.buildingsmart.org/IDS" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <ids:info><ids:title>BIM.Hamburg Brücke TBW1 — LOIN Überbau &amp; Unterbau Prüfung</ids:title></ids:info>
  <ids:specifications>
    <ids:specification name="Überbau > Kappen (LPH 8-9): Bauteilinformation/Feldnummer" ifcVersion="IFC2X3">
      <ids:applicability minOccurs="0" maxOccurs="unbounded">
        <ids:property>
          <ids:propertySet><ids:simpleValue>Klassifizierung</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>Objekttyp</ids:simpleValue></ids:baseName>
          <ids:value><ids:simpleValue>Kappe</ids:simpleValue></ids:value>
        </ids:property>
      </ids:applicability>
      <ids:requirements>
        <ids:property cardinality="required">
          <ids:propertySet><ids:simpleValue>Pset_Bauteilinformation</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>Feldnummer</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:requirements>
    </ids:specification>
    <ids:specification name="Überbau &amp; Unterbau: Klassifizierung/Objekttyp vorhanden" ifcVersion="IFC2X3">
      <ids:applicability minOccurs="0" maxOccurs="unbounded">
        <ids:property>
          <ids:propertySet><ids:simpleValue>Klassifizierung</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>Objektgruppe</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:applicability>
      <ids:requirements>
        <ids:property cardinality="required">
          <ids:propertySet><ids:simpleValue>Klassifizierung</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>Objekttyp</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:requirements>
    </ids:specification>
  </ids:specifications>
</ids:ids>`;

// Bundled BIM Portal Catalogue seed entries so the BIM Portal tab is immediately populated
// even before live fetch or when working offline, and merges seamlessly with live via.bund.de results
const SEED_CATALOGUE_LOINS: Array<CatalogueEntry & { fixtureUrl?: string }> = [
  {
    guid: '87f1c3b6-1e4a-4c3f-9b2a-010101010001',
    name: 'Ueberbau > Kappe LPH 8-9',
    version: '1.0.0',
    organisation: 'BIM.Hamburg / BIM-Portal Bund',
    objectTypes: ['Kappe'],
    disciplines: ['Ingenieurbau', 'Brückenbau'],
    constructionTypes: ['Brücke'],
    phases: ['LPH 8: Objektüberwachung', 'LPH 9: Objektbetreuung'],
    purposes: ['Bauwerksprüfung', 'Bestandsdokumentation', 'Mengenermittlung'],
    fixtureUrl: './fixtures/ueberbau-kappe-lph8-9.ids',
  },
  {
    guid: '36f83684-552e-4f16-a06c-020202020002',
    name: 'Ueberbau > Schaft LPH 1-9',
    version: '1.0.0',
    organisation: 'BIM.Hamburg / BIM-Portal Bund',
    objectTypes: ['Schaft'],
    disciplines: ['Ingenieurbau', 'Brückenbau'],
    constructionTypes: ['Brücke'],
    phases: ['LPH 1-9: Alle Leistungsphasen'],
    purposes: ['Tragwerksplanung', 'Bauwerksprüfung', 'Koordination'],
    fixtureUrl: './fixtures/ueberbau-schaft-lph1-9.ids',
  },
  {
    guid: 'a1b2c3d4-e5f6-4a7b-8c9d-030303030003',
    name: 'Unterbau > Widerlager & Pfeiler',
    version: '1.0.0',
    organisation: 'BIM.Hamburg',
    objectTypes: ['Widerlager', 'Schaft'],
    disciplines: ['Ingenieurbau', 'Brückenbau'],
    constructionTypes: ['Brücke'],
    phases: ['LPH 5: Ausführungsplanung', 'LPH 8: Objektüberwachung'],
    purposes: ['Koordination', 'Mengenermittlung'],
    fixtureUrl: './fixtures/mehrere-spezifikationen.ids',
  },
  {
    guid: 'f0e1d2c3-b4a5-4987-8765-040404040004',
    name: 'Hochbau > Fenster',
    version: '1.0.0',
    organisation: 'BIM-Portal Bund',
    objectTypes: ['Fenster'],
    disciplines: ['Architektur', 'Hochbau'],
    constructionTypes: ['Hochbau'],
    phases: ['LPH 5: Ausführungsplanung'],
    purposes: ['Ausschreibung', 'Mengenermittlung'],
    fixtureUrl: './fixtures/fenster.ids',
  },
];

const SEED_AIA_PROJECTS: Array<SimpleAiaProjectPublicDto & { fixtureUrl?: string }> = [
  {
    guid: 'c4a1e8f0-1122-4334-9556-050505050005',
    name: 'AIA-Projekt: Neubau Eisenbahnbrücke (ZIP mit mehreren LOINs)',
    version: '1.0',
    organisation: 'DB InfraGO / BIM-Portal',
    fixtureUrl: './fixtures/aia-neubau-eisenbahnbruecke.zip',
  },
  {
    guid: 'd7b2f9a1-2233-4445-a667-060606060006',
    name: 'AIA-Projekt: Straße Demonstrator (ZIP)',
    version: '1.0',
    organisation: 'Autobahn GmbH / BIM-Portal',
    fixtureUrl: './fixtures/aia-strasse-demonstrator.zip',
  },
];

// Pre-computed 3D bounding meshes of bruecke-tbw1-mit-merkmalen.ifc for instant 0ms rendering
const FALLBACK_BRIDGE_BOXES: Array<{
  id: string;
  expressId: number;
  t: string;
  n: string;
  s: 'PASSED' | 'FAILED';
  b: [number, number, number, number, number, number];
}> = [
  { id: '1cxNZQHk18xe_eYvhIpXRg', expressId: 1555, t: 'IfcBuildingElementProxy', n: 'Kappe_Mitte_BW:632563', s: 'FAILED', b: [-15.4, 5.65, -8.22, 15.83, 6.32, -6.62] },
  { id: '2MePDRxq95YBYnkQWyl$yu', expressId: 35001, t: 'IfcBuildingElementProxy', n: 'Kappe_Außen_BW:656578', s: 'FAILED', b: [-15.4, 5.65, 1.13, 15.83, 6.34, 3.23] },
  { id: '3e5NfjnF54mvxue2Mx5Kdh', expressId: 35050, t: 'IfcBuildingElementProxy', n: 'Kappe_Mitte_Flügel_Ost:700999', s: 'FAILED', b: [15.85, 5.65, -8.23, 22.5, 6.32, -6.62] },
  { id: '3e5NfjnF54mvxue2Mx5KZW', expressId: 35102, t: 'IfcBuildingElementProxy', n: 'Kappe_Mitte_Flügel_West:701260', s: 'FAILED', b: [-22.5, 5.65, -8.23, -15.5, 6.32, -6.62] },
  { id: '3e5NfjnF54mvxue2Mx5Koi', expressId: 35180, t: 'IfcBuildingElementProxy', n: 'Kappe_Außen_Flügel_Ost:702208', s: 'FAILED', b: [15.85, 5.65, 1.13, 22.5, 6.34, 3.23] },
  { id: '3e5NfjnF54mvxue2Mx5KnZ', expressId: 35257, t: 'IfcBuildingElementProxy', n: 'Kappe_Außen_Flügel_West:702415', s: 'FAILED', b: [-22.5, 5.65, 1.13, -15.5, 6.34, 3.23] },
  { id: '2MePDRxq95YBYnkQWyl1XG', expressId: 34900, t: 'IfcBuildingElementProxy', n: 'Ueberbau_OrthotropePlatte:648106', s: 'PASSED', b: [-15.4, 4.75, -7.87, 15.83, 5.65, 2.88] },
  { id: '1cxNZQHk18xe_eYvhIpXRY', expressId: 100835, t: 'IfcRailing', n: 'Rueckhaltesystem_Gelaender:632571', s: 'PASSED', b: [-22.3, 6.34, 2.95, 22.3, 7.54, 3.15] },
  { id: '2Fz$ybAnf4$ON9x3$d_E3N', expressId: 34950, t: 'IfcBuildingElementProxy', n: 'FahrzeugRueckhaltesystem_Schutzplanke:676920', s: 'PASSED', b: [-22.5, 6.29, 1.62, 22.5, 7.25, 2.12] },
  { id: '1cxNZQHk18xe_eYvhIpXRz', expressId: 901, t: 'IfcBuildingElementProxy', n: 'Unterbau_Widerlager_West:632548', s: 'PASSED', b: [-22.5, 0, -7.87, -14.5, 4.75, 2.88] },
  { id: '1cxNZQHk18xe_eYvhIpXR_ost', expressId: 902, t: 'IfcBuildingElementProxy', n: 'Unterbau_Widerlager_Ost:664160', s: 'PASSED', b: [14.5, 0, -7.87, 22.5, 4.75, 2.88] },
  { id: '1cxNZQHk18xe_eYvhIpXRu', expressId: 729, t: 'IfcColumn', n: 'Unterbau_PfeilerStuetze_Schaft_1:632545', s: 'PASSED', b: [-0.45, 0, -0.6, 0.45, 4.55, 0.6] },
  { id: '1cxNZQHk18xe_eYvhIpXRx', expressId: 778, t: 'IfcColumn', n: 'Unterbau_PfeilerStuetze_Schaft_2:632546', s: 'PASSED', b: [-0.45, 0, -5.6, 0.45, 4.55, -4.4] },
  { id: '1cxNZQHk18xe_eYvhIpXRU', expressId: 605, t: 'IfcSlab', n: 'Gruendung_Fundament_West:632519', s: 'PASSED', b: [-17.1, -1.25, -7.62, -14.25, 0, 2.63] },
  { id: '2MePDRxq95YBYnkQWylzcS', expressId: 606, t: 'IfcSlab', n: 'Gruendung_Fundament_Ost:664166', s: 'PASSED', b: [14.25, -1.25, -7.62, 17.1, 0, 2.63] },
  { id: '1cxNZQHk18xe_eYvhIpXRI', expressId: 669, t: 'IfcSlab', n: 'Gruendung_Pfahlkopfplatte_Mitte:632523', s: 'PASSED', b: [-0.95, -0.75, -8, 0.95, 0, 3] },
  { id: '2MePDRxq95YBYnkQWyl0vC', expressId: 2137, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M1:652726', s: 'PASSED', b: [-0.75, -8.5, 2.1, -0.2, -0.75, 2.75] },
  { id: '0Sb8twv_X778O9krzvDQsw', expressId: 2138, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M2:677452', s: 'PASSED', b: [0.2, -8.5, 2.1, 0.75, -0.75, 2.75] },
  { id: '0Sb8twv_X778O9krzvDQsc', expressId: 2139, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M3:677456', s: 'PASSED', b: [-0.75, -8.5, -1.5, -0.2, -0.75, -0.9] },
  { id: '0Sb8twv_X778O9krzvDQsa', expressId: 2140, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M4:677458', s: 'PASSED', b: [0.2, -8.5, -1.5, 0.75, -0.75, -0.9] },
  { id: '0Sb8twv_X778O9krzvDQsY', expressId: 2141, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M5:677460', s: 'PASSED', b: [-0.75, -8.5, -4.1, -0.2, -0.75, -3.5] },
  { id: '0Sb8twv_X778O9krzvDQsW', expressId: 2142, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M6:677462', s: 'PASSED', b: [0.2, -8.5, -4.1, 0.75, -0.75, -3.5] },
  { id: '2MePDRxq95YBYnkQWyl0tl', expressId: 2143, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M7:652809', s: 'PASSED', b: [-0.75, -8.5, -7.75, -0.2, -0.75, -7.15] },
  { id: '2MePDRxq95YBYnkQWyl0tn', expressId: 2144, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M8:652811', s: 'PASSED', b: [0.2, -8.5, -7.75, 0.75, -0.75, -7.15] },
];

// Use Vite dev proxy `/bim-portal-api` when running on localhost so CORS never blocks live requests
const portalBaseUrl =
  typeof window !== 'undefined' &&
  (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
    ? `${window.location.origin}/bim-portal-api`
    : 'https://via.bund.de/bim';

const portalClient = new BimPortalClient({ baseUrl: portalBaseUrl });
const checkService = new CheckService(portalClient, PropertyMapping.bundled());

let currentModel: LoadedModel | null = null;
let lastReport: CheckReport | null = null;
let lastSources: CheckSource[] = [
  { kind: 'idsXml', name: 'BIM.Hamburg Brücke — Kappen Feldnummer', xml: DEMO_BRIDGE_IDS },
];
let showFullReport = false;

let catalogueLoins: Array<CatalogueEntry & { fixtureUrl?: string }> = [...SEED_CATALOGUE_LOINS];
let catalogueProjects: Array<SimpleAiaProjectPublicDto & { fixtureUrl?: string }> = [
  ...SEED_AIA_PROJECTS,
];
const checkedLoinGuids = new Set<string>([SEED_CATALOGUE_LOINS[0].guid]);
let activeDescribedLoin: (CatalogueEntry & { fixtureUrl?: string }) | null = null;
let activeDescribedProject: (SimpleAiaProjectPublicDto & { fixtureUrl?: string }) | null = null;

let activeEntityFilter = '';
let activeElementNameFilter = '';

// WebGL 3D Viewer State
const canvas = document.getElementById('viewer3d') as HTMLCanvasElement;
const gl = canvas.getContext('webgl', { antialias: true });
let uint32Ext: OES_element_index_uint | null = null;
let prog: WebGLProgram | null = null;
let gpuMeshes: GpuMeshBuffer[] = [];
let bboxCenter: [number, number, number] = [0, 1.5, -2.5];
let bboxRadius = 21;
let rotX = -0.45;
let rotY = 0.62;
let zoom = 1.0;
let panX = 0;
let panY = 0;
let selectedGlobalIds = new Set<string>();
let filterFailedOnly = false;

// --- 1. WebGL 3D Engine ---

function initWebGL(): void {
  if (!gl) return;
  uint32Ext = gl.getExtension('OES_element_index_uint');
  const vs = `
    attribute vec3 aPos;
    attribute vec3 aNorm;
    uniform float uRotX, uRotY, uZoom, uAspect;
    uniform vec2 uPan;
    uniform vec3 uCenter;
    uniform float uRadius;
    varying vec3 vNorm;
    void main() {
      vec3 p = (aPos - uCenter) / uRadius;
      float cy = cos(uRotY), sy = sin(uRotY);
      float cx = cos(uRotX), sx = sin(uRotX);
      vec3 r1 = vec3(p.x * cy - p.z * sy, p.y, p.x * sy + p.z * cy);
      vec3 r2 = vec3(r1.x, r1.y * cx - r1.z * sx, r1.y * sx + r1.z * cx);
      vec3 n1 = vec3(aNorm.x * cy - aNorm.z * sy, aNorm.y, aNorm.x * sy + aNorm.z * cy);
      vNorm = vec3(n1.x, n1.y * cx - n1.z * sx, n1.y * sx + n1.z * cx);
      gl_Position = vec4((r2.x * uZoom / uAspect) + uPan.x, (r2.y * uZoom) + uPan.y, r2.z * 0.25, 1.0);
    }
  `;
  const fs = `
    precision mediump float;
    varying vec3 vNorm;
    uniform vec4 uColor;
    void main() {
      vec3 l = normalize(vec3(0.4, 0.7, 0.5));
      float d = abs(dot(normalize(vNorm), l)) * 0.65 + 0.35;
      gl_FragColor = vec4(uColor.rgb * d, uColor.a);
    }
  `;
  const compile = (type: number, src: string) => {
    const s = gl.createShader(type)!;
    gl.shaderSource(s, src);
    gl.compileShader(s);
    return s;
  };
  prog = gl.createProgram()!;
  gl.attachShader(prog, compile(gl.VERTEX_SHADER, vs));
  gl.attachShader(prog, compile(gl.FRAGMENT_SHADER, fs));
  gl.linkProgram(prog);
  gl.enable(gl.DEPTH_TEST);
  gl.enable(gl.BLEND);
  gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
}

function makeBoxMesh(b: [number, number, number, number, number, number]): {
  positions: Float32Array;
  normals: Float32Array;
  indices: Uint16Array;
} {
  const [x0, y0, z0, x1, y1, z1] = b;
  const positions = new Float32Array([
    x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
    x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
    x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0,
    x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1,
    x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,
    x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0,
  ]);
  const normals = new Float32Array([
    0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,
    0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1,
    0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0,
    0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0,
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0,
    -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0,
  ]);
  const indices = new Uint16Array([
    0, 1, 2, 0, 2, 3,
    4, 5, 6, 4, 6, 7,
    8, 9, 10, 8, 10, 11,
    12, 13, 14, 12, 14, 15,
    16, 17, 18, 16, 18, 19,
    20, 21, 22, 20, 22, 23,
  ]);
  return { positions, normals, indices };
}

function renderFallbackBoxes(): void {
  if (!gl || !prog) return;
  gpuMeshes = [];
  for (const item of FALLBACK_BRIDGE_BOXES) {
    const box = makeBoxMesh(item.b);
    const pb = gl.createBuffer()!;
    gl.bindBuffer(gl.ARRAY_BUFFER, pb);
    gl.bufferData(gl.ARRAY_BUFFER, box.positions, gl.STATIC_DRAW);
    const nb = gl.createBuffer()!;
    gl.bindBuffer(gl.ARRAY_BUFFER, nb);
    gl.bufferData(gl.ARRAY_BUFFER, box.normals, gl.STATIC_DRAW);
    const ib = gl.createBuffer()!;
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, ib);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, box.indices, gl.STATIC_DRAW);
    gpuMeshes.push({
      pb,
      nb,
      ib,
      count: box.indices.length,
      indexType: gl.UNSIGNED_SHORT,
      expressId: item.expressId,
      globalId: item.id,
      name: item.n,
      entityType: item.t,
      status: item.s,
    });
  }
  bboxCenter = [0, 1.5, -2.5];
  bboxRadius = 21;
  drawScene();
}

async function extractModelGeometryToWebGL(model: LoadedModel): Promise<void> {
  if (!gl || !prog) return;
  const hud = document.getElementById('geomStatsHud')!;
  hud.textContent = `3D: Extrahiere Dreiecksnetze für ${model.name} mit @ifc-lite/geometry WASM ...`;
  try {
    const processor = new GeometryProcessor();
    await processor.init();
    const geom = await processor.process(model.buffer);
    processor.dispose();

    if (geom.meshes.length > 0) {
      gpuMeshes = [];
      let minX = Infinity,
        minY = Infinity,
        minZ = Infinity,
        maxX = -Infinity,
        maxY = -Infinity,
        maxZ = -Infinity;
      let totalTriangles = 0;

      geom.meshes.forEach((m: MeshData) => {
        if (!m.positions || m.positions.length === 0) return;
        const orig = (m as unknown as { origin?: [number, number, number] }).origin ?? [0, 0, 0];
        const ox = orig[0] ?? 0,
          oy = orig[1] ?? 0,
          oz = orig[2] ?? 0;
        const p = new Float32Array(m.positions.length);
        for (let i = 0; i < m.positions.length; i += 3) {
          p[i] = m.positions[i] + ox;
          p[i + 1] = m.positions[i + 1] + oy;
          p[i + 2] = m.positions[i + 2] + oz;
          if (p[i] < minX) minX = p[i];
          if (p[i] > maxX) maxX = p[i];
          if (p[i + 1] < minY) minY = p[i + 1];
          if (p[i + 1] > maxY) maxY = p[i + 1];
          if (p[i + 2] < minZ) minZ = p[i + 2];
          if (p[i + 2] > maxZ) maxZ = p[i + 2];
        }
        const pb = gl.createBuffer()!;
        gl.bindBuffer(gl.ARRAY_BUFFER, pb);
        gl.bufferData(gl.ARRAY_BUFFER, p, gl.STATIC_DRAW);
        const nb = gl.createBuffer()!;
        gl.bindBuffer(gl.ARRAY_BUFFER, nb);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(m.normals), gl.STATIC_DRAW);
        const ib = gl.createBuffer()!;
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, ib);

        const useUint32 = uint32Ext && m.positions.length / 3 > 65535;
        if (useUint32) {
          gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint32Array(m.indices), gl.STATIC_DRAW);
        } else {
          gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(m.indices), gl.STATIC_DRAW);
        }

        const el = model.byExpressId.get(m.expressId);
        const gid = el?.id ?? model.baseAccessor.getGlobalId(m.expressId) ?? `#${m.expressId}`;
        totalTriangles += Math.floor(m.indices.length / 3);
        gpuMeshes.push({
          pb,
          nb,
          ib,
          count: m.indices.length,
          indexType: useUint32 ? gl.UNSIGNED_INT : gl.UNSIGNED_SHORT,
          expressId: m.expressId,
          globalId: gid,
          name: el?.name ?? model.baseAccessor.getEntityName(m.expressId) ?? '',
          entityType: el?.entityName ?? model.baseAccessor.getEntityType(m.expressId) ?? 'IfcProduct',
          status: 'UNCHECKED',
        });
      });

      if (Number.isFinite(minX)) {
        bboxCenter = [(minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2];
        const dx = maxX - minX,
          dy = maxY - minY,
          dz = maxZ - minZ;
        bboxRadius = Math.max(0.5, Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.55);
      }
      hud.textContent = `3D: ${gpuMeshes.length} IFClite Meshes · ${totalTriangles.toLocaleString('de-DE')} Dreiecke`;
      sync3DStatusesFromReport();
    }
  } catch (err) {
    hud.textContent = `3D: Vorschau-Geometrie aktiv (${(err as Error).message})`;
  }
}

function sync3DStatusesFromReport(): void {
  const byId = new Map<string, 'PASSED' | 'FAILED'>();
  if (lastReport) {
    for (const res of lastReport.results()) {
      for (const f of res.findings) {
        if (!isFindingOk(f.status)) {
          byId.set(f.elementId, 'FAILED');
        } else if (byId.get(f.elementId) !== 'FAILED') {
          byId.set(f.elementId, 'PASSED');
        }
      }
    }
  }

  for (const m of gpuMeshes) {
    m.status = byId.get(m.globalId) ?? 'UNCHECKED';
  }

  let failedCount = 0;
  let passedCount = 0;
  let uncheckedCount = 0;
  const totalElements = currentModel ? currentModel.elements.length : gpuMeshes.length;
  if (currentModel) {
    for (const el of currentModel.elements) {
      const st = byId.get(el.id);
      if (st === 'FAILED') failedCount++;
      else if (st === 'PASSED') passedCount++;
      else uncheckedCount++;
    }
  } else {
    failedCount = gpuMeshes.filter((m) => m.status === 'FAILED').length;
    passedCount = gpuMeshes.filter((m) => m.status === 'PASSED').length;
    uncheckedCount = Math.max(0, totalElements - failedCount - passedCount);
  }

  document.getElementById('countFailed')!.textContent = String(failedCount);
  document.getElementById('countPassed')!.textContent = String(passedCount);
  document.getElementById('countUnchecked')!.textContent = String(uncheckedCount);

  drawScene();
}

function drawScene(): void {
  if (!gl || !prog) return;
  gl.viewport(0, 0, canvas.width, canvas.height);
  gl.clearColor(0.04, 0.05, 0.08, 1.0);
  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  if (gpuMeshes.length === 0) return;

  gl.useProgram(prog);
  gl.uniform1f(gl.getUniformLocation(prog, 'uRotX'), rotX);
  gl.uniform1f(gl.getUniformLocation(prog, 'uRotY'), rotY);
  gl.uniform1f(gl.getUniformLocation(prog, 'uZoom'), zoom);
  gl.uniform1f(gl.getUniformLocation(prog, 'uAspect'), canvas.width / canvas.height);
  gl.uniform2f(gl.getUniformLocation(prog, 'uPan'), panX, panY);
  gl.uniform3f(
    gl.getUniformLocation(prog, 'uCenter'),
    bboxCenter[0],
    bboxCenter[1],
    bboxCenter[2]
  );
  gl.uniform1f(gl.getUniformLocation(prog, 'uRadius'), bboxRadius);

  const aPos = gl.getAttribLocation(prog, 'aPos');
  const aNorm = gl.getAttribLocation(prog, 'aNorm');
  const uColor = gl.getUniformLocation(prog, 'uColor');
  gl.enableVertexAttribArray(aPos);
  gl.enableVertexAttribArray(aNorm);

  const hasSelection = selectedGlobalIds.size > 0;

  // Draw non-selected elements first, then selected elements on top
  const ordered = hasSelection
    ? [
        ...gpuMeshes.filter((m) => !selectedGlobalIds.has(m.globalId)),
        ...gpuMeshes.filter((m) => selectedGlobalIds.has(m.globalId)),
      ]
    : gpuMeshes;

  for (const b of ordered) {
    const isSel = selectedGlobalIds.has(b.globalId);
    if (filterFailedOnly && b.status !== 'FAILED' && !isSel) {
      continue;
    }
    let col: [number, number, number, number] = [0.58, 0.63, 0.72, 0.72];
    if (b.status === 'FAILED') col = [0.94, 0.25, 0.25, 0.96];
    else if (b.status === 'PASSED') col = [0.14, 0.77, 0.37, 0.88];

    if (isSel) {
      col = [0.98, 0.82, 0.08, 1.0];
    } else if (hasSelection) {
      col = [col[0], col[1], col[2], 0.16];
    }

    gl.uniform4f(uColor, col[0], col[1], col[2], col[3]);
    gl.bindBuffer(gl.ARRAY_BUFFER, b.pb);
    gl.vertexAttribPointer(aPos, 3, gl.FLOAT, false, 0, 0);
    gl.bindBuffer(gl.ARRAY_BUFFER, b.nb);
    gl.vertexAttribPointer(aNorm, 3, gl.FLOAT, false, 0, 0);
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, b.ib);
    gl.drawElements(gl.TRIANGLES, b.count, b.indexType, 0);
  }
}

// --- 2. Header Status & Element Inspector (`element_report`) ---

function updateHeaderStatus(): void {
  const pill = document.getElementById('headerStatusPill')!;
  const modelText = currentModel
    ? `${currentModel.name} (${currentModel.rawSchema}, ${currentModel.elements.length} Objekte)`
    : 'bruecke-tbw1-mit-merkmalen.ifc (wird geladen ...)';
  const mapName = checkService.currentMapping().name;
  pill.textContent = `Modell: ${modelText} · Mapping: ${mapName}`;
  const badge = document.getElementById('activeMappingBadge')!;
  badge.textContent = mapName;
  badge.className = `badge ${mapName === 'keine' ? 'NOT_APPLICABLE' : 'PASSED'}`;
}

function inspectElementByGlobalIdOrName(query: string): void {
  if (!currentModel) return;
  const needle = query.trim();
  if (!needle) return;
  const lower = needle.toLowerCase();

  const matches = currentModel.elements
    .filter((e) => e.id === needle || e.name.toLowerCase().includes(lower))
    .slice(0, 5);

  const inspector = document.getElementById('elementInspector')!;
  if (matches.length === 0) {
    inspector.textContent = `Kein Element mit der GlobalId oder dem Namensteil '${needle}' gefunden.`;
    return;
  }

  selectedGlobalIds = new Set(matches.map((m) => m.id));
  drawScene();

  const lines: string[] = [];
  for (const match of matches) {
    lines.push(
      `=== ${match.entityName} '${match.name}' (${match.id}) [ExpressId #${match.expressId}]${match.predefinedType ? ` · PredefinedType: ${match.predefinedType}` : ''} ===`
    );
    let anyFinding = false;
    if (lastReport) {
      for (const result of lastReport.results()) {
        const findings = result.findings.filter((f) => f.elementId === match.id);
        if (findings.length > 0) {
          anyFinding = true;
          lines.push(`  Spezifikation: ${result.specificationLabel}`);
          for (const f of findings) {
            lines.push(`    ${f.requirementLine()}`);
          }
        }
      }
    }
    if (!anyFinding) {
      lines.push(
        lastReport
          ? '  Prüfung: von keiner geprüften Spezifikation direkt als anwendbar erfasst'
          : '  Prüfung: noch keine Prüfung ausgeführt'
      );
    }

    // Group properties by PropertySet like 0.3.2 ElementUI.createObjectTab
    const bySet = new Map<string, Array<{ name: string; val: string; dt: string }>>();
    for (const p of match.properties) {
      const list = bySet.get(p.key.propertySet) ?? [];
      list.push({
        name: p.key.baseName,
        val: p.values.join(', '),
        dt: p.dataType,
      });
      bySet.set(p.key.propertySet, list);
    }

    lines.push(`  Merkmale im Modell (${match.properties.length} in ${bySet.size} PropertySets):`);
    for (const [psetName, props] of bySet.entries()) {
      lines.push(`    [${psetName}]`);
      for (const pr of props) {
        lines.push(`      • ${pr.name} = '${pr.val}' (${pr.dt})`);
      }
    }
    lines.push('');
  }
  inspector.textContent = lines.join('\n');
}

// --- 3. Tab 1: Check Report, Summary Table & Near-Miss Diagnosis (`check_model`) ---

async function runCheckSources(sources: CheckSource[]): Promise<void> {
  if (!currentModel) return;
  lastSources = [...sources];
  const reportContainer = document.getElementById('reportContainer')!;
  reportContainer.innerHTML = '<div class="sub">Prüfe Modell mit @checkmate/core &amp; @ifc-lite/ids ...</div>';

  lastReport = await checkService.check(sources, currentModel, new Date());
  renderCheckReportUI();
  sync3DStatusesFromReport();
  renderModelExplorerTab();
}

function renderCheckReportUI(): void {
  const report = lastReport;
  const headerMeta = document.getElementById('reportHeaderMeta')!;
  const verdictBadge = document.getElementById('verdictBadge')!;
  const summaryTableWrap = document.getElementById('summaryTableWrap')!;
  const reportContainer = document.getElementById('reportContainer')!;
  const reportTextOutput = document.getElementById('reportTextOutput')!;

  if (!report) {
    headerMeta.textContent = 'Noch keine Prüfung ausgeführt.';
    summaryTableWrap.innerHTML = '';
    reportContainer.innerHTML = '';
    reportTextOutput.textContent = '';
    return;
  }

  const exitCode = report.exitCode();
  verdictBadge.textContent = `${report.verdict()} (Exit ${exitCode})`;
  verdictBadge.className = `badge ${
    exitCode === 0 ? 'PASSED' : exitCode === 3 ? 'FAILED' : 'NOT_APPLICABLE'
  }`;
  headerMeta.textContent = `${report.header().join(' · ')} · Aktives Mapping: ${checkService.currentMapping().name}`;

  // 1. Summary Table (matching 0.3.2 ElementUI summaryTable)
  const allResults: Array<{ runName: string; res: SpecCheckResult }> = [];
  for (const run of report.runs) {
    for (const res of run.results) {
      allResults.push({ runName: run.name, res });
    }
  }

  if (allResults.length > 0) {
    const tbl = document.createElement('table');
    tbl.className = 'summary-table';
    tbl.innerHTML = `<thead>
      <tr>
        <th>Anforderung / Spezifikation</th>
        <th>Anwendbar</th>
        <th>Erfüllt</th>
        <th>Offen</th>
        <th>Status</th>
      </tr>
    </thead>`;
    const tbody = document.createElement('tbody');
    allResults.forEach(({ runName, res }, idx) => {
      const tr = document.createElement('tr');
      tr.title = 'Klicken, um die Bauteile dieser Spezifikation in 3D hervorzuheben';
      tr.innerHTML = `
        <td><b>${escapeHtml(res.specificationLabel)}</b><div style="font-size:10.5px;color:var(--fg2)">${escapeHtml(runName)}</div></td>
        <td>${res.applicableElements} / ${res.examinedElements}</td>
        <td style="color:#4ade80">${res.passingElements}</td>
        <td style="color:${res.failingElements > 0 ? '#f87171' : 'inherit'}">${res.failingElements}</td>
        <td><span class="badge ${res.verdict}">${escapeHtml(res.verdictLabel)}</span></td>
      `;
      tr.addEventListener('click', () => {
        const ids = new Set<string>();
        for (const f of res.findings) ids.add(f.elementId);
        for (const nm of res.diagnosis.nearMisses) ids.add(nm.elementId);
        selectedGlobalIds = ids;
        drawScene();
        document.getElementById(`spec-card-${idx}`)?.scrollIntoView({ behavior: 'smooth' });
      });
      tbody.appendChild(tr);
    });
    tbl.appendChild(tbody);
    summaryTableWrap.innerHTML = '';
    summaryTableWrap.appendChild(tbl);
  } else {
    summaryTableWrap.innerHTML = '';
  }

  // 2. Per-Specification Cards with Findings, Hints, and Zero-Applicability Near-Miss Diagnosis
  reportContainer.innerHTML = '';
  let specIdx = 0;

  for (const run of report.runs) {
    if (run.error) {
      const errDiv = document.createElement('div');
      errDiv.className = 'spec-item';
      errDiv.innerHTML = `<strong style="color:var(--bad)">Fehler bei ${escapeHtml(run.name)}:</strong> ${escapeHtml(run.error)}`;
      reportContainer.appendChild(errDiv);
      continue;
    }

    for (const res of run.results) {
      const cardId = `spec-card-${specIdx++}`;
      const specDiv = document.createElement('div');
      specDiv.className = 'spec-item';
      specDiv.id = cardId;

      const headerRow = document.createElement('div');
      headerRow.style.cssText =
        'display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:6px;flex-wrap:wrap';
      headerRow.innerHTML = `
        <div>
          <strong>${escapeHtml(res.specificationLabel)}</strong>
          <div style="font-size:11px;color:var(--fg2)">${escapeHtml(res.summary())}</div>
        </div>
        <span class="badge ${res.verdict}">${escapeHtml(res.verdictLabel)}</span>
      `;
      specDiv.appendChild(headerRow);

      // Hints (related dataTypes or similar property names)
      for (const hint of res.hints) {
        const hDiv = document.createElement('div');
        hDiv.className = 'hint-box';
        hDiv.textContent = `💡 ${hint}`;
        specDiv.appendChild(hDiv);
      }

      // Zero-applicability Diagnosis & clickable Near-Misses
      if (res.verdict === 'NOT_APPLICABLE') {
        const diagBox = document.createElement('div');
        diagBox.className = 'diag-box';
        const title = document.createElement('div');
        title.innerHTML = `<strong>🔍 Diagnose der Anwendbarkeit (0 von ${res.examinedElements} Elementen anwendbar — nicht prüfbar):</strong>`;
        diagBox.appendChild(title);

        for (const f of res.diagnosis.facets) {
          const fl = document.createElement('div');
          fl.style.marginTop = '3px';
          fl.textContent = f.line();
          diagBox.appendChild(fl);
        }

        if (res.diagnosis.nearMisses.length > 0) {
          const nmHeader = document.createElement('div');
          nmHeader.style.marginTop = '6px';
          nmHeader.innerHTML =
            '<strong>🎯 Fast-Treffer (Elemente mit den meisten passenden Facetten — Klick zeigt Bauteil in 3D):</strong>';
          diagBox.appendChild(nmHeader);

          for (const nm of res.diagnosis.nearMisses) {
            const btn = document.createElement('button');
            btn.className = 'finding-btn';
            btn.textContent = `🟡 ${nm.line().trim()}`;
            btn.addEventListener('click', () => {
              inspectElementByGlobalIdOrName(nm.elementId);
            });
            diagBox.appendChild(btn);
          }
        }
        specDiv.appendChild(diagBox);
      }

      // Unmet Findings
      const unmet = res.findings.filter((f) => !isFindingOk(f.status));
      const passed = res.findings.filter((f) => isFindingOk(f.status));

      for (const f of unmet.slice(0, 25)) {
        const btn = document.createElement('button');
        btn.className = 'finding-btn';
        btn.textContent = `🔴 ${f.line()}`;
        btn.addEventListener('click', () => {
          inspectElementByGlobalIdOrName(f.elementId);
        });
        specDiv.appendChild(btn);
      }
      if (unmet.length > 25) {
        const more = document.createElement('div');
        more.className = 'sub';
        more.style.marginTop = '4px';
        more.textContent = `... und ${unmet.length - 25} weitere Abweichungen (siehe Vollbericht unten).`;
        specDiv.appendChild(more);
      }

      // Show a collapsible sample of passing findings so user sees "(als Pset_Objektinformation/_IDEbene3)"
      if (passed.length > 0) {
        const details = document.createElement('details');
        details.style.marginTop = '6px';
        const summary = document.createElement('summary');
        summary.style.cssText = 'cursor:pointer;font-size:11.5px;color:#4ade80';
        summary.textContent = `✅ ${passed.length} erfüllte Merkmal-Prüfung(en) anzeigen (inkl. Mapping-Nachweis)`;
        details.appendChild(summary);

        for (const f of passed.slice(0, 15)) {
          const btn = document.createElement('button');
          btn.className = 'finding-btn';
          btn.textContent = `🟢 ${f.line()}`;
          btn.addEventListener('click', () => {
            inspectElementByGlobalIdOrName(f.elementId);
          });
          details.appendChild(btn);
        }
        specDiv.appendChild(details);
      }

      reportContainer.appendChild(specDiv);
    }
  }

  // 3. Text Report Output (Brief vs Full)
  updateReportTextBox();
}

function updateReportTextBox(): void {
  const reportTextOutput = document.getElementById('reportTextOutput')!;
  if (!lastReport) {
    reportTextOutput.textContent = '';
    return;
  }
  if (showFullReport) {
    reportTextOutput.textContent = ReportText.full(lastReport).join('\n');
  } else {
    const lines: string[] = [...lastReport.header(), ''];
    for (const run of lastReport.runs) {
      lines.push(...ReportText.brief(run, 5, lastReport.model.rawSchema));
    }
    lines.push(ReportText.verdictLine(lastReport));
    reportTextOutput.textContent = lines.join('\n');
  }
}

// --- 4. Tab 2: BIM Portal Catalogue, Filter, ModelFit & LoinDescription (`find_loins`, `describe_requirements`) ---

function populateCatalogueFilterDropdowns(): void {
  const discSelect = document.getElementById('disciplineFilter') as HTMLSelectElement;
  const phaseSelect = document.getElementById('phaseFilter') as HTMLSelectElement;
  const purposeSelect = document.getElementById('purposeFilter') as HTMLSelectElement;

  const curDisc = discSelect.value;
  const curPhase = phaseSelect.value;
  const curPurpose = purposeSelect.value;

  const disciplines = new Set<string>();
  const phases = new Set<string>();
  const purposes = new Set<string>();

  for (const l of catalogueLoins) {
    for (const d of l.disciplines) disciplines.add(d);
    for (const ph of l.phases) phases.add(ph);
    for (const pu of l.purposes) purposes.add(pu);
  }

  const fill = (sel: HTMLSelectElement, label: string, values: Set<string>, current: string) => {
    sel.innerHTML = `<option value="">${label}: alle (${values.size})</option>`;
    for (const v of [...values].sort((a, b) => a.localeCompare(b, 'de'))) {
      const opt = document.createElement('option');
      opt.value = v;
      opt.textContent = v;
      if (v === current) opt.selected = true;
      sel.appendChild(opt);
    }
  };

  fill(discSelect, 'Fachmodell', disciplines, curDisc);
  fill(phaseSelect, 'Leistungsphase', phases, curPhase);
  fill(purposeSelect, 'Anwendungsfall', purposes, curPurpose);
}

function renderCatalogueList(): void {
  const listEl = document.getElementById('loinCatalogueList')!;
  const summaryEl = document.getElementById('catalogueSummaryLine')!;
  const searchInput = (document.getElementById('portalSearchInput') as HTMLInputElement).value
    .trim()
    .toLowerCase();
  const filter: CatalogueFilter = {
    discipline: (document.getElementById('disciplineFilter') as HTMLSelectElement).value || null,
    phase: (document.getElementById('phaseFilter') as HTMLSelectElement).value || null,
    purpose: (document.getElementById('purposeFilter') as HTMLSelectElement).value || null,
  };
  const includeUnfitting = (
    document.getElementById('showAllLoinsCheckbox') as HTMLInputElement
  ).checked;

  const nameFiltered = catalogueLoins.filter(
    (e) =>
      !searchInput ||
      e.name.toLowerCase().includes(searchInput) ||
      e.guid.toLowerCase().includes(searchInput) ||
      e.objectTypes.some((o) => o.toLowerCase().includes(searchInput))
  );
  const filtered = applyCatalogueFilter(nameFiltered, filter) as Array<
    CatalogueEntry & { fixtureUrl?: string }
  >;

  const modelFit = ModelFit.of(currentModel ? currentModel.elements : []);
  const ranked = modelFit.rank(filtered);
  const fittingCount = ranked.filter((r) => r.fit.kind !== 'NONE').length;

  summaryEl.textContent = `${catalogueLoins.length} LOIN(s) im Katalog, ${filtered.length} nach Filter, ${fittingCount} passend zum Modell; ${catalogueProjects.length} AIA-Projekt(e)`;

  listEl.innerHTML = '';

  // Render AIA Projects first
  for (const proj of catalogueProjects) {
    if (
      searchInput &&
      !proj.name.toLowerCase().includes(searchInput) &&
      !proj.guid.toLowerCase().includes(searchInput)
    ) {
      continue;
    }
    const row = document.createElement('div');
    row.className = `loin-row ${activeDescribedProject?.guid === proj.guid ? 'selected' : ''}`;
    const checked = checkedLoinGuids.has(proj.guid);
    row.innerHTML = `
      <input type="checkbox" ${checked ? 'checked' : ''} style="margin-top:3px">
      <div class="loin-meta">
        <div class="loin-title">
          <span>📦 ${escapeHtml(proj.name)}</span>
          <span class="badge PASSED">AIA-Projekt</span>
        </div>
        <div class="loin-tags">ID: ${escapeHtml(proj.guid)} · ${escapeHtml(proj.organisation ?? '')}</div>
      </div>
    `;
    const cb = row.querySelector('input')!;
    cb.addEventListener('click', (ev) => {
      ev.stopPropagation();
      if (cb.checked) checkedLoinGuids.add(proj.guid);
      else checkedLoinGuids.delete(proj.guid);
    });
    row.addEventListener('click', () => {
      activeDescribedLoin = null;
      activeDescribedProject = proj;
      renderCatalogueList();
      void describeSelectedProject(proj);
    });
    listEl.appendChild(row);
  }

  // Render ModelFit-ranked LOINs
  const visibleLoins = ranked.filter(
    (r) => includeUnfitting || fittingCount === 0 || r.fit.kind !== 'NONE'
  );

  for (const { entry, fit } of visibleLoins) {
    const row = document.createElement('div');
    row.className = `loin-row ${activeDescribedLoin?.guid === entry.guid ? 'selected' : ''}`;
    const checked = checkedLoinGuids.has(entry.guid);
    const fitText =
      fit.kind !== 'NONE' ? `✅ ${describeFit(fit)}` : '⚪ kein Element mit diesem Objekttyp im Modell';

    row.innerHTML = `
      <input type="checkbox" ${checked ? 'checked' : ''} style="margin-top:3px">
      <div class="loin-meta">
        <div class="loin-title">
          <span>📋 ${escapeHtml(entry.name)}</span>
          <span style="font-size:10.5px;color:var(--fg2)">v${escapeHtml(entry.version)}</span>
        </div>
        <div class="loin-fit ${fit.kind === 'NONE' ? 'none' : ''}">${escapeHtml(fitText)}</div>
        <div class="loin-tags">Objekttyp: ${escapeHtml(entry.objectTypes.join(', ') || '–')} · Fachmodell: ${escapeHtml(entry.disciplines.join(', ') || '–')} · ${escapeHtml(entry.phases.map((p) => p.split(':')[0]).join(', ') || '–')}</div>
      </div>
    `;
    const cb = row.querySelector('input')!;
    cb.addEventListener('click', (ev) => {
      ev.stopPropagation();
      if (cb.checked) checkedLoinGuids.add(entry.guid);
      else checkedLoinGuids.delete(entry.guid);
    });
    row.addEventListener('click', () => {
      activeDescribedProject = null;
      activeDescribedLoin = entry;
      renderCatalogueList();
      void describeSelectedLoin(entry);
    });
    listEl.appendChild(row);
  }
}

async function loadCheckSourceForCatalogueItem(item: {
  guid: string;
  name: string;
  fixtureUrl?: string;
}): Promise<CheckSource> {
  if (item.fixtureUrl) {
    const res = await fetch(item.fixtureUrl);
    if (res.ok) {
      const bytes = new Uint8Array(await res.arrayBuffer());
      return { kind: 'idsBytes', name: item.name, bytes };
    }
  }
  return { kind: 'portal', idOrLink: item.guid };
}

async function describeSelectedLoin(
  entry: CatalogueEntry & { fixtureUrl?: string }
): Promise<void> {
  if (!currentModel) return;
  const out = document.getElementById('loinDescriptionOutput')!;
  const checkBtn = document.getElementById('checkDescribedLoinBtn')!;
  checkBtn.style.display = 'inline-flex';

  const fit = ModelFit.of(currentModel.elements).fit(entry);
  const initialLines = await LoinDescription.describeCatalogueEntry(
    entry,
    fit,
    null,
    currentModel,
    checkService
  );
  out.textContent = initialLines.join('\n');

  try {
    const src = await loadCheckSourceForCatalogueItem(entry);
    const { document: doc } = await checkService.loadIdsDocument(src);
    const fullLines = await LoinDescription.describeCatalogueEntry(
      entry,
      fit,
      doc,
      currentModel,
      checkService
    );
    out.textContent = fullLines.join('\n');
  } catch (err) {
    out.textContent = `${initialLines.slice(0, -1).join('\n')}\nHinweis beim Laden der IDS-Datei: ${(err as Error).message}`;
  }
}

async function describeSelectedProject(
  proj: SimpleAiaProjectPublicDto & { fixtureUrl?: string }
): Promise<void> {
  if (!currentModel) return;
  const out = document.getElementById('loinDescriptionOutput')!;
  const checkBtn = document.getElementById('checkDescribedLoinBtn')!;
  checkBtn.style.display = 'inline-flex';
  out.textContent = `Lade AIA-Projekt '${proj.name}' (${proj.guid}) ...`;

  try {
    const src = await loadCheckSourceForCatalogueItem(proj);
    const { name, document: doc } = await checkService.loadIdsDocument(src);
    const lines = await LoinDescription.describeDocument(name, doc, currentModel, checkService);
    out.textContent = lines.join('\n');
  } catch (err) {
    out.textContent = `Fehler beim Beschreiben von ${proj.name}: ${(err as Error).message}`;
  }
}

async function fetchLiveBimPortalCatalogue(): Promise<void> {
  const btn = document.getElementById('portalSearchBtn') as HTMLButtonElement;
  const term = (document.getElementById('portalSearchInput') as HTMLInputElement).value.trim();
  const origText = btn.textContent;
  btn.textContent = '⏳ Lade vom BIM-Portal ...';
  btn.disabled = true;

  try {
    const found = await checkService.search(term);
    const byGuid = new Map<string, CatalogueEntry & { fixtureUrl?: string }>();
    for (const seed of SEED_CATALOGUE_LOINS) byGuid.set(seed.guid, seed);
    for (const live of found.loins) byGuid.set(live.guid, live);
    catalogueLoins = [...byGuid.values()];

    const projByGuid = new Map<string, SimpleAiaProjectPublicDto & { fixtureUrl?: string }>();
    for (const seed of SEED_AIA_PROJECTS) projByGuid.set(seed.guid, seed);
    for (const live of found.projects) projByGuid.set(live.guid, live);
    catalogueProjects = [...projByGuid.values()];

    populateCatalogueFilterDropdowns();
    renderCatalogueList();
  } catch (err) {
    document.getElementById('catalogueSummaryLine')!.textContent =
      `Live-Abruf Hinweis (${(err as Error).message}) — zeige mitgelieferte BIM-Portal LOINs & AIA-Projekte.`;
  } finally {
    btn.textContent = origText;
    btn.disabled = false;
  }
}

// --- 5. Tab 3: Model Overview, Entity Histogram & Element Browser (`describe_model`, `list_elements`) ---

function renderModelExplorerTab(): void {
  if (!currentModel) return;
  const summaryEl = document.getElementById('modelSummaryLine')!;
  const chipsEl = document.getElementById('entityHistogramChips')!;
  const listEl = document.getElementById('modelElementList')!;
  const countLabel = document.getElementById('elementCountLabel')!;

  summaryEl.textContent = `Modell: ${currentModel.summary()}`;

  const entityCounts = new Map<string, number>();
  for (const el of currentModel.elements) {
    entityCounts.set(el.entityName, (entityCounts.get(el.entityName) ?? 0) + 1);
  }
  const sortedEntities = [...entityCounts.entries()].sort((a, b) => b[1] - a[1]);

  chipsEl.innerHTML = '';
  const allChip = document.createElement('button');
  allChip.className = `chip ${activeEntityFilter === '' ? 'active' : ''}`;
  allChip.textContent = `Alle (${currentModel.elements.length})`;
  allChip.addEventListener('click', () => {
    activeEntityFilter = '';
    renderModelExplorerTab();
  });
  chipsEl.appendChild(allChip);

  for (const [ent, cnt] of sortedEntities) {
    const chip = document.createElement('button');
    chip.className = `chip ${activeEntityFilter === ent ? 'active' : ''}`;
    chip.textContent = `${ent}: ${cnt}`;
    chip.addEventListener('click', () => {
      activeEntityFilter = activeEntityFilter === ent ? '' : ent;
      renderModelExplorerTab();
    });
    chipsEl.appendChild(chip);
  }

  // Filter elements
  const namePart = activeElementNameFilter.trim().toLowerCase();
  const filteredElements = currentModel.elements.filter(
    (e) =>
      (!activeEntityFilter || e.entityName === activeEntityFilter) &&
      (!namePart ||
        e.name.toLowerCase().includes(namePart) ||
        e.id.toLowerCase().includes(namePart) ||
        (e.predefinedType && e.predefinedType.toLowerCase().includes(namePart)))
  );

  countLabel.textContent = `${filteredElements.length} von ${currentModel.elements.length} Element(en)`;

  // Compute status map
  const statusMap = new Map<string, 'PASSED' | 'FAILED'>();
  if (lastReport) {
    for (const res of lastReport.results()) {
      for (const f of res.findings) {
        if (!isFindingOk(f.status)) statusMap.set(f.elementId, 'FAILED');
        else if (statusMap.get(f.elementId) !== 'FAILED') statusMap.set(f.elementId, 'PASSED');
      }
    }
  }

  listEl.innerHTML = '';
  for (const el of filteredElements.slice(0, 120)) {
    const st = statusMap.get(el.id) ?? 'UNCHECKED';
    const row = document.createElement('div');
    row.className = `loin-row ${selectedGlobalIds.has(el.id) ? 'selected' : ''}`;
    row.innerHTML = `
      <div class="loin-meta">
        <div class="loin-title">
          <span>${escapeHtml(el.name || el.entityName)}</span>
          <span class="badge ${st}">${st === 'FAILED' ? 'nicht erfüllt' : st === 'PASSED' ? 'erfüllt' : el.entityName}</span>
        </div>
        <div class="loin-tags">${escapeHtml(el.id)} · ${escapeHtml(el.entityName)}${el.predefinedType ? ` · PredefinedType: ${escapeHtml(el.predefinedType)}` : ''} · ${el.properties.length} Merkmale</div>
      </div>
    `;
    row.addEventListener('click', () => {
      inspectElementByGlobalIdOrName(el.id);
      renderModelExplorerTab();
    });
    listEl.appendChild(row);
  }
}

// --- 6. Tab 4: Property Mapping, Rules & Exports (`load_mapping`, `export_bcf`) ---

function initMappingEditor(): void {
  const editor = document.getElementById('mappingJsonEditor') as HTMLTextAreaElement;
  editor.value = JSON.stringify(BUNDLED_BIM_HAMBURG_MAPPING_JSON, null, 2);
}

function downloadBlob(bytes: Uint8Array | string, fileName: string, mimeType: string): void {
  const blob =
    typeof bytes === 'string'
      ? new Blob([bytes], { type: mimeType })
      : new Blob([bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer], {
          type: mimeType,
        });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function switchTab(tabId: string): void {
  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
  });
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    panel.classList.toggle('active', panel.id === tabId);
  });
}

// --- 7. Event Wiring & Startup ---

initWebGL();
renderFallbackBoxes();
initMappingEditor();
populateCatalogueFilterDropdowns();
renderCatalogueList();

// Tab switching
document.querySelectorAll('.tab-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    const target = btn.getAttribute('data-tab');
    if (target) switchTab(target);
  });
});

// 3D Camera Orbit & Zoom
let dragging = false,
  lx = 0,
  ly = 0;
canvas.addEventListener('mousedown', (e) => {
  dragging = true;
  lx = e.clientX;
  ly = e.clientY;
});
window.addEventListener('mouseup', () => {
  dragging = false;
});
window.addEventListener('mousemove', (e) => {
  if (!dragging) return;
  rotY += (e.clientX - lx) * 0.01;
  rotX = Math.max(-1.5, Math.min(1.5, rotX + (e.clientY - ly) * 0.01));
  lx = e.clientX;
  ly = e.clientY;
  drawScene();
});
canvas.addEventListener(
  'wheel',
  (e) => {
    e.preventDefault();
    zoom = Math.max(0.2, Math.min(6.0, zoom * (e.deltaY < 0 ? 1.12 : 0.89)));
    drawScene();
  },
  { passive: false }
);

document.getElementById('filterFailedBtn')?.addEventListener('click', (e) => {
  filterFailedOnly = !filterFailedOnly;
  (e.currentTarget as HTMLButtonElement).classList.toggle('primary', filterFailedOnly);
  drawScene();
});
document.getElementById('clearSelectionBtn')?.addEventListener('click', () => {
  selectedGlobalIds.clear();
  filterFailedOnly = false;
  document.getElementById('filterFailedBtn')?.classList.remove('primary');
  drawScene();
});
document.getElementById('resetCamBtn')?.addEventListener('click', () => {
  rotX = -0.45;
  rotY = 0.62;
  zoom = 1.0;
  panX = 0;
  panY = 0;
  selectedGlobalIds.clear();
  drawScene();
});

// Quick Element Inspector search
document.getElementById('quickElementBtn')?.addEventListener('click', () => {
  const val = (document.getElementById('quickElementInput') as HTMLInputElement).value;
  inspectElementByGlobalIdOrName(val);
});
document.getElementById('quickElementInput')?.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    inspectElementByGlobalIdOrName((e.currentTarget as HTMLInputElement).value);
  }
});

async function loadBundledModelAndFixture(modelFile: string, fixtureVal: string): Promise<void> {
  switchTab('tab-report');
  if (!currentModel || currentModel.name !== modelFile) {
    const res = await fetch(`./${modelFile}`);
    if (res.ok) {
      const buf = new Uint8Array(await res.arrayBuffer());
      currentModel = await LoadedModel.fromBytes(buf, modelFile);
      updateHeaderStatus();
      renderCatalogueList();
      renderModelExplorerTab();
      await extractModelGeometryToWebGL(currentModel);
    }
  }
  (document.getElementById('bundledFixtureSelect') as HTMLSelectElement).value = fixtureVal;
  await runSelectedFixture();
}

// Header Preset Buttons
document.getElementById('presetMiniHochbauBtn')?.addEventListener('click', () => {
  void loadBundledModelAndFixture(
    'mini-hochbau-bundesbau.ifc',
    'fixtures/mini-hochbau-bundesbau.zip'
  );
});
document.getElementById('presetMiniInfraBtn')?.addEventListener('click', () => {
  void loadBundledModelAndFixture(
    'mini-infrastruktur-bruecke.ifc',
    'fixtures/mini-infrastruktur-bruecke.zip'
  );
});
document.getElementById('presetKappeFailBtn')?.addEventListener('click', () => {
  void loadBundledModelAndFixture('bruecke-tbw1-mit-merkmalen.ifc', 'demo-kappe-fail');
});
document.getElementById('presetPortalKappeBtn')?.addEventListener('click', () => {
  void loadBundledModelAndFixture(
    'bruecke-tbw1-mit-merkmalen.ifc',
    'fixtures/ueberbau-kappe-lph8-9.ids'
  );
});

async function runSelectedFixture(): Promise<void> {
  const val = (document.getElementById('bundledFixtureSelect') as HTMLSelectElement).value;
  if (val === 'demo-kappe-fail') {
    await runCheckSources([
      { kind: 'idsXml', name: 'BIM.Hamburg Brücke — Kappen Feldnummer', xml: DEMO_BRIDGE_IDS },
    ]);
    return;
  }
  const res = await fetch(`./${val}`);
  if (!res.ok) {
    alert(`Vorlage konnte nicht geladen werden: ${val}`);
    return;
  }
  const bytes = new Uint8Array(await res.arrayBuffer());
  await runCheckSources([{ kind: 'idsBytes', name: val.replace(/^fixtures\//, ''), bytes }]);
}

document.getElementById('runFixtureBtn')?.addEventListener('click', () => {
  void runSelectedFixture();
});

document.getElementById('toggleFullReportBtn')?.addEventListener('click', (e) => {
  showFullReport = !showFullReport;
  (e.currentTarget as HTMLButtonElement).textContent = showFullReport
    ? 'Kurzbericht (examples=5)'
    : 'Vollbericht (examples=0)';
  updateReportTextBox();
});
document.getElementById('copyReportTextBtn')?.addEventListener('click', () => {
  const txt = document.getElementById('reportTextOutput')?.textContent ?? '';
  void navigator.clipboard?.writeText(txt);
});

// File Inputs (IFC, IDS/ZIP, .rules.json)
document.getElementById('ifcFile')?.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const bytes = new Uint8Array(await file.arrayBuffer());
  currentModel = await LoadedModel.fromBytes(bytes, file.name);
  updateHeaderStatus();
  renderCatalogueList();
  renderModelExplorerTab();
  await extractModelGeometryToWebGL(currentModel);
  await runCheckSources(lastSources);
});

document.getElementById('idsFile')?.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const bytes = new Uint8Array(await file.arrayBuffer());
  switchTab('tab-report');
  await runCheckSources([{ kind: 'idsBytes', name: file.name, bytes }]);
});

document.getElementById('rulesFile')?.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const txt = await file.text();
  const ruleSet = JSON.parse(txt) as RuleSetFile;
  switchTab('tab-report');
  await runCheckSources([{ kind: 'rulesJson', name: ruleSet.name || file.name, ruleSet }]);
});

// Tab 2: BIM Portal Events
document.getElementById('portalSearchBtn')?.addEventListener('click', () => {
  void fetchLiveBimPortalCatalogue();
});
document.getElementById('portalSearchInput')?.addEventListener('input', () => {
  renderCatalogueList();
});
document.getElementById('portalSearchInput')?.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    const val = (e.currentTarget as HTMLInputElement).value.trim();
    if (extractPortalId(val)) {
      switchTab('tab-report');
      void runCheckSources([{ kind: 'portal', idOrLink: val }]);
    } else {
      void fetchLiveBimPortalCatalogue();
    }
  }
});
document.getElementById('checkPortalGuidBtn')?.addEventListener('click', () => {
  const val = (document.getElementById('portalSearchInput') as HTMLInputElement).value.trim();
  if (!val) return;
  switchTab('tab-report');
  void runCheckSources([{ kind: 'portal', idOrLink: val }]);
});

for (const id of ['disciplineFilter', 'phaseFilter', 'purposeFilter', 'showAllLoinsCheckbox']) {
  document.getElementById(id)?.addEventListener('change', () => {
    renderCatalogueList();
  });
}

document.getElementById('validateSelectedLoinsBtn')?.addEventListener('click', async () => {
  if (checkedLoinGuids.size === 0) {
    alert('Bitte mindestens eine LOIN oder ein AIA-Projekt in der Liste anhaken.');
    return;
  }
  const sources: CheckSource[] = [];
  for (const guid of checkedLoinGuids) {
    const loin = catalogueLoins.find((l) => l.guid === guid);
    const proj = catalogueProjects.find((p) => p.guid === guid);
    const item = loin ?? proj;
    if (item) {
      sources.push(await loadCheckSourceForCatalogueItem(item));
    } else {
      sources.push({ kind: 'portal', idOrLink: guid });
    }
  }
  switchTab('tab-report');
  await runCheckSources(sources);
});

document.getElementById('checkDescribedLoinBtn')?.addEventListener('click', async () => {
  const item = activeDescribedLoin ?? activeDescribedProject;
  if (!item) return;
  const src = await loadCheckSourceForCatalogueItem(item);
  switchTab('tab-report');
  await runCheckSources([src]);
});

// Tab 3: Model URL & Element Filter Events
document.getElementById('loadModelUrlBtn')?.addEventListener('click', async () => {
  const url = (document.getElementById('modelUrlInput') as HTMLInputElement).value.trim();
  if (!url) return;
  try {
    currentModel = await LoadedModel.read(url);
    updateHeaderStatus();
    renderCatalogueList();
    renderModelExplorerTab();
    await extractModelGeometryToWebGL(currentModel);
    await runCheckSources(lastSources);
  } catch (err) {
    alert((err as Error).message);
  }
});

document.getElementById('elementNameFilterInput')?.addEventListener('input', (e) => {
  activeElementNameFilter = (e.currentTarget as HTMLInputElement).value;
  renderModelExplorerTab();
});

document.getElementById('isolateFilteredIn3DBtn')?.addEventListener('click', () => {
  if (!currentModel) return;
  const namePart = activeElementNameFilter.trim().toLowerCase();
  const matching = currentModel.elements.filter(
    (e) =>
      (!activeEntityFilter || e.entityName === activeEntityFilter) &&
      (!namePart ||
        e.name.toLowerCase().includes(namePart) ||
        e.id.toLowerCase().includes(namePart))
  );
  selectedGlobalIds = new Set(matching.map((m) => m.id));
  drawScene();
});

// Tab 4: Property Mapping & Exports
document.getElementById('useDefaultMappingBtn')?.addEventListener('click', async () => {
  const mapping = PropertyMapping.bundled();
  checkService.useMapping(mapping);
  (document.getElementById('mappingJsonEditor') as HTMLTextAreaElement).value = JSON.stringify(
    mapping.toJson(),
    null,
    2
  );
  document.getElementById('mappingStatusLine')!.textContent = mapping.describe().join(' · ');
  updateHeaderStatus();
  await runCheckSources(lastSources);
});

document.getElementById('useNoneMappingBtn')?.addEventListener('click', async () => {
  const mapping = PropertyMapping.NONE;
  checkService.useMapping(mapping);
  (document.getElementById('mappingJsonEditor') as HTMLTextAreaElement).value = JSON.stringify(
    { name: 'keine', aliases: [] },
    null,
    2
  );
  document.getElementById('mappingStatusLine')!.textContent =
    "Mapping 'keine': 0 Aliase (strikte IDS-Prüfung)";
  updateHeaderStatus();
  await runCheckSources(lastSources);
});

document.getElementById('mappingFileInput')?.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const txt = await file.text();
  (document.getElementById('mappingJsonEditor') as HTMLTextAreaElement).value = txt;
});

document.getElementById('applyMappingJsonBtn')?.addEventListener('click', async () => {
  const txt = (document.getElementById('mappingJsonEditor') as HTMLTextAreaElement).value;
  try {
    const mapping = PropertyMapping.parse(txt);
    checkService.useMapping(mapping);
    document.getElementById('mappingStatusLine')!.textContent = mapping.describe().join(' · ');
    updateHeaderStatus();
    switchTab('tab-report');
    await runCheckSources(lastSources);
  } catch (err) {
    alert(`Mapping-Fehler: ${(err as Error).message}`);
  }
});

document.getElementById('exportBcfBtn')?.addEventListener('click', async () => {
  if (!lastReport) return;
  try {
    const bytes = await checkService.exportBcf(lastReport);
    downloadBlob(bytes, `checkmate-${lastReport.model.name}.bcfzip`, 'application/zip');
  } catch (err) {
    alert(`BCF-Export: ${(err as Error).message}`);
  }
});

document.getElementById('exportJsonBtn')?.addEventListener('click', () => {
  if (!lastReport) return;
  const json = ReportJson.stringify(lastReport);
  const preview = document.getElementById('exportPreviewBox')!;
  preview.style.display = 'block';
  preview.textContent = json;
  downloadBlob(json, `checkmate-report-${lastReport.model.name}.json`, 'application/json');
});

document.getElementById('exportTextBtn')?.addEventListener('click', () => {
  if (!lastReport) return;
  const txt = ReportText.full(lastReport).join('\n');
  downloadBlob(txt, `checkmate-report-${lastReport.model.name}.txt`, 'text/plain;charset=utf-8');
});

document.getElementById('convertIdsToRulesBtn')?.addEventListener('click', async () => {
  const firstSrc = lastSources[0];
  if (!firstSrc || firstSrc.kind === 'rulesJson' || firstSrc.kind === 'rulesFile') return;
  try {
    const { name, document: doc } = await checkService.loadIdsDocument(firstSrc);
    const rules = checkService.convertIdsToRuleSet(doc);
    const json = JSON.stringify(rules, null, 2);
    const preview = document.getElementById('exportPreviewBox')!;
    preview.style.display = 'block';
    preview.textContent = json;
    downloadBlob(json, `${name.replace(/[^a-zA-Z0-9_-]+/g, '_')}.rules.json`, 'application/json');
  } catch (err) {
    alert(`Konvertierungsfehler: ${(err as Error).message}`);
  }
});

// Initial Startup: load bundled bridge model, run checkService, extract 3D meshes, and describe first LOIN
async function bootstrapWorkbench(): Promise<void> {
  try {
    const res = await fetch('./bruecke-tbw1-mit-merkmalen.ifc');
    if (!res.ok) return;
    const buf = new Uint8Array(await res.arrayBuffer());
    currentModel = await LoadedModel.fromBytes(buf, 'bruecke-tbw1-mit-merkmalen.ifc');
    updateHeaderStatus();
    renderCatalogueList();
    renderModelExplorerTab();
    await runCheckSources(lastSources);
    void describeSelectedLoin(SEED_CATALOGUE_LOINS[0]);
    await extractModelGeometryToWebGL(currentModel);
  } catch (err) {
    console.warn('Bootstrap fallback active:', err);
  }
}

void bootstrapWorkbench();
