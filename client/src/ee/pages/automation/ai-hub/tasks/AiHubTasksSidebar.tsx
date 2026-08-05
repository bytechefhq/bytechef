import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {probeInFlightStatus} from '@/ee/pages/automation/ai-hub/runtime-providers/inFlightRunClient';
import {
    aiHubRunStateStore,
    isTaskRunning,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useLiveWorkflowLabel} from '@/ee/pages/automation/ai-hub/tasks/hooks/useLiveWorkflowLabel';
import {useSwitchTask} from '@/ee/pages/automation/ai-hub/tasks/hooks/useSwitchTask';
import {
    useAiHubTaskArtifactsQuery,
    useAiHubTasksQuery,
    useDeleteAiHubTaskMutation,
    usePatchAiHubTaskMutation,
} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {aiHubTasksStore, useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {
    useCancelAiHubRunMutation,
    useCancelWorkflowChatTurnMutation,
    useDeleteAiHubTaskArtifactMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ArchiveIcon,
    ArchiveRestoreIcon,
    BlocksIcon,
    BotIcon,
    BrainIcon,
    ChevronDownIcon,
    ChevronLeftIcon,
    ChevronRightIcon,
    CodeIcon,
    DatabaseIcon,
    FileTextIcon,
    HexagonIcon,
    ImageIcon,
    LinkIcon,
    Loader2Icon,
    MessageCircleQuestionIcon,
    MessageSquareIcon,
    MessageSquarePlusIcon,
    MoreVerticalIcon,
    PencilIcon,
    PlayIcon,
    RotateCcwIcon,
    Trash2Icon,
    WorkflowIcon,
    WrenchIcon,
    XIcon,
} from 'lucide-react';
import {type MouseEvent, useEffect, useMemo, useRef, useState} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import {AiHubArtifactKindType, AiHubTaskArtifactI, AiHubTaskI, generateAiHubTaskTitle} from './api/tasks.api';

export const TASKS_PAGE_SIZE = 25;

export function getTasksPage<T>(tasks: T[], visibleCount: number): {hiddenCount: number; visibleTasks: T[]} {
    return {
        hiddenCount: Math.max(0, tasks.length - visibleCount),
        visibleTasks: tasks.slice(0, visibleCount),
    };
}

/*
 * Single continuous column mirroring the settled task list (flat rows, `gap-0.5`) — the list has no
 * visual grouping, so a grouped skeleton would read as two separate loading boxes on every reload.
 */
const TasksSidebarSkeleton = () => (
    <div className="flex flex-col gap-0.5 px-2">
        {Array.from({length: 8}).map((_, itemIndex) => (
            <Skeleton className="h-8 w-full" key={itemIndex} />
        ))}
    </div>
);

function getArtifactIcon(kind: AiHubArtifactKindType) {
    if (kind === 'FILE_CREATED' || kind === 'FILE_UPDATED' || kind === 'FILE_REFERENCED') {
        return <FileTextIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'BINARY_FILE_CREATED') {
        return <ImageIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'WORKFLOW_EXECUTION_STARTED') {
        return <PlayIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'WORKFLOW_CREATED' || kind === 'WORKFLOW_UPDATED' || kind === 'WORKFLOW_REFERENCED') {
        return <WorkflowIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (
        kind === 'DATA_TABLE_ROW_ADDED' ||
        kind === 'DATA_TABLE_ROW_UPDATED' ||
        kind === 'DATA_TABLE_ROW_DELETED' ||
        kind === 'DATA_TABLE_COLUMN_ADDED' ||
        kind === 'DATA_TABLE_REFERENCED'
    ) {
        return <DatabaseIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'KB_DOCUMENT_ADDED' || kind === 'KB_DOCUMENT_DELETED' || kind === 'KB_REFERENCED') {
        return <BrainIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'SKILL_REFERENCED') {
        return <HexagonIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'CUSTOM_COMPONENT_REFERENCED') {
        return <BlocksIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'CODE_WORKFLOW_REFERENCED') {
        return <CodeIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    return <WrenchIcon className="size-3.5 shrink-0 text-muted-foreground" />;
}

function parseMetadataJson(metadataJson: string | null): Record<string, string> {
    if (!metadataJson) {
        return {};
    }

    try {
        return JSON.parse(metadataJson) as Record<string, string>;
    } catch (parseError) {
        // A server-side serialization bug would otherwise make every artifact non-clickable with no diagnostic
        // trail. Surface a console warning so devtools makes the failure visible; we still fall back to {} to keep
        // the rest of the sidebar rendering.
        console.warn('[AiHubTasksSidebar] Failed to parse artifact metadataJson; quick-open will be disabled', {
            parseError,
            raw: metadataJson,
        });

        return {};
    }
}

/*
 * Quick-open affordance:
 * - FILE_CREATED / BINARY_FILE_CREATED → opens file tab
 * - WORKFLOW_EXECUTION_STARTED → opens the execution as a right-panel tab
 * - WORKFLOW_CREATED / WORKFLOW_UPDATED → opens workflow tab if metadataJson.projectId is present;
 *   otherwise no quick-open (projectId needed for tab routing)
 * - DATA_TABLE_* → opens data table tab using metadataJson.dataTableId if present;
 *   DATA_TABLE_ROW_* artifactId is a row id which doesn't open cleanly, so we fall back to metadataJson
 * - KB_DOCUMENT_* → opens knowledge-base tab using metadataJson.knowledgeBaseId if present
 * - SKILL_REFERENCED → opens skill tab using artifactId directly (the artifact is the skill itself,
 *   same shape as DATA_TABLE_REFERENCED / KB_REFERENCED)
 * - CUSTOM_COMPONENT_REFERENCED → opens custom-component tab using artifactId directly, same shape as
 *   SKILL_REFERENCED
 * - CODE_WORKFLOW_REFERENCED → artifactId IS the projectId (same shape as CUSTOM_COMPONENT_REFERENCED),
 *   but openCodeWorkflowTab also needs a `language`, which the artifact row doesn't carry (the recorder
 *   only stashes artifactId + name). We fetch the project via ProjectApi().getProject on click and read
 *   its `codeWorkflowLanguage`; if the fetch fails or the project is no longer code-backed, we surface a
 *   toast instead of opening a tab.
 *
 * Limitation: if metadataJson doesn't carry the parent entity id (projectId / dataTableId /
 * knowledgeBaseId), the artifact row is rendered as non-clickable (icon + name + timestamp only).
 */
/**
 * Cancel a task's in-flight run when it is deleted mid-stream, so the server stops producing events
 * instead of leaving the run in InFlightAiHubRunRegistry until its TTL (where it keeps consuming model
 * tokens for a task the user just removed). Mirrors {@link AiHubChatComposer}'s handleCancelTurn but is
 * keyed off the deleted task's thread id, because a delete can target a background (non-focused) task.
 *
 * "Streaming" spans two signals: the focused task sets runningByTask via RUN_STARTED, while a background
 * task's visible pulse comes from the probe-driven 'running' activity state. Both are covered. The server
 * cancel is idempotent, so a false positive is harmless. No-op when the task isn't streaming.
 */
export function cancelTaskRunIfStreaming(
    task: Pick<AiHubTaskI, 'id' | 'kind' | 'threadId'>,
    workspaceId: number | undefined,
    cancel: {
        cancelAiHubRun: (variables: {id: string; runId: string | undefined; workspaceId: string}) => void;
        cancelWorkflowChatTurn: (variables: {id: string; workspaceId: string}) => void;
    }
): void {
    const runState = aiHubRunStateStore.getState();

    const isStreaming =
        isTaskRunning(runState, task.threadId) || aiHubTasksStore.getState().taskActivity[task.threadId] === 'running';

    if (!isStreaming || workspaceId == null) {
        return;
    }

    if (task.kind === 'WORKFLOW_CHAT') {
        cancel.cancelWorkflowChatTurn({id: String(task.id), workspaceId: String(workspaceId)});
    } else {
        cancel.cancelAiHubRun({
            id: String(task.id),
            runId: runState.runIdByTask[task.threadId],
            workspaceId: String(workspaceId),
        });
    }

    runState.setTaskRunning(task.threadId, false);

    aiHubTasksStore.getState().clearActivityState(task.threadId);
}

export interface ProbedTaskActivityDecisionI {
    clearActivity?: boolean;
    clearRunningFlag?: boolean;
    setActivityRunning?: boolean;
}

/**
 * Pure reconciliation for one task row in the sidebar's in-flight probe. Returns which store mutations to apply.
 *
 * The FOCUSED task's activity is owned by the runtime's own lifecycle hooks (onNew / onRunFinished / onCancel):
 * the probe must neither SET nor CLEAR it out from under them. In particular, NOT re-setting 'running' for the
 * focused thread is what fixes the stale-spinner-after-Stop bug — after the user cancels, the async server-cancel
 * hasn't landed yet, so a probe firing in that window still reports in-flight and would otherwise re-pulse the dot
 * the cancel just cleared. Background tasks have no runtime hooks, so the probe is authoritative for them.
 */
export function reconcileProbedTaskActivity({
    currentActivity,
    isFocused,
    isInFlight,
    isRunningByTask,
}: {
    currentActivity: string | undefined;
    isFocused: boolean;
    isInFlight: boolean;
    isRunningByTask: boolean;
}): ProbedTaskActivityDecisionI {
    if (isInFlight) {
        if (currentActivity == null && !isFocused) {
            return {setActivityRunning: true};
        }

        return {};
    }

    if (currentActivity === 'paused') {
        return {};
    }

    if (isFocused) {
        if (currentActivity === 'running' && !isRunningByTask) {
            return {clearActivity: true};
        }

        return {};
    }

    const decision: ProbedTaskActivityDecisionI = {};

    if (isRunningByTask) {
        decision.clearRunningFlag = true;
    }

    if (currentActivity === 'running') {
        decision.clearActivity = true;
    }

    return decision;
}

/**
 * CODE_WORKFLOW_REFERENCED's artifact row only carries artifactId (= projectId) + name — the language
 * `openCodeWorkflowTab` needs was never stashed on the artifact (see {@link OpenCodeWorkflowTabToolCallback}
 * on the server, which only records projectId + name). Fetch the project and read its
 * `codeWorkflowLanguage` (added alongside code workflows) rather than inventing a value. A missing
 * language means the project is no longer code-backed (e.g. converted back to a visual workflow) — surface
 * that as a toast instead of opening a tab with a bogus language.
 */
async function openCodeWorkflowArtifact(artifact: AiHubTaskArtifactI): Promise<void> {
    try {
        const project = await new ProjectApi().getProject({id: Number(artifact.artifactId)});

        if (!project.codeWorkflowLanguage) {
            toast.error(`"${artifact.artifactName}" is no longer a code workflow.`);

            return;
        }

        aiHubTabsStore
            .getState()
            .openCodeWorkflowTab(artifact.artifactId, project.codeWorkflowLanguage, artifact.artifactName);
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);

        toast.error(`Failed to open "${artifact.artifactName}": ${message}`);
    }
}

export async function handleArtifactQuickOpen(artifact: AiHubTaskArtifactI): Promise<void> {
    const metadata = parseMetadataJson(artifact.metadataJson);

    // FILE_REFERENCED carries the same artifactId shape as FILE_CREATED (asset_file id), so a single
    // openFileTab call covers both quick-open paths. Same applies to the *_REFERENCED variants below for
    // workflow / data-table / knowledge-base — they were missing here, so the rows in the sidebar artifact
    // list looked clickable-shaped (wrench icon, hover) but had no handler firing on click.
    if (
        artifact.kind === 'FILE_CREATED' ||
        artifact.kind === 'FILE_UPDATED' ||
        artifact.kind === 'BINARY_FILE_CREATED' ||
        artifact.kind === 'FILE_REFERENCED'
    ) {
        aiHubTabsStore.getState().openFileTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (artifact.kind === 'WORKFLOW_EXECUTION_STARTED') {
        // Opens the execution as a right-panel tab instead of a new browser tab. artifactId is the
        // workflow execution id; openWorkflowExecutionTab takes a number.
        aiHubTabsStore.getState().openWorkflowExecutionTab(Number(artifact.artifactId), artifact.artifactName);

        return;
    }

    if (
        artifact.kind === 'WORKFLOW_CREATED' ||
        artifact.kind === 'WORKFLOW_UPDATED' ||
        artifact.kind === 'WORKFLOW_REFERENCED'
    ) {
        const projectId = metadata['projectId'];
        const projectWorkflowId = Number(metadata['projectWorkflowId'] ?? 0);

        if (projectId) {
            aiHubTabsStore
                .getState()
                .openWorkflowTab(artifact.artifactId, projectId, projectWorkflowId, artifact.artifactName);
        }

        return;
    }

    if (
        artifact.kind === 'DATA_TABLE_ROW_ADDED' ||
        artifact.kind === 'DATA_TABLE_ROW_UPDATED' ||
        artifact.kind === 'DATA_TABLE_ROW_DELETED' ||
        artifact.kind === 'DATA_TABLE_COLUMN_ADDED' ||
        artifact.kind === 'DATA_TABLE_REFERENCED'
    ) {
        // DATA_TABLE_REFERENCED stores the dataTableId directly as artifactId (the artifact is the table
        // itself). The DATA_TABLE_ROW_* and DATA_TABLE_COLUMN_ADDED variants store a row id / column id as
        // artifactId and stash the parent dataTableId in metadata — fall back to artifactId for the
        // referenced case.
        const dataTableId = metadata['dataTableId'] ?? artifact.artifactId;

        if (dataTableId) {
            aiHubTabsStore.getState().openDataTableTab(dataTableId, metadata['name'] ?? artifact.artifactName);
        }

        return;
    }

    if (
        artifact.kind === 'KB_DOCUMENT_ADDED' ||
        artifact.kind === 'KB_DOCUMENT_DELETED' ||
        artifact.kind === 'KB_REFERENCED'
    ) {
        // KB_REFERENCED stores the knowledgeBaseId directly as artifactId. Document-add/delete variants stash
        // it in metadata — same fallback logic as the data-table case above.
        const knowledgeBaseId = metadata['knowledgeBaseId'] ?? artifact.artifactId;

        if (knowledgeBaseId) {
            aiHubTabsStore.getState().openKnowledgeBaseTab(knowledgeBaseId, metadata['name'] ?? artifact.artifactName);
        }

        return;
    }

    if (artifact.kind === 'SKILL_REFERENCED') {
        aiHubTabsStore.getState().openSkillTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (artifact.kind === 'CUSTOM_COMPONENT_REFERENCED') {
        aiHubTabsStore.getState().openCustomComponentTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (artifact.kind === 'CODE_WORKFLOW_REFERENCED') {
        await openCodeWorkflowArtifact(artifact);

        return;
    }
}

/**
 * Open an artifact that lives under a (possibly non-active) task row in the sidebar.
 *
 * The resource panel ({@link aiHubTabsStore}) is per-task: {@link handleArtifactQuickOpen}'s
 * openFileTab/openWorkflowTab/etc. always append to whichever task is currently mirrored. So clicking a
 * resource nested under a DIFFERENT task than the open conversation would otherwise dump the resource into
 * the active conversation's right panel, leaving the middle (chat) panel out of sync with the resource the
 * user clicked. We switch to the owning task first so the conversation and the resource stay coherent.
 *
 * Ordering matters: {@link useSwitchTask} flips `currentTaskId`, but the tabs store only re-mirrors via an
 * effect in AiHub.tsx that runs on the NEXT render. We mirror it synchronously with
 * {@code setActiveTaskId(task.id)} BEFORE opening so the tab lands on the newly-switched task's set — without
 * it, the open would append to the previous task and the later effect-driven snapshot/restore would wipe it.
 * That explicit call is idempotent with the effect (same id → no-op guard in setActiveTaskId).
 *
 * On switch failure we bail without opening — switchTask already surfaced the error toast, and opening the
 * resource against the still-active (wrong) conversation would compound the confusion.
 */
export async function openArtifactInTask(
    artifact: AiHubTaskArtifactI,
    task: AiHubTaskI,
    currentTaskId: number | undefined,
    switchTask: (task: AiHubTaskI) => Promise<boolean>
): Promise<void> {
    if (task.id !== currentTaskId) {
        const switched = await switchTask(task);

        if (!switched) {
            return;
        }

        aiHubTabsStore.getState().setActiveTaskId(task.id);
    }

    await handleArtifactQuickOpen(artifact);
}

// Reference-kind artifacts are user-attached and removable; agent-driven audit rows
// (FILE_CREATED, WORKFLOW_EXECUTION_STARTED, MEMORY_*, etc.) are immutable history and must NOT
// expose a remove affordance — deleting them would corrupt the workspace-wide audit listing.
function isArtifactRemovable(artifact: AiHubTaskArtifactI): boolean {
    return (
        artifact.kind === 'FILE_REFERENCED' ||
        artifact.kind === 'WORKFLOW_REFERENCED' ||
        artifact.kind === 'DATA_TABLE_REFERENCED' ||
        artifact.kind === 'KB_REFERENCED' ||
        artifact.kind === 'SKILL_REFERENCED' ||
        artifact.kind === 'CUSTOM_COMPONENT_REFERENCED' ||
        artifact.kind === 'CODE_WORKFLOW_REFERENCED'
    );
}

function isArtifactClickable(artifact: AiHubTaskArtifactI): boolean {
    if (
        artifact.kind === 'FILE_CREATED' ||
        artifact.kind === 'FILE_UPDATED' ||
        artifact.kind === 'BINARY_FILE_CREATED' ||
        artifact.kind === 'FILE_REFERENCED'
    ) {
        return true;
    }

    if (artifact.kind === 'WORKFLOW_EXECUTION_STARTED') {
        return true;
    }

    const metadata = parseMetadataJson(artifact.metadataJson);

    if (artifact.kind === 'WORKFLOW_CREATED' || artifact.kind === 'WORKFLOW_UPDATED') {
        return !!metadata['projectId'];
    }

    if (artifact.kind === 'WORKFLOW_REFERENCED') {
        // Referenced-workflow rows came from a tab open — they may not have projectId in metadata yet
        // (the recordReference mutation doesn't currently stash it). Fall back to artifactId being non-empty
        // so the click attempt at least dispatches; if projectId is truly missing, openWorkflowTab no-ops
        // without surfacing an error.
        return !!artifact.artifactId;
    }

    if (
        artifact.kind === 'DATA_TABLE_ROW_ADDED' ||
        artifact.kind === 'DATA_TABLE_ROW_UPDATED' ||
        artifact.kind === 'DATA_TABLE_ROW_DELETED' ||
        artifact.kind === 'DATA_TABLE_COLUMN_ADDED'
    ) {
        return !!metadata['dataTableId'];
    }

    if (artifact.kind === 'DATA_TABLE_REFERENCED') {
        // For referenced tables, artifactId IS the dataTableId — no metadata lookup needed.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'KB_DOCUMENT_ADDED' || artifact.kind === 'KB_DOCUMENT_DELETED') {
        return !!metadata['knowledgeBaseId'];
    }

    if (artifact.kind === 'KB_REFERENCED') {
        // Same logic as DATA_TABLE_REFERENCED — artifactId IS the knowledgeBaseId.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'SKILL_REFERENCED') {
        // Same logic as DATA_TABLE_REFERENCED / KB_REFERENCED — artifactId IS the skillId.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'CUSTOM_COMPONENT_REFERENCED') {
        // Same logic as SKILL_REFERENCED — artifactId IS the custom component id.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'CODE_WORKFLOW_REFERENCED') {
        // Same logic as CUSTOM_COMPONENT_REFERENCED — artifactId IS the projectId. The language fetch
        // that quick-open needs happens on click (see openCodeWorkflowArtifact), not here.
        return !!artifact.artifactId;
    }

    return false;
}

interface ArtifactRowProps {
    artifact: AiHubTaskArtifactI;
    task: AiHubTaskI;
    workspaceId: number;
}

const ArtifactRow = ({artifact, task, workspaceId}: ArtifactRowProps) => {
    const clickable = isArtifactClickable(artifact);
    const removable = isArtifactRemovable(artifact);

    const queryClient = useQueryClient();

    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);
    const switchTask = useSwitchTask();

    const deleteMutation = useDeleteAiHubTaskArtifactMutation({
        onError: (error) => {
            const message = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to remove ${artifact.artifactName}: ${message}`);
        },
        // The artifact list query is keyed by ['aiHubTasks', 'artifacts', taskId, workspaceId]
        // (see AiHubTasksKeys.artifacts) — invalidate by that prefix so the row disappears from the
        // sidebar without needing the exact key shape react-query built it under. Matching on both
        // segments avoids needlessly refetching the task list/messages, which share the 'aiHubTasks'
        // root. Mirrors useRecordReferencedArtifacts' invalidation strategy.
        onSuccess: () => {
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

    const handleRemove = (event: MouseEvent<HTMLButtonElement>) => {
        // The remove button sits inside the quick-open button when the row is clickable; stop the click
        // from bubbling so removing doesn't also trigger the artifact viewer.
        event.stopPropagation();

        deleteMutation.mutate({
            input: {
                artifactId: String(artifact.id),
                workspaceId: String(workspaceId),
            },
        });
    };

    const content = (
        <div className="flex min-w-0 flex-1 items-center gap-1.5">
            {getArtifactIcon(artifact.kind)}

            <span className="min-w-0 flex-1 truncate text-xs text-foreground">{artifact.artifactName}</span>

            {removable && (
                <button
                    aria-label={`Remove ${artifact.artifactName}`}
                    className="shrink-0 rounded p-0.5 text-muted-foreground opacity-0 group-hover:opacity-100 hover:bg-muted hover:text-foreground"
                    disabled={deleteMutation.isPending}
                    onClick={handleRemove}
                    type="button"
                >
                    <XIcon className="size-3" />
                </button>
            )}
        </div>
    );

    if (clickable) {
        return (
            <button
                className="group flex w-full items-center gap-1.5 rounded px-1.5 py-1 text-left hover:bg-accent"
                onClick={() => void openArtifactInTask(artifact, task, currentTaskId, switchTask)}
                type="button"
            >
                {content}
            </button>
        );
    }

    return <div className="group flex items-center gap-1.5 px-1.5 py-1">{content}</div>;
};

interface ArtifactListProps {
    task: AiHubTaskI;
    workspaceId: number;
}

const ArtifactList = ({task, workspaceId}: ArtifactListProps) => {
    const {data: artifacts, isLoading} = useAiHubTaskArtifactsQuery(task.id, workspaceId);

    if (isLoading) {
        return (
            <div className="mt-1 ml-4 flex flex-col gap-1 px-1">
                <Skeleton className="h-5 w-full" />

                <Skeleton className="h-5 w-3/4" />
            </div>
        );
    }

    if (!artifacts || artifacts.length === 0) {
        return (
            <div className="mt-1 ml-4 px-1.5 py-1">
                <span className="text-xs text-muted-foreground">No artifacts yet</span>
            </div>
        );
    }

    return (
        <div className="mt-1 ml-4 flex flex-col">
            {artifacts.map((artifact) => (
                <ArtifactRow artifact={artifact} key={artifact.id} task={task} workspaceId={workspaceId} />
            ))}
        </div>
    );
};

interface TaskItemProps {
    task: AiHubTaskI;
    isCurrent: boolean;
    isExpanded: boolean;
    onArchive: () => void;
    onDelete: () => void;
    onRename: () => void;
    onSelect: () => void;
    onToggleExpand: () => void;
    onUnarchive: () => void;
    workspaceId: number;
}

const TaskItem = ({
    isCurrent,
    isExpanded,
    onArchive,
    onDelete,
    onRename,
    onSelect,
    onToggleExpand,
    onUnarchive,
    task,
    workspaceId,
}: TaskItemProps) => {
    const queryClient = useQueryClient();
    const isArchived = task.status === 'ARCHIVED';
    // Workflow-chat tasks route through a webhook, not the LLM agent, so they never produce artifacts.
    // Hide the whole artifact affordance for them (no expand chevron, no count, no "No artifacts yet").
    const showArtifacts = task.kind !== 'WORKFLOW_CHAT';

    // Subscribe specifically to this task's title-generation failure flag so a retry icon appears
    // on the row when the most recent automatic title-generation attempt failed. Without this, the toast
    // shown at failure time is the only signal — once dismissed, the user has no recovery path.
    const titleGenerationError = useAiHubTasksStore((state) => state.titleGenerationFailures[task.id]);

    // Surface the LIVE workflow label so a stale task title (set on first creation as
    // "${projectName} — ${workflowLabel}") doesn't hide the fact that the workflow has since been renamed.
    // We only enable the query for kind=WORKFLOW_CHAT rows so standard-only sidebars don't pay the workflow
    // lookup cost. The query is cached workspace-wide so multiple workflow-chat rows share one fetch.
    const liveWorkflowLabel = useLiveWorkflowLabel(task);
    // In-flight signal for this row: 'running' = the task is mid-turn, 'paused' = the workflow is
    // paused at ask_user_question waiting for the user. Same store as titleGenerationError so subscriptions
    // re-render the row independently — a paused task buried at the bottom of the sidebar still
    // updates without re-rendering all the others.
    const activityState = useAiHubTasksStore((state) => state.taskActivity[task.threadId]);
    const [isRetryingTitle, setIsRetryingTitle] = useState(false);

    /*
     * Artifact count badge: we only know the count once the list has been fetched
     * (expand-on-demand approach). Before first expand, no badge is shown.
     * After first expand, the badge reflects the loaded count even when collapsed.
     * This avoids firing N artifact queries for N sidebar rows on initial load.
     */
    const artifactsEnabled = isExpanded && showArtifacts;
    const {data: artifacts} = useAiHubTaskArtifactsQuery(
        artifactsEnabled ? task.id : undefined,
        workspaceId,
        artifactsEnabled
    );

    const artifactCount = artifacts?.length ?? 0;

    const handleRetryTitleGeneration = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (isRetryingTitle) {
            return;
        }

        setIsRetryingTitle(true);

        try {
            await generateAiHubTaskTitle({taskId: task.id, workspaceId});

            aiHubTasksStore.getState().clearTitleGenerationFailure(task.id);

            await queryClient.invalidateQueries({
                queryKey: ['aiHub', 'tasks'],
            });
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            // Preserve the failure flag so the retry icon stays visible and the user can try again later.
            aiHubTasksStore.getState().markTitleGenerationFailed(task.id, errorMessage);

            toast.error(`Failed to generate task title: ${errorMessage}`);
        } finally {
            setIsRetryingTitle(false);
        }
    };

    return (
        <div>
            <div
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md px-2 py-1.5 text-sm hover:bg-accent',
                    isCurrent && 'bg-accent'
                )}
            >
                <button
                    className="flex flex-1 items-center gap-1.5 truncate text-left"
                    onClick={onSelect}
                    type="button"
                >
                    {task.kind === 'PERSONAL_AGENT' && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <BotIcon
                                    aria-label="Personal agent chat"
                                    className="size-3.5 shrink-0 text-muted-foreground"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Personal agent — your custom instructions are appended to the agent's system prompt
                                every turn.
                            </TooltipContent>
                        </Tooltip>
                    )}

                    {task.kind === 'WORKFLOW_CHAT' && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <WorkflowIcon
                                    aria-label="Workflow chat"
                                    className="size-3.5 shrink-0 text-muted-foreground"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Workflow chat — bound to a workflow execution. Messages are forwarded to the workflow's
                                webhook trigger instead of an LLM.
                                {liveWorkflowLabel && liveWorkflowLabel !== task.title && (
                                    <div className="mt-1 text-xs opacity-90">
                                        Current workflow label: <span className="font-medium">{liveWorkflowLabel}</span>
                                    </div>
                                )}
                            </TooltipContent>
                        </Tooltip>
                    )}

                    {/*
                     * Default-kind icon: a plain MessageSquare for the STANDARD tasks so every
                     * row is visually anchored. Without this, Personal Agent and Workflow Chat rows had icons
                     * but standard rows started with text — the eye lost its alignment column when scanning the
                     * list. We treat any non-{PERSONAL_AGENT, WORKFLOW_CHAT} row as a standard chat;
                     * future kinds appended to the discriminator should add their own branch here. No tooltip:
                     * STANDARD is the implicit default kind, and a tooltip on every standard row would be noise.
                     */}

                    {task.kind !== 'PERSONAL_AGENT' && task.kind !== 'WORKFLOW_CHAT' && (
                        <MessageSquareIcon aria-label="Task" className="size-3.5 shrink-0 text-muted-foreground" />
                    )}

                    <span className={twMerge('truncate', isCurrent && 'font-semibold')}>
                        {task.title || 'New Task'}
                    </span>

                    {/*
                     * Activity indicator. 'running' shows a spinning loader so the user knows their last turn
                     * is still in flight even if they switched away from this task; 'paused' shows a
                     * question-mark badge meaning "this chat needs your answer to continue." Both signals are
                     * driven from AiHubRuntimeProvider's lifecycle event handlers.
                     */}

                    {activityState === 'running' && (
                        <Loader2Icon
                            aria-label="Task is running"
                            className="size-3 shrink-0 animate-spin text-muted-foreground"
                        />
                    )}

                    {activityState === 'paused' && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <MessageCircleQuestionIcon
                                    aria-label="Task is paused waiting for your answer"
                                    className="size-3.5 shrink-0 text-content-warning-primary"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Workflow paused for your answer. Reply to resume the workflow run.
                            </TooltipContent>
                        </Tooltip>
                    )}
                </button>

                <div className="ml-1 flex shrink-0 items-center gap-0.5">
                    {titleGenerationError && (
                        <button
                            aria-label="Retry title generation"
                            className="rounded p-0.5 text-content-warning-primary hover:bg-muted"
                            data-testid="retry-title-generation"
                            disabled={isRetryingTitle}
                            onClick={handleRetryTitleGeneration}
                            title={`Title generation failed: ${titleGenerationError}. Click to retry.`}
                            type="button"
                        >
                            <RotateCcwIcon className={twMerge('size-3.5', isRetryingTitle && 'animate-spin')} />
                        </button>
                    )}

                    {artifactCount > 0 && (
                        <span className="rounded bg-muted px-1 py-0.5 text-xs font-medium text-muted-foreground">
                            {artifactCount}
                        </span>
                    )}

                    {/*
                     * Hover-only action buttons. Switched from `opacity-0 group-hover:opacity-100` to
                     * `hidden group-hover:flex` so the buttons don't reserve layout space when invisible —
                     * earlier, the chevron + more-horizontal kept ~36px of room on every row, truncating
                     * the task title way before the panel's right edge. With `hidden`, the title
                     * stretches to the full row width when the user isn't hovering; on hover the buttons
                     * appear and re-truncate the title (acceptable since the user is mousing over).
                     * `focus-within:flex` keeps keyboard navigation working without a hover.
                     */}

                    {showArtifacts && (
                        <button
                            aria-label={isExpanded ? 'Collapse artifacts' : 'Expand artifacts'}
                            className="hidden rounded p-0.5 group-focus-within:flex group-hover:flex hover:bg-muted focus:flex"
                            onClick={(event) => {
                                event.stopPropagation();
                                onToggleExpand();
                            }}
                            type="button"
                        >
                            {isExpanded ? (
                                <ChevronDownIcon className="size-3.5" />
                            ) : (
                                <ChevronRightIcon className="size-3.5" />
                            )}
                        </button>
                    )}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <button
                                aria-label="Task actions"
                                className="hidden rounded p-0.5 group-focus-within:flex group-hover:flex hover:bg-muted focus:flex data-[state=open]:flex"
                                data-testid="task-actions-trigger"
                                onClick={(event) => event.stopPropagation()}
                                type="button"
                            >
                                <MoreVerticalIcon className="size-4" />
                            </button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={onRename}>
                                <PencilIcon />
                                Rename
                            </DropdownMenuItem>

                            {isArchived ? (
                                <DropdownMenuItem onClick={onUnarchive}>
                                    <ArchiveRestoreIcon />
                                    Unarchive
                                </DropdownMenuItem>
                            ) : (
                                <DropdownMenuItem onClick={onArchive}>
                                    <ArchiveIcon />
                                    Archive
                                </DropdownMenuItem>
                            )}

                            <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={onDelete}>
                                <Trash2Icon />
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            {isExpanded && showArtifacts && <ArtifactList task={task} workspaceId={workspaceId} />}
        </div>
    );
};

interface RenameDialogStateI {
    taskId: number;
    currentTitle: string;
}

interface DeleteDialogStateI {
    targetTask: AiHubTaskI;
}

const AiHubTasksSidebar = () => {
    const [expandedTaskIds, setExpandedTaskIds] = useState<Set<number>>(new Set());
    const [renameState, setRenameState] = useState<RenameDialogStateI | null>(null);
    const [renameInputValue, setRenameInputValue] = useState('');
    const [deleteDialogState, setDeleteDialogState] = useState<DeleteDialogStateI | null>(null);
    const [visibleCount, setVisibleCount] = useState(TASKS_PAGE_SIZE);
    const renameInputRef = useRef<HTMLInputElement>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const activeFilter = useAiHubTasksStore((state) => state.activeFilter);
    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);
    const searchTerm = useAiHubTasksStore((state) => state.searchTerm);
    const setActiveFilter = useAiHubTasksStore((state) => state.setActiveFilter);
    const setSearchTerm = useAiHubTasksStore((state) => state.setSearchTerm);

    const switchTask = useSwitchTask();

    const {data: tasks, isLoading} = useAiHubTasksQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        activeFilter === 'ACTIVE' ? 'ACTIVE' : 'ARCHIVED'
    );

    const patchTaskMutation = usePatchAiHubTaskMutation();
    const deleteTaskMutation = useDeleteAiHubTaskMutation();
    const cancelAiHubRunMutation = useCancelAiHubRunMutation();
    const cancelWorkflowChatTurnMutation = useCancelWorkflowChatTurnMutation();

    // Hydrate the sidebar's per-task running pulse from the server. Activity state is otherwise driven only
    // by the runtime provider's lifecycle hooks (RUN_STARTED → 'running' / RUN_FINISHED → cleared), which
    // means a page refresh blows the whole map away even though server-side runs are still streaming. Probing
    // on tasks list change rehydrates the dot for every task the user has actively running, not just the
    // focused one — which is what makes the "switch between concurrent streams" UX work: the user sees
    // multiple pulses, can switch, and the runtime provider's own attach effect resumes whichever task they
    // land on.
    //
    // The probe is also responsible for CLEARING stale 'running' indicators on background tasks:
    // {@link AiHubRuntimeProvider}'s {@code onRunFinishedEvent} clears state only for the task whose
    // {@code /attach} EventSource it owns (the focused one). Background in-flight tasks that finish
    // server-side have no other code path to clear their pulse, so without probe-driven clearing the
    // indicator stuck forever. Two guards keep the clear safe:
    //
    //   1. Preserve {@code 'paused'} — {@link AiHubRuntimeProvider}'s {@code onCustomEvent} sets it on
    //      workflow-chat tasks that hit {@code ask_user_question}, and the runtime intentionally keeps it
    //      across the immediately-following RUN_FINISHED. A probe issued after that RUN_FINISHED would
    //      otherwise wipe the user-actionable "needs your answer" indicator.
    //
    //   2. Skip clearing when the local runtime claims the task via {@link aiHubRunStateStore.runningByTask}.
    //      Between user-clicks-send and agent-POST-registers-run-on-the-server there's a narrow window where
    //      a probe issued moments earlier could come back reporting {@code false}; deferring to the
    //      runtime's RUN_FINISHED / onCancel / onClose clear paths avoids clobbering a turn the local
    //      client is actively driving.
    //
    // Also re-runs on a 20-second interval so background tasks get cleaned up even when the tasks list
    // query is otherwise idle (the list has a 30s staleTime and no refetchInterval).
    useEffect(() => {
        if (!tasks || tasks.length === 0) {
            return;
        }

        const visibleThreadIds = tasks.map((task) => task.threadId);

        let cancelled = false;

        const runProbe = () => {
            void probeInFlightStatus(visibleThreadIds).then((statusByThreadId) => {
                if (cancelled) {
                    return;
                }

                const tasksState = aiHubTasksStore.getState();
                const runState = aiHubRunStateStore.getState();

                const focusedThreadId = tasks.find((task) => task.id === currentTaskId)?.threadId;

                visibleThreadIds.forEach((threadId) => {
                    const decision = reconcileProbedTaskActivity({
                        currentActivity: tasksState.taskActivity[threadId],
                        isFocused: threadId === focusedThreadId,
                        isInFlight: statusByThreadId[threadId] ?? false,
                        isRunningByTask: Boolean(runState.runningByTask[threadId]),
                    });

                    if (decision.setActivityRunning) {
                        tasksState.setActivityState(threadId, 'running');
                    }

                    if (decision.clearRunningFlag) {
                        runState.setTaskRunning(threadId, false);
                    }

                    if (decision.clearActivity) {
                        tasksState.clearActivityState(threadId);
                    }
                });
            });
        };

        runProbe();

        const intervalHandle = setInterval(runProbe, 20_000);

        return () => {
            cancelled = true;

            clearInterval(intervalHandle);
        };
    }, [tasks, currentTaskId]);

    const filteredTasks = useMemo(() => {
        if (!tasks) {
            return [];
        }

        const lowerSearch = searchTerm.toLowerCase();

        if (!lowerSearch) {
            return tasks;
        }

        return tasks.filter(
            (task) =>
                (task.title ?? '').toLowerCase().includes(lowerSearch) ||
                (task.lastPreview ?? '').toLowerCase().includes(lowerSearch)
        );
    }, [tasks, searchTerm]);

    const {hiddenCount, visibleTasks} = useMemo(
        () => getTasksPage(filteredTasks, visibleCount),
        [filteredTasks, visibleCount]
    );

    useEffect(() => {
        setVisibleCount(TASKS_PAGE_SIZE);
    }, [searchTerm, activeFilter]);

    const handleSelectTask = (task: AiHubTaskI) => {
        switchTask(task);
    };

    const handleRename = (task: AiHubTaskI) => {
        setRenameState({currentTitle: task.title ?? '', taskId: task.id});
        setRenameInputValue(task.title ?? '');

        setTimeout(() => {
            renameInputRef.current?.focus();
            renameInputRef.current?.select();
        }, 0);
    };

    const handleRenameSubmit = () => {
        if (!renameState) {
            return;
        }

        const trimmedTitle = renameInputValue.trim();

        if (trimmedTitle && trimmedTitle !== renameState.currentTitle) {
            patchTaskMutation.mutate({
                patch: {title: trimmedTitle},
                taskId: renameState.taskId,
                workspaceId: currentWorkspaceId,
            });
        }

        setRenameState(null);
    };

    const handleArchive = (task: AiHubTaskI) => {
        patchTaskMutation.mutate({
            patch: {status: 'ARCHIVED'},
            taskId: task.id,
            workspaceId: currentWorkspaceId,
        });

        if (currentTaskId === task.id) {
            aiHubTasksStore.getState().setCurrentTaskId(undefined);
        }
    };

    const handleUnarchive = (task: AiHubTaskI) => {
        patchTaskMutation.mutate({
            patch: {status: 'ACTIVE'},
            taskId: task.id,
            workspaceId: currentWorkspaceId,
        });
    };

    const handleDelete = (task: AiHubTaskI) => {
        setDeleteDialogState({targetTask: task});
    };

    const handleDeleteDialogConfirm = () => {
        if (!deleteDialogState) {
            return;
        }

        const task = deleteDialogState.targetTask;

        // Stop the stream before removing the task: cancel its in-flight run (server-side) so it doesn't
        // keep running for a task the user just deleted. Clearing currentTaskId below tears down the
        // focused runtime's SSE for the active task; this handles the background tasks too.
        cancelTaskRunIfStreaming(task, currentWorkspaceId, {
            cancelAiHubRun: (variables) => cancelAiHubRunMutation.mutate(variables),
            cancelWorkflowChatTurn: (variables) => cancelWorkflowChatTurnMutation.mutate(variables),
        });

        deleteTaskMutation.mutate({
            taskId: task.id,
            workspaceId: currentWorkspaceId,
        });

        if (currentTaskId === task.id) {
            // Deleting the task the user is currently viewing: reset to the home view and redirect so they
            // don't sit on a stale /tasks/<id> route for a task that no longer exists. Mirrors AiHub.tsx's
            // home-reset (clear messages + fresh thread id); the explicit navigate guarantees the redirect.
            aiHubTasksStore.getState().setCurrentTaskId(undefined);
            aiHubStore.getState().resetMessages();
            aiHubStore.getState().generateTaskId();

            navigate('/automation/ai-hub');
        }

        setDeleteDialogState(null);
    };

    const handleToggleExpand = (taskId: number) => {
        setExpandedTaskIds((previous) => {
            const next = new Set(previous);

            if (next.has(taskId)) {
                next.delete(taskId);
            } else {
                next.add(taskId);
            }

            return next;
        });
    };

    const isViewingArchived = activeFilter === 'ARCHIVED';

    const {pathname} = useLocation();
    const navigate = useNavigate();

    // Active-state predicates for the five top-level menu items.
    //
    // - New Task matches ONLY the bare /automation/ai-hub landing. When the user is on a specific
    //   /automation/ai-hub/tasks/<id> page, that task's row in the list below already shows the active
    //   highlight — also lighting up "New Task" would imply two simultaneous selections and visually
    //   conflate "start a fresh task" with "currently on a task".
    // - Personal Agents, Workflow Chats, Memories, and Audit use exact prefix matches against their
    //   canonical routes.
    const isOnAiHubPersonalAgents = pathname.startsWith('/automation/ai-hub/personal-agents');
    const isOnWorkflowChats = pathname.startsWith('/automation/ai-hub/workflow-chats');
    const isOnNewTask = pathname === '/automation/ai-hub';
    const isOnMemories = pathname.startsWith('/automation/ai/memories');
    const isOnConnectors = pathname.startsWith('/automation/settings/ai-hub/connectors');
    const isOnSkills = pathname.startsWith('/automation/ai/skills');

    // "Context" is a collapsible group of AI Hub-wide resources (Memories, Connectors, Skills). Default it open
    // when the user is already on one of its target pages so the active child is visible; otherwise
    // collapsed by default so it doesn't crowd the task list.
    const [contextExpanded, setContextExpanded] = useState(isOnMemories || isOnConnectors || isOnSkills);

    // `bg-accent` alone is the same shade as `hover:bg-accent`, so an "active" menu item is visually
    // indistinguishable from a hovered one — the user reads the row as not-selected. Mirror the active-task
    // row treatment (font-semibold + foreground text) so the active state has a real visual delta even with the
    // subtle background tint. `text-foreground` on active anchors the label at full contrast; `text-muted-foreground`
    // on inactive rows lets the active label "step forward" rather than relying on the background alone.
    const menuItemClass = (isActive: boolean) =>
        twMerge(
            'flex items-center gap-2 rounded-md px-2 py-1.5 text-sm font-medium text-muted-foreground hover:bg-accent hover:text-foreground',
            isActive && 'bg-accent font-semibold text-foreground'
        );

    return (
        <div className="flex flex-col gap-2 px-2 pb-2">
            {/*
             * Three top-level menu items: New Task / Personal Agents / Workflow Chats. Label-only
             * (no leading icons) and aligned at px-2 so labels land at 8 (body p-2) + 8 (item px-2) = 16px
             * from the sidebar edge — matches the px-4 padding on the "AI Hub" Header above.
             *
             * Personal Agents and Workflow Chats are full-page routes (see /automation/ai-hub/
             * personal-agents and /workflow-chats) — same pattern as Memories and Audit. Links here, not
             * buttons setting state. Each row applies a `bg-accent` highlight when its route is active so
             * the user can see at a glance which CC destination they're on.
             */}

            {/*
             * Tight `gap-0.5` (2px) between the three menu rows. The outer wrapper still uses `gap-2`
             * to keep clear visual separation between this menu block, the Tasks title row,
             * the search input, and the task list — those are different sections, not
             * siblings of the three menu items.
             */}

            {/*
             * Icons match the rest of the AI Hub surface for visual continuity:
             *   - MessageSquarePlus signals "start a new chat" — same metaphor used in mainstream chat UIs.
             *   - BotIcon is the same icon already used to mark PERSONAL_AGENT rows in the task list
             *     and tooltip below; reusing it here ties the menu entry to the per-task badge.
             *   - WorkflowIcon mirrors the icon used on WORKFLOW_CHAT rows and the workflow editor.
             * `text-muted-foreground` keeps the icons quiet so the label remains the primary anchor.
             */}

            <div className="flex flex-col gap-0.5">
                <Link className={menuItemClass(isOnNewTask)} to="/automation/ai-hub">
                    <MessageSquarePlusIcon className="size-4 shrink-0 text-muted-foreground" />

                    <span>New Task</span>
                </Link>

                <Link className={menuItemClass(isOnAiHubPersonalAgents)} to="/automation/ai-hub/personal-agents">
                    <BotIcon className="size-4 shrink-0 text-muted-foreground" />

                    <span>Personal Agents</span>
                </Link>

                <Link className={menuItemClass(isOnWorkflowChats)} to="/automation/ai-hub/workflow-chats">
                    <WorkflowIcon className="size-4 shrink-0 text-muted-foreground" />

                    <span>Workflow Chats</span>
                </Link>

                {/*
                 * "Context" — a collapsible group of AI Hub-wide resources that aren't scoped to a single
                 * task. Collapsed by default (see contextExpanded) so it stays out of the way; the chevron
                 * toggles it. The sub-items are plain links to their canonical target pages: Memories reuses
                 * the standalone /automation/ai/memories page, and Connectors links out to the
                 * Settings > AI Hub > Connectors page.
                 */}

                <Collapsible className="flex flex-col gap-0.5" onOpenChange={setContextExpanded} open={contextExpanded}>
                    <CollapsibleTrigger
                        className={twMerge(
                            menuItemClass(isOnMemories || isOnConnectors || isOnSkills),
                            'justify-between'
                        )}
                    >
                        <span className="flex items-center gap-2">
                            <BlocksIcon className="size-4 shrink-0 text-muted-foreground" />

                            <span>Context</span>
                        </span>

                        {contextExpanded ? (
                            <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                        ) : (
                            <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" />
                        )}
                    </CollapsibleTrigger>

                    <CollapsibleContent className="flex flex-col gap-0.5">
                        <Link className={twMerge(menuItemClass(isOnMemories), 'pl-8')} to="/automation/ai/memories">
                            <BrainIcon className="size-4 shrink-0 text-muted-foreground" />

                            <span>Memories</span>
                        </Link>

                        <Link
                            className={twMerge(menuItemClass(isOnConnectors), 'pl-8')}
                            to="/automation/settings/ai-hub/connectors"
                        >
                            <LinkIcon className="size-4 shrink-0 text-muted-foreground" />

                            <span>Connectors</span>
                        </Link>

                        <Link className={twMerge(menuItemClass(isOnSkills), 'pl-8')} to="/automation/ai/skills">
                            <HexagonIcon className="size-4 shrink-0 text-muted-foreground" />

                            <span>Skills</span>
                        </Link>
                    </CollapsibleContent>
                </Collapsible>
            </div>

            {/*
             * Title row pinned at the top: page-anchor "Tasks" label on the left, Archive toggle
             * affordance on the right. The Archive toggle flips the same `activeFilter` store value the
             * bottom toggle used to drive — when in ARCHIVED view the icon swaps to a "back to active"
             * affordance so the user always has a clear way back.
             *
             * Memories used to live here as a small icon; it's been promoted to a full top-level menu item
             * above (alongside Personal Agents, Workflow Chats, and Artifact History). One canonical entry point per
             * destination keeps the sidebar uncluttered and avoids the "two ways to do the same thing"
             * trap where users wonder whether the small icon and the top-level item differ.
             */}

            <div className="flex items-center justify-between gap-2 px-2">
                <h4 className="text-sm font-semibold text-foreground">Tasks</h4>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            aria-label={isViewingArchived ? 'Back to active tasks' : 'Archive'}
                            aria-pressed={isViewingArchived}
                            className={twMerge(
                                'inline-flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground',
                                isViewingArchived && 'bg-accent text-foreground'
                            )}
                            onClick={() => setActiveFilter(isViewingArchived ? 'ACTIVE' : 'ARCHIVED')}
                            type="button"
                        >
                            {isViewingArchived ? (
                                <ChevronLeftIcon className="size-3.5" />
                            ) : (
                                <ArchiveIcon className="size-3.5" />
                            )}
                        </button>
                    </TooltipTrigger>

                    <TooltipContent>{isViewingArchived ? 'Back to active' : 'Archive'}</TooltipContent>
                </Tooltip>
            </div>

            {/*
             * Search-only row. The previous "+" quick-create button next to the input is removed — the
             * "New Task" entry in the top menu block is the canonical create entry point, and the
             * duplicate "+" here read as visual noise once the menu items moved up to the top of the sidebar.
             *
             * px-2 wrapper so the input aligns at 16px from the sidebar edge — same as the Header title
             * and the menu items above. Without it, the Input touches the p-2 body edge at 8px.
             */}

            <div className="px-2">
                <Input
                    onChange={(event) => setSearchTerm(event.target.value)}
                    placeholder="Search tasks..."
                    value={searchTerm}
                />
            </div>

            {isLoading ? (
                <TasksSidebarSkeleton />
            ) : filteredTasks.length === 0 ? (
                <div className="px-2 py-4">
                    <span className="text-xs text-muted-foreground">
                        {isViewingArchived ? 'No archived tasks yet' : 'No tasks yet'}
                    </span>
                </div>
            ) : (
                <div className="flex flex-col gap-0.5">
                    {visibleTasks.map((task) =>
                        renameState?.taskId === task.id ? (
                            <div className="px-1" key={task.id}>
                                <Input
                                    className="h-8 text-sm"
                                    onBlur={handleRenameSubmit}
                                    onChange={(event) => setRenameInputValue(event.target.value)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter') {
                                            handleRenameSubmit();
                                        } else if (event.key === 'Escape') {
                                            setRenameState(null);
                                        }
                                    }}
                                    ref={renameInputRef}
                                    value={renameInputValue}
                                />
                            </div>
                        ) : (
                            <TaskItem
                                isCurrent={currentTaskId === task.id}
                                isExpanded={expandedTaskIds.has(task.id)}
                                key={task.id}
                                onArchive={() => handleArchive(task)}
                                onDelete={() => handleDelete(task)}
                                onRename={() => handleRename(task)}
                                onSelect={() => handleSelectTask(task)}
                                onToggleExpand={() => handleToggleExpand(task.id)}
                                onUnarchive={() => handleUnarchive(task)}
                                task={task}
                                workspaceId={currentWorkspaceId}
                            />
                        )
                    )}

                    {hiddenCount > 0 && (
                        <button
                            className="rounded-md px-2 py-1.5 text-left text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                            onClick={() => setVisibleCount((previousCount) => previousCount + TASKS_PAGE_SIZE)}
                            type="button"
                        >
                            {`Show ${Math.min(hiddenCount, TASKS_PAGE_SIZE)} more`}
                        </button>
                    )}
                </div>
            )}

            <AlertDialog
                onOpenChange={(nextOpen) => {
                    if (!nextOpen) {
                        setDeleteDialogState(null);
                    }
                }}
                open={deleteDialogState !== null}
            >
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Delete this task?</AlertDialogTitle>

                        <AlertDialogDescription>
                            {`"${deleteDialogState?.targetTask.title || 'New Task'}" will be permanently deleted. This cannot be undone.`}
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>

                        <AlertDialogAction onClick={handleDeleteDialogConfirm}>Delete</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

export default AiHubTasksSidebar;
