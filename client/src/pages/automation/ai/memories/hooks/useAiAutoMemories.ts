import {
    AiAutoMemoriesQuery,
    AiAutoMemoryPrincipalType,
    AiAutoMemoryPrincipalsQuery,
    AiAutoMemoryQuery,
    AiAutoMemoryType,
    UpdateAiAutoMemoryInput,
    useAiAutoMemoriesQuery as useGeneratedAiAutoMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery as useGeneratedAiAutoMemoryPrincipalsQuery,
    useAiAutoMemoryQuery as useGeneratedAiAutoMemoryQuery,
    useDeleteAiAutoMemoryMutation as useGeneratedDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation as useGeneratedUpdateAiAutoMemoryMutation,
} from '@/shared/middleware/graphql';
import {QueryClient, useQueryClient} from '@tanstack/react-query';

export type AiAutoMemoryTypeType = `${AiAutoMemoryType}`;

export const AI_AUTO_MEMORY_TYPE_META: Record<AiAutoMemoryTypeType, {label: string; order: number}> = {
    FEEDBACK: {label: 'Feedback', order: 1},
    PROJECT: {label: 'Project', order: 2},
    REFERENCE: {label: 'Reference', order: 3},
    USER: {label: 'User', order: 0},
};

export const AI_AUTO_MEMORY_TYPES = (Object.keys(AI_AUTO_MEMORY_TYPE_META) as AiAutoMemoryTypeType[]).sort(
    (left, right) => AI_AUTO_MEMORY_TYPE_META[left].order - AI_AUTO_MEMORY_TYPE_META[right].order
);

const AI_AUTO_MEMORY_TYPE_META_BY_NAME: Partial<Record<string, {label: string}>> = AI_AUTO_MEMORY_TYPE_META;

export const getAiAutoMemoryTypeLabel = (memoryType: string): string =>
    AI_AUTO_MEMORY_TYPE_META_BY_NAME[memoryType]?.label || memoryType;

export type AiAutoMemoryPrincipalTypeType = `${AiAutoMemoryPrincipalType}`;

export interface AiAutoMemoryPrincipalI {
    label: string;
    memoryCount: number;
    principalId: number;
    principalType: AiAutoMemoryPrincipalTypeType;
}

export interface AiAutoMemoryI {
    createdAt: string;
    description: string | null;
    environmentId: number;
    id: number;
    memoryType: AiAutoMemoryTypeType;
    name: string;
    principalId: number;
    principalType: AiAutoMemoryPrincipalTypeType;
    title: string;
    updatedAt: string;
    version: number;
    workspaceId: number;
}

export interface AiAutoMemoryDetailI extends AiAutoMemoryI {
    content: string;
}

export interface AiAutoMemoryPatchI {
    content?: string;
    description?: string;
    memoryType?: AiAutoMemoryTypeType;
    title?: string;
}

type GraphQlMemoryType = AiAutoMemoriesQuery['aiAutoMemories'][number];
type GraphQlMemoryDetailType = NonNullable<AiAutoMemoryQuery['aiAutoMemory']>;

function toMemory(memory: GraphQlMemoryType): AiAutoMemoryI {
    return {
        createdAt: memory.createdAt != null ? new Date(Number(memory.createdAt)).toISOString() : '',
        description: memory.description ?? null,
        environmentId: Number(memory.environmentId),
        id: Number(memory.id),
        memoryType: memory.memoryType as AiAutoMemoryTypeType,
        name: memory.name,
        principalId: Number(memory.principalId),
        principalType: memory.principalType as AiAutoMemoryPrincipalTypeType,
        title: memory.title,
        updatedAt: memory.updatedAt != null ? new Date(Number(memory.updatedAt)).toISOString() : '',
        version: Number(memory.version),
        workspaceId: Number(memory.workspaceId),
    };
}

function toMemoryDetail(memory: GraphQlMemoryDetailType): AiAutoMemoryDetailI {
    return {...toMemory(memory), content: memory.content};
}

export const AiAutoMemoriesKeys = {
    all: ['aiAutoMemories'] as const,
    detail: (
        memoryId: number,
        workspaceId: number,
        environmentId: number,
        principalType?: AiAutoMemoryPrincipalTypeType,
        principalId?: number
    ) =>
        [
            ...AiAutoMemoriesKeys.all,
            'detail',
            memoryId,
            workspaceId,
            environmentId,
            principalType ?? 'SELF',
            principalId ?? 'SELF',
        ] as const,
    detailPrefix: (memoryId: number, workspaceId: number) =>
        [...AiAutoMemoriesKeys.all, 'detail', memoryId, workspaceId] as const,
    list: (
        workspaceId: number,
        environmentId: number,
        memoryType?: AiAutoMemoryTypeType,
        principalType?: AiAutoMemoryPrincipalTypeType,
        principalId?: number
    ) =>
        [
            ...AiAutoMemoriesKeys.all,
            'list',
            workspaceId,
            environmentId,
            memoryType ?? 'ALL',
            principalType ?? 'ALL_OWNERS',
            principalId ?? 'ALL_OWNERS',
        ] as const,
    listPrefix: (workspaceId: number) => [...AiAutoMemoriesKeys.all, 'list', workspaceId] as const,
    principals: (workspaceId: number, environmentId: number) =>
        [...AiAutoMemoriesKeys.all, 'principals', workspaceId, environmentId] as const,
    principalsPrefix: (workspaceId: number) => [...AiAutoMemoriesKeys.all, 'principals', workspaceId] as const,
};

