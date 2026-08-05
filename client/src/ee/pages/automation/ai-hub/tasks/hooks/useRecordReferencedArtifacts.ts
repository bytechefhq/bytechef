import {type AiHubTabType, useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {AiHubTaskArtifactKind, useRecordReferencedAiHubTaskArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';

// Partial map: only the resource kinds that have a corresponding {@link AiHubTaskArtifactKind} on the
// server are recorded as artifacts. The newer attachment kinds (apiCollection, mcpServer, task,
// workflowExecution) are still tracked client-side in the composer's referencedResources but don't have
// a server-side artifact row yet — they'd require a new GraphQL enum value and a {@code recordReference}
// branch to support. They surface in the right panel via the tab store; the artifact list intentionally
// stays scoped to the seven kinds the agent can mutate (files, workflows, data tables, knowledge bases, skills, custom components, code workflows).
const KIND_TO_ARTIFACT_KIND: Partial<Record<AiHubTabType['kind'], AiHubTaskArtifactKind>> = {
    codeWorkflow: AiHubTaskArtifactKind.CodeWorkflowReferenced,
    customComponent: AiHubTaskArtifactKind.CustomComponentReferenced,
    dataTable: AiHubTaskArtifactKind.DataTableReferenced,
    file: AiHubTaskArtifactKind.FileReferenced,
    knowledgeBase: AiHubTaskArtifactKind.KbReferenced,
    skill: AiHubTaskArtifactKind.SkillReferenced,
    workflow: AiHubTaskArtifactKind.WorkflowReferenced,
};

/**
 * Watches the right-panel tabs store and records each open tab as a {@code ai_hub_task_artifact} for the
 * active task. Bridges the gap between the UI-only tab/reference state and the persistent artifact
 * sidebar list — without this hook, files attached via the composer plus-button OR opened by the agent via
 * tool calls would surface in the right panel but never appear in the task's artifact list.
 *
 * <p>Idempotency lives at two layers:</p>
 * <ul>
 *   <li><b>Server</b> — {@code recordReference} short-circuits when a {@code (taskId, kind, artifactId)}
 *     row already exists. Re-attaching the same file in a fresh tab hits the existing artifact row.</li>
 *   <li><b>Client</b> — a per-mount {@code seenRef} set tracks the {@code (taskId, kind, artifactId)}
 *     keys this hook has fired the mutation for, so a single tab open doesn't keep firing the mutation across
 *     re-renders. Cleared when the task switches.</li>
 * </ul>
 *
 * <p>Skips when there is no active task (home view) — the hook resumes recording as soon as a
 * task is created (which auto-fires this effect via the taskId dependency).</p>
 */
export default function useRecordReferencedArtifacts(taskId: number | undefined, workspaceId: number, enabled = true) {
    const openTabs = useAiHubTabsStore((state) => state.openTabs);
    // The tabs store mirrors which task its `openTabs` belong to. AiHub.tsx mirrors the task selection
    // into this field via a separate `setActiveTaskId` effect — but the recording effect below registers
    // BEFORE that mirror effect runs during commit, so on a task switch the recording hook sees the new
    // `taskId` prop while `openTabs` is still the previous task's snapshot. Gating on
    // `tabsActiveTaskId === taskId` skips that bleed window: the hook only records when the tabs store
    // has caught up. Once the mirror effect runs and `tabsActiveTaskId` flips to match, this effect
    // re-fires with consistent (taskId, openTabs) pair and records correctly.
    const tabsActiveTaskId = useAiHubTabsStore((state) => state.activeTaskId);

    const queryClient = useQueryClient();

    const seenRef = useRef<Set<string>>(new Set());

    const previousTaskIdRef = useRef<number | undefined>(taskId);

    const recordMutation = useRecordReferencedAiHubTaskArtifactMutation({
        onSuccess: () => {
            // The artifact list query is keyed by ['aiHubTasks', 'artifacts', taskId, workspaceId]
            // (see AiHubTasksKeys.artifacts) — match by that prefix so the invalidation works regardless
            // of the exact arg shape react-query keys by. The sidebar's artifact-list panel rerenders
            // with the new row.
            queryClient.invalidateQueries({
                predicate: (query) => {
                    const key = query.queryKey;

                    if (!Array.isArray(key) || key.length < 2) {
                        return false;
                    }

                    return key[0] === 'aiHubTasks' && key[1] === 'artifacts';
                },
            });
        },
    });

    useEffect(() => {
        // Reset the seen set on task switch — a tab that was recorded against task A should
        // be re-evaluated under task B (it might already exist on B's row, but the server's
        // idempotency check absorbs the no-op cleanly).
        if (previousTaskIdRef.current !== taskId) {
            seenRef.current = new Set();
            previousTaskIdRef.current = taskId;
        }

        if (taskId == null) {
            return;
        }

        // Recording disabled for this task kind (e.g. WORKFLOW_CHAT, which routes through a webhook and
        // never produces artifacts — its auto-opened workflow tab must NOT be recorded as one).
        if (!enabled) {
            return;
        }

        // Don't record across a stale tabs-store mirror: when the user just switched to task B but
        // the tabs store is still on task A, openTabs is A's content. Recording it under B would
        // create cross-task artifact bleed — the file shows up as an artifact of a task the user
        // never attached it to. Wait for the mirror effect to catch up; this effect re-fires when
        // `tabsActiveTaskId` changes (it's in the dep array below).
        if (tabsActiveTaskId !== taskId) {
            return;
        }

        for (const tab of openTabs) {
            const {artifactId, kind, metadata} = resolveArtifactKey(tab);

            if (artifactId == null || kind == null) {
                continue;
            }

            const dedupeKey = `${taskId}:${kind}:${artifactId}`;

            if (seenRef.current.has(dedupeKey)) {
                continue;
            }

            seenRef.current.add(dedupeKey);

            recordMutation.mutate({
                input: {
                    artifactId,
                    artifactName: tab.name,
                    kind,
                    // Workflow tabs ship the parent projectId / projectWorkflowId so the sidebar's
                    // quick-open handler can route the workflow tab (which keys on the project). For
                    // other kinds metadata is undefined and the server records a null metadata_json.
                    metadataJson: metadata ? JSON.stringify(metadata) : null,
                    taskId: String(taskId),
                    workspaceId: String(workspaceId),
                },
            });
        }
        // recordMutation is intentionally omitted from deps — its identity changes on every render via
        // react-query, which would cause the effect to re-fire infinitely. We only care about openTabs +
        // taskId + tabsActiveTaskId changes for record triggering; the mutation reference is captured
        // at call time.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [enabled, openTabs, taskId, tabsActiveTaskId, workspaceId]);
}

function resolveArtifactKey(tab: ReturnType<typeof useAiHubTabsStore.getState>['openTabs'][number]): {
    artifactId: string | null;
    kind: AiHubTaskArtifactKind | null | undefined;
    metadata?: Record<string, string | number>;
} {
    switch (tab.kind) {
        case 'codeWorkflow':
            return {artifactId: tab.projectId, kind: KIND_TO_ARTIFACT_KIND.codeWorkflow};
        case 'customComponent':
            return {artifactId: tab.customComponentId, kind: KIND_TO_ARTIFACT_KIND.customComponent};
        case 'file':
            return {artifactId: tab.fileId, kind: KIND_TO_ARTIFACT_KIND.file};
        case 'workflow':
            // The sidebar's handleArtifactQuickOpen for WORKFLOW_REFERENCED needs projectId +
            // projectWorkflowId to open the workflow tab — the workflow viewer routes by parent
            // project. Without these in metadata the sidebar row appears clickable but no-ops on
            // click. Send them alongside so they're persisted in metadata_json and round-trip back
            // through the artifact query.
            return {
                artifactId: tab.workflowId,
                kind: KIND_TO_ARTIFACT_KIND.workflow,
                metadata: {projectId: tab.projectId, projectWorkflowId: tab.projectWorkflowId},
            };
        case 'dataTable':
            return {artifactId: tab.dataTableId, kind: KIND_TO_ARTIFACT_KIND.dataTable};
        case 'knowledgeBase':
            return {artifactId: tab.knowledgeBaseId, kind: KIND_TO_ARTIFACT_KIND.knowledgeBase};
        case 'skill':
            return {artifactId: tab.skillId, kind: KIND_TO_ARTIFACT_KIND.skill};
        default:
            return {artifactId: null, kind: null};
    }
}
