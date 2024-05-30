import {WorkflowModel} from '@/shared/middleware/platform/configuration';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {UseMutationResult, useQueryClient} from '@tanstack/react-query';

export interface UpdateWorkflowRequestI {
    id: string;
    workflowModel: WorkflowModel;
}

interface UpdateWorkflowMutationPropsI {
    onSuccess?: (result: WorkflowModel, variables: UpdateWorkflowRequestI) => void;
    onError?: (error: Error, variables: UpdateWorkflowRequestI) => void;
}

interface WorkflowKeysI {
    workflow: (id: string) => Array<string>;
    workflows: Array<string>;
}

const useUpdatePlatformWorkflowMutation = ({
    onError,
    onSuccess,
    useUpdateWorkflowMutation,
    workflowId,
    workflowKeys,
}: {
    useUpdateWorkflowMutation: (
        mutationProps?: UpdateWorkflowMutationPropsI | undefined
    ) => UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequestI, unknown>;
    workflowId: string;
    workflowKeys: WorkflowKeysI;
    onError?: () => void;
    onSuccess?: () => void;
}) => {
    const queryClient = useQueryClient();

    return useUpdateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({
                queryKey: workflowKeys.workflow(workflowId),
            });

            if (onError) {
                onError();
            }
        },
        onSuccess: (workflow: WorkflowModel) => {
            queryClient.invalidateQueries({
                queryKey: workflowKeys.workflow(workflow.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            if (onSuccess) {
                onSuccess();
            }
        },
    });
};

export default useUpdatePlatformWorkflowMutation;
