import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project} from '@/shared/middleware/automation/configuration';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectVersionKeys} from '@/shared/queries/automation/projectVersions.queries';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseProjectHeaderProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
    projectId: number;
}

export const useProjectHeader = ({bottomResizablePanelRef, chatTrigger, projectId}: UseProjectHeaderProps) => {
    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const {setShowBottomPanelOpen, setWorkflowIsRunning, setWorkflowTestExecution, showBottomPanel} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                setShowBottomPanelOpen: state.setShowBottomPanelOpen,
                setWorkflowIsRunning: state.setWorkflowIsRunning,
                setWorkflowTestExecution: state.setWorkflowTestExecution,
                showBottomPanel: state.showBottomPanel,
            }))
        );
    const {setCurrentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                setCurrentNode: state.setCurrentNode,
                setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
                workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
            }))
        );
    const {resetMessages, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} = useWorkflowTestChatStore(
        useShallow((state) => ({
            resetMessages: state.resetMessages,
            setWorkflowTestChatPanelOpen: state.setWorkflowTestChatPanelOpen,
            workflowTestChatPanelOpen: state.workflowTestChatPanelOpen,
        }))
    );
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {captureProjectPublished, captureProjectWorkflowTested} = useAnalytics();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {toast} = useToast();

    const {data: project} = useGetProjectQuery(projectId, useLoaderData() as Project);

    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(projectId, !!projectId);

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            captureProjectPublished();

            if (project) {
                queryClient.invalidateQueries({
                    queryKey: ProjectKeys.project(project.id!),
                });

                queryClient.invalidateQueries({
                    queryKey: ProjectVersionKeys.projectProjectVersions(project.id!),
                });
            }

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.filteredProjects({id: currentWorkspaceId!}),
            });

            toast({
                description: 'The project has been published.',
            });
        },
    });

    const handleProjectWorkflowValueChange = (projectWorkflowId: number) => {
        setWorkflowTestExecution(undefined);
        setCurrentNode(undefined);

        navigate(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}?${searchParams}`);
    };

    const handlePublishProjectSubmit = ({description, onSuccess}: {description?: string; onSuccess: () => void}) => {
        if (project) {
            publishProjectMutation.mutate(
                {
                    id: project.id!,
                    publishProjectRequest: {
                        description,
                    },
                },
                {
                    onSuccess: onSuccess,
                }
            );
        }
    };

    const handleRunClick = () => {
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow.id) {
            captureProjectWorkflowTested();

            if (chatTrigger) {
                resetMessages();
                setDataPillPanelOpen(false);
                setWorkflowNodeDetailsPanelOpen(false);
                setWorkflowTestChatPanelOpen(true);
            } else {
                setWorkflowIsRunning(true);

                workflowTestApi
                    .testWorkflow({
                        environmentId: currentEnvironmentId,
                        id: workflow.id,
                    })
                    .then((workflowTestExecution) => {
                        setWorkflowTestExecution(workflowTestExecution);
                        setWorkflowIsRunning(false);

                        if (bottomResizablePanelRef.current && bottomResizablePanelRef.current.getSize() === 0) {
                            bottomResizablePanelRef.current.resize(35);
                        }
                    })
                    .catch(() => {
                        setWorkflowIsRunning(false);
                        setWorkflowTestExecution(undefined);
                    });
            }
        }
    };

    const handleShowOutputClick = () => {
        setShowBottomPanelOpen(!showBottomPanel);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(!showBottomPanel ? 35 : 0);
        }
    };

    const handleStopClick = useCallback(() => {
        setWorkflowIsRunning(false);

        if (chatTrigger) {
            setWorkflowTestChatPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }
        }
    }, [bottomResizablePanelRef, chatTrigger, setWorkflowIsRunning, setWorkflowTestChatPanelOpen]);

    useEffect(() => {
        if (workflowNodeDetailsPanelOpen || !workflowTestChatPanelOpen) {
            handleStopClick();
        }
    }, [handleStopClick, workflowNodeDetailsPanelOpen, workflowTestChatPanelOpen]);

    return {
        handleProjectWorkflowValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        project,
        projectWorkflows,
        publishProjectMutationIsPending: publishProjectMutation.isPending,
    };
};
