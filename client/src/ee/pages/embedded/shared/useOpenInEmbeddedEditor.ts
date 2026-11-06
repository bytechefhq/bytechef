import {Integration, WorkflowApi} from '@/ee/shared/middleware/embedded/configuration';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';
import {flushSync} from 'react-dom';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

export default function useOpenInEmbeddedEditor() {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCurrentEnvironmentId = useEnvironmentStore((state) => state.setCurrentEnvironmentId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const navigateInDevelopment = useCallback(
        (path: string) => {
            if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
                flushSync(() => setCurrentEnvironmentId(DEVELOPMENT_ENVIRONMENT));
            }

            void navigate(path);
        },
        [currentEnvironmentId, navigate, setCurrentEnvironmentId]
    );

    const openAutomationWorkflow = useCallback(
        (workflowUuid?: string) => {
            if (!workflowUuid) {
                toast('The project has no workflows to open.');

                return;
            }

            navigateInDevelopment(`/embedded/automation-workflows/${workflowUuid}/editor`);
        },
        [navigateInDevelopment]
    );

    const openIntegration = useCallback(
        (integration: Integration) => {
            const firstIntegrationWorkflowId = integration.integrationWorkflowIds?.[0];

            if (integration.id == null || firstIntegrationWorkflowId == null) {
                toast('The integration has no workflows to open.');

                return;
            }

            navigateInDevelopment(
                `/embedded/integrations/${integration.id}/integration-workflows/${firstIntegrationWorkflowId}`
            );
        },
        [navigateInDevelopment]
    );

    const openIntegrationWorkflow = useCallback(
        async (integrationId: number, workflowUuid?: string) => {
            try {
                const integrationWorkflows = await queryClient.query({
                    queryFn: () => new WorkflowApi().getIntegrationWorkflows({id: integrationId}),
                    queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
                });

                const matchingWorkflow = integrationWorkflows.find(
                    (integrationWorkflow) => workflowUuid != null && integrationWorkflow.workflowUuid === workflowUuid
                );

                if (matchingWorkflow?.integrationWorkflowId == null) {
                    toast('This workflow no longer exists in the latest integration version.');

                    return;
                }

                navigateInDevelopment(
                    `/embedded/integrations/${integrationId}/integration-workflows/${matchingWorkflow.integrationWorkflowId}`
                );
            } catch {
                return;
            }
        },
        [navigateInDevelopment, queryClient]
    );

    return {openAutomationWorkflow, openIntegration, openIntegrationWorkflow};
}
