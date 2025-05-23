import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useUpdateWorkflowMutation} from '@/shared/mutations/embedded/workflows.mutations';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {
    IntegrationWorkflowKeys,
    useGetIntegrationWorkflowQuery,
} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {WorkflowKeys} from '@/shared/queries/embedded/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';

export const useIntegration = ({
    integrationId,
    integrationWorkflowId,
}: {
    integrationId: number;
    integrationWorkflowId: number;
}) => {
    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setWorkflowTestChatPanelOpen} = useWorkflowTestChatStore();
    const {setWorkflow, workflow} = useWorkflowDataStore();
    const {setShowBottomPanelOpen, setShowEditWorkflowDialog} = useWorkflowEditorStore();

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const {data: curWorkflow} = useGetIntegrationWorkflowQuery(+integrationId!, +integrationWorkflowId!);

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(integrationId),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });

            setShowEditWorkflowDialog(false);
        },
        useUpdateWorkflowMutation: useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflow(integrationId, integrationWorkflowId),
            });
        },
        useUpdateWorkflowMutation: useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(integrationId),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const handleWorkflowExecutionsTestOutputCloseClick = () => {
        setShowBottomPanelOpen(false);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(0);
        }
    };

    useEffect(() => {
        setShowBottomPanelOpen(false);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(0);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        setWorkflowNodeDetailsPanelOpen(false);
        setWorkflowTestChatPanelOpen(false);

        useWorkflowDataStore.getState().reset();
        useWorkflowNodeDetailsPanelStore.getState().reset();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [integrationWorkflowId]);

    useEffect(() => {
        if (curWorkflow) {
            setWorkflow({...curWorkflow});
        }

        // Reset state when component unmounts
        return () => {
            setWorkflow({});
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [curWorkflow]);

    return {
        bottomResizablePanelRef,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
    };
};
