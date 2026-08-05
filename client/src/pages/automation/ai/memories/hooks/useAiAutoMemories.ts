import {reportMutationError, useReportQueryError} from '@/shared/error/useReportQueryError';
import {
    AiAutoMemoriesQuery,
    AiAutoMemoryQuery,
    AiAutoMemoryType,
    UpdateAiAutoMemoryInput,
    useAiAutoMemoriesQuery as useGeneratedAiAutoMemoriesQuery,
    useAiAutoMemoryQuery as useGeneratedAiAutoMemoryQuery,
    useDeleteAiAutoMemoryMutation as useGeneratedDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation as useGeneratedUpdateAiAutoMemoryMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

// Public consumer-facing type. Codegen produces a TypeScript enum (`AiAutoMemoryType.User`) but the existing UI
// is built around the SCREAMING string literals — keeping the union form lets callers write `'USER'` without
// enum imports while still narrowing.
export type AiAutoMemoryTypeType = 'FEEDBACK' | 'PROJECT' | 'REFERENCE' | 'USER';

export interface AiAutoMemoryI {
    content: string;
    createdAt: string;
    description: string | null;
    environmentId: number;
    id: number;
    memoryType: AiAutoMemoryTypeType;
    name: string;
    title: string;
    updatedAt: string;
    userId: number;
    workspaceId: number;
}

export interface AiAutoMemoryPatchI {
    content?: string;
    description?: string;
    memoryType?: AiAutoMemoryTypeType;
    title?: string;
}

type GraphQlMemoryType = NonNullable<AiAutoMemoryQuery['aiAutoMemory']>;

function toMemory(memory: GraphQlMemoryType): AiAutoMemoryI {
    return {
        content: memory.content,
        createdAt: memory.createdAt != null ? new Date(Number(memory.createdAt)).toISOString() : '',
        description: memory.description ?? null,
        environmentId: Number(memory.environmentId),
        id: Number(memory.id),
        memoryType: memory.memoryType as AiAutoMemoryTypeType,
        name: memory.name,
        title: memory.title,
        updatedAt: memory.updatedAt != null ? new Date(Number(memory.updatedAt)).toISOString() : '',
        userId: Number(memory.userId),
        workspaceId: Number(memory.workspaceId),
    };
}

export const AiAutoMemoriesKeys = {
    all: ['aiAutoMemories'] as const,
    detail: (memoryId: number, workspaceId: number) =>
        [...AiAutoMemoriesKeys.all, 'detail', memoryId, workspaceId] as const,
    list: (workspaceId: number, environmentId: number, memoryType?: AiAutoMemoryTypeType) =>
        [...AiAutoMemoriesKeys.all, 'list', workspaceId, environmentId, memoryType ?? 'ALL'] as const,
};

export function useAiAutoMemoriesQuery(workspaceId: number, environmentId: number, memoryType?: AiAutoMemoryTypeType) {
    const query = useGeneratedAiAutoMemoriesQuery<AiAutoMemoryI[], Error>(
        {
            environment: environmentId,
            memoryType: memoryType as AiAutoMemoryType | undefined,
            workspaceId: String(workspaceId),
        },
        {
            enabled: workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.list(workspaceId, environmentId, memoryType),
            select: (data: AiAutoMemoriesQuery) => data.aiAutoMemories.map(toMemory),
            staleTime: 60_000,
        }
    );

    useReportQueryError('List memories', query.error);

    return query;
}

export function useAiAutoMemoryQuery(memoryId: number | undefined, workspaceId: number, enabled = true) {
    const query = useGeneratedAiAutoMemoryQuery<AiAutoMemoryI | null, Error>(
        {id: String(memoryId ?? -1), workspaceId: String(workspaceId)},
        {
            enabled: enabled && memoryId !== undefined && workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.detail(memoryId ?? -1, workspaceId),
            select: (data: AiAutoMemoryQuery) => (data.aiAutoMemory ? toMemory(data.aiAutoMemory) : null),
        }
    );

    useReportQueryError('Load memory', query.error);

    return query;
}

export function useUpdateAiAutoMemoryMutation() {
    const queryClient = useQueryClient();

    return useGeneratedUpdateAiAutoMemoryMutation({
        onError: (error) => reportMutationError('Update memory', error as Error),
        onSuccess: (_data, variables) => {
            const updateInput = (variables as {input: UpdateAiAutoMemoryInput}).input;

            queryClient.invalidateQueries({
                queryKey: [...AiAutoMemoriesKeys.all, 'list', Number(updateInput.workspaceId)],
            });

            queryClient.invalidateQueries({
                queryKey: AiAutoMemoriesKeys.detail(Number(updateInput.id), Number(updateInput.workspaceId)),
            });
        },
    });
}

export function useDeleteAiAutoMemoryMutation() {
    const queryClient = useQueryClient();

    return useGeneratedDeleteAiAutoMemoryMutation({
        onError: (error) => reportMutationError('Delete memory', error as Error),
        onSuccess: (_data, variables) => {
            const deleteVars = variables as {id: string; workspaceId: string};

            queryClient.invalidateQueries({
                queryKey: [...AiAutoMemoriesKeys.all, 'list', Number(deleteVars.workspaceId)],
            });
        },
    });
}
