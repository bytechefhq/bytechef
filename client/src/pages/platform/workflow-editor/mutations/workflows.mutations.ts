import {WorkflowModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/platform/workflows.mutations';
import {WorkflowTestConfigurationKeys} from '@/queries/platform/workflowTestConfigurations.queries';
import {WorkflowKeys} from '@/queries/platform/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';

const useUpdatePlatformWorkflowMutation = ({
    onError,
    onSuccess,
    workflowId,
}: {
    workflowId: string;
    onError?: () => void;
    onSuccess?: () => void;
}) => {
    const queryClient = useQueryClient();

    return useUpdateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflowId),
            });

            if (onError) {
                onError();
            }
        },
        onSuccess: (workflow: WorkflowModel) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
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
