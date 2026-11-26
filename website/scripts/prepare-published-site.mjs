import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const publisher = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const [releaseSourceArg, outputArg, ...extraArchives] = process.argv.slice(2);

if (!releaseSourceArg || !outputArg) {
	throw new Error(
		'Usage: node prepare-published-site.mjs <release website> <new output directory> [<slug>=<websiteDir> ...]'
	);
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

const ARCHIVE_PAGES = [
	'index.mdx',
	'guides/vision.md',
	'guides/versions.md',
	'guides/quickstart.md',
	'guides/mapping.md',
	'guides/example.md',
	'guides/chat.md',
	'architecture/overview.md',
	'architecture/ifc-ids-checking.md',
	'mcp/overview.md',
	'mcp/clients.md',
	'project/contributors.md',
	'project/roadmap.md',
];

function materializeVersion(slug, srcWebsiteDir) {
	const logoSrc = path.join(srcWebsiteDir, 'src/assets/checkmate-logo.png');
	if (existsSync(logoSrc)) {
		const logoDestDir = path.join(output, 'src/assets', slug);
		mkdirSync(logoDestDir, { recursive: true });
		cpSync(logoSrc, path.join(logoDestDir, 'checkmate-logo.png'));
	}

	const copyLocale = (srcPrefix, destPrefix, slugPrefix, isEn) => {
		for (const rel of ARCHIVE_PAGES) {
			const srcFile = path.join(srcWebsiteDir, 'src/content/docs', srcPrefix, rel);
			if (!existsSync(srcFile)) continue;
			const destFile = path.join(output, 'src/content/docs', destPrefix, rel);
			mkdirSync(path.dirname(destFile), { recursive: true });
			let content = readFileSync(srcFile, 'utf-8');

			const bareSlug = rel.replace(/\.(md|mdx)$/, '');
			const targetSlug = bareSlug === 'index' ? slugPrefix : `${slugPrefix}/${bareSlug}`;

			content = content.replace(/^---\n([\s\S]*?)\n---/, (_, fm) => {
				const cleaned = fm
					.split('\n')
					.filter((line) => !line.startsWith('editUrl:') && !line.startsWith('slug:'))
					.join('\n');
				return `---\neditUrl: false\n${cleaned}\nslug: ${targetSlug}\n---`;
			});

			if (rel === 'index.mdx') {
				content = content.replace(
					/file:\s*\.\.\/(?:\.\.\/)*assets\/checkmate-logo\.png/,
					isEn
						? `file: ../../../../assets/${slug}/checkmate-logo.png`
						: `file: ../../../assets/${slug}/checkmate-logo.png`
				);
			}

			// Safely rewrite internal site links without corrupting https://github.com/max-sternal/CheckMate_BIMPortal/...
			if (isEn) {
				content = content.replace(
					/(?<!github\.com\/max-sternal)\/CheckMate_BIMPortal\/en\/(?!v0\.)/g,
					`/CheckMate_BIMPortal/en/${slug}/`
				);
			} else {
				content = content.replace(
					/(?<!github\.com\/max-sternal)\/CheckMate_BIMPortal\/(?!en\/|v0\.)/g,
					`/CheckMate_BIMPortal/${slug}/`
				);
			}

			writeFileSync(destFile, content, 'utf-8');
		}
	};

	copyLocale('', slug, slug, false);
	copyLocale('en', `en/${slug}`, `en/${slug}`, true);
}

for (const spec of extraArchives) {
	const eq = spec.indexOf('=');
	if (eq <= 0) continue;
	const slug = spec.slice(0, eq).trim();
	const dir = path.resolve(spec.slice(eq + 1).trim());
	if (existsSync(path.join(dir, 'src', 'content', 'docs'))) {
		materializeVersion(slug, dir);
	}
}
