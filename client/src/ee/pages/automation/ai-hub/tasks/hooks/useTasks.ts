import {reportMutationError, useReportQueryError} from '@/shared/error/useReportQueryError';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {
    AiHubArtifactKindType,
    AiHubTaskArtifactI,
    AiHubTaskI,
    AiHubTaskMessageI,
    AiHubTaskPatchI,
    ArtifactPageResponseI,
    createAiHubTask,
    deleteAiHubTask,
    generateAiHubTaskTitle,
    getTaskArtifacts,
    getTaskMessages,
    listArtifacts,
    listTasks,
    patchTask,
} from '../api/tasks.api';

export const AiHubTasksKeys = {
    all: ['aiHubTasks'] as const,
    artifactAudit: (workspaceId: number, filters: object) =>
        [...AiHubTasksKeys.all, 'artifactAudit', workspaceId, filters] as const,
    artifacts: (taskId: number, workspaceId: number) =>
        [...AiHubTasksKeys.all, 'artifacts', taskId, workspaceId] as const,
    list: (workspaceId: number, environment: number, status: 'ACTIVE' | 'ARCHIVED') =>
        [...AiHubTasksKeys.all, 'list', workspaceId, environment, status] as const,
    messages: (taskId: number, workspaceId: number) =>
        [...AiHubTasksKeys.all, 'messages', taskId, workspaceId] as const,
};

export function useAiHubTasksQuery(workspaceId: number, environment: number, status: 'ACTIVE' | 'ARCHIVED') {
    const query = useQuery<AiHubTaskI[], Error>({
        queryFn: () => listTasks({environment, status, workspaceId}),
        queryKey: AiHubTasksKeys.list(workspaceId, environment, status),
        staleTime: 30_000,
    });

    useReportQueryError('List tasks', query.error);

    return query;
}

export function useAiHubTaskMessagesQuery(taskId: number, workspaceId: number, enabled: boolean = true) {
    const query = useQuery<AiHubTaskMessageI[], Error>({
        enabled,
        queryFn: () => getTaskMessages({taskId, workspaceId}),
        queryKey: AiHubTasksKeys.messages(taskId, workspaceId),
    });

    useReportQueryError('Load task messages', query.error);

    return query;
}

export function useCreateAiHubTaskMutation() {
    const queryClient = useQueryClient();

    return useMutation<AiHubTaskI, Error, {environment: number; threadId: string; workspaceId: number}>({
        mutationFn: createAiHubTask,
        onError: (error) => reportMutationError('Create task', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: AiHubTasksKeys.list(variables.workspaceId, variables.environment, 'ACTIVE'),
            });
        },
    });
}

/**
 * The patch/delete/title mutations operate on a single task by id; the task's environment isn't part
 * of the mutation payload (it lives on the persisted row), so the cache invalidations use a prefix that catches every
 * environment + status combination for the workspace. This is correct (we don't know which env-bucket the task
 * lives in client-side, and the worst case is one extra refetch) and matches React Query's default prefix-matching
 * semantics for queryKey arrays.
 */
function listPrefixKeyForWorkspace(workspaceId: number) {
    return [...AiHubTasksKeys.all, 'list', workspaceId] as const;
}

export function usePatchAiHubTaskMutation() {
    const queryClient = useQueryClient();

    return useMutation<AiHubTaskI, Error, {taskId: number; patch: AiHubTaskPatchI; workspaceId: number}>({
        mutationFn: patchTask,
        onError: (error) => reportMutationError('Update task', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: listPrefixKeyForWorkspace(variables.workspaceId)});

            queryClient.invalidateQueries({
                queryKey: AiHubTasksKeys.messages(variables.taskId, variables.workspaceId),
            });
        },
    });
}

export function useDeleteAiHubTaskMutation() {
    const queryClient = useQueryClient();

    return useMutation<void, Error, {taskId: number; workspaceId: number}>({
        mutationFn: deleteAiHubTask,
        onError: (error) => reportMutationError('Delete task', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: listPrefixKeyForWorkspace(variables.workspaceId)});
        },
    });
}

export function useGenerateAiHubTaskTitleMutation() {
    const queryClient = useQueryClient();

    return useMutation<AiHubTaskI, Error, {taskId: number; workspaceId: number}>({
        mutationFn: generateAiHubTaskTitle,
        onError: (error) => reportMutationError('Generate task title', error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: listPrefixKeyForWorkspace(variables.workspaceId)});
        },
    });
}

export function useAiHubTaskArtifactsQuery(taskId: number | undefined, workspaceId: number, enabled = true) {
    const query = useQuery<AiHubTaskArtifactI[], Error>({
        enabled: enabled && taskId !== undefined,
        queryFn: () => getTaskArtifacts({taskId: taskId!, workspaceId}),
        queryKey: AiHubTasksKeys.artifacts(taskId ?? -1, workspaceId),
        staleTime: 60_000,
    });

    useReportQueryError('Load task artifacts', query.error);

    return query;
}

export function useArtifactAuditQuery(
    workspaceId: number,
    filters: {
        environment?: number;
        from?: string;
        kind?: AiHubArtifactKindType;
        page: number;
        size: number;
        to?: string;
        userId?: number;
    }
) {
    const query = useQuery<ArtifactPageResponseI, Error>({
        queryFn: () => listArtifacts({...filters, workspaceId}),
        queryKey: AiHubTasksKeys.artifactAudit(workspaceId, filters),
        staleTime: 30_000,
    });

    useReportQueryError('Load artifact audit', query.error);

    return query;
}
