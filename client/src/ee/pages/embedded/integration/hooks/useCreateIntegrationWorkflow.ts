import {useCreateIntegrationWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

export const useCreateIntegrationWorkflow = ({
    bottomResizablePanelRef,
    integrationId,
}: {
    bottomResizablePanelRef?: RefObject<PanelImperativeHandle | null>;
    integrationId: number;
}) => {
    const setShowBottomPanelOpen = useWorkflowEditorStore((state) => state.setShowBottomPanelOpen);

    const {captureIntegrationWorkflowCreated} = useAnalytics();

    const queryClient = useQueryClient();

    const navigate = useNavigate();

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (createdIntegrationWorkflowId) => {
            captureIntegrationWorkflowCreated();

            void queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });

            void queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });

            setShowBottomPanelOpen(false);

            bottomResizablePanelRef?.current?.resize(0);

            void navigate(
                `/embedded/integrations/${integrationId}/integration-workflows/${createdIntegrationWorkflowId}`
            );
        },
    });

    return createIntegrationWorkflowMutation;
};
