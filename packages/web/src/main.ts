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
  expressId: number;
  globalId: string;
  name: string;
  entityType: string;
  status: 'PASSED' | 'FAILED' | 'UNCHECKED';
}

const DEMO_BRIDGE_IDS = `<?xml version="1.0" encoding="UTF-8"?>
<ids:ids xmlns:ids="http://standards.buildingsmart.org/IDS" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <ids:info><ids:title>BIM.Hamburg Brücke — LOIN Überbau &amp; Unterbau Prüfung</ids:title></ids:info>
  <ids:specifications>
    <ids:specification name="Kappen: Bauteilinformation &amp; Objektinformation" ifcVersion="IFC2X3">
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
    <ids:specification name="Überbau-Platten (IfcSlab): Tragfähigkeit geprüft" ifcVersion="IFC2X3">
      <ids:applicability minOccurs="0" maxOccurs="unbounded">
        <ids:entity><ids:name><ids:simpleValue>IFCSLAB</ids:simpleValue></ids:name></ids:entity>
      </ids:applicability>
      <ids:requirements>
        <ids:property cardinality="required">
          <ids:propertySet><ids:simpleValue>Pset_Objektinformation</ids:simpleValue></ids:propertySet>
          <ids:baseName><ids:simpleValue>_IDEbene1</ids:simpleValue></ids:baseName>
        </ids:property>
      </ids:requirements>
    </ids:specification>
  </ids:specifications>
</ids:ids>`;

const canvas = document.getElementById('viewer3d') as HTMLCanvasElement;
const gl = canvas.getContext('webgl', { antialias: true });

let prog: WebGLProgram | null = null;
let gpuMeshes: GpuMeshBuffer[] = [];
let bboxCenter: [number, number, number] = [0, 0, 0];
let bboxRadius = 10;
let rotX = -0.55;
let rotY = 0.65;
let zoom = 1.0;
let panX = 0;
let panY = 0;
let selectedGlobalId: string | null = null;
let filterFailedOnly = false;

let currentBytes: Uint8Array | null = null;
let currentModelName = 'model.ifc';
let currentIdsXml = DEMO_BRIDGE_IDS;

function initWebGL(): void {
  if (!gl) return;
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
      vec3 r1 = vec3(p.x * cy - p.y * sy, p.x * sy + p.y * cy, p.z);
      vec3 r2 = vec3(r1.x, r1.y * cx - r1.z * sx, r1.y * sx + r1.z * cx);
      vec3 n1 = vec3(aNorm.x * cy - aNorm.y * sy, aNorm.x * sy + aNorm.y * cy, aNorm.z);
      vNorm = vec3(n1.x, n1.y * cx - n1.z * sx, n1.y * sx + n1.z * cx);
      gl_Position = vec4((r2.x * uZoom / uAspect) + uPan.x, (r2.z * uZoom) + uPan.y, r2.y * 0.25, 1.0);
    }
  `;
  const fs = `
    precision mediump float;
    varying vec3 vNorm;
    uniform vec4 uColor;
    void main() {
      vec3 l = normalize(vec3(0.4, -0.6, 0.7));
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
    gl.drawElements(gl.TRIANGLES, b.count, gl.UNSIGNED_SHORT, 0);
  }
}

async function loadAndCheck(ifcBytes: Uint8Array, modelName: string, idsXml: string): Promise<void> {
  currentBytes = ifcBytes;
  currentModelName = modelName;
  currentIdsXml = idsXml;

  const metaEl = document.getElementById('modelMeta')!;
  metaEl.textContent = `Parse ${modelName} mit @ifc-lite/parser & @ifc-lite/geometry ...`;

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
          .filter((r) => !r.passed)
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
    const processor = new GeometryProcessor();
    try {
      await processor.init();
      const geom = await processor.process(ifcBytes);
      gpuMeshes = [];
      let minX = Infinity,
        minY = Infinity,
        minZ = Infinity,
        maxX = -Infinity,
        maxY = -Infinity,
        maxZ = -Infinity;

      geom.meshes.forEach((m: MeshData) => {
        if (!m.positions || m.positions.length === 0) return;
        const p = m.positions;
        for (let i = 0; i < p.length; i += 3) {
          if (p[i] < minX) minX = p[i];
          if (p[i] > maxX) maxX = p[i];
          if (p[i + 1] < minY) minY = p[i + 1];
          if (p[i + 1] > maxY) maxY = p[i + 1];
          if (p[i + 2] < minZ) minZ = p[i + 2];
          if (p[i + 2] > maxZ) maxZ = p[i + 2];
        }
        const pb = gl.createBuffer()!;
        gl.bindBuffer(gl.ARRAY_BUFFER, pb);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(m.positions), gl.STATIC_DRAW);
        const nb = gl.createBuffer()!;
        gl.bindBuffer(gl.ARRAY_BUFFER, nb);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(m.normals), gl.STATIC_DRAW);
        const ib = gl.createBuffer()!;
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, ib);
        gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(m.indices), gl.STATIC_DRAW);

        const gid = baseAccessor.getGlobalId(m.expressId) ?? `#${m.expressId}`;
        gpuMeshes.push({
          pb,
          nb,
          ib,
          count: m.indices.length,
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
    } finally {
      processor.dispose();
    }
  }
}

initWebGL();

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
  rotX = -0.55;
  rotY = 0.65;
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
