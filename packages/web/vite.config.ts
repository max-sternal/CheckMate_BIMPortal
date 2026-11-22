import { copyFileSync, existsSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

const __dirname = dirname(fileURLToPath(import.meta.url));
const publicDir = resolve(__dirname, 'public');
const demoIfcSrc = resolve(__dirname, '../../resources/models/bruecke-tbw1-mit-merkmalen.ifc');
const demoIfcDest = resolve(publicDir, 'bruecke-tbw1-mit-merkmalen.ifc');

if (existsSync(demoIfcSrc) && !existsSync(demoIfcDest)) {
  mkdirSync(publicDir, { recursive: true });
  copyFileSync(demoIfcSrc, demoIfcDest);
}

export default defineConfig({
  base: './',
  esbuild: {
    target: 'esnext',
  },
  worker: {
    format: 'es',
  },
  optimizeDeps: {
    esbuildOptions: {
      target: 'esnext',
    },
    include: ['jszip', 'decimal.js', 'fflate'],
    exclude: ['@ifc-lite/wasm', '@ifc-lite/geometry'],
  },
  build: {
    outDir: 'dist',
    target: 'esnext',
  },
});
