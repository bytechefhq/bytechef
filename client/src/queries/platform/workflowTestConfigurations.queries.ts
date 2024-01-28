import {
    GetWorkflowTestConfigurationConnectionsRequest,
    GetWorkflowTestConfigurationRequest,
    WorkflowTestConfigurationApi,
    WorkflowTestConfigurationConnectionModel,
    WorkflowTestConfigurationModel,
} from '@/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowTestConfigurationKeys = {
    workflowTestConfiguration: (request: GetWorkflowTestConfigurationRequest) => [
        ...WorkflowTestConfigurationKeys.workflowTestConfigurations,
        request,
    ],
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

export const useGetWorkflowTestConfigurationQuery = (requestParameters: GetWorkflowTestConfigurationRequest) =>
    useQuery<WorkflowTestConfigurationModel, Error>({
        queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(requestParameters),
        queryFn: () => new WorkflowTestConfigurationApi().getWorkflowTestConfiguration(requestParameters),
    });