export function useAiAutoMemoriesQuery(
    workspaceId: number,
    environmentId: number,
    memoryType?: AiAutoMemoryTypeType,
    principalType?: AiAutoMemoryPrincipalTypeType,
    principalId?: number
) {
    return useGeneratedAiAutoMemoriesQuery<AiAutoMemoryI[], Error>(
        {
            environment: environmentId,
            memoryType: memoryType as AiAutoMemoryType | undefined,
            principal:
                principalType !== undefined && principalId !== undefined
                    ? {principalId, principalType: principalType as AiAutoMemoryPrincipalType}
                    : undefined,
            workspaceId: String(workspaceId),
        },
        {
            enabled: workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.list(workspaceId, environmentId, memoryType, principalType, principalId),
            select: (data: AiAutoMemoriesQuery) => data.aiAutoMemories.map(toMemory),
            staleTime: 60_000,
        }
    );
}

export function useAiAutoMemoryDetailQuery(memory: AiAutoMemoryI | null, workspaceId: number, enabled = true) {
    return useGeneratedAiAutoMemoryQuery<AiAutoMemoryDetailI | null, Error>(
        {
            environment: memory?.environmentId ?? 0,
            id: String(memory?.id ?? -1),
            principal: memory
                ? {principalId: memory.principalId, principalType: memory.principalType as AiAutoMemoryPrincipalType}
                : undefined,
            workspaceId: String(workspaceId),
        },
        {
            enabled: enabled && memory !== null && workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.detail(
                memory?.id ?? -1,
                workspaceId,
                memory?.environmentId ?? 0,
                memory?.principalType,
                memory?.principalId
            ),
            select: (data: AiAutoMemoryQuery) => (data.aiAutoMemory ? toMemoryDetail(data.aiAutoMemory) : null),
        }
    );
}

export function useAiAutoMemoryPrincipalsQuery(workspaceId: number, environmentId: number) {
    return useGeneratedAiAutoMemoryPrincipalsQuery<AiAutoMemoryPrincipalI[], Error>(
        {environment: environmentId, workspaceId: String(workspaceId)},
        {
            enabled: workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.principals(workspaceId, environmentId),
            select: (data: AiAutoMemoryPrincipalsQuery) =>
                data.aiAutoMemoryPrincipals.map((principal) => ({
                    label: principal.label,
                    memoryCount: principal.memoryCount,
                    principalId: Number(principal.principalId),
                    principalType: principal.principalType as AiAutoMemoryPrincipalTypeType,
                })),
            staleTime: 60_000,
        }
    );
}

function invalidateWorkspaceMemories(queryClient: QueryClient, workspaceId: number, memoryId?: number) {
    return Promise.all([
        queryClient.invalidateQueries({queryKey: AiAutoMemoriesKeys.listPrefix(workspaceId)}),
        queryClient.invalidateQueries({queryKey: AiAutoMemoriesKeys.principalsPrefix(workspaceId)}),
        ...(memoryId === undefined
            ? []
            : [queryClient.invalidateQueries({queryKey: AiAutoMemoriesKeys.detailPrefix(memoryId, workspaceId)})]),
    ]);
}

export function useUpdateAiAutoMemoryMutation() {
    const queryClient = useQueryClient();

    return useGeneratedUpdateAiAutoMemoryMutation({
        onSettled: (_data, _error, variables) => {
            const updateInput = (variables as {input: UpdateAiAutoMemoryInput}).input;

            return invalidateWorkspaceMemories(queryClient, Number(updateInput.workspaceId), Number(updateInput.id));
        },
    });
}

export function useDeleteAiAutoMemoryMutation() {
    const queryClient = useQueryClient();

    return useGeneratedDeleteAiAutoMemoryMutation({
        onSettled: (_data, _error, variables) => {
            const deleteVariables = variables as {id: string; workspaceId: string};

            return invalidateWorkspaceMemories(
                queryClient,
                Number(deleteVariables.workspaceId),
                Number(deleteVariables.id)
            );
        },
    });
}
