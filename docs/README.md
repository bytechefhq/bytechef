# docs

This is a Next.js application generated with
[Create Fumadocs](https://github.com/fuma-nama/fumadocs).

Run development server:

```bash
npm run dev
# or
pnpm dev
# or
yarn dev
```

Open http://localhost:3000 with your browser to see the result.

## Learn More

To learn more about Next.js and Fumadocs, take a look at the following
resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js
  features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.
- [Fumadocs](https://fumadocs.vercel.app) - learn about Fumadocs

## API Reference pages

Everything under `content/docs/openapi/` except `index.mdx` and `meta.json` is generated from the
OpenAPI specs in the monorepo. Do not edit the generated files by hand — the next run overwrites
them.

| Group prefix | Spec |
|---|---|
| `automation-*` | `server/ee/libs/automation/automation-configuration/automation-configuration-public-rest/openapi.yaml` |
| `embedded-action`, `embedded-tool*`, `embedded-workflow-execution` | `server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest/openapi.yaml` |
| `embedded-configuration-*` | `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml` |
| `embedded-webhook-*` | `server/ee/libs/embedded/embedded-webhook/embedded-webhook-public-rest/openapi.yaml` |
| `custom-components` | `server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-rest/openapi.yaml` |

After editing a spec, regenerate from this directory:

```bash
npm run generate:openapi
```

It runs on Node (`node --experimental-strip-types`), so `npm install` is the only setup needed.

### How pages are grouped

One group per **OpenAPI tag**, with the group id `<schemaId>-<tag>` — so the sidebar mirrors the
grouping the specs declare, and retagging an operation moves it without touching the script. A spec
declaring a single tag stays one group under its own id (`custom-components`, not
`custom-components-custom-component`).

The id is prefixed with the schema id because tag names repeat across specs: `workflow-execution`,
`app-event-trigger` and `request-trigger` each appear in two. fumadocs' built-in `groupBy: 'tag'`
keys on the bare tag and would merge those, which is why `scripts/generate-openapi.mts` parses the
specs and computes the ids itself.

Adding a tag needs a matching `GROUP_META` entry in that script — title, optional `navTitle` for the
sidebar, and description. Two guards fail the build rather than emitting something wrong: an
operation carrying no tag cannot be placed, and a group with no `GROUP_META` entry would generate
with an empty title. Both name the offender.

`GROUP_META` also carries `comingSoon` for a whole group and `comingSoonOperations` for individual
operations, which stamp the frontmatter flag that renders the *Coming soon* badge and callout. An
operation id listed there that the spec no longer emits fails the build too.

### Hand-written files

`content/docs/openapi/index.mdx` and `content/docs/openapi/meta.json` are written by hand and left
alone by the generator. `meta.json` sets the sidebar order and the `--- Automation ---` /
`--- Embedded ---` / `--- Platform ---` section headings, so a new group has to be added there to
appear.
