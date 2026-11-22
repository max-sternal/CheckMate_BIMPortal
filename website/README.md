# CheckMate documentation website

The bilingual documentation is built with Astro Starlight. German pages live in
`src/content/docs/`, English pages in `src/content/docs/en/`. Keep corresponding
pages consistent and distinguish implemented features from plans.

From `website/`:

```bash
npm ci
npm run dev
npm run build
```

The site uses `/CheckMate_BIMPortal` as its base path. GitHub Actions builds and
deploys the documentation via `.github/workflows/docs.yml`.
