#!/usr/bin/env bash
# Re-syncs the local dev database after workspace_<entity> relation tables are collapsed
# into a nullable workspace_id column (see
# docs/superpowers/specs/2026-07-25-workspace-relation-table-convention-revision.md).
#
# The collapse edits Liquibase init changelogs IN PLACE, which breaks a local DB two ways:
#   1. schema drift  - the entity lacks workspace_id and the relation table still exists
#   2. stale md5sums - Liquibase refuses to start when a changelog no longer matches its record
#
# This script fixes both, idempotently. It only ever ADDS a nullable column and DROPS a
# relation table that the changelogs no longer create, so it is safe to re-run.
#
# Usage: scripts/dev/sync-local-schema-after-collapse.sh [container] [database]
set -euo pipefail

CONTAINER="${1:-server-postgres-1}"
DATABASE="${2:-bytechef}"

# Relation tables the collapse removes, paired with the entity that gains workspace_id.
# Add a line per table as each one is collapsed.
PAIRS="
workspace_ai_auto_memory:ai_auto_memory
workspace_context_store:context_store
workspace_context_store_source:context_store_source
workspace_knowledge_base_source:knowledge_base_source
workspace_ai_hub_personal_agent:ai_hub_personal_agent
workspace_ai_hub_task:ai_hub_task
workspace_ai_gateway_budget:ai_gateway_budget
workspace_ai_gateway_project:ai_gateway_project
workspace_ai_gateway_provider:ai_gateway_provider
workspace_ai_gateway_rate_limit:ai_gateway_rate_limit
workspace_ai_gateway_routing_policy:ai_gateway_routing_policy
workspace_ai_gateway_spend_summary:ai_gateway_spend_summary
workspace_ai_observability_alert_rule:ai_observability_alert_rule
workspace_ai_observability_export_job:ai_observability_export_job
workspace_ai_observability_session:ai_observability_session
workspace_ai_observability_trace:ai_observability_trace
workspace_ai_observability_webhook_subscription:ai_observability_webhook_subscription
workspace_notification:notification
workspace_ai_eval_dataset:ai_eval_dataset
workspace_ai_eval_experiment:ai_eval_experiment
workspace_ai_eval_rule:ai_eval_rule
workspace_ai_eval_score:ai_eval_score
workspace_ai_eval_score_config:ai_eval_score_config
workspace_ai_llm_usage:ai_llm_usage
workspace_ai_tool_usage:ai_tool_usage
workspace_ai_prompt:ai_prompt
workspace_workflow_alert_rule:workflow_alert_rule
workspace_workflow_execution_cost:workflow_execution_cost
"

SQL="BEGIN;"

while read -r pair; do
    [ -z "$pair" ] && continue

    relation="${pair%%:*}"
    entity="${pair##*:}"

    # Only act when the entity table actually exists locally - modules differ per deployment.
    SQL="$SQL
DO \$\$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '$entity') THEN
        EXECUTE 'ALTER TABLE $entity ADD COLUMN IF NOT EXISTS workspace_id BIGINT';

        -- Carry membership across before dropping it, so local data survives the collapse.
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '$relation') THEN
            BEGIN
                EXECUTE 'UPDATE $entity e SET workspace_id = r.workspace_id
                           FROM $relation r WHERE r.${entity}_id = e.id AND e.workspace_id IS NULL';
            EXCEPTION WHEN undefined_column THEN
                RAISE NOTICE 'skipped backfill for $entity (relation column is not ${entity}_id)';
            END;

            EXECUTE 'DROP TABLE $relation CASCADE';
        END IF;
    END IF;
END
\$\$;"
done <<< "$PAIRS"

# Init changelogs were edited in place, so recorded checksums no longer match. Nulling md5sum
# makes Liquibase recompute on next run; it does NOT re-run changesets (they stay marked executed).
SQL="$SQL
UPDATE databasechangelog SET md5sum = NULL;
COMMIT;"

echo "$SQL" | docker exec -i "$CONTAINER" psql -U postgres -d "$DATABASE" -v ON_ERROR_STOP=1

echo "Local schema re-synced. Remaining workspace_* relation tables:"
docker exec "$CONTAINER" psql -U postgres -d "$DATABASE" -tAc \
    "SELECT table_name FROM information_schema.tables WHERE table_name LIKE 'workspace!_%' ESCAPE '!' ORDER BY 1;"
