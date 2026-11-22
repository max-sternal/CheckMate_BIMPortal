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
Starlight Versions. Archived `v0.3.1` pages were created from its release
snapshot; `v0.1.0` and `v0.2.0` are retrospective pages, not files from the
historical tags.

`.github/workflows/versioned-docs.yml` builds and deploys all versions together.
Its `CHECKMATE_DOCS_LATEST` setting selects the release tag used for the root
site, currently `v0.3.1`; publishing from the `0.3.2` branch does not make its
draft documentation current. To make `0.3.2` current, first create and push its
release tag, then change `CHECKMATE_DOCS_LATEST` to `v0.3.2` in the workflow and
push the change. The archived `v0.3.1` remains available.

The old `.github/workflows/docs.yml` workflow must remain disabled: it deploys a
single unversioned site and could overwrite the versioned deployment.
