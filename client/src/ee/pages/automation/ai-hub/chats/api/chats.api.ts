import {
    AiHubChatArtifactsByAiHubChatDocument,
    AiHubChatArtifactsByAiHubChatQuery,
    AiHubChatArtifactsByAiHubChatQueryVariables,
    AiHubChatMessagesDocument,
    AiHubChatMessagesQuery,
    AiHubChatMessagesQueryVariables,
    AiHubChatsDocument,
    AiHubChatsQuery,
    AiHubChatsQueryVariables,
    AiHubToolApprovalsDocument,
    AiHubToolApprovalsQuery,
    AiHubToolApprovalsQueryVariables,
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
import {AiHubChatStatus as GraphQlChatStatus} from '@/shared/middleware/graphql-types';
import {fetcher} from '@/shared/middleware/graphqlFetcher';

/**
 * Mirrors the server's {@code AiHubChatArtifactKind}, ported whole so ordinals match; only WORKFLOW_CREATED,
 * WORKFLOW_UPDATED and WORKFLOW_REFERENCED are produced on this line.
 */
export type AiHubArtifactKindType =
    | 'AI_AGENT_REFERENCED'
    | 'API_COLLECTION_REFERENCED'
    | 'BINARY_FILE_CREATED'
    | 'CHAT_REFERENCED'
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

export interface AiHubChatI {
    /**
     * Whether the title was set automatically and is still eligible for LLM regeneration. Flips to false once the
     * LLM regenerates a title or the user renames the chat.
     */
    autoTitled: boolean;
    createdAt: string;
    environmentId: number;
    id: number;
    /** Whether the current caller is this chat's owner. */
    isOwner: boolean;
    lastPreview: string | null;
    messageCount: number;
    ownerUserId: number;
    status: ChatStatusType;
    threadId: string;
    title: string | null;
    updatedAt: string;
    userId: number;
    workspaceId: number;
}

export interface AiHubChatMessageI {
    content: string;
    role: string;
    timestamp: string;
    /** Nullable JSON array of tool activity attached to this row. */
    toolEventsJson: string | null;
}

export type AiHubToolApprovalStatusType = 'APPROVED' | 'EXPIRED' | 'FAILED' | 'PENDING' | 'REJECTED' | 'SUPERSEDED';

export interface AiHubToolApprovalI {
    /** The gated call's arguments as the JSON object string the gate recorded. */
    arguments: string;
    componentName: string | null;
    decidedByUserId: number | null;
    executionError: string | null;
    id: number;
    status: AiHubToolApprovalStatusType;
    toolName: string;
}

export interface AiHubChatPatchI {
    lastPreview?: string;
    messageCount?: number;
    status?: ChatStatusType;
    title?: string;
}

type GraphQlChatType = AiHubChatsQuery['aiHubChats'][number];

type GraphQlArtifactType = AiHubChatArtifactsByAiHubChatQuery['aiHubChatArtifactsByAiHubChat'][number];

const toIsoString = (epochMillis: unknown): string =>
    epochMillis != null ? new Date(Number(epochMillis)).toISOString() : '';

function toArtifact(artifact: GraphQlArtifactType): AiHubChatArtifactI {
    return {
        artifactId: artifact.artifactId,
        artifactName: artifact.artifactName,
        chatId: Number(artifact.chatId),
        createdAt: toIsoString(artifact.createdAt),
        id: Number(artifact.id),
        kind: artifact.kind as AiHubArtifactKindType,
        metadataJson: artifact.metadataJson ?? null,
        status: artifact.status as AiHubArtifactStatusType,
    };
}

export function toChat(chat: GraphQlChatType): AiHubChatI {
    return {
        autoTitled: chat.autoTitled,
        createdAt: toIsoString(chat.createdAt),
        environmentId: Number(chat.environmentId),
        id: Number(chat.id),
        isOwner: chat.isOwner,
        lastPreview: chat.lastPreview ?? null,
        messageCount: chat.messageCount,
        ownerUserId: Number(chat.ownerUserId),
        status: chat.status as ChatStatusType,
        threadId: chat.threadId,
        title: chat.title ?? null,
        updatedAt: toIsoString(chat.updatedAt),
        userId: Number(chat.userId),
        workspaceId: Number(chat.workspaceId),
    };
}

/** Client-side display title for a chat, falling back when the stored title is null. */
export function getChatDisplayTitle(chat: Pick<AiHubChatI, 'title'>): string {
    return chat.title || 'New Chat';
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
        timestamp: toIsoString(message.timestamp),
        toolEventsJson: message.toolEventsJson ?? null,
    }));
}

/**
 * Fetches the tool approvals recorded for a chat, so a reload can tell a still-pending
 * {@code data-tool-approval-request} card apart from one that settled while the client was away.
 */
export async function getToolApprovals({
    chatId,
    workspaceId,
}: {
    chatId: number;
    workspaceId: number;
}): Promise<AiHubToolApprovalI[]> {
    const result = await fetcher<AiHubToolApprovalsQuery, AiHubToolApprovalsQueryVariables>(
        AiHubToolApprovalsDocument,
        {chatId: String(chatId), workspaceId: String(workspaceId)}
    )();

    return result.aiHubToolApprovals.map((approval) => ({
        arguments: approval.arguments,
        componentName: approval.componentName ?? null,
        decidedByUserId: approval.decidedByUserId != null ? Number(approval.decidedByUserId) : null,
        executionError: approval.executionError ?? null,
        id: Number(approval.id),
        status: approval.status as AiHubToolApprovalStatusType,
        toolName: approval.toolName,
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
        {id: String(chatId), workspaceId: String(workspaceId)}
    )();

    return result.aiHubChatArtifactsByAiHubChat.map(toArtifact);
}
