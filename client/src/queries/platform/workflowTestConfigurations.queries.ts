import {
    GetWorkflowTestConfigurationConnectionsRequest,
    WorkflowTestConfigurationApi,
    WorkflowTestConfigurationConnectionModel,
    WorkflowTestConfigurationModel,
} from '@/middleware/platform/workflow/test';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowTestConfigurationKeys = {
    workflowTestConfigurations: ['workflowTestConfigurations'] as const,
    workflowTestConfigurationConnections: (request: GetWorkflowTestConfigurationConnectionsRequest) => [
        ...WorkflowTestConfigurationKeys.workflowTestConfigurations,
        request,
    ],
};

export const useGetWorkflowTestConfigurationConnectionsQuery = (
    request: GetWorkflowTestConfigurationConnectionsRequest
) =>
    useQuery<WorkflowTestConfigurationConnectionModel[], Error>({
        queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurationConnections(request),
        queryFn: () => new WorkflowTestConfigurationApi().getWorkflowTestConfigurationConnections(request),
    });

export const useGetWorkflowTestConfigurationsQuery = () =>
    useQuery<WorkflowTestConfigurationModel[], Error>({
        queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
        queryFn: () => new WorkflowTestConfigurationApi().getWorkflowTestConfigurations(),
    });
