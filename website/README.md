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

The `0.3.2` documentation is prepared on that branch. The workflow currently
deploys on pushes to `main` and `0.3.1`, not `0.3.2`. To publish `0.3.2`, change
the workflow's push branch filter on `0.3.2` to `0.3.2` and push that change.
Each branch runs its own workflow file: future matching pushes to `0.3.1` could
still overwrite the site. Update or disable that branch's trigger separately
when switching. Check the GitHub Pages environment and deployment permissions.
