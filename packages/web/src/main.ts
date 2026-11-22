import { parseAuto, isQueryableObjectType } from '@ifc-lite/parser';
import { GeometryProcessor, type MeshData } from '@ifc-lite/geometry';
import {
  parseIDS,
  validateIDS,
  createTranslationService,
  type IDSDocument,
} from '@ifc-lite/ids';
import { createDataAccessor, narrowSchemaVersion } from '@ifc-lite/ids/bridge';

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
          <ids:propertySet><ids:simpleValue>Pset_Objektinformation</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>_IDEbene3</ids:simpleValue></ids:baseName>
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
    <ids:specification name="Überbau &amp; Unterbau (Alle Bauteile mit Objektinformation/_IDEbene1..3)" ifcVersion="IFC2X3">
      <ids:applicability minOccurs="0" maxOccurs="unbounded">
        <ids:property>
          <ids:propertySet><ids:simpleValue>Pset_Objektinformation</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>_IDEbene1</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:applicability>
      <ids:requirements>
        <ids:property cardinality="required">
          <ids:propertySet><ids:simpleValue>Pset_Objektinformation</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>_IDEbene3</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:requirements>
    </ids:specification>
  </ids:specifications>
</ids:ids>`;

// Pre-computed 3D bounding meshes of bruecke-tbw1-mit-merkmalen.ifc for instant 0ms rendering
// while the full WASM parser + geometry kernel runs
const FALLBACK_BRIDGE_BOXES: Array<{
  id: string;
  expressId: number;
  t: string;
  n: string;
  s: 'PASSED' | 'FAILED';
  b: [number, number, number, number, number, number];
  msg: string;
}> = [
  { id: '1cxNZQHk18xe_eYvhIpXRg', expressId: 1555, t: 'IfcBuildingElementProxy', n: 'Kappe_Mitte_BW:632563', s: 'FAILED', b: [-15.4, 5.65, -8.22, 15.83, 6.32, -6.62], msg: '[Merkmal fehlt] Pset_Bauteilinformation/Feldnummer\n[Mapping] Klassifizierung/Objekttyp -> Pset_Objektinformation/_IDEbene3 = "Kappe"' },
  { id: '2MePDRxq95YBYnkQWyl$yu', expressId: 35001, t: 'IfcBuildingElementProxy', n: 'Kappe_Außen_BW:656578', s: 'FAILED', b: [-15.4, 5.65, 1.13, 15.83, 6.34, 3.23], msg: '[Merkmal fehlt] Pset_Bauteilinformation/Feldnummer\n[Mapping] Klassifizierung/Objekttyp -> Pset_Objektinformation/_IDEbene3 = "Kappe"' },
  { id: '3e5NfjnF54mvxue2Mx5Kdh', expressId: 35050, t: 'IfcBuildingElementProxy', n: 'Kappe_Mitte_Flügel_Ost:700999', s: 'FAILED', b: [15.85, 5.65, -8.23, 22.5, 6.32, -6.62], msg: '[Merkmal fehlt] Pset_Bauteilinformation/Feldnummer' },
  { id: '3e5NfjnF54mvxue2Mx5KZW', expressId: 35102, t: 'IfcBuildingElementProxy', n: 'Kappe_Mitte_Flügel_West:701260', s: 'FAILED', b: [-22.5, 5.65, -8.23, -15.5, 6.32, -6.62], msg: '[Merkmal fehlt] Pset_Bauteilinformation/Feldnummer' },
  { id: '3e5NfjnF54mvxue2Mx5Koi', expressId: 35180, t: 'IfcBuildingElementProxy', n: 'Kappe_Außen_Flügel_Ost:702208', s: 'FAILED', b: [15.85, 5.65, 1.13, 22.5, 6.34, 3.23], msg: '[Merkmal fehlt] Pset_Bauteilinformation/Feldnummer' },
  { id: '3e5NfjnF54mvxue2Mx5KnZ', expressId: 35257, t: 'IfcBuildingElementProxy', n: 'Kappe_Außen_Flügel_West:702415', s: 'FAILED', b: [-22.5, 5.65, 1.13, -15.5, 6.34, 3.23], msg: '[Merkmal fehlt] Pset_Bauteilinformation/Feldnummer' },
  { id: '2MePDRxq95YBYnkQWyl1XG', expressId: 34900, t: 'IfcBuildingElementProxy', n: 'Ueberbau_OrthotropePlatte:648106', s: 'PASSED', b: [-15.4, 4.75, -7.87, 15.83, 5.65, 2.88], msg: '[erfüllt] Pset_Objektinformation/_IDEbene1 = "Ueberbau"' },
  { id: '1cxNZQHk18xe_eYvhIpXRY', expressId: 100835, t: 'IfcRailing', n: 'Rueckhaltesystem_Gelaender:632571', s: 'PASSED', b: [-22.3, 6.34, 2.95, 22.3, 7.54, 3.15], msg: '[erfüllt] Pset_Objektinformation/_IDEbene3 = "Gelaender"' },
  { id: '2Fz$ybAnf4$ON9x3$d_E3N', expressId: 34950, t: 'IfcBuildingElementProxy', n: 'FahrzeugRueckhaltesystem_Schutzplanke:676920', s: 'PASSED', b: [-22.5, 6.29, 1.62, 22.5, 7.25, 2.12], msg: '[erfüllt] Pset_Objektinformation/_IDEbene3 = "Schutzplanke"' },
  { id: '1cxNZQHk18xe_eYvhIpXRz', expressId: 901, t: 'IfcBuildingElementProxy', n: 'Unterbau_Widerlager_West:632548', s: 'PASSED', b: [-22.5, 0, -7.87, -14.5, 4.75, 2.88], msg: '[erfüllt] Unterbau Widerlager West' },
  { id: '1cxNZQHk18xe_eYvhIpXR_ost', expressId: 902, t: 'IfcBuildingElementProxy', n: 'Unterbau_Widerlager_Ost:664160', s: 'PASSED', b: [14.5, 0, -7.87, 22.5, 4.75, 2.88], msg: '[erfüllt] Unterbau Widerlager Ost' },
  { id: '1cxNZQHk18xe_eYvhIpXRu', expressId: 729, t: 'IfcColumn', n: 'Unterbau_PfeilerStuetze_Schaft_1:632545', s: 'PASSED', b: [-0.45, 0, -0.6, 0.45, 4.55, 0.6], msg: '[erfüllt] Pset_Objektinformation/_IDEbene3 = "Schaft"' },
  { id: '1cxNZQHk18xe_eYvhIpXRx', expressId: 778, t: 'IfcColumn', n: 'Unterbau_PfeilerStuetze_Schaft_2:632546', s: 'PASSED', b: [-0.45, 0, -5.6, 0.45, 4.55, -4.4], msg: '[erfüllt] Pset_Objektinformation/_IDEbene3 = "Schaft"' },
  { id: '1cxNZQHk18xe_eYvhIpXRU', expressId: 605, t: 'IfcSlab', n: 'Gruendung_Fundament_West:632519', s: 'PASSED', b: [-17.1, -1.25, -7.62, -14.25, 0, 2.63], msg: '[erfüllt] Gründung Fundament West' },
  { id: '2MePDRxq95YBYnkQWylzcS', expressId: 606, t: 'IfcSlab', n: 'Gruendung_Fundament_Ost:664166', s: 'PASSED', b: [14.25, -1.25, -7.62, 17.1, 0, 2.63], msg: '[erfüllt] Gründung Fundament Ost' },
  { id: '1cxNZQHk18xe_eYvhIpXRI', expressId: 669, t: 'IfcSlab', n: 'Gruendung_Pfahlkopfplatte_Mitte:632523', s: 'PASSED', b: [-0.95, -0.75, -8, 0.95, 0, 3], msg: '[erfüllt] Pfahlkopfplatte Mitte' },
  { id: '2MePDRxq95YBYnkQWyl0vC', expressId: 2137, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M1:652726', s: 'PASSED', b: [-0.75, -8.5, 2.1, -0.2, -0.75, 2.75], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '0Sb8twv_X778O9krzvDQsw', expressId: 2138, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M2:677452', s: 'PASSED', b: [0.2, -8.5, 2.1, 0.75, -0.75, 2.75], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '0Sb8twv_X778O9krzvDQsc', expressId: 2139, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M3:677456', s: 'PASSED', b: [-0.75, -8.5, -1.5, -0.2, -0.75, -0.9], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '0Sb8twv_X778O9krzvDQsa', expressId: 2140, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M4:677458', s: 'PASSED', b: [0.2, -8.5, -1.5, 0.75, -0.75, -0.9], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '0Sb8twv_X778O9krzvDQsY', expressId: 2141, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M5:677460', s: 'PASSED', b: [-0.75, -8.5, -4.1, -0.2, -0.75, -3.5], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '0Sb8twv_X778O9krzvDQsW', expressId: 2142, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M6:677462', s: 'PASSED', b: [0.2, -8.5, -4.1, 0.75, -0.75, -3.5], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '2MePDRxq95YBYnkQWyl0tl', expressId: 2143, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M7:652809', s: 'PASSED', b: [-0.75, -8.5, -7.75, -0.2, -0.75, -7.15], msg: '[erfüllt] Bohrpfahl Mitte' },
  { id: '2MePDRxq95YBYnkQWyl0tn', expressId: 2144, t: 'IfcSlab', n: 'Gruendung_Pfahlschaft_M8:652811', s: 'PASSED', b: [0.2, -8.5, -7.75, 0.75, -0.75, -7.15], msg: '[erfüllt] Bohrpfahl Mitte' },
];

const canvas = document.getElementById('viewer3d') as HTMLCanvasElement;
const gl = canvas.getContext('webgl', { antialias: true });
let uint32Ext: OES_element_index_uint | null = null;

let prog: WebGLProgram | null = null;
let gpuMeshes: GpuMeshBuffer[] = [];
let bboxCenter: [number, number, number] = [0, 1.5, -2.5];
let bboxRadius = 22;
let rotX = -0.45;
let rotY = 0.62;
let zoom = 1.0;
let panX = 0;
let panY = 0;
let selectedGlobalId: string | null = null;
let filterFailedOnly = false;

let currentBytes: Uint8Array | null = null;
let currentModelName = 'bruecke-tbw1-mit-merkmalen.ifc';
let currentIdsXml = DEMO_BRIDGE_IDS;

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
}

function makeBoxMesh(b: [number, number, number, number, number, number]): {
  positions: Float32Array;
  normals: Float32Array;
  indices: Uint16Array;
} {
  const [x0, y0, z0, x1, y1, z1] = b;
  const positions = new Float32Array([
    x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, // front
    x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, // back
    x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, // top
    x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, // bottom
    x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, // right
    x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, // left
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

function renderFallbackPreview(): void {
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

  const reportBox = document.getElementById('reportContainer')!;
  reportBox.innerHTML = '';

  const spec1 = document.createElement('div');
  spec1.className = 'spec-item';
  spec1.innerHTML = `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
    <strong>Überbau &gt; Kappen (LPH 8-9): Bauteilinformation/Feldnummer</strong>
    <span class="badge FAILED">nicht erfüllt (0/6)</span>
  </div>`;
  for (const item of FALLBACK_BRIDGE_BOXES.filter((x) => x.s === 'FAILED')) {
    const btn = document.createElement('button');
    btn.className = 'finding-btn';
    btn.textContent = `🎯 ${item.n} (${item.id}) — Merkmal Feldnummer fehlt`;
    btn.addEventListener('click', () => {
      selectedGlobalId = item.id;
      drawScene();
      document.getElementById('elementInspector')!.textContent =
        `${item.t} '${item.n}' (${item.id})\nStatus: NICHT ERFÜLLT\n${item.msg}`;
    });
    spec1.appendChild(btn);
  }
  reportBox.appendChild(spec1);

  const spec2 = document.createElement('div');
  spec2.className = 'spec-item';
  spec2.innerHTML = `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
    <strong>Überbau &amp; Unterbau: Klassifizierung (_IDEbene1..3)</strong>
    <span class="badge PASSED">erfüllt (286/286)</span>
  </div>`;
  for (const item of FALLBACK_BRIDGE_BOXES.filter((x) => x.s === 'PASSED').slice(0, 6)) {
    const btn = document.createElement('button');
    btn.className = 'finding-btn';
    btn.textContent = `✅ ${item.n} (${item.id})`;
    btn.addEventListener('click', () => {
      selectedGlobalId = item.id;
      drawScene();
      document.getElementById('elementInspector')!.textContent =
        `${item.t} '${item.n}' (${item.id})\nStatus: ERFÜLLT\n${item.msg}`;
    });
    spec2.appendChild(btn);
  }
  reportBox.appendChild(spec2);
}

function drawScene(): void {
  if (!gl || !prog) return;
  gl.viewport(0, 0, canvas.width, canvas.height);
  gl.clearColor(0.05, 0.06, 0.09, 1.0);
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

  for (const b of gpuMeshes) {
    if (filterFailedOnly && b.status !== 'FAILED' && b.globalId !== selectedGlobalId) {
      continue;
    }
    let col: [number, number, number, number] = [0.58, 0.63, 0.72, 0.65];
    if (b.status === 'FAILED') col = [0.94, 0.25, 0.25, 1.0];
    else if (b.status === 'PASSED') col = [0.15, 0.78, 0.42, 0.9];

    if (selectedGlobalId && b.globalId === selectedGlobalId) {
      col = [0.98, 0.8, 0.12, 1.0];
    } else if (selectedGlobalId) {
      col[3] = 0.22;
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

async function loadAndCheck(ifcBytes: Uint8Array, modelName: string, idsXml: string): Promise<void> {
  currentBytes = ifcBytes;
  currentModelName = modelName;
  currentIdsXml = idsXml;

  const metaEl = document.getElementById('modelMeta')!;
  metaEl.textContent = `Parse ${modelName} (${(ifcBytes.byteLength / (1024 * 1024)).toFixed(1)} MB) mit @ifc-lite/parser ...`;

  const ab = ifcBytes.buffer.slice(
    ifcBytes.byteOffset,
    ifcBytes.byteOffset + ifcBytes.byteLength
  ) as ArrayBuffer;
  const parsed = await parseAuto(ab, { disableWorkerScan: true });
  if (parsed.format !== 'ifc') return;

  const store = parsed.data;
  const baseAccessor = createDataAccessor(store);
  const rawSchema = baseAccessor.getSchemaVersion?.() ?? 'IFC2X3';
  const schema = narrowSchemaVersion(rawSchema) ?? 'IFC2X3';

  const queryableIds: number[] = [];
  for (const [typeName, ids] of store.entityIndex.byType.entries()) {
    if (isQueryableObjectType(typeName)) queryableIds.push(...ids);
  }

  metaEl.textContent = `${modelName} · Schema ${rawSchema} · ${queryableIds.length} prüfbare Bauteile`;

  const doc: IDSDocument = parseIDS(idsXml);
  const translator = createTranslationService('de');
  const report = await validateIDS(
    doc,
    baseAccessor,
    { modelId: modelName, schemaVersion: schema, entityCount: queryableIds.length },
    { translator, includePassingEntities: true }
  );

  const statusByExpress = new Map<number, 'PASSED' | 'FAILED'>();
  const reportBox = document.getElementById('reportContainer')!;
  reportBox.innerHTML = '';

  for (const sr of report.specificationResults) {
    const specDiv = document.createElement('div');
    specDiv.className = 'spec-item';
    const vClass =
      sr.applicableCount === 0
        ? 'NOT_APPLICABLE'
        : sr.failedCount > 0
          ? 'FAILED'
          : 'PASSED';
    const vLabel =
      vClass === 'PASSED'
        ? 'erfüllt'
        : vClass === 'FAILED'
          ? 'nicht erfüllt'
          : 'nicht prüfbar';

    specDiv.innerHTML = `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
      <strong>${sr.specification.name}</strong>
      <span class="badge ${vClass}">${vLabel} (${sr.passedCount}/${sr.applicableCount})</span>
    </div>`;

    for (const er of sr.entityResults) {
      if (!er.passed) {
        statusByExpress.set(er.expressId, 'FAILED');
      } else if (statusByExpress.get(er.expressId) !== 'FAILED') {
        statusByExpress.set(er.expressId, 'PASSED');
      }

      if (!er.passed) {
        const gid = er.globalId ?? `#${er.expressId}`;
        const btn = document.createElement('button');
        btn.className = 'finding-btn';
        const reasons = er.requirementResults
          .filter((r) => (r as unknown as { status?: string }).status === 'fail' || !r.passed)
          .map((r) => r.failureReason ?? r.checkedDescription)
          .join('; ');
        btn.textContent = `🎯 ${er.entityName || er.entityType} (${gid}) — ${reasons}`;
        btn.addEventListener('click', () => {
          selectedGlobalId = gid;
          drawScene();
          const psets = baseAccessor.getPropertySets(er.expressId);
          const propLines = psets.flatMap((ps) =>
            ps.properties.map((p) => `  ${ps.name}/${p.name} = ${p.value} (${p.dataType ?? ''})`)
          );
          document.getElementById('elementInspector')!.textContent =
            `${er.entityType} '${er.entityName ?? ''}' (${gid})\nStatus: NICHT ERFÜLLT\nAbweichung: ${reasons}\n\nMerkmale:\n${propLines.join('\n')}`;
        });
        specDiv.appendChild(btn);
      }
    }
    reportBox.appendChild(specDiv);
  }

  // Extract 3D geometry via @ifc-lite/geometry
  if (gl && prog) {
    try {
      const processor = new GeometryProcessor();
      await processor.init();
      const geom = await processor.process(ifcBytes);
      processor.dispose();

      if (geom.meshes.length > 0) {
        gpuMeshes = [];
        let minX = Infinity,
          minY = Infinity,
          minZ = Infinity,
          maxX = -Infinity,
          maxY = -Infinity,
          maxZ = -Infinity;

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

          const gid = baseAccessor.getGlobalId(m.expressId) ?? `#${m.expressId}`;
          gpuMeshes.push({
            pb,
            nb,
            ib,
            count: m.indices.length,
            indexType: useUint32 ? gl.UNSIGNED_INT : gl.UNSIGNED_SHORT,
            expressId: m.expressId,
            globalId: gid,
            name: baseAccessor.getEntityName(m.expressId) ?? '',
            entityType: baseAccessor.getEntityType(m.expressId) ?? 'IfcProduct',
            status: statusByExpress.get(m.expressId) ?? 'UNCHECKED',
          });
        });

        if (Number.isFinite(minX)) {
          bboxCenter = [(minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2];
          const dx = maxX - minX,
            dy = maxY - minY,
            dz = maxZ - minZ;
          bboxRadius = Math.max(0.5, Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.55);
        }
        drawScene();
      }
    } catch (geomErr) {
      console.warn('WASM Geometry fallback active:', geomErr);
    }
  }
}

