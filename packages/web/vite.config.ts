import { copyFileSync, existsSync, mkdirSync, readdirSync } from 'node:fs';
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

// Copy bundled portal & local IDS fixtures into public/fixtures/ so the web workbench can
// inspect and check all portal LOINs and AIA project archives offline or online
const fixturesDestDir = resolve(publicDir, 'fixtures');
mkdirSync(fixturesDestDir, { recursive: true });
for (const sub of ['portal', 'local']) {
  const srcSub = resolve(__dirname, '../../resources/fixtures/ids', sub);
  if (existsSync(srcSub)) {
    for (const file of readdirSync(srcSub)) {
      if (file.endsWith('.ids') || file.endsWith('.zip')) {
        const destFile = resolve(fixturesDestDir, file);
        if (!existsSync(destFile)) {
          copyFileSync(resolve(srcSub, file), destFile);
        }
      }
    }
  }
}

export default defineConfig({
  base: './',
  server: {
    proxy: {
      '/bim-portal-api': {
        target: 'https://via.bund.de/bim',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/bim-portal-api/, ''),
      },
    },
  },
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
    include: [
      '@ifc-lite/parser',
      '@ifc-lite/ids',
      '@ifc-lite/ids/bridge',
      '@ifc-lite/geometry',
      '@ifc-lite/data',
      '@ifc-lite/rules',
      '@ifc-lite/bcf',
    ],
    exclude: ['@ifc-lite/wasm'],
  },
  build: {
    outDir: 'dist',
    target: 'esnext',
  },
});

