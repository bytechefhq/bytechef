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
    workflowTestConfiguration: (workflowId: string) => [
        ...WorkflowTestConfigurationKeys.workflowTestConfigurations,
        workflowId,
    ],
    workflowTestConfigurations: ['workflowTestConfigurations'] as const,
    workflowTestConfigurationConnections: (request: GetWorkflowTestConfigurationConnectionsRequest) => [
        ...WorkflowTestConfigurationKeys.workflowTestConfigurations,
        request.workflowId,
        request.workflowNodeName,
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
        queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(requestParameters.workflowId),
        queryFn: () => new WorkflowTestConfigurationApi().getWorkflowTestConfiguration(requestParameters),
    });
