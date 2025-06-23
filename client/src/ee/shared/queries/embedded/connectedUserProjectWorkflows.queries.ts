import {
    ConnectedUserProjectWorkflow,
    ConnectedUserProjectWorkflowApi,
} from '@/ee/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ConnectedUserProjectWorkflowKeys = {
    connectedUserProjectWorkflow: (workflowReferenceCode: string) => [
        ...ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflows,
        workflowReferenceCode,
    ],
    connectedUserProjectWorkflows: ['connectedUserProjectWorkflows'] as const,
};

export const useGetConnectedUserProjectWorkflowQuery = (workflowReferenceCode: string) =>
    useQuery<ConnectedUserProjectWorkflow, Error>({
        queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowReferenceCode),
        queryFn: () =>
            new ConnectedUserProjectWorkflowApi().getConnectedUserProjectWorkflow({
                workflowReferenceCode,
            }),
    });
