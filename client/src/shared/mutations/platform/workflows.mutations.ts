import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {UseMutationResult, useQueryClient} from '@tanstack/react-query';

export interface UpdateWorkflowRequestI {
    id: string;
    workflow: Workflow;
}

interface UpdateWorkflowMutationPropsI {
    onSuccess?: (result: void, variables: UpdateWorkflowRequestI) => void;
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
    ) => UseMutationResult<void, Error, UpdateWorkflowRequestI, unknown>;
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
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: workflowKeys.workflow(workflowId),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflowId),
            });

            if (onSuccess) {
                onSuccess();
            }
        },
    });
};

export default useUpdatePlatformWorkflowMutation;
