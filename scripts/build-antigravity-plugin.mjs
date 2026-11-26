#!/usr/bin/env node
/**
 * Creates a local Antigravity MCP plugin directory in `build/checkmate-antigravity-plugin`
 * pointing to this repository's `@checkmate/mcp` server, ready for:
 *   agy plugin validate build/checkmate-antigravity-plugin --strict
 *   agy plugin install build/checkmate-antigravity-plugin
 */
import { copyFileSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = resolve(rootDir, 'build/checkmate-antigravity-plugin');
const assetsDir = resolve(outDir, 'assets');

rmSync(outDir, { recursive: true, force: true });
mkdirSync(assetsDir, { recursive: true });

const repoLogo = resolve(rootDir, 'website/src/assets/checkmate-logo.png');
let logoField = 'assets/logo.svg';
if (existsSync(repoLogo)) {
  copyFileSync(repoLogo, resolve(assetsDir, 'logo.png'));
  logoField = 'assets/logo.png';
} else {
  writeFileSync(
    resolve(assetsDir, 'logo.svg'),
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="12" fill="#2563eb"/><path d="M18 34l10 10 18-22" fill="none" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/></svg>\n',
    'utf-8'
  );
}

const mcpLauncher = resolve(rootDir, 'packages/mcp/bin/checkmate-mcp.mjs');

writeFileSync(
  resolve(outDir, 'plugin.json'),
  JSON.stringify(
    {
      name: 'checkmate-bimportal',
      version: '0.4.1-beta',
      description:
        'CheckMate 0.4.1-beta — 3D IFC & BIM-Portal LOIN/IDS checker with interactive 3D MCP App views',
      logo: logoField,
    },
    null,
    2
  ) + '\n',
  'utf-8'
);

writeFileSync(
  resolve(outDir, 'mcp_config.json'),
  JSON.stringify(
    {
      mcpServers: {
        checkmate: {
          command: 'node',
          args: [mcpLauncher],
          env: {
            CHECKMATE_MAPPING: 'default',
          },
        },
      },
    },
    null,
    2
  ) + '\n',
  'utf-8'
);

console.log(`Antigravity plugin written to ${outDir}`);
