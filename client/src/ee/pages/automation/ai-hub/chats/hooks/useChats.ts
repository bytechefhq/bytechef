import {reportMutationError, useReportQueryError} from '@/shared/error/useReportQueryError';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {
    AiHubChatArtifactI,
    AiHubChatI,
    AiHubChatMessageI,
    AiHubChatPatchI,
    createAiHubChat,
    deleteAiHubChat,
    generateAiHubChatTitle,
    getChatArtifacts,
    getChatMessages,
    listChats,
    patchChat,
} from '../api/chats.api';

export const AiHubChatsKeys = {
    all: ['aiHubChats'] as const,
    artifacts: (chatId: number, workspaceId: number) =>
        [...AiHubChatsKeys.all, 'artifacts', chatId, workspaceId] as const,
    list: (workspaceId: number, environment: number, status: 'ACTIVE' | 'ARCHIVED') =>
        [...AiHubChatsKeys.all, 'list', workspaceId, environment, status] as const,
    messages: (chatId: number, workspaceId: number) =>
        [...AiHubChatsKeys.all, 'messages', chatId, workspaceId] as const,
};

/**
 * Prefix covering every environment and status list of a workspace; a chat mutation does not know which bucket the
 * chat lives in, so it invalidates them all.
 */
function listPrefixKeyForWorkspace(workspaceId: number) {
    return [...AiHubChatsKeys.all, 'list', workspaceId] as const;
}

export function useAiHubChatsQuery(workspaceId: number, environment: number, status: 'ACTIVE' | 'ARCHIVED') {
    const query = useQuery<AiHubChatI[], Error>({
        queryFn: () => listChats({environment, status, workspaceId}),
        queryKey: AiHubChatsKeys.list(workspaceId, environment, status),
        staleTime: 30_000,
    });

    useReportQueryError('List chats', query.error);

    return query;
}

export function useAiHubChatMessagesQuery(chatId: number, workspaceId: number, enabled: boolean = true) {
    const query = useQuery<AiHubChatMessageI[], Error>({
        enabled,
        queryFn: () => getChatMessages({chatId, workspaceId}),
        queryKey: AiHubChatsKeys.messages(chatId, workspaceId),
    });

    useReportQueryError('Load chat messages', query.error);

    return query;
}

export function useAiHubChatArtifactsQuery(chatId: number | undefined, workspaceId: number, enabled = true) {
    const query = useQuery<AiHubChatArtifactI[], Error>({
        enabled: enabled && chatId !== undefined,
        queryFn: () => getChatArtifacts({chatId: chatId!, workspaceId}),
        queryKey: AiHubChatsKeys.artifacts(chatId ?? -1, workspaceId),
        staleTime: 60_000,
    });

    useReportQueryError('Load chat artifacts', query.error);

    return query;
}

export function useCreateAiHubChatMutation() {
    const queryClient = useQueryClient();

    return useMutation<AiHubChatI, Error, {environment: number; threadId: string; workspaceId: number}>({
        mutationFn: createAiHubChat,
        onError: (error) => reportMutationError('Create chat', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: AiHubChatsKeys.list(variables.workspaceId, variables.environment, 'ACTIVE'),
            });
        },
    });
}

export function usePatchAiHubChatMutation() {
    const queryClient = useQueryClient();

    return useMutation<AiHubChatI, Error, {chatId: number; patch: AiHubChatPatchI; workspaceId: number}>({
        mutationFn: patchChat,
        onError: (error) => reportMutationError('Update chat', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: listPrefixKeyForWorkspace(variables.workspaceId)});

            queryClient.invalidateQueries({
                queryKey: AiHubChatsKeys.messages(variables.chatId, variables.workspaceId),
            });
        },
    });
}

export function useDeleteAiHubChatMutation() {
    const queryClient = useQueryClient();

    return useMutation<void, Error, {chatId: number; workspaceId: number}>({
        mutationFn: deleteAiHubChat,
        onError: (error) => reportMutationError('Delete chat', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: listPrefixKeyForWorkspace(variables.workspaceId)});
        },
    });
}

export function useGenerateAiHubChatTitleMutation() {
    const queryClient = useQueryClient();

    return useMutation<AiHubChatI, Error, {chatId: number; workspaceId: number}>({
        mutationFn: generateAiHubChatTitle,
        onError: (error) => reportMutationError('Generate chat title', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: listPrefixKeyForWorkspace(variables.workspaceId)});
        },
    });
}
