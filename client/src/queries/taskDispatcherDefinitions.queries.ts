import {useQuery} from '@tanstack/react-query';

import {
    TaskDispatcherDefinitionModel,
    TaskDispatcherDefinitionsApi,
} from '../middleware/core/definition-registry';

export const TaskDispatcherKeys = {
    taskDispatcherDefinitions: ['taskDispatcherDefinitions'] as const,
};

export const useGetTaskDispatcherDefinitionsQuery = () =>
    useQuery<TaskDispatcherDefinitionModel[], Error>(
        TaskDispatcherKeys.taskDispatcherDefinitions,
        () => new TaskDispatcherDefinitionsApi().getTaskDispatcherDefinitions()
    );
