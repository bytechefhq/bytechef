import {
    EnableProjectInstanceWorkflowRequest,
    ProjectInstanceApi,
    ProjectInstanceWorkflowModel,
} from '@/middleware/helios/configuration';
import {useMutation} from '@tanstack/react-query';

type EnableProjectInstanceWorkflowMutationProps = {
    onSuccess?: (result: void, variables: EnableProjectInstanceWorkflowRequest) => void;
    onError?: (error: Error, variables: EnableProjectInstanceWorkflowRequest) => void;
};

export const useEnableProjectInstanceWorkflowMutation = (mutationProps: EnableProjectInstanceWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: EnableProjectInstanceWorkflowRequest) => {
            return new ProjectInstanceApi().enableProjectInstanceWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UpdateProjectInstanceWorkflowMutationProps = {
    onSuccess?: (result: ProjectInstanceWorkflowModel, variables: ProjectInstanceWorkflowModel) => void;
    onError?: (error: Error, variables: ProjectInstanceWorkflowModel) => void;
};

export const useUpdateProjectInstanceWorkflowMutation = (mutationProps?: UpdateProjectInstanceWorkflowMutationProps) =>
    useMutation({
        mutationFn: (projectInstanceWorkflowModel: ProjectInstanceWorkflowModel) => {
            return new ProjectInstanceApi().updateProjectInstanceWorkflow({
                id: projectInstanceWorkflowModel.projectInstanceId!,
                projectInstanceWorkflowId: projectInstanceWorkflowModel.id!,
                projectInstanceWorkflowModel,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
