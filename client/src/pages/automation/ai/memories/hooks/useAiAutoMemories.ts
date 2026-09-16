import {
    AiAutoMemoriesQuery,
    AiAutoMemoryPrincipalType,
    AiAutoMemoryPrincipalsQuery,
    AiAutoMemoryType,
    UpdateAiAutoMemoryInput,
    useAiAutoMemoriesQuery as useGeneratedAiAutoMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery as useGeneratedAiAutoMemoryPrincipalsQuery,
    useDeleteAiAutoMemoryMutation as useGeneratedDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation as useGeneratedUpdateAiAutoMemoryMutation,
} from '@/shared/middleware/graphql';
import {QueryClient, useQueryClient} from '@tanstack/react-query';

// Public consumer-facing type. Codegen produces a TypeScript enum (`AiAutoMemoryType.User`) but the UI uses the
// SCREAMING string literals — templating over the enum yields the union of its values, so callers write `'USER'`
// without enum imports while the set stays tied to the generated schema.
export type AiAutoMemoryTypeType = `${AiAutoMemoryType}`;

// Label and list order for every memory type. Typed as an exhaustive Record so appending a value to the server's
// AiAutoMemoryType is a compile error here, rather than a type that silently never appears as a filter option.
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

const AI_AUTO_MEMORY_TYPE_META_BY_NAME: Partial<Record<string, {label: string}>> = AI_AUTO_MEMORY_TYPE_META;

export const getAiAutoMemoryTypeLabel = (memoryType: string): string =>
    AI_AUTO_MEMORY_TYPE_META_BY_NAME[memoryType]?.label || memoryType;

// Owner kind of the memory. The pair is carried through rather than collapsed to a single id — the same numeric id
// means a different owner under a different principal type. Derived from the generated enum like
// AiAutoMemoryTypeType, so a principal type added server-side reaches this union without a hand edit.
export type AiAutoMemoryPrincipalTypeType = `${AiAutoMemoryPrincipalType}`;

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
    version: number;
    workspaceId: number;
}

export interface AiAutoMemoryPatchI {
    content?: string;
    description?: string;
    memoryType?: AiAutoMemoryTypeType;
    title?: string;
}

type GraphQlMemoryType = AiAutoMemoriesQuery['aiAutoMemories'][number];

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
        version: Number(memory.version),
        workspaceId: Number(memory.workspaceId),
    };
}

export const AiAutoMemoriesKeys = {
    all: ['aiAutoMemories'] as const,
    // The principal pair is part of the list key: the same workspace/environment/type returns a different set per
    // owner, so leaving it out would serve one owner's memories from another owner's cache entry. An omitted pair is
    // the server's All-owners scope (every owner the caller may address, see the aiAutoMemories schema description),
    // not the caller alone, and the placeholder says so.
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
    // Prefixes for invalidation that span every environment and filter of a workspace: a mutation knows its
    // workspace, and touching one memory can change any list or owner count in it.
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

// The owners list carries per-owner memory counts and drops an owner whose last memory is gone, so it is refreshed
// alongside the lists rather than left to its staleTime.
function invalidateWorkspaceMemories(queryClient: QueryClient, workspaceId: number) {
    return Promise.all([
        queryClient.invalidateQueries({queryKey: AiAutoMemoriesKeys.listPrefix(workspaceId)}),
        queryClient.invalidateQueries({queryKey: AiAutoMemoriesKeys.principalsPrefix(workspaceId)}),
    ]);
}

export function useUpdateAiAutoMemoryMutation() {
    const queryClient = useQueryClient();

    return useGeneratedUpdateAiAutoMemoryMutation({
        // onSettled rather than onSuccess: an update rejected because someone else changed the memory meanwhile
        // leaves the list showing the old content, and refreshing it is what brings the newer version to the open
        // edit dialog, which offers to reload it.
        onSettled: (_data, _error, variables) => {
            const updateInput = (variables as {input: UpdateAiAutoMemoryInput}).input;

            return invalidateWorkspaceMemories(queryClient, Number(updateInput.workspaceId));
        },
    });
}

export function useDeleteAiAutoMemoryMutation() {
    const queryClient = useQueryClient();

    return useGeneratedDeleteAiAutoMemoryMutation({
        // onSettled rather than onSuccess: a delete answered with NotFound usually means the row was already removed
        // elsewhere, and the list still showing it is exactly the stale view to refresh. Closing the dialog stays a
        // success-only concern of the caller's own onSuccess.
        onSettled: (_data, _error, variables) => {
            const deleteVariables = variables as {id: string; workspaceId: string};

            return invalidateWorkspaceMemories(queryClient, Number(deleteVariables.workspaceId));
        },
    });
}
