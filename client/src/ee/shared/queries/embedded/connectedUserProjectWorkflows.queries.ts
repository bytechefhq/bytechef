import {
    ConnectedUserProjectWorkflow,
    ConnectedUserProjectWorkflowApi,
} from '@/ee/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ConnectedUserProjectWorkflowKeys = {
    connectedUserProjectWorkflow: (workflowUuid: string) => [
        ...ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflows,
        workflowUuid,
    ],
    connectedUserProjectWorkflows: ['connectedUserProjectWorkflows'] as const,
};

export const useGetConnectedUserProjectWorkflowQuery = (workflowUuid: string, enabled = true) =>
    useQuery<ConnectedUserProjectWorkflow, Error>({
        queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
        queryFn: () =>
            new ConnectedUserProjectWorkflowApi().getConnectedUserProjectWorkflow({
                workflowUuid,
            }),
        enabled,
    });
