import {CreateProjectWorkflowRequest, ProjectApi, WorkflowModel} from '@/middleware/helios/configuration';
import {useMutation} from '@tanstack/react-query';

type CreateProjectWorkflowMutationProps = {
    onSuccess?: (result: WorkflowModel, variables: CreateProjectWorkflowRequest) => void;
    onError?: (error: Error, variables: CreateProjectWorkflowRequest) => void;
};

export const useCreateProjectWorkflowMutation = (mutationProps?: CreateProjectWorkflowMutationProps) => {
    return useMutation({
        mutationFn: (request: CreateProjectWorkflowRequest) => {
            return new ProjectApi().createProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};
