import {
    AiHubTaskArtifactKind,
    AiHubTaskArtifactsByAiHubTaskDocument,
    AiHubTaskArtifactsByAiHubTaskQuery,
    AiHubTaskArtifactsByAiHubTaskQueryVariables,
    AiHubTaskArtifactsDocument,
    AiHubTaskArtifactsQuery,
    AiHubTaskArtifactsQueryVariables,
    AiHubTaskMessagesDocument,
    AiHubTaskMessagesQuery,
    AiHubTaskMessagesQueryVariables,
    AiHubTaskStatus as GraphQlTaskStatus,
    AiHubTasksDocument,
    AiHubTasksQuery,
    AiHubTasksQueryVariables,
    CreateAiHubTaskDocument,
    CreateAiHubTaskMutation,
    CreateAiHubTaskMutationVariables,
    DeleteAiHubTaskDocument,
    DeleteAiHubTaskMutationVariables,
    GenerateAiHubTaskTitleDocument,
    GenerateAiHubTaskTitleMutation,
    GenerateAiHubTaskTitleMutationVariables,
    UpdateAiHubTaskDocument,
    UpdateAiHubTaskMutation,
    UpdateAiHubTaskMutationVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';

// Mirrors the Java enum at server/ee/.../task/AiHubTaskArtifactKind.java. Adding a new kind on the
// server side without updating this list is a silent type-drift bug — the UI ends up dispatching `default` on
// strings it doesn't recognize, which silently disables the artifact's row in the audit/sidebar viewers.
export type AiHubArtifactKindType =
    | 'API_COLLECTION_REFERENCED'
    | 'BINARY_FILE_CREATED'
    | 'CODE_WORKFLOW_REFERENCED'
    | 'CUSTOM_COMPONENT_REFERENCED'
    | 'DATA_TABLE_COLUMN_ADDED'
    | 'DATA_TABLE_REFERENCED'
    | 'DATA_TABLE_ROW_ADDED'
    | 'DATA_TABLE_ROW_DELETED'
    | 'DATA_TABLE_ROW_UPDATED'
    | 'FILE_CREATED'
    | 'FILE_REFERENCED'
    | 'FILE_UPDATED'
    | 'KB_DOCUMENT_ADDED'
    | 'KB_DOCUMENT_DELETED'
    | 'KB_REFERENCED'
    | 'MCP_SERVER_REFERENCED'
    | 'MEMORY_CREATED'
    | 'MEMORY_DELETED'
    | 'MEMORY_RENAMED'
    | 'MEMORY_UPDATED'
    | 'SKILL_REFERENCED'
    | 'TASK_REFERENCED'
    | 'WORKFLOW_CREATED'
    | 'WORKFLOW_EXECUTION_REFERENCED'
    | 'WORKFLOW_EXECUTION_STARTED'
    | 'WORKFLOW_REFERENCED'
    | 'WORKFLOW_UPDATED';

export type AiHubArtifactStatusType = 'APPLIED' | 'EXPIRED' | 'IRREVERSIBLE';

export type TaskStatusType = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface AiHubTaskArtifactI {
    artifactId: string;
    artifactName: string;
    taskId: number;
    createdAt: string;
    id: number;
    kind: AiHubArtifactKindType;
    metadataJson: string | null;
    status: AiHubArtifactStatusType;
}

export interface ArtifactPageResponseI {
    hasMore: boolean;
    items: AiHubTaskArtifactI[];
    pageClamped?: boolean;
    sizeClamped?: boolean;
    totalCount: number;
}

type GraphQlArtifactType = NonNullable<AiHubTaskArtifactsQuery['aiHubTaskArtifacts']['items']>[number];

function toArtifact(artifact: GraphQlArtifactType): AiHubTaskArtifactI {
    return {
        artifactId: artifact.artifactId,
        artifactName: artifact.artifactName,
        createdAt: artifact.createdAt != null ? new Date(Number(artifact.createdAt)).toISOString() : '',
        id: Number(artifact.id),
        kind: artifact.kind as AiHubArtifactKindType,
        metadataJson: artifact.metadataJson ?? null,
        status: artifact.status as AiHubArtifactStatusType,
        taskId: Number(artifact.taskId),
    };
}

export type TaskKindType = 'PERSONAL_AGENT' | 'STANDARD' | 'WORKFLOW_CHAT';

export interface AiHubTaskI {
    /**
     * Whether the title was set automatically and is still eligible for LLM regeneration. The client
     * fires {@code generateAiHubTaskTitle} on every turn while {@code autoTitled} is true; once the
     * LLM regenerates a title (or the user renames the task) the flag flips to false and the
     * regen loop stops. Workflow chats start at {@code true} so their initial label-based title can be
     * replaced by a more meaningful LLM-generated title after a few turns.
     */
    autoTitled: boolean;
    createdAt: string;
    id: number;
    /**
     * Discriminator for routing UI affordances. {@code STANDARD} → LLM-driven task;
     * {@code WORKFLOW_CHAT} → bound to a specific workflow execution and bridged to the webhook executor
     * server-side; {@code PERSONAL_AGENT} → LLM agent with a user-defined instructions overlay applied
     * every turn. Each kind renders a distinct icon in the sidebar so users can tell them apart at a glance.
     */
    kind: TaskKindType;
    lastPreview: string | null;
    messageCount: number;
    /**
     * Owning Personal Agent id when {@code kind === 'PERSONAL_AGENT'}; otherwise {@code null}. Lets the
     * sidebar resolve the agent's display title for grouping and lets routing detail views jump back to
     * the agent definition.
     */
    aiHubPersonalAgentId: number | null;
    status: TaskStatusType;
    threadId: string;
    title: string | null;
    updatedAt: string;
    userId: number;
    /** Composite tenant+UUID string when {@code kind === 'WORKFLOW_CHAT'}; otherwise {@code null}. */
    workflowExecutionId: string | null;
    workspaceId: number;
}

export interface AiHubTaskMessageI {
    content: string;
    role: string;
    timestamp: string;
    // Nullable JSON array of tool activity attached to this row — see the server's AiHubTaskMessage record.
    toolEventsJson: string | null;
}

export interface AiHubTaskPatchI {
    lastPreview?: string;
    messageCount?: number;
    status?: TaskStatusType;
    title?: string;
}

type GraphQlTaskType = AiHubTasksQuery['aiHubTasks'][number];

// Exported for tests: the GraphQL → domain mapping is the source of truth for aiHubPersonalAgentId/workflowExecutionId
// null-handling and the kind discriminator. Direct test coverage avoids round-tripping through every public
// function that calls toTask, and keeps the mapping invariants pinned in one place.
export function toTask(task: GraphQlTaskType): AiHubTaskI {
    // Older tasks created before the kind column landed surface as `null` from the resolver — they
    // predate workflow chats and are unambiguously STANDARD. Treat any unrecognised value the same way so
    // a future kind addition on the server doesn't crash the sidebar render.
    let kind: TaskKindType = 'STANDARD';

    if (task.kind === 'WORKFLOW_CHAT') {
        kind = 'WORKFLOW_CHAT';
    } else if (task.kind === 'PERSONAL_AGENT') {
        kind = 'PERSONAL_AGENT';
    }

    return {
        aiHubPersonalAgentId: task.aiHubPersonalAgentId != null ? Number(task.aiHubPersonalAgentId) : null,
        autoTitled: task.autoTitled,
        createdAt: task.createdAt != null ? new Date(Number(task.createdAt)).toISOString() : '',
        id: Number(task.id),
        kind,
        lastPreview: task.lastPreview ?? null,
        messageCount: task.messageCount,
        status: task.status as TaskStatusType,
        threadId: task.threadId,
        title: task.title ?? null,
        updatedAt: task.updatedAt != null ? new Date(Number(task.updatedAt)).toISOString() : '',
        userId: Number(task.userId),
        workflowExecutionId: task.workflowExecutionId ?? null,
        workspaceId: Number(task.workspaceId),
    };
}

export async function createAiHubTask({
    environment,
    threadId,
    workspaceId,
}: {
    environment: number;
    threadId: string;
    workspaceId: number;
}): Promise<AiHubTaskI> {
    const result = await fetcher<CreateAiHubTaskMutation, CreateAiHubTaskMutationVariables>(CreateAiHubTaskDocument, {
        environment,
        threadId,
        workspaceId: String(workspaceId),
    })();

    return toTask(result.createAiHubTask);
}

export async function listTasks({
    environment,
    status,
    workspaceId,
}: {
    environment: number;
    status: Exclude<TaskStatusType, 'DELETED'>;
    workspaceId: number;
}): Promise<AiHubTaskI[]> {
    const result = await fetcher<AiHubTasksQuery, AiHubTasksQueryVariables>(AiHubTasksDocument, {
        environment,
        status: status as GraphQlTaskStatus,
        workspaceId: String(workspaceId),
    })();

    return result.aiHubTasks.map(toTask);
}

export async function getTaskMessages({
    taskId,
    workspaceId,
}: {
    taskId: number;
    workspaceId: number;
}): Promise<AiHubTaskMessageI[]> {
    const result = await fetcher<AiHubTaskMessagesQuery, AiHubTaskMessagesQueryVariables>(AiHubTaskMessagesDocument, {
        id: String(taskId),
        workspaceId: String(workspaceId),
    })();

    return result.aiHubTaskMessages.map((message) => ({
        content: message.content,
        role: message.role,
        timestamp: new Date(Number(message.timestamp)).toISOString(),
        toolEventsJson: message.toolEventsJson ?? null,
    }));
}

export async function patchTask({
    patch,
    taskId,
    workspaceId,
}: {
    taskId: number;
    patch: AiHubTaskPatchI;
    workspaceId: number;
}): Promise<AiHubTaskI> {
    const result = await fetcher<UpdateAiHubTaskMutation, UpdateAiHubTaskMutationVariables>(UpdateAiHubTaskDocument, {
        input: {
            id: String(taskId),
            lastPreview: patch.lastPreview,
            messageCount: patch.messageCount,
            status: patch.status as GraphQlTaskStatus | undefined,
            title: patch.title,
            workspaceId: String(workspaceId),
        },
    })();

    return toTask(result.updateAiHubTask);
}

export async function generateAiHubTaskTitle({
    taskId,
    workspaceId,
}: {
    taskId: number;
    workspaceId: number;
}): Promise<AiHubTaskI> {
    const result = await fetcher<GenerateAiHubTaskTitleMutation, GenerateAiHubTaskTitleMutationVariables>(
        GenerateAiHubTaskTitleDocument,
        {id: String(taskId), workspaceId: String(workspaceId)}
    )();

    return toTask(result.generateAiHubTaskTitle);
}

export async function deleteAiHubTask({taskId, workspaceId}: {taskId: number; workspaceId: number}): Promise<void> {
    await fetcher<unknown, DeleteAiHubTaskMutationVariables>(DeleteAiHubTaskDocument, {
        id: String(taskId),
        workspaceId: String(workspaceId),
    })();
}

export async function getTaskArtifacts({
    taskId,
    workspaceId,
}: {
    taskId: number;
    workspaceId: number;
}): Promise<AiHubTaskArtifactI[]> {
    const result = await fetcher<AiHubTaskArtifactsByAiHubTaskQuery, AiHubTaskArtifactsByAiHubTaskQueryVariables>(
        AiHubTaskArtifactsByAiHubTaskDocument,
        {
            id: String(taskId),
            workspaceId: String(workspaceId),
        }
    )();

    return result.aiHubTaskArtifactsByAiHubTask.map(toArtifact);
}

export async function listArtifacts({
    environment,
    from,
    kind,
    page,
    size,
    to,
    userId,
    workspaceId,
}: {
    environment?: number;
    from?: string;
    kind?: AiHubArtifactKindType;
    page: number;
    size: number;
    to?: string;
    userId?: number;
    workspaceId: number;
}): Promise<ArtifactPageResponseI> {
    const variables: AiHubTaskArtifactsQueryVariables = {
        environment,
        from: from ? new Date(from).getTime() : undefined,
        kind: kind as AiHubTaskArtifactKind | undefined,
        page,
        size,
        to: to ? new Date(to).getTime() : undefined,
        userId: userId !== undefined ? String(userId) : undefined,
        workspaceId: String(workspaceId),
    };

    const result = await fetcher<AiHubTaskArtifactsQuery, AiHubTaskArtifactsQueryVariables>(
        AiHubTaskArtifactsDocument,
        variables
    )();

    const page_ = result.aiHubTaskArtifacts;

    return {
        hasMore: page_.hasMore,
        items: page_.items.map(toArtifact),
        pageClamped: page_.pageClamped,
        sizeClamped: page_.sizeClamped,
        totalCount: Number(page_.totalCount),
    };
}
