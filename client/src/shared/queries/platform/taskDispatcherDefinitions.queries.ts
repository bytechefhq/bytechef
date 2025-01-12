/* eslint-disable sort-keys */
import {
    GetTaskDispatcherDefinitionRequest,
    TaskDispatcherDefinition,
    TaskDispatcherDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const TaskDispatcherKeys = {
    taskDispatcherDefinition: (request: GetTaskDispatcherDefinitionRequest) => [
        ...TaskDispatcherKeys.taskDispatcherDefinitions,
        request.taskDispatcherName,
        request.taskDispatcherVersion,
    ],
    taskDispatcherDefinitions: ['taskDispatcherDefinitions'] as const,
};

export const useGetTaskDispatcherDefinitionsQuery = () =>
    useQuery<TaskDispatcherDefinition[], Error>({
        queryKey: TaskDispatcherKeys.taskDispatcherDefinitions,
        queryFn: () => new TaskDispatcherDefinitionApi().getTaskDispatcherDefinitions(),
    });

export const useGetTaskDispatcherDefinitionQuery = (request: GetTaskDispatcherDefinitionRequest, enabled?: boolean) =>
    useQuery<TaskDispatcherDefinition, Error>({
        queryKey: TaskDispatcherKeys.taskDispatcherDefinition(request),
        queryFn: () => new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });
