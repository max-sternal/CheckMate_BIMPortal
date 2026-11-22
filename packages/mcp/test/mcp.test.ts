import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  CheckMateTools,
  LOINS_VIEW,
  MODEL_VIEW,
  REPORT_VIEW,
  TOOL_DEFINITIONS,
  UI_MIME,
  VIEW_RESOURCES,
} from '../src/index.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const RESOURCES = resolve(__dirname, '../../../resources');

describe('@checkmate/mcp (MCP Server + 3D MCP Apps + App-only Tools)', () => {
  it('registers all 10 model tools and 5 app-only tools with MCP Apps metadata', () => {
    const names = TOOL_DEFINITIONS.map((t) => t.name);
    expect(names).toEqual([
      'load_model',
      'describe_model',
      'list_elements',
      'find_loins',
      'describe_requirements',
      'check_model',
      'element_report',
      'app_report_page',
      'app_element_page',
      'app_set_filter',
      'app_check_selected',
      'app_element_mesh',
      'load_mapping',
      'export_bcf',
      'server_status',
    ]);

    const appOnly = TOOL_DEFINITIONS.filter((t) => t.name.startsWith('app_'));
    expect(appOnly).toHaveLength(5);
    for (const tool of appOnly) {
      expect((tool._meta?.ui as { visibility?: string[] })?.visibility).toEqual(['app']);
    }

    const checkTool = TOOL_DEFINITIONS.find((t) => t.name === 'check_model')!;
    expect((checkTool._meta?.ui as { resourceUri?: string })?.resourceUri).toBe(REPORT_VIEW);

    const modelTool = TOOL_DEFINITIONS.find((t) => t.name === 'describe_model')!;
    expect((modelTool._meta?.ui as { resourceUri?: string })?.resourceUri).toBe(MODEL_VIEW);

    const loinsTool = TOOL_DEFINITIONS.find((t) => t.name === 'find_loins')!;
    expect((loinsTool._meta?.ui as { resourceUri?: string })?.resourceUri).toBe(LOINS_VIEW);
    expect(VIEW_RESOURCES.map((v) => v.mimeType)).toEqual([UI_MIME, UI_MIME, UI_MIME]);
  });

  it('executes check_model, app_element_page, app_report_page and app_element_mesh with 3D AIA/LOIN status coloring', async () => {
    const tools = new CheckMateTools();
    const modelText = await tools.describeModel();
    expect(modelText).toContain('bruecke-tbw1-mit-merkmalen.ifc');

    const page = await tools.elementPage('IFCSLAB', null, 0, 5);
    expect(Number(page.total)).toBeGreaterThan(0);

    const idsPath = resolve(RESOURCES, 'fixtures/ids/portal/ueberbau-kappe-lph8-9.ids');
    const checkText = await tools.check([], [idsPath], 3);
    expect(checkText).toContain('nicht prüfbar');

    const rData = await tools.reportData(5);
    expect(Array.isArray(rData.runs)).toBe(true);

    const meshData = await tools.elementMeshes();
    expect(meshData.totalElementsWithGeometry).toBeGreaterThan(0);
    expect(meshData.meshes.length).toBeGreaterThan(0);
    expect(['PASSED', 'FAILED', 'UNCHECKED']).toContain(meshData.meshes[0].status);
  });
});
