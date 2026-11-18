import {
    MemoryDeleteRequestI,
    MemoryDetailResultI,
    MemoryDialogDataI,
    MemoryMutationResultI,
    MemoryUpdateRequestI,
} from '@/pages/automation/ai/memories/dialogs/memoryDialogData';
import {
    AiAutoMemoryDetailI,
    AiAutoMemoryI,
    AiAutoMemoryTypeType,
} from '@/pages/automation/ai/memories/hooks/useAiAutoMemories';
import {
    AiAutoMemoryType,
    EmbeddedAiAutoMemoriesQuery,
    EmbeddedAiAutoMemoryConnectedUsersQuery,
    useDeleteEmbeddedAiAutoMemoryMutation as useGeneratedDeleteEmbeddedAiAutoMemoryMutation,
    useEmbeddedAiAutoMemoriesQuery as useGeneratedEmbeddedAiAutoMemoriesQuery,
    useEmbeddedAiAutoMemoryConnectedUsersQuery as useGeneratedEmbeddedAiAutoMemoryConnectedUsersQuery,
    useUpdateEmbeddedAiAutoMemoryMutation as useGeneratedUpdateEmbeddedAiAutoMemoryMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

/** Integration-instance memories all live in the default workspace. */
export const EMBEDDED_MEMORY_WORKSPACE_ID = 1049;

export interface EmbeddedAiAutoMemoryConnectedUserRefI {
    email: string | null;
    externalId: string;
    id: number;
    name: string | null;
}

export interface EmbeddedAiAutoMemoryIntegrationI {
    componentName: string | null;
    id: number;
    name: string | null;
}

export interface EmbeddedAiAutoMemoryI extends AiAutoMemoryDetailI {
    connectedUser: EmbeddedAiAutoMemoryConnectedUserRefI | null;
    integration: EmbeddedAiAutoMemoryIntegrationI | null;
}

export interface EmbeddedAiAutoMemoryConnectedUserI extends EmbeddedAiAutoMemoryConnectedUserRefI {
    memoryCount: number;
}

type GraphQlEmbeddedMemoryType = EmbeddedAiAutoMemoriesQuery['embeddedAiAutoMemories'][number];

function toTimestamp(value: unknown): string {
    return value != null ? new Date(Number(value)).toISOString() : '';
}

function toEmbeddedMemory(memory: GraphQlEmbeddedMemoryType): EmbeddedAiAutoMemoryI {
    return {
        connectedUser: memory.connectedUser
            ? {
                  email: memory.connectedUser.email ?? null,
                  externalId: memory.connectedUser.externalId,
                  id: Number(memory.connectedUser.id),
                  name: memory.connectedUser.name ?? null,
              }
            : null,
        content: memory.content,
        createdAt: toTimestamp(memory.createdAt),
        description: memory.description ?? null,
        environmentId: Number(memory.environmentId),
        id: Number(memory.id),
        integration: memory.integration
            ? {
                  componentName: memory.integration.componentName ?? null,
                  id: Number(memory.integration.id),
                  name: memory.integration.name ?? null,
              }
            : null,
        memoryType: memory.memoryType as AiAutoMemoryTypeType,
        name: memory.name,
        principalId: Number(memory.integrationInstanceId),
        principalType: 'INTEGRATION_INSTANCE',
        title: memory.title,
        updatedAt: toTimestamp(memory.updatedAt),
        version: Number(memory.version),
        workspaceId: EMBEDDED_MEMORY_WORKSPACE_ID,
    };
}

export const EmbeddedAiAutoMemoriesKeys = {
    all: ['embeddedAiAutoMemories'] as const,
    connectedUsers: (environmentId: number) =>
        [...EmbeddedAiAutoMemoriesKeys.all, 'connectedUsers', environmentId] as const,
    list: (environmentId: number, connectedUserId?: number, memoryType?: AiAutoMemoryTypeType) =>
        [
            ...EmbeddedAiAutoMemoriesKeys.all,
            'list',
            environmentId,
            connectedUserId ?? 'ALL',
            memoryType ?? 'ALL',
        ] as const,
};

export function useEmbeddedAiAutoMemoriesQuery(
    environmentId: number,
    connectedUserId?: number,
    memoryType?: AiAutoMemoryTypeType
) {
    return useGeneratedEmbeddedAiAutoMemoriesQuery<EmbeddedAiAutoMemoryI[], Error>(
        {
            connectedUserId: connectedUserId === undefined ? undefined : String(connectedUserId),
            environment: environmentId,
            memoryType: memoryType as AiAutoMemoryType | undefined,
        },
        {
            queryKey: EmbeddedAiAutoMemoriesKeys.list(environmentId, connectedUserId, memoryType),
            select: (data: EmbeddedAiAutoMemoriesQuery) => data.embeddedAiAutoMemories.map(toEmbeddedMemory),
            staleTime: 60_000,
        }
    );
}

export function useEmbeddedAiAutoMemoryConnectedUsersQuery(environmentId: number) {
    return useGeneratedEmbeddedAiAutoMemoryConnectedUsersQuery<EmbeddedAiAutoMemoryConnectedUserI[], Error>(
        {environment: environmentId},
        {
            queryKey: EmbeddedAiAutoMemoriesKeys.connectedUsers(environmentId),
            select: (data: EmbeddedAiAutoMemoryConnectedUsersQuery) =>
                data.embeddedAiAutoMemoryConnectedUsers.map((connectedUser) => ({
                    email: connectedUser.email ?? null,
                    externalId: connectedUser.externalId,
                    id: Number(connectedUser.id),
                    memoryCount: connectedUser.memoryCount,
                    name: connectedUser.name ?? null,
                })),
            staleTime: 60_000,
        }
    );
}

export function getConnectedUserLabel(connectedUser: EmbeddedAiAutoMemoryConnectedUserRefI): string {
    return connectedUser.name || connectedUser.email || connectedUser.externalId;
}

function useInvalidateEmbeddedMemories() {
    const queryClient = useQueryClient();

    return () => queryClient.invalidateQueries({queryKey: EmbeddedAiAutoMemoriesKeys.all});
}

function useEmbeddedDeleteMemory(): MemoryMutationResultI<MemoryDeleteRequestI> {
    const invalidateEmbeddedMemories = useInvalidateEmbeddedMemories();

    const deleteMemoryMutation = useGeneratedDeleteEmbeddedAiAutoMemoryMutation({
        onSettled: invalidateEmbeddedMemories,
    });

    return {
        isPending: deleteMemoryMutation.isPending,
        mutate: ({environmentId, memory}, options) =>
            deleteMemoryMutation.mutate({environment: environmentId, id: String(memory.id)}, options),
    };
}

function useEmbeddedMemoryDetail(memory: AiAutoMemoryI | null): MemoryDetailResultI {
    const memoryDetail = memory !== null && 'content' in memory ? (memory as AiAutoMemoryDetailI) : null;

    return {
        data: memoryDetail,
        isPending: false,
        refetch: () => Promise.resolve({data: memoryDetail}),
    };
}

function useEmbeddedUpdateMemory(): MemoryMutationResultI<MemoryUpdateRequestI> {
    const invalidateEmbeddedMemories = useInvalidateEmbeddedMemories();

    const updateMemoryMutation = useGeneratedUpdateEmbeddedAiAutoMemoryMutation({
        onSettled: invalidateEmbeddedMemories,
    });

    return {
        isPending: updateMemoryMutation.isPending,
        mutate: ({environmentId, expectedVersion, memory, patch}, options) =>
            updateMemoryMutation.mutate(
                {
                    input: {
                        content: patch.content,
                        description: patch.description,
                        environment: environmentId,
                        expectedVersion,
                        id: String(memory.id),
                        memoryType: patch.memoryType as AiAutoMemoryType | undefined,
                        title: patch.title,
                    },
                },
                options
            ),
    };
}

/**
 * The list query already carries each memory's content, so the detail is the row the dialog was opened with, and a
 * reload re-reads it from the refreshed list.
 */
export const EMBEDDED_MEMORY_DIALOG_DATA: MemoryDialogDataI = {
    useDeleteMemory: useEmbeddedDeleteMemory,
    useMemoryDetail: useEmbeddedMemoryDetail,
    useUpdateMemory: useEmbeddedUpdateMemory,
};
