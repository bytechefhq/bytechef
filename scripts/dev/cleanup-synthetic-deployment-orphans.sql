--
-- Companion DELETE for scripts/dev/diagnose-synthetic-deployment-orphans.sql.
--
-- ############################################################################
-- #  RUN THE DIAGNOSTIC FIRST AND READ ITS OUTPUT. This script deletes rows.  #
-- ############################################################################
--
-- Why this is a script and NOT a Liquibase changeset
-- -------------------------------------------------
-- A changeset would run unattended on every customer upgrade. The rows here are user-visible
-- configuration -- REST endpoints and MCP tools -- and they are selected by a join, not by a marker
-- column. If the join is wrong on some schema the operator never sees it happen, and there is nothing to
-- roll back to. The population is also small by construction: it needs an installation that shipped on
-- v0.31.4 or earlier, used API collections or MCP servers, AND deleted a workflow of a project backing
-- one. That combination does not justify an automatic delete against customer data.
--
-- The fix that stops NEW orphans is already in the application
-- (`732 Delete synthetic deployment mapping rows when a workflow is deleted`). This script is only for
-- rows stranded before that shipped.
--
-- How to run it
-- -------------
--   1. Run the diagnostic. If every count is 0, stop -- there is nothing to do.
--   2. Read sections 2 and 3: those are the endpoints and tools that will disappear. Confirm with whoever
--      owns them that they are genuinely dead and not, say, pointing at a workflow that is about to be
--      restored from a backup.
--   3. Take a backup.
--   4. Run this inside the transaction below, check the reported counts against the diagnostic, and only
--      then change ROLLBACK to COMMIT.
--
-- Multi-tenant: run once per tenant schema (`SET search_path TO tenant_000001, public;`).
--
-- Delete order is child-first and is NOT interchangeable: the foreign keys pointing at
-- project_deployment_workflow do not cascade, so removing the parent first fails.
--

BEGIN;

\echo '== deleting api_collection_endpoint rows whose workflow is gone =='
DELETE FROM api_collection_endpoint ace
USING project_deployment_workflow pdw
WHERE pdw.id = ace.project_deployment_workflow_id
  AND NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id);

\echo '== deleting mcp_project_workflow rows whose workflow is gone =='
DELETE FROM mcp_project_workflow mpw
USING project_deployment_workflow pdw
WHERE pdw.id = mpw.project_deployment_workflow_id
  AND NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id);

-- Only on schemas newer than v0.31.4. Uncomment when the diagnostic's section 4 says the table exists.
-- \echo '== deleting a2a_project_workflow rows whose workflow is gone =='
-- DELETE FROM a2a_project_workflow apw
-- USING project_deployment_workflow pdw
-- WHERE pdw.id = apw.project_deployment_workflow_id
--   AND NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id);

-- Scoped to SYNTHETIC deployments on purpose. A row on an ordinary deployment pointing at a missing
-- workflow is a different bug with a different cause, and this script must not silently absorb it.
\echo '== deleting the orphaned project_deployment_workflow rows themselves =='
DELETE FROM project_deployment_workflow pdw
USING project_deployment pd
WHERE pd.id = pdw.project_deployment_id
  AND (pd.name LIKE '__API_COLLECTION__%' OR pd.name LIKE '__MCP_SERVER__%' OR pd.name LIKE '__A2A_SERVER__%')
  AND NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id);

\echo ''
\echo '== re-running the diagnostic summary: every count below must now be 0 =='
SELECT 'api_collection_endpoint' AS table_name, count(*) AS remaining
FROM api_collection_endpoint ace
JOIN project_deployment_workflow pdw ON pdw.id = ace.project_deployment_workflow_id
WHERE NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id)
UNION ALL
SELECT 'mcp_project_workflow', count(*)
FROM mcp_project_workflow mpw
JOIN project_deployment_workflow pdw ON pdw.id = mpw.project_deployment_workflow_id
WHERE NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id)
UNION ALL
SELECT 'project_deployment_workflow', count(*)
FROM project_deployment_workflow pdw
JOIN project_deployment pd ON pd.id = pdw.project_deployment_id
WHERE (pd.name LIKE '__API_COLLECTION__%' OR pd.name LIKE '__MCP_SERVER__%' OR pd.name LIKE '__A2A_SERVER__%')
  AND NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id);

-- Change to COMMIT once the counts above are all 0 and you are satisfied with what was removed.
ROLLBACK;
