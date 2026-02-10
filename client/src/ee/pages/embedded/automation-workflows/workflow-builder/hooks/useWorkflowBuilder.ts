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
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';

export const useWorkflowBuilder = () => {
    const [initialized, setInitialized] = useState(false);
    const [includeComponents, setIncludeComponents] = useState<string[] | undefined>(undefined);
    const [sharedConnectionIds, setSharedConnectionIds] = useState<number[] | undefined>(undefined);

    const {setWorkflow, workflow} = useWorkflowDataStore();
    const {setShowConnectionNote} = useConnectionNoteStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {setRightSidebarOpen} = useRightSidebarStore();
    const {setShowBottomPanelOpen, setShowEditWorkflowDialog} = useWorkflowEditorStore();
    const {
        reset: workflowNodeDetailsPanelStoreReset,
        setConnectionDialogAllowed,
        setWorkflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore();

    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);

    const {workflowUuid} = useParams();

    const {data: connectedUserProjectWorkflow} = useGetConnectedUserProjectWorkflowQuery(workflowUuid!, initialized);

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            if (workflowUuid) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }
        },
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
        onSuccess: () => {
            if (workflowUuid) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }
        },
    });

    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation({
        onSuccess: () => {
            if (workflowUuid) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }
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

        const listener = (event: MessageEvent) => {
            if (event.data.type === 'EMBED_INIT') {
                const sharedConnectionIds = event.data.params.sharedConnectionIds;
                const connectionDialogAllowed = event.data.params.connectionDialogAllowed ?? false;
                const environment = event.data.params.environment || 'PRODUCTION';
                const includeComponents = event.data.params.includeComponents;
                const jwtToken = event.data.params.jwtToken;

                setConnectionDialogAllowed(connectionDialogAllowed);
                setIncludeComponents(includeComponents);
                setSharedConnectionIds(sharedConnectionIds);

                if (jwtToken) {
                    sessionStorage.setItem('jwtToken', jwtToken);
                    sessionStorage.setItem('environment', environment);
                }

                setInitialized(true);
            }
        };

        window.addEventListener('message', listener);

        return () => {
            setRightSidebarOpen(false);

            window.removeEventListener('message', listener);
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
        connectedUserProjectWorkflow,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        includeComponents,
        initialized,
        projectId: connectedUserProjectWorkflow?.projectId,
        sharedConnectionIds,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        workflowUuid,
    };
};