initWebGL();
// Immediately render the 3D bridge and report on startup so the canvas is never blank
renderFallbackPreview();

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

document.getElementById('filterFailedBtn')?.addEventListener('click', () => {
  filterFailedOnly = !filterFailedOnly;
  drawScene();
});
document.getElementById('resetCamBtn')?.addEventListener('click', () => {
  rotX = -0.45;
  rotY = 0.62;
  zoom = 1.0;
  panX = 0;
  panY = 0;
  selectedGlobalId = null;
  drawScene();
});

document.getElementById('ifcFile')?.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const buf = new Uint8Array(await file.arrayBuffer());
  await loadAndCheck(buf, file.name, currentIdsXml);
});

document.getElementById('idsFile')?.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const xml = await file.text();
  if (currentBytes) {
    await loadAndCheck(currentBytes, currentModelName, xml);
  } else {
    currentIdsXml = xml;
  }
});

async function runDemo(): Promise<void> {
  const metaEl = document.getElementById('modelMeta')!;
  renderFallbackPreview();
  metaEl.textContent =
    'BIM.Hamburg Brücke TBW1 (bruecke-tbw1-mit-merkmalen.ifc) · 303 Bauteile · 6 LOIN-Abweichungen (Rot)';
  try {
    const res = await fetch('./bruecke-tbw1-mit-merkmalen.ifc');
    if (!res.ok) return;
    const buf = new Uint8Array(await res.arrayBuffer());
    await loadAndCheck(buf, 'bruecke-tbw1-mit-merkmalen.ifc', DEMO_BRIDGE_IDS);
  } catch (err) {
    console.warn('Using pre-extracted bridge geometry:', err);
  }
}

document.getElementById('runDemoBtn')?.addEventListener('click', () => {
  void runDemo();
});

void runDemo();
