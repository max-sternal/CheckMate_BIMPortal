import { cpSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const publisher = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const [releaseSourceArg, outputArg] = process.argv.slice(2);

if (!releaseSourceArg || !outputArg) {
	throw new Error('Usage: node prepare-published-site.mjs <release website> <new output directory>');
}
const releaseSource = path.resolve(releaseSourceArg);
const output = path.resolve(outputArg);
if (!existsSync(path.join(releaseSource, 'src', 'content', 'docs'))) {
	throw new Error(`Release documentation not found: ${releaseSource}`);
}
if (existsSync(output)) {
	throw new Error(`Output already exists: ${output}`);
}

cpSync(releaseSource, output, {
	recursive: true,
	filter: (source) => !['node_modules', '.astro', 'dist'].includes(path.basename(source)),
});

for (const relativePath of [
	'package.json',
	'package-lock.json',
	'astro.config.mjs',
	'src/content.config.ts',
	'src/content/versions',
	'src/assets/v0.3.1',
	...['v0.1.0', 'v0.2.0', 'v0.3.1'].flatMap((version) => [
		`src/content/docs/${version}`,
		`src/content/docs/en/${version}`,
	]),
]) {
	cpSync(path.join(publisher, relativePath), path.join(output, relativePath), {
		recursive: true,
		force: true,
	});
}
