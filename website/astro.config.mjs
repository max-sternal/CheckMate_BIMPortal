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
				'CheckMate prüft IFC-Modelle anhand von IDS-Anforderungen aus dem BIM-Portal oder lokalen Dateien.',
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
					'https://github.com/max-sternal/CheckMate_BIMPortal/edit/0.3.2/website/',
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
							label: 'Schnellstart (0.3.2)',
							translations: { en: 'Quick Start (0.3.2)' },
							slug: 'guides/quickstart',
						},
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
					label: 'MCP-Schnittstelle',
					translations: { en: 'MCP Interface' },
					items: [
						{
							label: 'MCP-Server',
							translations: { en: 'MCP Server' },
							slug: 'mcp/overview',
						},
						{
							label: 'Client einrichten',
							translations: { en: 'Set Up a Client' },
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
