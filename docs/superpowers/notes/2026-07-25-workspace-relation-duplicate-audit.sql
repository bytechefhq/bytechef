-- Duplicate-membership audit for the workspace_<entity> relation tables that REMAIN.
--
-- Context: docs/superpowers/specs/2026-07-25-workspace-relation-table-convention-revision.md
--
-- 28 unreleased relation tables were collapsed into a nullable workspace_id column on their
-- entities. These five were NOT collapsed: they are present in release v0.31.2, so collapsing
-- them would mean data migrations against customer data for no user-facing benefit.
--
-- They therefore keep an invariant the schema does not enforce. Every one of them declares only
-- the composite UNIQUE (workspace_id, <entity>_id), never UNIQUE (<entity>_id) alone — so the
-- database permits an entity to belong to two workspaces, while the code assumes it never does
-- (each relation row is written at exactly one site, one row per entity).
--
-- ANY ROW RETURNED BY THIS SCRIPT IS A LIVE BUG: that entity is currently reachable from two
-- workspaces with no code path that intended it.
--
-- This script is READ-ONLY and safe to run against production.
--
-- Acting on the results — adding UNIQUE (<entity>_id) — is a SEPARATE decision and explicitly out
-- of scope here. Do not add those constraints blind: on production data the migration FAILS if
-- duplicates already exist, and a failed migration on customer data is worse than the gap it
-- closes. Run this first; let the output decide.
--
-- workspace_user is deliberately EXCLUDED: it is the one genuinely many-to-many relation (a user
-- really does belong to several workspaces), so duplicates there are correct by design.

SELECT 'workspace_api_key' AS relation, api_key_id AS entity_id, COUNT(*) AS memberships
FROM workspace_api_key
GROUP BY api_key_id
HAVING COUNT(*) > 1

UNION ALL

SELECT 'workspace_connection', connection_id, COUNT(*)
FROM workspace_connection
GROUP BY connection_id
HAVING COUNT(*) > 1

UNION ALL

SELECT 'workspace_data_table', data_table_id, COUNT(*)
FROM workspace_data_table
GROUP BY data_table_id
HAVING COUNT(*) > 1

UNION ALL

SELECT 'workspace_knowledge_base', knowledge_base_id, COUNT(*)
FROM workspace_knowledge_base
GROUP BY knowledge_base_id
HAVING COUNT(*) > 1

UNION ALL

SELECT 'workspace_mcp_server', mcp_server_id, COUNT(*)
FROM workspace_mcp_server
GROUP BY mcp_server_id
HAVING COUNT(*) > 1

ORDER BY relation, entity_id;
