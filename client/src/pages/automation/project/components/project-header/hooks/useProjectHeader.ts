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
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';

const workflowTestApi = new WorkflowTestApi();

interface UseProjectHeaderProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
    projectId: number;
}

export const useProjectHeader = ({bottomResizablePanelRef, chatTrigger, projectId}: UseProjectHeaderProps) => {
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {workflow} = useWorkflowDataStore();
    const {setShowBottomPanelOpen, setWorkflowIsRunning, setWorkflowTestExecution, showBottomPanel} =
        useWorkflowEditorStore();
    const {setCurrentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {resetMessages, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} = useWorkflowTestChatStore();
    const {currentWorkspaceId} = useWorkspaceStore();

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
