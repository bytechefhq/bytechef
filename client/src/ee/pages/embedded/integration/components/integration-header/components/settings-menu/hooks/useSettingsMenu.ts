import {Integration, IntegrationApi, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {
    useDeleteIntegrationMutation,
    useRestoreIntegrationVersionMutation,
} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {
    useCreateIntegrationWorkflowMutation,
    useDeleteWorkflowMutation,
} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationCategoryKeys} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationVersionKeys} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys} from '@/ee/shared/queries/embedded/workflows.queries';
import {clearWorkflowHistory} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
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
            void navigate('/embedded/integrations');

            void queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});
            void queryClient.invalidateQueries({
                queryKey: IntegrationCategoryKeys.integrationCategories,
            });
            void queryClient.invalidateQueries({
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
                void navigate(
                    `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowIds[0]}?${searchParams}`
                );
            } else {
                void navigate('/embedded/integrations');
            }

            queryClient.removeQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                    integrationId!,
                    (workflow as Workflow).integrationWorkflowId!
                ),
            });
            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            void queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});
        },
    });

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            captureIntegrationWorkflowImported();

            void queryClient.invalidateQueries({queryKey: IntegrationKeys.integration(integrationId!)});
            void queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId!),
            });

            toast('Workflow is imported.');
        },
    });

    const restoreIntegrationVersionMutation = useRestoreIntegrationVersionMutation({
        onSuccess: async (_, {id, integrationVersion}) => {
            const restoredIntegration = await queryClient.query({
                queryFn: () => new IntegrationApi().getIntegration({id}),
                queryKey: IntegrationKeys.integration(id),
                staleTime: 0,
            });

            const currentIntegrationWorkflowId = workflow.integrationWorkflowId;
            const restoredIntegrationWorkflowIds = restoredIntegration.integrationWorkflowIds ?? [];

            if (
                currentIntegrationWorkflowId &&
                !restoredIntegrationWorkflowIds.includes(currentIntegrationWorkflowId)
            ) {
                const firstIntegrationWorkflowId = restoredIntegrationWorkflowIds[0];

                void navigate(
                    firstIntegrationWorkflowId
                        ? `/embedded/integrations/${id}/integration-workflows/${firstIntegrationWorkflowId}?${searchParams}`
                        : '/embedded/integrations'
                );

                queryClient.removeQueries({
                    queryKey: IntegrationWorkflowKeys.integrationWorkflow(id, currentIntegrationWorkflowId),
                });
            } else if (currentIntegrationWorkflowId) {
                void queryClient.invalidateQueries({
                    queryKey: IntegrationWorkflowKeys.integrationWorkflow(id, currentIntegrationWorkflowId),
                });
            }

            clearWorkflowHistory();

            useWorkflowNodeDetailsPanelStore.getState().reset();

            void queryClient.invalidateQueries({queryKey: IntegrationKeys.integration(id)});
            void queryClient.invalidateQueries({
                queryKey: IntegrationVersionKeys.integrationIntegrationVersions(id),
            });

            toast(`Version V${integrationVersion} applied to the draft.`);
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

    const handleRestoreIntegrationVersion = (integrationVersion: number, onSuccess: () => void) => {
        restoreIntegrationVersionMutation.mutate({id: integrationId!, integrationVersion}, {onSuccess});
    };

    return {
        handleDeleteIntegrationAlertDialogClick,
        handleDeleteWorkflowAlertDialogClick,
        handleImportWorkflow,
        handleRestoreIntegrationVersion,
        isDeleteIntegrationPending: deleteIntegrationMutation.isPending,
        restoreIntegrationVersionPending: restoreIntegrationVersionMutation.isPending,
    };
};
