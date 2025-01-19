import {
    EnableProjectDeploymentWorkflowRequest,
    ProjectDeploymentApi,
    ProjectDeploymentWorkflow,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface EnableProjectDeploymentWorkflowMutationProps {
    onSuccess?: (result: void, variables: EnableProjectDeploymentWorkflowRequest) => void;
    onError?: (error: Error, variables: EnableProjectDeploymentWorkflowRequest) => void;
}

export const useEnableProjectDeploymentWorkflowMutation = (
    mutationProps: EnableProjectDeploymentWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (request: EnableProjectDeploymentWorkflowRequest) => {
            return new ProjectDeploymentApi().enableProjectDeploymentWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateProjectDeploymentWorkflowMutationProps {
    onSuccess?: (result: void, variables: ProjectDeploymentWorkflow) => void;
    onError?: (error: Error, variables: ProjectDeploymentWorkflow) => void;
}

export const useUpdateProjectDeploymentWorkflowMutation = (
    mutationProps?: UpdateProjectDeploymentWorkflowMutationProps
) =>
    useMutation({
        mutationFn: (projectDeploymentWorkflow: ProjectDeploymentWorkflow) => {
            return new ProjectDeploymentApi().updateProjectDeploymentWorkflow({
                id: projectDeploymentWorkflow.projectDeploymentId!,
                projectDeploymentWorkflow,
                projectDeploymentWorkflowId: projectDeploymentWorkflow.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
