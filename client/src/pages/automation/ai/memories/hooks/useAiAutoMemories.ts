import {reportMutationError, useReportQueryError} from '@/shared/error/useReportQueryError';
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
import {useQueryClient} from '@tanstack/react-query';

// Public consumer-facing type. Codegen produces a TypeScript enum (`AiAutoMemoryType.User`) but the existing UI
// is built around the SCREAMING string literals — templating over the enum yields the union of its values, so
// callers still write `'USER'` without enum imports while the set stays tied to the generated schema.
export type AiAutoMemoryTypeType = `${AiAutoMemoryType}`;

// Label and list order for every memory type. Typed as an exhaustive Record so appending a value to the server's
// AiAutoMemoryType is a compile error here, rather than a type that silently never appears as a sidebar filter.
export const AI_AUTO_MEMORY_TYPE_META: Record<AiAutoMemoryTypeType, {label: string; order: number}> = {
    FEEDBACK: {label: 'Feedback', order: 1},
    PROJECT: {label: 'Project', order: 2},
    REFERENCE: {label: 'Reference', order: 3},
    USER: {label: 'User', order: 0},
};

// Every memory type, in the order the UI lists them (which mirrors the server enum's declaration order rather than
// the alphabetical key order the sort-keys rule imposes on the Record above).
export const AI_AUTO_MEMORY_TYPES = (Object.keys(AI_AUTO_MEMORY_TYPE_META) as AiAutoMemoryTypeType[]).sort(
    (left, right) => AI_AUTO_MEMORY_TYPE_META[left].order - AI_AUTO_MEMORY_TYPE_META[right].order
);

// Owner kind of the memory. The pair is carried through rather than collapsed to a single id — the same numeric id
// means a different owner under a different principal type.
export type AiAutoMemoryPrincipalTypeType = 'INTEGRATION_INSTANCE' | 'PROJECT_DEPLOYMENT' | 'USER';

// One selectable owner in the Memories page's Owner picker. `label` is resolved server-side ("My memories" for the
// caller's own entry, the deployment's name otherwise) — render it verbatim rather than deriving a label from the
// principal type, which would surface the word "User" and collide with the USER memory *category*.
export interface AiAutoMemoryPrincipalI {
    label: string;
    memoryCount: number;
    principalId: number;
    principalType: AiAutoMemoryPrincipalTypeType;
}

export interface AiAutoMemoryI {
    content: string;
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
        principalId: Number(memory.principalId),
        principalType: memory.principalType as AiAutoMemoryPrincipalTypeType,
        title: memory.title,
        updatedAt: memory.updatedAt != null ? new Date(Number(memory.updatedAt)).toISOString() : '',
        workspaceId: Number(memory.workspaceId),
    };
}

export const AiAutoMemoriesKeys = {
    all: ['aiAutoMemories'] as const,
    // The environment is part of the detail key because a memory id alone no longer identifies a row the caller can
    // reach: the same id in another environment resolves to null rather than to this row.
    detail: (memoryId: number, workspaceId: number, environmentId: number) =>
        [...AiAutoMemoriesKeys.all, 'detail', memoryId, workspaceId, environmentId] as const,
    // The principal pair is part of the list key: the same workspace/environment/type returns a different set per
    // owner, so leaving it out would serve one owner's memories from another owner's cache entry.
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
            principalType ?? 'SELF',
            principalId ?? 'SELF',
        ] as const,
    principals: (workspaceId: number, environmentId: number) =>
        [...AiAutoMemoriesKeys.all, 'principals', workspaceId, environmentId] as const,
};

export function useAiAutoMemoriesQuery(
    workspaceId: number,
    environmentId: number,
    memoryType?: AiAutoMemoryTypeType,
    principalType?: AiAutoMemoryPrincipalTypeType,
    principalId?: number
) {
    const query = useGeneratedAiAutoMemoriesQuery<AiAutoMemoryI[], Error>(
        {
            environment: environmentId,
            memoryType: memoryType as AiAutoMemoryType | undefined,
            principalId,
            principalType: principalType as AiAutoMemoryPrincipalType | undefined,
            workspaceId: String(workspaceId),
        },
        {
            enabled: workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.list(workspaceId, environmentId, memoryType, principalType, principalId),
            select: (data: AiAutoMemoriesQuery) => data.aiAutoMemories.map(toMemory),
            staleTime: 60_000,
        }
    );

    useReportQueryError('List memories', query.error);

    return query;
}

export function useAiAutoMemoryQuery(
    memoryId: number | undefined,
    workspaceId: number,
    environmentId: number,
    enabled = true,
    principalType?: AiAutoMemoryPrincipalTypeType,
    principalId?: number
) {
    const query = useGeneratedAiAutoMemoryQuery<AiAutoMemoryI | null, Error>(
        {
            environment: environmentId,
            id: String(memoryId ?? -1),
            principalId,
            principalType: principalType as AiAutoMemoryPrincipalType | undefined,
            workspaceId: String(workspaceId),
        },
        {
            enabled: enabled && memoryId !== undefined && workspaceId > 0,
            queryKey: AiAutoMemoriesKeys.detail(memoryId ?? -1, workspaceId, environmentId),
            select: (data: AiAutoMemoryQuery) => (data.aiAutoMemory ? toMemory(data.aiAutoMemory) : null),
        }
    );

    useReportQueryError('Load memory', query.error);

    return query;
}

export function useAiAutoMemoryPrincipalsQuery(workspaceId: number, environmentId: number) {
    const query = useGeneratedAiAutoMemoryPrincipalsQuery<AiAutoMemoryPrincipalI[], Error>(
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

    useReportQueryError('List memory owners', query.error);

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
                queryKey: AiAutoMemoriesKeys.detail(
                    Number(updateInput.id),
                    Number(updateInput.workspaceId),
                    Number(updateInput.environment)
                ),
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
