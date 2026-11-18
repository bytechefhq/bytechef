import {AiAutoMemoryPrincipalType, AiAutoMemoryType} from '@/shared/middleware/graphql';
import {createContext, useContext} from 'react';

import {
    AiAutoMemoryDetailI,
    AiAutoMemoryI,
    AiAutoMemoryPatchI,
    useAiAutoMemoryDetailQuery,
    useDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation,
} from '../hooks/useAiAutoMemories';

export interface MemoryMutationOptionsI {
    onSuccess: () => void;
}

export interface MemoryDeleteRequestI {
    environmentId: number;
    memory: AiAutoMemoryI;
    workspaceId: number;
}

export interface MemoryUpdateRequestI {
    environmentId: number;
    expectedVersion: number;
    memory: AiAutoMemoryI;
    patch: AiAutoMemoryPatchI;
    workspaceId: number;
}

export interface MemoryDetailResultI {
    data: AiAutoMemoryDetailI | null | undefined;
    isPending: boolean;
    refetch: () => Promise<{data?: AiAutoMemoryDetailI | null}>;
}

export interface MemoryMutationResultI<RequestType> {
    isPending: boolean;
    mutate: (request: RequestType, options: MemoryMutationOptionsI) => void;
}

/**
 * The data layer behind the memory dialogs. The default reads and writes workspace memories; the Embedded Memories
 * page provides its own, so the same dialogs edit integration-instance memories. Every member is called as a hook, so
 * a provided value must keep the same functions for the lifetime of the dialogs.
 */
export interface MemoryDialogDataI {
    useDeleteMemory: () => MemoryMutationResultI<MemoryDeleteRequestI>;
    useMemoryDetail: (memory: AiAutoMemoryI | null, workspaceId: number, enabled: boolean) => MemoryDetailResultI;
    useUpdateMemory: () => MemoryMutationResultI<MemoryUpdateRequestI>;
}

function useWorkspaceDeleteMemory(): MemoryMutationResultI<MemoryDeleteRequestI> {
    const deleteMemoryMutation = useDeleteAiAutoMemoryMutation();

    return {
        isPending: deleteMemoryMutation.isPending,
        mutate: ({environmentId, memory, workspaceId}, options) =>
            deleteMemoryMutation.mutate(
                {
                    environment: environmentId,
                    id: String(memory.id),
                    principal: {
                        principalId: memory.principalId,
                        principalType: memory.principalType as AiAutoMemoryPrincipalType,
                    },
                    workspaceId: String(workspaceId),
                },
                options
            ),
    };
}

function useWorkspaceMemoryDetail(
    memory: AiAutoMemoryI | null,
    workspaceId: number,
    enabled: boolean
): MemoryDetailResultI {
    return useAiAutoMemoryDetailQuery(memory, workspaceId, enabled);
}

function useWorkspaceUpdateMemory(): MemoryMutationResultI<MemoryUpdateRequestI> {
    const updateMemoryMutation = useUpdateAiAutoMemoryMutation();

    return {
        isPending: updateMemoryMutation.isPending,
        mutate: ({environmentId, expectedVersion, memory, patch, workspaceId}, options) =>
            updateMemoryMutation.mutate(
                {
                    input: {
                        content: patch.content,
                        description: patch.description,
                        environment: environmentId,
                        expectedVersion,
                        id: String(memory.id),
                        memoryType: patch.memoryType as AiAutoMemoryType | undefined,
                        principal: {
                            principalId: memory.principalId,
                            principalType: memory.principalType as AiAutoMemoryPrincipalType,
                        },
                        title: patch.title,
                        workspaceId: String(workspaceId),
                    },
                },
                options
            ),
    };
}

export const WORKSPACE_MEMORY_DIALOG_DATA: MemoryDialogDataI = {
    useDeleteMemory: useWorkspaceDeleteMemory,
    useMemoryDetail: useWorkspaceMemoryDetail,
    useUpdateMemory: useWorkspaceUpdateMemory,
};

export const MemoryDialogDataContext = createContext<MemoryDialogDataI>(WORKSPACE_MEMORY_DIALOG_DATA);

export const useMemoryDialogData = (): MemoryDialogDataI => useContext(MemoryDialogDataContext);
