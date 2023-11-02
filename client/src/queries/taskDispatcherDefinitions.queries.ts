import {
    TaskDispatcherDefinitionApi,
    TaskDispatcherDefinitionModel,
} from '@/middleware/hermes/configuration';
import {useQuery} from '@tanstack/react-query';

export const TaskDispatcherKeys = {
    taskDispatcherDefinitions: ['taskDispatcherDefinitions'] as const,
};

export const useGetTaskDispatcherDefinitionsQuery = () =>
    useQuery<TaskDispatcherDefinitionModel[], Error>(
        TaskDispatcherKeys.taskDispatcherDefinitions,
        () => new TaskDispatcherDefinitionApi().getTaskDispatcherDefinitions()
    );
