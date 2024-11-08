import {
    EnableProjectInstanceWorkflowRequest,
    ProjectInstanceApi,
    ProjectInstanceWorkflow,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface EnableProjectInstanceWorkflowMutationProps {
    onSuccess?: (result: void, variables: EnableProjectInstanceWorkflowRequest) => void;
    onError?: (error: Error, variables: EnableProjectInstanceWorkflowRequest) => void;
}

export const useEnableProjectInstanceWorkflowMutation = (mutationProps: EnableProjectInstanceWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: EnableProjectInstanceWorkflowRequest) => {
            return new ProjectInstanceApi().enableProjectInstanceWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectInstanceWorkflowMutationProps {
    onSuccess?: (result: void, variables: ProjectInstanceWorkflow) => void;
    onError?: (error: Error, variables: ProjectInstanceWorkflow) => void;
}

export const useUpdateProjectInstanceWorkflowMutation = (mutationProps?: UpdateProjectInstanceWorkflowMutationProps) =>
    useMutation({
        mutationFn: (projectInstanceWorkflow: ProjectInstanceWorkflow) => {
            return new ProjectInstanceApi().updateProjectInstanceWorkflow({
                id: projectInstanceWorkflow.projectInstanceId!,
                projectInstanceWorkflow,
                projectInstanceWorkflowId: projectInstanceWorkflow.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
