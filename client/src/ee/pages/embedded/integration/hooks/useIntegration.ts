import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {useUpdateWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {useGetConnectionsQuery as useGetEmbeddedConnectionsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import {
    IntegrationWorkflowKeys,
    useGetIntegrationWorkflowQuery,
} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys} from '@/ee/shared/queries/embedded/workflows.queries';
import {RequestI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

export const useIntegration = () => {
    const [sidebarLoaded, setSidebarLoaded] = useState(false);

    const {setIsWorkflowLoaded, setWorkflow, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setIsWorkflowLoaded: state.setIsWorkflowLoaded,
            setWorkflow: state.setWorkflow,
            workflow: state.workflow,
        }))
    );
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const leftSidebarOpen = useIntegrationsLeftSidebarStore((state) => state.leftSidebarOpen);
    const {setShowBottomPanelOpen, setShowEditWorkflowDialog} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowBottomPanelOpen: state.setShowBottomPanelOpen,
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
        }))
    );
    const setWorkflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.setWorkflowNodeDetailsPanelOpen
    );
    const setWorkflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.setWorkflowTestChatPanelOpen);

    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);
    const sidebarLoadedRef = useRef(false);

    const {integrationId, integrationWorkflowId} = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {data: currentWorkflow, isLoading: isWorkflowLoading} = useGetIntegrationWorkflowQuery(
        +integrationId!,
        +integrationWorkflowId!,
        !!integrationId && !!integrationWorkflowId
    );

    const useGetConnectionsQuery = (request: RequestI, enabled?: boolean) => {
        return useGetEmbeddedConnectionsQuery(
            {
                ...request,
            },
            enabled
        );
    };

    const {data: integrations} = useGetIntegrationsQuery();

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(+integrationId!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(+integrationId!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(+integrationId!),
            });
        },
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(+integrationId!),
            });

            setShowEditWorkflowDialog(false);
        },
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflow(+integrationId!, +integrationWorkflowId!),
            });
        },
    });

    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflow(+integrationId!, +integrationWorkflowId!),
            });
        },
    });

    const invalidateWorkflowQueries = () => {
        const queryKey = IntegrationWorkflowKeys.integrationWorkflows(+integrationId!);

        return queryClient.invalidateQueries({
            queryKey,
        });
    };

    const handleIntegrationClick = (integrationId: number, integrationWorkflowId: number) => {
        navigate(
            `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowId}?${searchParams}`
        );
    };

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

        // Reset state when the component unmounts
        return () => {
            setCopilotPanelOpen(false);
            setWorkflow({});
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        setDataPillPanelOpen(false);
        setWorkflowNodeDetailsPanelOpen(false);
        setWorkflowTestChatPanelOpen(false);

        useWorkflowNodeDetailsPanelStore.getState().reset();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [integrationWorkflowId]);

    useEffect(() => {
        if (leftSidebarOpen && !sidebarLoadedRef.current) {
            sidebarLoadedRef.current = true;
            setSidebarLoaded(true);
        }
    }, [leftSidebarOpen]);

    // Reset loading state when workflow ID changes
    useEffect(() => {
        setIsWorkflowLoaded(false);
    }, [integrationWorkflowId, setIsWorkflowLoaded]);

    // Use useEffect to handle workflow updates with proper synchronization
    useEffect(() => {
        if (currentWorkflow && !isWorkflowLoading) {
            const timeoutId = setTimeout(() => {
                setWorkflow({...currentWorkflow});
            }, 0);

            return () => clearTimeout(timeoutId);
        }
    }, [currentWorkflow, isWorkflowLoading, setWorkflow]);

    return {
        bottomResizablePanelRef,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleIntegrationClick,
        handleWorkflowExecutionsTestOutputCloseClick,
        integrationId: parseInt(integrationId!),
        integrationWorkflowId: parseInt(integrationWorkflowId!),
        integrations,
        invalidateWorkflowQueries,
        leftSidebarOpen,
        sidebarLoaded,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    };
};
