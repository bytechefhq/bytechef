import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {RequestI} from '@/shared/components/connection/providers/connectionReactQueryProvider';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {ProjectWorkflowKeys, useGetProjectWorkflowQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';

export const useProject = ({projectId, projectWorkflowId}: {projectId: number; projectWorkflowId: number}) => {
    const {setWorkflow, workflow} = useWorkflowDataStore();

    const {setShowBottomPanelOpen, setShowEditWorkflowDialog} = useWorkflowEditorStore();
    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setWorkflowTestChatPanelOpen} = useWorkflowTestChatStore();
    const {currentWorkspaceId} = useWorkspaceStore();

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const {data: currentWorkflow} = useGetProjectWorkflowQuery(
        projectId,
        projectWorkflowId,
        !!projectId && !!projectWorkflowId
    );

    const useGetConnectionsQuery = (request: RequestI, enabled?: boolean) => {
        return useGetWorkspaceConnectionsQuery(
            {
                id: currentWorkspaceId!,
                ...request,
            },
            enabled
        );
    };

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(projectId),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(projectId, projectWorkflowId),
            });
        },
        useUpdateWorkflowMutation: useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
            });

            setShowEditWorkflowDialog(false);
        },
        useUpdateWorkflowMutation: useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(projectId, projectWorkflowId),
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

        useWorkflowNodeDetailsPanelStore.getState().reset();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectWorkflowId]);

    useEffect(() => {
        if (currentWorkflow) {
            setWorkflow({...currentWorkflow});
        }

        // Reset state when component unmounts
        return () => {
            setWorkflow({});
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentWorkflow]);

    return {
        bottomResizablePanelRef,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    };
};
