import {
    ConnectedUserProjectWorkflowApi,
    PublishConnectedUserProjectWorkflowOperationRequest,
} from '@/ee/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface PublishConnectedUserProjectWorkflowOperationProps {
    onError?: (error: Error, variables: PublishConnectedUserProjectWorkflowOperationRequest) => void;
    onSuccess?: (result: void, variables: PublishConnectedUserProjectWorkflowOperationRequest) => void;
}

export const usePublishConnectedUserProjectWorkflowMutation = (
    mutationProps?: PublishConnectedUserProjectWorkflowOperationProps
) =>
    useMutation({
        mutationFn: (request: PublishConnectedUserProjectWorkflowOperationRequest) => {
            return new ConnectedUserProjectWorkflowApi().publishConnectedUserProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
