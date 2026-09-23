import {HubBuilderContext} from '@/ee/pages/embedded/automation-hub/hubBuilderContext';
import {useEmbedHandshake} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import {
    ConnectedUserProjectWorkflowKeys,
    useGetConnectedUserProjectWorkflowQuery,
} from '@/ee/shared/queries/embedded/connectedUserProjectWorkflows.queries';
import {useConnectionNoteStore} from '@/pages/platform/workflow-editor/stores/useConnectionNoteStore';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {WorkflowNodeDescriptionKeys} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {WorkflowNodeParameterKeys} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useContext, useEffect, useRef, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

export const useWorkflowBuilder = () => {
    const [initialized, setInitialized] = useState(false);
    const [includeComponents, setIncludeComponents] = useState<string[] | undefined>(undefined);

    const hubContext = useContext(HubBuilderContext);

    const {setWorkflow, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setWorkflow: state.setWorkflow,
            workflow: state.workflow,
        }))
    );
    const setShowConnectionNote = useConnectionNoteStore((state) => state.setShowConnectionNote);
    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const setRightSidebarOpen = useRightSidebarStore((state) => state.setRightSidebarOpen);
    const {setShowBottomPanelOpen, setShowEditWorkflowDialog} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowBottomPanelOpen: state.setShowBottomPanelOpen,
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
        }))
    );
    const {
        reset: workflowNodeDetailsPanelStoreReset,
        setConnectionDialogAllowed,
        setWorkflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            reset: state.reset,
            setConnectionDialogAllowed: state.setConnectionDialogAllowed,
            setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
        }))
    );

    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);

    const {workflowUuid} = useParams();

    const {data: connectedUserProjectWorkflow} = useGetConnectedUserProjectWorkflowQuery(workflowUuid!, initialized);

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation();

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation();

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            if (workflowUuid) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }

            setShowEditWorkflowDialog(false);
        },
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: (_result, variables) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowNodeDescriptionKeys.workflowNodeDescription({
                    environmentId: variables.environmentId,
                    id: variables.id,
                    workflowNodeName: variables.workflowNodeName,
                }),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions({
                    environmentId: variables.environmentId,
                    id: variables.id,
                    workflowNodeName: variables.workflowNodeName,
                }),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOutputKeys.workflowNodeOutputs,
            });
        },
    });

    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation();

    const cancelWorkflowQueries = () => {
        if (workflowUuid) {
            const queryKey = ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid);

            return queryClient.cancelQueries({queryKey});
        }

        return Promise.resolve();
    };

    const invalidateWorkflowQueries = () => {
        if (workflowUuid) {
            const queryKey = ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid);

            return queryClient.invalidateQueries({queryKey});
        }

        return Promise.resolve();
    };

    const handleWorkflowExecutionsTestOutputCloseClick = () => {
        setShowBottomPanelOpen(false);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(0);
        }
    };

    useEmbedHandshake((params) => {
        setConnectionDialogAllowed(params.connectionDialogAllowed ?? false);
        setIncludeComponents(params.includeComponents);
        setInitialized(true);
    }, !hubContext);

    useEffect(() => {
        if (!hubContext) {
            return;
        }

        setConnectionDialogAllowed(hubContext.connectionDialogAllowed);
        setIncludeComponents(hubContext.includeComponents);
        setInitialized(true);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [hubContext]);

    useEffect(() => {
        setShowBottomPanelOpen(false);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(0);
        }

        return () => {
            setRightSidebarOpen(false);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        setDataPillPanelOpen(false);
        setWorkflowNodeDetailsPanelOpen(false);
        setShowConnectionNote(false);

        workflowNodeDetailsPanelStoreReset();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowUuid]);

    useEffect(() => {
        if (connectedUserProjectWorkflow) {
            setWorkflow({...connectedUserProjectWorkflow.workflow});
        }

        // Reset state when the component unmounts
        return () => {
            setWorkflow({});
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [connectedUserProjectWorkflow]);

    return {
        bottomResizablePanelRef,
        cancelWorkflowQueries,
        connectedUserProjectWorkflow,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        includeComponents,
        initialized,
        invalidateWorkflowQueries,
        projectId: connectedUserProjectWorkflow?.projectId,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        workflowUuid,
    };
};
