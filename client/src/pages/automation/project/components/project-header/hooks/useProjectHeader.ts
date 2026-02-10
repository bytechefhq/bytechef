import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {Project} from '@/shared/middleware/automation/configuration';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectVersionKeys} from '@/shared/queries/automation/projectVersions.queries';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle>;
    chatTrigger?: boolean;
    projectId: number;
}

export const useProjectHeader = ({bottomResizablePanelRef, chatTrigger, projectId}: UseProjectHeaderProps) => {
    const [jobId, setJobId] = useState<string | null>(null);

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
    const {setCurrentNode, setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            setCurrentNode: state.setCurrentNode,
            setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
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

    const {
        close: closeWorkflowTestStream,
        error: workflowTestStreamError,
        getPersistedJobId,
        persistJobId,
        setStreamRequest,
    } = useWorkflowTestStream({
        onError: () => setJobId(null),
        onResult: () => {
            if (bottomResizablePanelRef.current && bottomResizablePanelRef.current.getSize().asPercentage === 0) {
                bottomResizablePanelRef.current.resize(350);
            }

            setJobId(null);
        },
        onStart: (jobId) => setJobId(jobId),
        workflowId: workflow.id!,
    });

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

    const handleRunClick = useCallback(() => {
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(350);
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
                setJobId(null);
                persistJobId(null);

                const request = getTestWorkflowStreamPostRequest({
                    environmentId: currentEnvironmentId,
                    id: workflow.id,
                });
                setStreamRequest(request);
            }
        }
    }, [
        captureProjectWorkflowTested,
        currentEnvironmentId,
        bottomResizablePanelRef,
        chatTrigger,
        persistJobId,
        resetMessages,
        setDataPillPanelOpen,
        setShowBottomPanelOpen,
        setStreamRequest,
        setWorkflowIsRunning,
        setWorkflowNodeDetailsPanelOpen,
        setWorkflowTestExecution,
        setWorkflowTestChatPanelOpen,
        workflow.id,
    ]);

    const handleShowOutputClick = () => {
        setShowBottomPanelOpen(!showBottomPanel);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(!showBottomPanel ? 350 : 0);
        }
    };

    const handleStopClick = useCallback(() => {
        setWorkflowIsRunning(false);
        closeWorkflowTestStream();
        setStreamRequest(null);

        const currentJobId = jobId || getPersistedJobId();

        if (currentJobId) {
            workflowTestApi.stopWorkflowTest({jobId: currentJobId}, {keepalive: true}).finally(() => {
                persistJobId(null);
                setJobId(null);
            });
        }

        if (chatTrigger) {
            setWorkflowTestChatPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }
        }
    }, [
        bottomResizablePanelRef,
        chatTrigger,
        closeWorkflowTestStream,
        getPersistedJobId,
        jobId,
        persistJobId,
        setStreamRequest,
        setWorkflowIsRunning,
        setWorkflowTestChatPanelOpen,
    ]);

    // Stop the workflow execution when:
    // - We are in chat mode (`chatTrigger` is true) and the chat panel is not open (`!workflowTestChatPanelOpen`)
    useEffect(() => {
        if (chatTrigger && !workflowTestChatPanelOpen) {
            handleStopClick();
        }
    }, [chatTrigger, handleStopClick, workflowTestChatPanelOpen]);

    // On mount: try to restore an ongoing workflow execution using jobId persisted in localStorage by calling
    // attach endpoint.
    useEffect(() => {
        if (!workflow.id || currentEnvironmentId === undefined) {
            return;
        }

        const jobId = getPersistedJobId();

        if (!jobId) {
            return;
        }

        setWorkflowIsRunning(true);
        setJobId(jobId);

        setStreamRequest(getTestWorkflowAttachRequest({jobId}));
    }, [workflow.id, currentEnvironmentId, getPersistedJobId, setWorkflowIsRunning, setJobId, setStreamRequest]);

    useEffect(() => {
        if (workflowTestStreamError) {
            setWorkflowIsRunning(false);
            setStreamRequest(null);
            persistJobId(null);
            setJobId(null);
        }
    }, [workflowTestStreamError, persistJobId, setWorkflowIsRunning, setStreamRequest]);

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
