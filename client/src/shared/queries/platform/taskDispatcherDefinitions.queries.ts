/* eslint-disable sort-keys */
import {
    GetTaskDispatcherDefinitionRequest,
    TaskDispatcherDefinitionApi,
    TaskDispatcherDefinitionModel,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const TaskDispatcherKeys = {
    taskDispatcherDefinition: (request: GetTaskDispatcherDefinitionRequest) => [
        request.taskDispatcherName,
        request.taskDispatcherVersion,
    ],
    taskDispatcherDefinitions: ['taskDispatcherDefinitions'] as const,
};

export const useGetTaskDispatcherDefinitionsQuery = () =>
    useQuery<TaskDispatcherDefinitionModel[], Error>({
        queryKey: TaskDispatcherKeys.taskDispatcherDefinitions,
        queryFn: () => new TaskDispatcherDefinitionApi().getTaskDispatcherDefinitions(),
    });

export const useGetTaskDispatcherDefinitionQuery = (request: GetTaskDispatcherDefinitionRequest, enabled?: boolean) =>
    useQuery<TaskDispatcherDefinitionModel, Error>({
        queryKey: TaskDispatcherKeys.taskDispatcherDefinition(request),
        queryFn: () => new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });
