#!/usr/bin/env node
import { existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

// Ensure stdout carries only MCP JSON-RPC protocol frames
console.log = (...args) => console.error(...args);

const __dirname = dirname(fileURLToPath(import.meta.url));
const distEntry = resolve(__dirname, '../dist/checkmate-mcp.mjs');
const srcEntry = resolve(__dirname, '../src/index.ts');

if (existsSync(distEntry)) {
  await import(pathToFileURL(distEntry).href);
} else {
  const { register } = await import('tsx/esm/api');
  register();
  await import(pathToFileURL(srcEntry).href);
}
