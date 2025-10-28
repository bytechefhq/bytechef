import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {Type} from '@/pages/automation/projects/Projects';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {RequestI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {ProjectWorkflowKeys, useGetProjectWorkflowQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

export const useProject = () => {
    const {setIsWorkflowLoaded, setWorkflow, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setIsWorkflowLoaded: state.setIsWorkflowLoaded,
            setWorkflow: state.setWorkflow,
            workflow: state.workflow,
        }))
    );
    const setCopilotPanelOpen = useCopilotStore((state) => state.setCopilotPanelOpen);
    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setProjectLeftSidebarOpen = useProjectsLeftSidebarStore((state) => state.setProjectLeftSidebarOpen);
    const setRightSidebarOpen = useRightSidebarStore((state) => state.setRightSidebarOpen);
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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const {projectId, projectWorkflowId} = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const filterData = {
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const {data: currentWorkflow, isLoading: isWorkflowLoading} = useGetProjectWorkflowQuery(
        +projectId!,
        +projectWorkflowId!,
        !!projectId && !!projectWorkflowId
    );

    const useGetComponentDefinitionsQuery = (request: GetComponentDefinitionsRequestI, enabled?: boolean) => {
        return useGetComponentDefinitionsQuery(request, enabled);
    };

    const useGetConnectionsQuery = (request: RequestI, enabled?: boolean) => {
        return useGetWorkspaceConnectionsQuery(
            {
                environmentId: currentEnvironmentId,
                id: currentWorkspaceId!,
                ...request,
            },
            enabled
        );
    };

    const {data: categories} = useGetProjectCategoriesQuery();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        id: currentWorkspaceId!,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags} = useGetProjectTagsQuery();

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(+projectId!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(+projectId!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(+projectId!),
            });
        },
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(+projectId!),
            });

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.workflows,
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
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
        },
    });

    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
        },
    });

    const handleProjectClick = (projectId: number, projectWorkflowId: number) => {
        navigate(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}?${searchParams}`);
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
    }, [projectWorkflowId]);

    useEffect(() => {
        return () => {
            setProjectLeftSidebarOpen(false);

            setRightSidebarOpen(false);
        };
    }, [setProjectLeftSidebarOpen, setRightSidebarOpen]);

    // Reset loading state when workflow ID changes
    useEffect(() => {
        setIsWorkflowLoaded(false);
    }, [projectWorkflowId, setIsWorkflowLoaded]);

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
        categories,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        filterData,
        handleProjectClick,
        handleWorkflowExecutionsTestOutputCloseClick,
        projectId: parseInt(projectId!),
        projectWorkflowId: parseInt(projectWorkflowId!),
        projects,
        tags,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionsQuery,
    };
};
