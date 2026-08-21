# AI Skill tags (sidebar filter + per-item tag management)

User request (2026-08-06): "add tags for skills like we have for all other elements, add sidebar
with tags like in DataTables, add support for add/list tags for each skill item."

Model everything on the **DataTable tags** precedent (`platform-data-table`, `DataTableTag`,
`data_table_tag` join, client `useDataTables` → sidebar filter + row TagList).

## Server (CE `server/libs/platform/platform-ai/platform-ai-skill`)

1. **Liquibase**: `ai_skill_tag` join table (`ai_skill_id` FK cascade, `tag_id` FK, PK on both) in
   the platform-ai-skill-service changelog. Check released-ness first
   (`git ls-tree -r --name-only <latest-tag> | grep ai_skill` + merge-base rule from CLAUDE.md):
   unreleased ⇒ edit init changelog in place; released ⇒ new changeset.
2. **Domain**: `AiSkillTag` (mirror `DataTableTag`: `@Table("ai_skill_tag")`, tagId column, ctor
   from `Tag`), `@MappedCollection` set on the `AiSkill` aggregate + `getTagIds()`.
3. **Service/facade**: mirror the data-table shape — list tags for workspace skills, update a
   skill's tags (create-or-reuse via platform `TagService`, same as data-table/connection).
   IMPORTANT per CLAUDE.md: add new methods to BOTH `AiSkillApiFacade` (HTTP surface, owns
   `checkOwnerOrAdmin`) AND the shared `AiSkillFacade` where relevant — controllers/GraphQL wire
   ONLY through the API facade.
4. **Surface**: whatever the Skills client page already uses (check `client/src/pages/automation/
   ai/skills` queries — GraphQL vs REST) and mirror data-table's tag operations there
   (`dataTableTags` query / `updateDataTableTags` mutation naming convention).

## Client (`client/src/pages/automation/ai/skills`)

5. Sidebar: `LeftSidebarNav` "Tags" section listing all skill tags with counts/current selection
   (copy `DataTablesFilterTitle` + `useDataTables` filter mechanics; filter by `tagId`).
6. Per-skill row: `TagList` (shared component the data-table/context-store rows use) with
   remainingTags computed from allTags, add/remove wired to the update mutation, stopPropagation
   wrapper so tag clicks don't trigger row navigation.
7. If GraphQL: add `.graphql` operations under `client/src/graphql/`, run `npx graphql-codegen`,
   commit operations and generated file separately (CLAUDE.md convention).

## Verification

- Server: module `check` + any existing `AiSkill*Test` extended for tag round-trip.
- Client: `npm run check`; live Playwright pass on /automation/ai/skills (sidebar filter +
  adding a tag on a row).
- Commits: server and client separately, `0 <desc>` / `0 client - <desc>`.
