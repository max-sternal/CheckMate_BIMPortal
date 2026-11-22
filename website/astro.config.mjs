// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	site: 'https://max-sternal.github.io',
	base: '/CheckMate_BIMPortal',
	integrations: [
		starlight({
			title: 'CheckMate',
			description:
				'Open-Source LOIN- & IDS-Prüfwerkzeug für das BIM-Portal des Bundes — deterministische IFC-Prüfung mit KI-/MCP-Unterstützung.',
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
				baseUrl:
					'https://github.com/max-sternal/CheckMate_BIMPortal/edit/0.3.1/website/',
			},
			sidebar: [
				{
					label: 'Einstieg',
					translations: { en: 'Getting Started' },
					items: [
						{
							label: 'Vision & Idee',
							translations: { en: 'Vision & Mission' },
							slug: 'guides/vision',
						},
						{
							label: 'Versionen & Branches',
							translations: { en: 'Versions & Branches' },
							slug: 'guides/versions',
						},
						{
							label: 'Schnellstart (0.3.1)',
							translations: { en: 'Quick Start (0.3.1)' },
							slug: 'guides/quickstart',
						},
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
							label: 'IFC-Parsing & IDS 1.0',
							translations: { en: 'IFC Parsing & IDS 1.0' },
							slug: 'architecture/ifc-ids-checking',
						},
					],
				},
				{
					label: 'KI & MCP-Anschluss',
					translations: { en: 'AI & MCP Integration' },
					items: [
						{
							label: 'MCP-Server & MCP Apps',
							translations: { en: 'MCP Server & MCP Apps' },
							slug: 'mcp/overview',
						},
						{
							label: 'Clients: Claude, OpenCode, Ollama & Strata',
							translations: { en: 'Clients: Claude, OpenCode, Ollama & Strata' },
							slug: 'mcp/clients',
						},
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
							label: 'Status & Roadmap',
							translations: { en: 'Status & Roadmap' },
							slug: 'project/roadmap',
						},
					],
				},
			],
		}),
	],
});
