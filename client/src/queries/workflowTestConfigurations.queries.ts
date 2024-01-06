import {WorkflowTestConfigurationApi, WorkflowTestConfigurationModel} from '@/middleware/hermes/test';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowTestConfigurationKeys = {
    workflowTestConfigurations: ['workflowTestConfigurations'] as const,
};

export const useGetWorkflowTestConfigurationsQuery = () =>
    useQuery<WorkflowTestConfigurationModel[], Error>({
        queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
        queryFn: () => new WorkflowTestConfigurationApi().getWorkflowTestConfigurations(),
    });
