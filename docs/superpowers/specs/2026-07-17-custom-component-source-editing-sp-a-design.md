# Custom Component Source Editing + Create-Empty (SP-A) — Design

**Date:** 2026-07-17
**Status:** Approved (design)
**Sub-project:** SP-A of the Custom Components initiative (SP-B = AI Hub tools + open-in-panel; SP-C = copilot build subagent — separate specs later).
**Area:** `server/ee/libs/platform/platform-custom-component/**`, `client/src/ee/pages/settings/platform/custom-components/**`

## Problem

Custom components (JS/Python/Ruby single-file scripts, or Java jars) can today only be **uploaded**
(multipart), **enabled/disabled**, and **deleted**. There is no way to view or edit a component's
source, and no way to create a new one from scratch — you must author a valid file offline and
upload it. This sub-project adds an in-app **details page with a Monaco editor** for the three
non-Java languages, plus a **create-empty** flow, on the existing single-file storage model.

## Scope decisions (from brainstorming)

- **Single-file model** — a custom component is one source file (`.js`/`.py`/`.rb`), matching
  today's storage (`CustomComponent.component: FileEntry`). No multi-file / no engine changes.
- **Compile-gate save** — saving edited source re-evals it via `ComponentHandlerLoader`; if it fails
  to load (or is missing required fields), the save is **rejected** with the eval error and the
  last-good version is kept. A stored component is therefore always loadable.
- **Detail route for non-Java; Java read-only** — clicking a JS/Python/Ruby component navigates to a
  detail route with an editable Monaco editor. Java components keep today's read-only behavior (a
  jar has no editable source).
- **Create-empty for JavaScript** (SP-A) — seeded from an authored minimal-valid JS starter
  template. The **edit page covers all non-Java languages** (JS/Python/Ruby) since editing is
  language-agnostic text validated by the compile-gate; only *create-empty* is JS-first.
  **Python/Ruby create-empty are deferred**: the GraalVM polyglot member-access shape those templates
  must satisfy (a value whose `getMember("name")` etc. resolve, with a callable `perform`) is
  unverified and has **no precedent anywhere in the repo** — authoring them blind would risk shipping
  a template that fails its own compile-gate. They become a fast-follow once the shape is verified.
  (Java create-empty is out of scope entirely; AI-assisted create is SP-C.)

## Non-goals (this sub-project)

- Multi-file components; any polyglot-engine change.
- Renaming a component via source edit (the `name` field is the workflow-facing identity — see below).
- AI Hub tools / open-in-panel (SP-B) and the copilot build subagent (SP-C).
- Java source editing.

## Server design (`platform-custom-component`)

### File storage (`CustomComponentFileStorage` + impl)
Add one read method to the existing interface (which today has only
`storeCustomComponentFile`, `getCustomComponentFileURL`, `deleteCustomComponentFile`):
- `String readCustomComponentFileContent(FileEntry componentFile)` — returns the single source
  file's text (UTF-8). Update = overwrite via the existing `storeCustomComponentFile`.

### Facade (`CustomComponentFacade` + impl)
Add three methods (all admin-gated, consistent with `save`/`delete`):
- `String getCustomComponentSource(long id)` — the component's source text. Throws (or returns a
  clear error) for a Java component (no editable source).
- `void updateCustomComponentSource(long id, String content)` — the **compile-gate**:
  1. Load the existing `CustomComponent` by id; reject if its language is `JAVA`.
  2. Write `content` to a temp file with the component's `name`+extension and load it via
     `ComponentHandlerLoader.loadComponentHandler(url, language, ...)` (the same path `save` uses) to
     produce a `ComponentDefinition`.
  3. **If load fails → throw** (surface the eval error; nothing is written).
  4. **If the loaded definition's `name` != the existing component's `name` → throw** ("renaming a
     component by editing its source is not supported"). Name is the identity workflows bind to.
  5. Otherwise overwrite the stored file (`storeCustomComponentFile`) and refresh the mutable
     metadata (title, description, icon, componentVersion) from the reloaded definition.
- `long createEmptyCustomComponent(String name, CustomComponent.Language language)` — generate the
  authored starter template for `language` with the component `name` substituted, then run it
  through the same validate-and-store path as `save` (the template is valid, so it passes the gate),
  and return the new component's id. Reject `JAVA`. Reject a name that collides with an existing
  component.

The **JavaScript** starter template lives as a classpath resource in the configuration-service
module: a minimal valid component whose evaluated value exposes `name`, `title`, `version` (1),
`description`, and one no-op `actions` entry (`{name, title, description, perform}`), matching the
`ComponentHandlerPolyglotEngine` `getMember` contract. `createEmptyCustomComponent` keeps a
`language` parameter for forward-compatibility, but SP-A only ships the JS template and only offers
JavaScript in the create dialog; passing `PYTHON`/`RUBY` is rejected until their templates are
authored + verified.

### GraphQL (`custom-component.graphqls` + controller)
Add to the existing schema (which has `customComponent`/`customComponentDefinition`/
`customComponents` queries and `deleteCustomComponent`/`enableCustomComponent` mutations):
- Query `customComponentSource(id: ID!): String!`
- Mutation `updateCustomComponentSource(id: ID!, content: String!): Boolean!` — returns true on
  success; a rejected (non-loadable / renamed) save surfaces as a GraphQL error carrying the eval
  message so the client can show it inline.
- Mutation `createCustomComponent(name: String!, language: CustomComponentLanguage!): CustomComponent!`
  — returns the created component (so the client can navigate to its detail route).

## Client design (`settings/platform/custom-components`)

Today `CustomComponents.tsx` is a list of expandable rows (`CustomComponentListItem` lazily loads
the definition on expand) with an upload dialog, enable toggle, and delete.

- **Detail route** — add a route (e.g. `/settings/platform/custom-components/:id`) rendering:
  - non-Java: a header (name/title/language) + an editable `MonacoEditorWrapper`
    (`client/src/shared/components/MonacoEditorWrapper.tsx`, language derived from the component)
    loaded from `customComponentSource`, with a **Save** action calling `updateCustomComponentSource`.
    On a rejected save, show the returned eval error inline (toast + editor banner) and keep the
    buffer dirty.
  - Java: read-only metadata + actions/triggers (reuse the existing definition view); no editor.
- **List navigation** — clicking a **non-Java** row navigates to its detail route; **Java** rows keep
  today's expand behavior. (Enable toggle + delete remain on the list.)
- **Create dialog** — a "New component" dialog: pick a language (**JavaScript only** in SP-A) + enter
  a name → `createCustomComponent` → navigate to the new component's detail route to edit. (Distinct
  from the existing "Upload" dialog, which stays.)
- **GraphQL ops** — add `.graphql` operation files under `client/src/graphql/platform/custom-component/`
  for the new query/mutations and regenerate the client types.

## Data flow (edit + save)

1. Detail route loads → `customComponentSource(id)` → Monaco buffer.
2. User edits → **Save** → `updateCustomComponentSource(id, content)`.
3. Server re-evals: fails → GraphQL error with the eval message → editor shows it, buffer stays
   dirty. Succeeds → file overwritten, metadata refreshed, editor marked clean.

## Data flow (create-empty)

1. "New component" dialog → language + name → `createCustomComponent(name, language)`.
2. Server writes the starter template, validates (always passes), persists → returns the component.
3. Client navigates to `/settings/platform/custom-components/:id` to edit.

## Error handling

- Non-loadable / renamed / Java-source edits → server throws; the mutation surfaces the message; the
  client shows it inline without losing the buffer.
- Duplicate name on create → clear error in the dialog.

## Testing

- **Server**: `CustomComponentFacade` unit/integration — `getCustomComponentSource` (non-Java returns
  text; Java rejected); `updateCustomComponentSource` (valid content persists + refreshes metadata;
  invalid content throws and does NOT overwrite; name-change throws); `createEmptyCustomComponent`
  (each non-Java language produces a component that loads; duplicate-name rejected; Java rejected).
  GraphQL controller tests for the new query/mutations.
- **Client**: detail route renders the editor for non-Java and read-only for Java; Save success/reject
  paths; create dialog → navigate. Follow the existing custom-components test patterns.

## Rollout / compatibility

- Additive: new storage read method, new facade methods, new GraphQL query/mutations, a new client
  route + dialog. Existing upload/enable/delete/list untouched.
- Starter templates are new classpath resources; no migration.
