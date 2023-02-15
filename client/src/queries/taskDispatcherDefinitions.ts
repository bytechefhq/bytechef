import {useQuery} from '@tanstack/react-query';
import {
    TaskDispatcherDefinitionModel,
    TaskDispatcherDefinitionsApi,
} from '../data-access/task-dispatcher-definition';

export const TaskDispatcherKeys = {
    taskDispatcherDefinitions: ['taskDispatcherDefinitions'] as const,
};

export const useGetTaskDispatcherDefinitionsQuery = () =>
    useQuery<TaskDispatcherDefinitionModel[], Error>(
        TaskDispatcherKeys.taskDispatcherDefinitions,
        () => new TaskDispatcherDefinitionsApi().getTaskDispatcherDefinitions(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
