import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
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
	'src/styles/custom.css',
	'src/content/versions',
	'src/assets/v0.3.1',
	'src/assets/v0.3.2',
	...['v0.1.0', 'v0.2.0', 'v0.3.1', 'v0.3.2'].flatMap((version) => [
		`src/content/docs/${version}`,
		`src/content/docs/en/${version}`,
	]),
]) {
	if (existsSync(path.join(publisher, relativePath))) {
		cpSync(path.join(publisher, relativePath), path.join(output, relativePath), {
			recursive: true,
			force: true,
		});
	}
}

// If CHECKMATE_DOCS_LATEST is v0.3.1 or v0.3.2, also materialize the publisher's root 0.4 docs
// into /v0.4-alpha/ and /en/v0.4-alpha/ so the v0.4-alpha pre-release documentation is live!
const publishedVersion = process.env.CHECKMATE_DOCS_LATEST ?? 'v0.4-alpha';
if (publishedVersion === 'v0.3.1' || publishedVersion === 'v0.3.2') {
	const logoSrc = path.join(publisher, 'src/assets/checkmate-logo.png');
	const logoDestDir = path.join(output, 'src/assets/v0.4-alpha');
	mkdirSync(logoDestDir, { recursive: true });
	cpSync(logoSrc, path.join(logoDestDir, 'checkmate-logo.png'));

	const pages = [
		'index.mdx',
		'guides/vision.md',
		'guides/versions.md',
		'guides/quickstart.md',
		'guides/mapping.md',
		'guides/example.md',
		'architecture/overview.md',
		'architecture/ifc-ids-checking.md',
		'mcp/overview.md',
		'mcp/clients.md',
		'project/contributors.md',
		'project/roadmap.md',
	];

	const archive04 = (srcPrefix, destPrefix, slugPrefix, isEn) => {
		for (const rel of pages) {
			const srcFile = path.join(publisher, 'src/content/docs', srcPrefix, rel);
			if (!existsSync(srcFile)) continue;
			const destFile = path.join(output, 'src/content/docs', destPrefix, rel);
			mkdirSync(path.dirname(destFile), { recursive: true });
			let content = readFileSync(srcFile, 'utf-8');

			const bareSlug = rel.replace(/\.(md|mdx)$/, '');
			const targetSlug = bareSlug === 'index' ? slugPrefix : `${slugPrefix}/${bareSlug}`;

			content = content.replace(/^---\n([\s\S]*?)\n---/, (_, fm) => {
				return `---\neditUrl: false\n${fm}\nslug: ${targetSlug}\n---`;
			});

			if (rel === 'index.mdx') {
				content = content.replace(
					/file:\s*\.\.\/(?:\.\.\/)*assets\/checkmate-logo\.png/,
					isEn
						? 'file: ../../../../assets/v0.4-alpha/checkmate-logo.png'
						: 'file: ../../../assets/v0.4-alpha/checkmate-logo.png'
				);
			}

			if (isEn) {
				content = content.replace(
					/\/CheckMate_BIMPortal\/en\/(?!v0\.)/g,
					'/CheckMate_BIMPortal/en/v0.4-alpha/'
				);
			} else {
				content = content.replace(
					/\/CheckMate_BIMPortal\/(?!en\/|v0\.)/g,
					'/CheckMate_BIMPortal/v0.4-alpha/'
				);
			}

			writeFileSync(destFile, content, 'utf-8');
		}
	};

	archive04('', 'v0.4-alpha', 'v0.4-alpha', false);
	archive04('en', 'en/v0.4-alpha', 'en/v0.4-alpha', true);
}
