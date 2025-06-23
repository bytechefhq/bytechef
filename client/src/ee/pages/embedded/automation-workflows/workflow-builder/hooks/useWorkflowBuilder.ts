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
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';

export const useWorkflowBuilder = () => {
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

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const {workflowReferenceCode} = useParams();

    const {data: connectedUserProjectWorkflow} = useGetConnectedUserProjectWorkflowQuery(workflowReferenceCode!);

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            if (workflowReferenceCode) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowReferenceCode),
                });
            }
        },
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            if (workflowReferenceCode) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowReferenceCode),
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
            if (workflowReferenceCode) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowReferenceCode),
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
    }, [workflowReferenceCode]);

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
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        includeComponents,
        projectId: connectedUserProjectWorkflow?.projectId,
        sharedConnectionIds,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        workflowReferenceCode,
    };
};
