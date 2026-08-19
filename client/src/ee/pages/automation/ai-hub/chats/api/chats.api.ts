import {
    AiHubChatArtifactKind,
    AiHubChatArtifactsByAiHubChatDocument,
    AiHubChatArtifactsByAiHubChatQuery,
    AiHubChatArtifactsByAiHubChatQueryVariables,
    AiHubChatArtifactsDocument,
    AiHubChatArtifactsQuery,
    AiHubChatArtifactsQueryVariables,
    AiHubChatMessagesDocument,
    AiHubChatMessagesQuery,
    AiHubChatMessagesQueryVariables,
    AiHubChatStatus as GraphQlChatStatus,
    AiHubChatsDocument,
    AiHubChatsQuery,
    AiHubChatsQueryVariables,
    CreateAiHubChatDocument,
    CreateAiHubChatMutation,
    CreateAiHubChatMutationVariables,
    DeleteAiHubChatDocument,
    DeleteAiHubChatMutationVariables,
    GenerateAiHubChatTitleDocument,
    GenerateAiHubChatTitleMutation,
    GenerateAiHubChatTitleMutationVariables,
    UpdateAiHubChatDocument,
    UpdateAiHubChatMutation,
    UpdateAiHubChatMutationVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';

// Mirrors the Java enum at server/ee/.../chat/AiHubChatArtifactKind.java. Adding a new kind on the
// server side without updating this list is a silent type-drift bug — the UI ends up dispatching `default` on
// strings it doesn't recognize, which silently disables the artifact's row in the audit/sidebar viewers.
export type AiHubArtifactKindType =
    | 'AI_AGENT_REFERENCED'
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
    | 'CHAT_REFERENCED'
    | 'WORKFLOW_CREATED'
    | 'WORKFLOW_EXECUTION_REFERENCED'
    | 'WORKFLOW_EXECUTION_STARTED'
    | 'WORKFLOW_REFERENCED'
    | 'WORKFLOW_UPDATED';

export type AiHubArtifactStatusType = 'APPLIED' | 'EXPIRED' | 'IRREVERSIBLE';

export type ChatStatusType = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface AiHubChatArtifactI {
    artifactId: string;
    artifactName: string;
    chatId: number;
    createdAt: string;
    id: number;
    kind: AiHubArtifactKindType;
    metadataJson: string | null;
    status: AiHubArtifactStatusType;
}

export interface ArtifactPageResponseI {
    hasMore: boolean;
    items: AiHubChatArtifactI[];
    pageClamped?: boolean;
    sizeClamped?: boolean;
    totalCount: number;
}

type GraphQlArtifactType = NonNullable<AiHubChatArtifactsQuery['aiHubChatArtifacts']['items']>[number];

function toArtifact(artifact: GraphQlArtifactType): AiHubChatArtifactI {
    return {
        artifactId: artifact.artifactId,
        artifactName: artifact.artifactName,
        chatId: Number(artifact.chatId),
        createdAt: artifact.createdAt != null ? new Date(Number(artifact.createdAt)).toISOString() : '',
        id: Number(artifact.id),
        kind: artifact.kind as AiHubArtifactKindType,
        metadataJson: artifact.metadataJson ?? null,
        status: artifact.status as AiHubArtifactStatusType,
    };
}

export type ChatKindType = 'AGENT_CHAT' | 'STANDARD' | 'WORKFLOW_CHAT';

/**
 * Whether a chat's turns are served by the server's webhook bridge (messages forwarded to a workflow's
 * webhook trigger) rather than by the AI Hub's own LLM agent. Mirrors {@code AiHubChatKind#isWebhookBridged}.
 *
 * <p>Every UI affordance that exists because a chat has no LLM behind it — no model picker, no artifacts, no
 * suggestion chips, no attachments, cancel via the workflow-stop mutation — asks this instead of comparing
 * against kind constants. Enumerating the constants per call site is how a newly added bridged kind ends up
 * offering an LLM-only affordance in a chat that has no LLM.</p>
 */
export function isWebhookBridgedChat(kind: ChatKindType | undefined): boolean {
    return kind === 'WORKFLOW_CHAT' || kind === 'AGENT_CHAT';
}

export interface AiHubChatI {
    /**
     * Owning {@code AiAgent} id for a chat that originated from an agent's channel run (Slack, a schedule,
     * ...); {@code null} for composer-created chats of every kind, including composer-created
     * {@code AGENT_CHAT} rows. See {@link isChannelAgentChat}, which combines this with
     * {@code workflowExecutionId} rather than trusting either alone.
     */
    aiAgentId: number | null;
    /**
     * Whether the title was set automatically and is still eligible for LLM regeneration. The client
     * fires {@code generateAiHubChatTitle} on every turn while {@code autoTitled} is true; once the
     * LLM regenerates a title (or the user renames the chat) the flag flips to false and the
     * regen loop stops. Workflow chats start at {@code true} so their initial label-based title can be
     * replaced by a more meaningful LLM-generated title after a few turns.
     */
    autoTitled: boolean;
    createdAt: string;
    id: number;
    /**
     * Discriminator for routing UI affordances. {@code STANDARD} → LLM-driven chat;
     * {@code WORKFLOW_CHAT} → bound to a specific workflow execution and bridged to the webhook executor
     * server-side; {@code AGENT_CHAT} → the same bridge, but bound to an AI Agent's generated workflow, which
     * is what the user actually picked. Each kind renders a distinct icon in the sidebar so users can tell
     * them apart at a glance. Use {@link isWebhookBridgedChat} rather than comparing kinds when the question
     * is "does this chat have an LLM behind it".
     */
    kind: ChatKindType;
    lastPreview: string | null;
    messageCount: number;
    status: ChatStatusType;
    threadId: string;
    title: string | null;
    updatedAt: string;
    userId: number;
    /**
     * Composite tenant+UUID string for a webhook-bridged chat ({@code WORKFLOW_CHAT} / composer-created
     * {@code AGENT_CHAT}); otherwise {@code null}. A channel-born {@code AGENT_CHAT} row — recorded when the
     * agent is reached through Slack, a schedule, etc. rather than started from the composer — has no
     * execution to bind to and leaves this {@code null} too. See {@link isChannelAgentChat}, which combines
     * this with {@code aiAgentId} rather than trusting either alone.
     */
    workflowExecutionId: string | null;
    workspaceId: number;
}

/**
 * Whether an {@code AGENT_CHAT} row was recorded from the agent being reached through one of its channels
 * (Slack, a schedule, …) rather than started from the composer's Agents cascade.
 *
 * <p>Mirrors the two-signal guard {@code AiHubAgentConversationRecorder#adoptChat} uses server-side to
 * decide whether an existing row may be adopted by a new turn: {@code aiAgentId != null} (composer-created
 * rows of every kind, including {@code AGENT_CHAT}, leave this null — only the conversation recorder stamps
 * it) AND {@code workflowExecutionId == null} (composer-created {@code AGENT_CHAT} rows always carry one;
 * {@code createAgentChatAiHubChat} requires it as an argument, while the recorder has no execution to bind
 * a channel-born row to). Neither signal alone is trustworthy — the recorder's own doc comment on
 * {@code adoptChat} spells out why it insists on both — so this function does too, matching the
 * server rather than taking a shortcut it deliberately avoids.</p>
 */
export function isChannelAgentChat(chat: Pick<AiHubChatI, 'aiAgentId' | 'kind' | 'workflowExecutionId'>): boolean {
    return chat.kind === 'AGENT_CHAT' && chat.aiAgentId != null && chat.workflowExecutionId == null;
}

/**
 * Client-side display title for a chat, falling back when the server-stored {@code title} is null.
 *
 * <p>A channel-born agent chat (see {@link isChannelAgentChat}) is always untitled: CE passes no title when
 * it reports the turn, bridged kinds never run the LLM-driven title generator, and the channel type it
 * arrives with is dropped server-side (no column carries it). Falling back to the generic "New Chat"
 * placeholder shared with an unstarted draft would make a busy Slack agent's conversations read as the
 * user's own in-progress chats, so channel-born rows get their own generic label instead. The label names
 * neither the agent nor the channel — that data isn't available client-side without new server plumbing,
 * which is deliberately out of scope here.</p>
 */
export function getChatDisplayTitle(
    chat: Pick<AiHubChatI, 'aiAgentId' | 'kind' | 'title' | 'workflowExecutionId'>
): string {
    if (chat.title) {
        return chat.title;
    }

    return isChannelAgentChat(chat) ? 'Agent Conversation' : 'New Chat';
}

export interface AiHubChatMessageI {
    content: string;
    role: string;
    timestamp: string;
    // Nullable JSON array of tool activity attached to this row — see the server's AiHubChatMessage record.
    toolEventsJson: string | null;
}

export interface AiHubChatPatchI {
    lastPreview?: string;
    messageCount?: number;
    status?: ChatStatusType;
    title?: string;
}

type GraphQlChatType = AiHubChatsQuery['aiHubChats'][number];

// Exported for tests: the GraphQL → domain mapping is the source of truth for aiAgentId/workflowExecutionId
// null-handling and the kind discriminator. Direct test coverage avoids round-tripping through every public
// function that calls toChat, and keeps the mapping invariants pinned in one place.
export function toChat(chat: GraphQlChatType): AiHubChatI {
    // Older chats created before the kind column landed surface as `null` from the resolver — they
    // predate workflow chats and are unambiguously STANDARD. Treat any unrecognised value the same way so
    // a future kind addition on the server doesn't crash the sidebar render.
    let kind: ChatKindType = 'STANDARD';

    if (chat.kind === 'WORKFLOW_CHAT') {
        kind = 'WORKFLOW_CHAT';
    } else if (chat.kind === 'AGENT_CHAT') {
        kind = 'AGENT_CHAT';
    }

    return {
        aiAgentId: chat.aiAgentId != null ? Number(chat.aiAgentId) : null,
        autoTitled: chat.autoTitled,
        createdAt: chat.createdAt != null ? new Date(Number(chat.createdAt)).toISOString() : '',
        id: Number(chat.id),
        kind,
        lastPreview: chat.lastPreview ?? null,
        messageCount: chat.messageCount,
        status: chat.status as ChatStatusType,
        threadId: chat.threadId,
        title: chat.title ?? null,
        updatedAt: chat.updatedAt != null ? new Date(Number(chat.updatedAt)).toISOString() : '',
        userId: Number(chat.userId),
        workflowExecutionId: chat.workflowExecutionId ?? null,
        workspaceId: Number(chat.workspaceId),
    };
}

export async function createAiHubChat({
    environment,
    threadId,
    workspaceId,
}: {
    environment: number;
    threadId: string;
    workspaceId: number;
}): Promise<AiHubChatI> {
    const result = await fetcher<CreateAiHubChatMutation, CreateAiHubChatMutationVariables>(CreateAiHubChatDocument, {
        environment,
        threadId,
        workspaceId: String(workspaceId),
    })();

    return toChat(result.createAiHubChat);
}

export async function listChats({
    environment,
    status,
    workspaceId,
}: {
    environment: number;
    status: Exclude<ChatStatusType, 'DELETED'>;
    workspaceId: number;
}): Promise<AiHubChatI[]> {
    const result = await fetcher<AiHubChatsQuery, AiHubChatsQueryVariables>(AiHubChatsDocument, {
        environment,
        status: status as GraphQlChatStatus,
        workspaceId: String(workspaceId),
    })();

    return result.aiHubChats.map(toChat);
}

export async function getChatMessages({
    chatId,
    workspaceId,
}: {
    chatId: number;
    workspaceId: number;
}): Promise<AiHubChatMessageI[]> {
    const result = await fetcher<AiHubChatMessagesQuery, AiHubChatMessagesQueryVariables>(AiHubChatMessagesDocument, {
        id: String(chatId),
        workspaceId: String(workspaceId),
    })();

    return result.aiHubChatMessages.map((message) => ({
        content: message.content,
        role: message.role,
        timestamp: new Date(Number(message.timestamp)).toISOString(),
        toolEventsJson: message.toolEventsJson ?? null,
    }));
}

export async function patchChat({
    chatId,
    patch,
    workspaceId,
}: {
    chatId: number;
    patch: AiHubChatPatchI;
    workspaceId: number;
}): Promise<AiHubChatI> {
    const result = await fetcher<UpdateAiHubChatMutation, UpdateAiHubChatMutationVariables>(UpdateAiHubChatDocument, {
        input: {
            id: String(chatId),
            lastPreview: patch.lastPreview,
            messageCount: patch.messageCount,
            status: patch.status as GraphQlChatStatus | undefined,
            title: patch.title,
            workspaceId: String(workspaceId),
        },
    })();

    return toChat(result.updateAiHubChat);
}

export async function generateAiHubChatTitle({
    chatId,
    workspaceId,
}: {
    chatId: number;
    workspaceId: number;
}): Promise<AiHubChatI> {
    const result = await fetcher<GenerateAiHubChatTitleMutation, GenerateAiHubChatTitleMutationVariables>(
        GenerateAiHubChatTitleDocument,
        {id: String(chatId), workspaceId: String(workspaceId)}
    )();

    return toChat(result.generateAiHubChatTitle);
}

export async function deleteAiHubChat({chatId, workspaceId}: {chatId: number; workspaceId: number}): Promise<void> {
    await fetcher<unknown, DeleteAiHubChatMutationVariables>(DeleteAiHubChatDocument, {
        id: String(chatId),
        workspaceId: String(workspaceId),
    })();
}

export async function getChatArtifacts({
    chatId,
    workspaceId,
}: {
    chatId: number;
    workspaceId: number;
}): Promise<AiHubChatArtifactI[]> {
    const result = await fetcher<AiHubChatArtifactsByAiHubChatQuery, AiHubChatArtifactsByAiHubChatQueryVariables>(
        AiHubChatArtifactsByAiHubChatDocument,
        {
            id: String(chatId),
            workspaceId: String(workspaceId),
        }
    )();

    return result.aiHubChatArtifactsByAiHubChat.map(toArtifact);
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
    const variables: AiHubChatArtifactsQueryVariables = {
        environment,
        from: from ? new Date(from).getTime() : undefined,
        kind: kind as AiHubChatArtifactKind | undefined,
        page,
        size,
        to: to ? new Date(to).getTime() : undefined,
        userId: userId !== undefined ? String(userId) : undefined,
        workspaceId: String(workspaceId),
    };

    const result = await fetcher<AiHubChatArtifactsQuery, AiHubChatArtifactsQueryVariables>(
        AiHubChatArtifactsDocument,
        variables
    )();

    const page_ = result.aiHubChatArtifacts;

    return {
        hasMore: page_.hasMore,
        items: page_.items.map(toArtifact),
        pageClamped: page_.pageClamped,
        sizeClamped: page_.sizeClamped,
        totalCount: Number(page_.totalCount),
    };
}
