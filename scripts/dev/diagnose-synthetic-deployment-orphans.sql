--
-- Diagnostic for the orphan class that `732 Delete synthetic deployment mapping rows when a workflow is
-- deleted` fixed. READ ONLY: this script SELECTs and nothing else. Run it before considering any cleanup.
--
-- What went wrong, and why it is silent
-- ------------------------------------
-- Deleting a workflow of a project that also backed an MCP server or an API collection used to leave the
-- SYNTHETIC deployment's `project_deployment_workflow` row alive, together with the
-- `mcp_project_workflow` / `api_collection_endpoint` row pointing at it. `ProjectWorkflowFacadeImpl`
-- enumerated deployments through the display-filtered query, which deliberately excludes those
-- deployments, and `project_deployment_workflow.workflow_id` carries NO foreign key -- so nothing
-- complained. The visible symptom is a stale MCP tool or REST endpoint pointing at a workflow that no
-- longer exists.
--
-- Only the WORKFLOW-delete path produced orphans. The sibling project-delete bug failed loudly with a
-- foreign key violation on `fk_project_deployment_project`, so it left nothing behind to clean up.
--
-- Scope
-- -----
-- `api_collection`, `api_collection_endpoint`, `mcp_project`, `mcp_project_workflow` and
-- `project_deployment_workflow` all shipped in v0.31.4, so a database upgraded from that release can hold
-- these rows. `a2a_project_workflow` did NOT ship and is guarded with `to_regclass` below, so this script
-- runs unchanged against either schema.
--
-- Multi-tenant note: ByteChef puts each tenant in its own schema. Run this once per tenant schema, e.g.
--   SET search_path TO tenant_000001, public;
-- and repeat, or wrap it in a loop over the tenant schemas.
--

\echo '== 1. Summary: orphaned project_deployment_workflow rows on synthetic deployments =='

SELECT
    CASE
        WHEN pd.name LIKE '__API_COLLECTION__%' THEN 'API collection'
        WHEN pd.name LIKE '__MCP_SERVER__%' THEN 'MCP server'
        WHEN pd.name LIKE '__A2A_SERVER__%' THEN 'A2A server'
    END                                                          AS owning_surface,
    count(*)                                                     AS orphaned_rows
FROM project_deployment_workflow pdw
JOIN project_deployment pd ON pd.id = pdw.project_deployment_id
WHERE (pd.name LIKE '__API_COLLECTION__%' OR pd.name LIKE '__MCP_SERVER__%' OR pd.name LIKE '__A2A_SERVER__%')
  AND NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id)
GROUP BY 1
ORDER BY 1;

\echo ''
\echo '== 2. The user-visible half: API collection endpoints serving a deleted workflow =='

SELECT
    ac.id                        AS api_collection_id,
    ac.name                      AS api_collection_name,
    ace.id                       AS endpoint_id,
    ace.path                     AS endpoint_path,
    pdw.workflow_id              AS missing_workflow_id
FROM api_collection_endpoint ace
JOIN project_deployment_workflow pdw ON pdw.id = ace.project_deployment_workflow_id
JOIN api_collection ac ON ac.id = ace.api_collection_id
WHERE NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id)
ORDER BY ac.id, ace.id;

\echo ''
\echo '== 3. The user-visible half: MCP tools pointing at a deleted workflow =='

SELECT
    ms.id                        AS mcp_server_id,
    ms.name                      AS mcp_server_name,
    mpw.id                       AS mcp_project_workflow_id,
    pdw.workflow_id              AS missing_workflow_id
FROM mcp_project_workflow mpw
JOIN project_deployment_workflow pdw ON pdw.id = mpw.project_deployment_workflow_id
JOIN mcp_project mp ON mp.id = mpw.mcp_project_id
JOIN mcp_server ms ON ms.id = mp.mcp_server_id
WHERE NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id)
ORDER BY ms.id, mpw.id;

\echo ''
\echo '== 4. A2A servers (only present on schemas newer than v0.31.4) =='

SELECT
    CASE
        WHEN to_regclass('a2a_project_workflow') IS NULL
            THEN 'a2a_project_workflow does not exist in this schema - nothing to check'
        ELSE 'a2a_project_workflow exists - see the query in the comment below'
    END AS a2a_status;

-- Run this only when section 4 reports the table exists:
--
-- SELECT s.id AS a2a_server_id, s.name AS a2a_server_name, apw.id AS a2a_project_workflow_id,
--        pdw.workflow_id AS missing_workflow_id
-- FROM a2a_project_workflow apw
-- JOIN project_deployment_workflow pdw ON pdw.id = apw.project_deployment_workflow_id
-- JOIN a2a_project ap ON ap.id = apw.a2a_project_id
-- JOIN a2a_server s ON s.id = ap.a2a_server_id
-- WHERE NOT EXISTS (SELECT 1 FROM workflow w WHERE w.id = pdw.workflow_id)
-- ORDER BY s.id, apw.id;

\echo ''
\echo '== 5. Rows a cleanup would have to delete, child-first =='
\echo '   (delete order matters: the foreign keys into project_deployment_workflow do NOT cascade)'

SELECT 'api_collection_endpoint' AS table_name, count(*) AS rows_to_delete
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
