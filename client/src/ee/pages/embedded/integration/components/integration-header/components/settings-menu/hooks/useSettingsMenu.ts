import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteIntegrationMutation} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {
    useCreateIntegrationWorkflowMutation,
    useDeleteWorkflowMutation,
} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationCategoryKeys} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys} from '@/ee/shared/queries/embedded/workflows.queries';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

export const useSettingsMenu = ({integration, workflow}: {integration: Integration; workflow: Workflow}) => {
    const integrationId = integration.id;

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {captureIntegrationWorkflowImported} = useAnalytics();

    const queryClient = useQueryClient();

    const deleteIntegrationMutation = useDeleteIntegrationMutation({
        onSuccess: () => {
            navigate('/embedded/integrations');

            queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});
            queryClient.invalidateQueries({
                queryKey: IntegrationCategoryKeys.integrationCategories,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationTagKeys.integrationTags,
            });
        },
    });

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            const integrationWorkflowIds = integration?.integrationWorkflowIds?.filter(
                (currentIntegrationWorkflowId) =>
                    currentIntegrationWorkflowId !== (workflow as Workflow).integrationWorkflowId
            );

            if (integrationWorkflowIds?.length) {
                navigate(
                    `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowIds[0]}?${searchParams}`
                );
            } else {
                navigate('/embedded/integrations');
            }

            queryClient.removeQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                    integrationId!,
                    (workflow as Workflow).integrationWorkflowId!
                ),
            });
            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});
        },
    });

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            captureIntegrationWorkflowImported();

            queryClient.invalidateQueries({queryKey: IntegrationKeys.integration(integrationId!)});
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId!),
            });

            toast('Workflow is imported.');
        },
    });

    const handleDeleteIntegrationAlertDialogClick = () => {
        if (integrationId) {
            deleteIntegrationMutation.mutate(integrationId);
        }
    };

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (integrationId && workflow.id) {
            deleteWorkflowMutation.mutate({
                id: workflow.id!,
            });
        }
    };

    const handleImportWorkflow = (workflowDefinition: string) => {
        importIntegrationWorkflowMutation.mutate({
            id: integrationId!,
            workflow: {
                definition: workflowDefinition,
            },
        });
    };

    return {
        handleDeleteIntegrationAlertDialogClick,
        handleDeleteWorkflowAlertDialogClick,
        handleImportWorkflow,
    };
};
