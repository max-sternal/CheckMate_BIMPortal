import { describe, expect, it } from 'vitest';
import { zipSync, strToU8 } from 'fflate';
import {
  applyCatalogueFilter,
  BimPortalClient,
  extractPortalId,
  RequirementsError,
  toCatalogueEntry,
  unpackIdsBytes,
} from '../src/index.js';

describe('@checkmate/bimportal', () => {
  it('extracts portal UUIDs from links and rejects multiple distinct UUIDs', () => {
    const id = 'be959b9d-daef-5315-9aba-f60b6ad54ccb';
    expect(extractPortalId(`https://via.bund.de/bim/aia/loin/${id}`)).toBe(id);
    expect(extractPortalId(`ID ${id} und nochmal ${id.toUpperCase()}`)).toBe(id);
    expect(extractPortalId('kein link')).toBeNull();
    expect(() =>
      extractPortalId(
        'be959b9d-daef-5315-9aba-f60b6ad54ccb und 11111111-2222-3333-4444-555555555555'
      )
    ).toThrow(RequirementsError);
  });

  it('filters catalogue entries by discipline, phase and purpose', () => {
    const kappe = toCatalogueEntry({
      guid: 'be959b9d-daef-5315-9aba-f60b6ad54ccb',
      name: 'Ueberbau > Kappe',
      objectTypes: ['Kappe'],
      domainSpecificModelTypes: ['Ingenieurbau'],
      projectPhases: ['LPH 8'],
      purposes: ['Qualitätsprüfung'],
    });
    const wand = toCatalogueEntry({
      guid: '11111111-2222-3333-4444-555555555555',
      name: 'Wand',
      objectTypes: ['Wand'],
      domainSpecificModelTypes: ['Hochbau'],
      projectPhases: ['LPH 5'],
      purposes: ['Mengenermittlung'],
    });

    expect(
      applyCatalogueFilter([kappe, wand], { discipline: 'ingenieur', phase: 'lph 8' })
    ).toEqual([kappe]);
  });

  it('unpacks IDS ZIP archives with combined AIA_*.ids entries first', () => {
    const zipBytes = zipSync({
      'IDS/LOIN_Kappe_1.ids': strToU8('<ids:ids><ids:info><ids:title>Kappe</ids:title></ids:info></ids:ids>'),
      'IDS/AIA_Bruecke_all.ids': strToU8('<ids:ids><ids:info><ids:title>AIA</ids:title></ids:info></ids:ids>'),
      'README.txt': strToU8('ignore me'),
    });

    const entries = unpackIdsBytes(zipBytes, 'Projekt.zip');
    expect(entries).toHaveLength(2);
    expect(entries[0].isCombinedAia).toBe(true);
    expect(entries[0].fileName).toBe('IDS/AIA_Bruecke_all.ids');
    expect(entries[1].fileName).toBe('IDS/LOIN_Kappe_1.ids');
  });

  it('fetches LOIN requirements via mocked fetch', async () => {
    const guid = 'be959b9d-daef-5315-9aba-f60b6ad54ccb';
    const client = new BimPortalClient({
      baseUrl: 'https://portal.test/bim',
      fetchImpl: (async (url: string | URL | Request) => {
        const u = String(url);
        if (u.endsWith(`/loin/${guid}/IDS`)) {
          return new Response('<ids><info><title>Kappe</title></info></ids>', { status: 200 });
        }
        return new Response('', { status: 404 });
      }) as typeof fetch,
    });

    const result = await client.fetchRequirementsById(guid);
    expect(result.guid).toBe(guid);
    expect(result.entries).toHaveLength(1);
    expect(result.entries[0].xml).toContain('<title>Kappe</title>');
  });
});
