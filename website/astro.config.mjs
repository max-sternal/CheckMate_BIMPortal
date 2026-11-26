// @ts-check
import { existsSync } from 'node:fs';
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightVersions from 'starlight-versions';

const publishedVersion = process.env.CHECKMATE_DOCS_LATEST ?? 'v0.4.1-beta';
const is031 = publishedVersion === 'v0.3.1';
const is032 = publishedVersion === 'v0.3.2';
const is033 = publishedVersion === 'v0.3.3-stable' || publishedVersion === '0.3.3-stable' || publishedVersion === '0.3.3';

const hasVersionDir = (slug) => existsSync(new URL(`./src/content/docs/${slug}`, import.meta.url));

const currentBranch = is031
	? '0.3.1'
	: is032
		? '0.3.2'
		: is033
			? '0.3.3-stable'
			: '0.4.1-beta';

const currentVersionLabel = is031
	? '0.3.1'
	: is032
		? '0.3.2'
		: is033
			? '0.3.3-stable'
			: '0.4.1-beta';

const currentBadgeLabel = is033
	? 'Latest (0.3.3-stable)'
	: is032
		? 'Latest (v0.3.2)'
		: is031
			? 'Latest (v0.3.1)'
			: 'v0.4.1-beta';

// https://astro.build/config
export default defineConfig({
	site: 'https://max-sternal.github.io',
	base: '/CheckMate_BIMPortal',
	integrations: [
		starlight({
			plugins: [
				starlightVersions({
					current: {
						label: currentBadgeLabel,
						redirect: 'root',
					},
					versions: [
						...(hasVersionDir('v0.4.1-beta')
							? [{ slug: 'v0.4.1-beta', label: 'v0.4.1-beta (Beta)', redirect: 'root' }]
							: []),
						...(hasVersionDir('v0.4-alpha')
							? [{ slug: 'v0.4-alpha', label: 'v0.4-alpha (Alpha)', redirect: 'root' }]
							: []),
						...(hasVersionDir('v0.3.2')
							? [{ slug: 'v0.3.2', label: 'v0.3.2', redirect: 'root' }]
							: []),
						{ slug: 'v0.3.1', label: 'v0.3.1', redirect: 'root' },
						{ slug: 'v0.2.0', label: 'v0.2.0', redirect: 'root' },
						{ slug: 'v0.1.0', label: 'v0.1.0', redirect: 'root' },
					],
				}),
			],
			title: 'CheckMate',
			description:
				'CheckMate prüft IFC-Modelle anhand von IDS-Anforderungen aus dem BIM-Portal oder lokalen Dateien – als Java-25-Release (0.3.3-stable) und in 3D mit IFClite (0.4.1-beta).',
			logo: {
				src: './src/assets/checkmate-logo.png',
				alt: 'CheckMate Logo',
			},
			customCss: ['./src/styles/custom.css'],
			defaultLocale: 'root',
			locales: {
				root: {
					label: 'Deutsch',
					lang: 'de',
				},
				en: {
					label: 'English',
					lang: 'en',
				},
			},
			social: [
				{
					icon: 'github',
					label: 'GitHub',
					href: 'https://github.com/max-sternal/CheckMate_BIMPortal',
				},
			],
			editLink: {
				baseUrl: `https://github.com/max-sternal/CheckMate_BIMPortal/edit/${currentBranch}/website/`,
			},
			sidebar: [
				{
					label: 'Einstieg',
					translations: { en: 'Getting Started' },
					items: [
						{
							label: 'Einordnung & Grundlagen',
							translations: { en: 'Context & Foundations' },
							slug: 'guides/vision',
						},
						{
							label: 'Versionen & Branches',
							translations: { en: 'Versions & Branches' },
							slug: 'guides/versions',
						},
						{
							label: `Schnellstart (${currentVersionLabel})`,
							translations: { en: `Quick Start (${currentVersionLabel})` },
							slug: 'guides/quickstart',
						},
						...(is031
							? []
							: [
									{
										label: 'Beispiele & Mapping',
										translations: { en: 'Examples & Mapping' },
										slug: 'guides/mapping',
									},
									{
										label: 'Eine Prüfung nachvollziehen',
										translations: { en: 'Walk Through a Check' },
										slug: 'guides/example',
									},
								]),
					],
				},
				{
					label: 'Architektur & Prüfung',
					translations: { en: 'Architecture & Checking' },
					items: [
						{
							label: 'Systemarchitektur',
							translations: { en: 'System Architecture' },
							slug: 'architecture/overview',
						},
						{
							label: 'IFC & IDS',
							translations: { en: 'IFC & IDS' },
							slug: 'architecture/ifc-ids-checking',
						},
					],
				},
				{
					label: is033 ? 'KI-Anbindung' : 'MCP-Schnittstelle',
					translations: { en: is033 ? 'AI Integration' : 'MCP Interface' },
					items: [
						{
							label: is031 || is033 ? 'MCP-Server' : 'MCP-Server & 3D MCP Apps',
							translations: {
								en: is031 || is033 ? 'MCP Server' : 'MCP Server & 3D MCP Apps',
							},
							slug: 'mcp/overview',
						},
						{
							label: 'Client einrichten',
							translations: { en: 'Set Up a Client' },
							slug: 'mcp/clients',
						},
						...(is033
							? [
									{
										label: 'Chat in der Oberfläche',
										translations: { en: 'Chat in the Interface' },
										slug: 'guides/chat',
									},
								]
							: []),
					],
				},
				{
					label: 'Projekt & Transparenz',
					translations: { en: 'Project & Transparency' },
					items: [
						{
							label: 'Mitwirkende & Transparenz',
							translations: { en: 'Contributors & Transparency' },
							slug: 'project/contributors',
						},
						{
							label: 'Projektstatus',
							translations: { en: 'Project Status' },
							slug: 'project/roadmap',
						},
					],
				},
			],
		}),
	],
});
