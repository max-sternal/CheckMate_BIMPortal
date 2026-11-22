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

The site uses `/CheckMate_BIMPortal` as its base path. The version selector uses
Starlight Versions and provides archived snapshots for `v0.3.2`, `v0.3.1`,
`v0.2.0` and `v0.1.0` alongside the current documentation (`0.4`).

`.github/workflows/versioned-docs.yml` builds and deploys all versions together.
Its `CHECKMATE_DOCS_LATEST` setting selects the release tag or branch used for
the root site (`v0.3.1`, `v0.3.2`, or `0.4`), while keeping all archived
versions accessible via the version selector dropdown.
