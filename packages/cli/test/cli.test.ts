import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { describe, expect, it } from 'vitest';
import { parseCliArguments, runCli } from '../src/index.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const RESOURCES = resolve(__dirname, '../../../resources');

describe('@checkmate/cli', () => {
  it('parses CLI arguments and portal UUIDs in search terms', () => {
    const args = parseCliArguments([
      '--ids',
      'a.ids',
      '--mapping',
      'none',
      '--full',
      'be959b9d-daef-5315-9aba-f60b6ad54ccb',
    ]);
    expect(args.idsFiles).toEqual(['a.ids']);
    expect(args.mapping).toBe('none');
    expect(args.full).toBe(true);
    expect(args.portalIds).toEqual(['be959b9d-daef-5315-9aba-f60b6ad54ccb']);
    expect(args.search).toBeNull();
  });

  it('runs a local IDS check and writes JSON report with expected exit code', async () => {
    const tmp = mkdtempSync(resolve(tmpdir(), 'checkmate-cli-'));
    const jsonFile = resolve(tmp, 'report.json');
    const idsPath = resolve(RESOURCES, 'fixtures/ids/portal/ueberbau-kappe-lph8-9.ids');
    const lines: string[] = [];

    const code = await runCli(
      ['--ids', idsPath, '--json', jsonFile],
      (l) => lines.push(l),
      () => {}
    );

    expect(code).toBe(4);
    expect(lines.join('\n')).toContain('Modell: bruecke-tbw1-mit-merkmalen.ifc');
    expect(lines.join('\n')).toContain('Exit-Code 4');
    const jsonObj = JSON.parse(readFileSync(jsonFile, 'utf-8'));
    expect(jsonObj.exitCode).toBe(4);
  });
});
