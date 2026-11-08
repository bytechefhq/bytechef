import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import endChatTestSession from '@/pages/platform/workflow-editor/utils/endChatTestSession';
import startChatTestSession from '@/pages/platform/workflow-editor/utils/startChatTestSession';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {Project} from '@/shared/middleware/automation/configuration';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectVersionKeys} from '@/shared/queries/automation/projectVersions.queries';
import {
    useGetProjectVersionWorkflowsQuery,
    useGetProjectWorkflowsQuery,
} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useMemo, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    projectId: number;
}

export const useProjectHeader = ({bottomResizablePanelRef, chatTrigger, projectId}: UseProjectHeaderProps) => {
    const [jobId, setJobId] = useState<string | null>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setProjectName, workflow} = useWorkflowDataStore();
    const {setShowBottomPanelOpen, setWorkflowIsRunning, setWorkflowTestExecution, showBottomPanel} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                setShowBottomPanelOpen: state.setShowBottomPanelOpen,
                setWorkflowIsRunning: state.setWorkflowIsRunning,
                setWorkflowTestExecution: state.setWorkflowTestExecution,
                showBottomPanel: state.showBottomPanel,
            }))
        );
    const setCurrentNode = useWorkflowNodeDetailsPanelStore((state) => state.setCurrentNode);
    const {chatTriggerName, workflowTestChatPanelOpen} = useWorkflowTestChatStore(
        useShallow((state) => ({
            chatTriggerName: state.chatTriggerName,
            workflowTestChatPanelOpen: state.workflowTestChatPanelOpen,
        }))
    );
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {captureProjectPublished, captureProjectWorkflowTested} = useAnalytics();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {data: project} = useGetProjectQuery(projectId, useLoaderData() as Project);

    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(projectId, !!projectId);

    const lastPublishedProjectVersion = (project?.lastProjectVersion ?? 0) - 1;

    const {data: lastPublishedProjectWorkflows} = useGetProjectVersionWorkflowsQuery(
        projectId,
        lastPublishedProjectVersion,
        false,
        !!projectId && !!project?.lastPublishedDate && lastPublishedProjectVersion > 0
    );

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

            toast('The project has been published.');
        },
    });

    const hasUnpublishedChanges = useMemo(() => {
        if (!project?.lastPublishedDate) {
            return true;
        }

        if ((projectWorkflows ?? []).some((projectWorkflow) => (projectWorkflow.version ?? 1) > 1)) {
            return true;
        }

        if (!projectWorkflows || !lastPublishedProjectWorkflows) {
            return false;
        }

        const publishedWorkflowUuids = new Set(
            lastPublishedProjectWorkflows.map((publishedWorkflow) => publishedWorkflow.workflowUuid)
        );

        return (
            projectWorkflows.length !== publishedWorkflowUuids.size ||
            projectWorkflows.some((projectWorkflow) => !publishedWorkflowUuids.has(projectWorkflow.workflowUuid))
        );
    }, [lastPublishedProjectWorkflows, project?.lastPublishedDate, projectWorkflows]);

    const handleProjectWorkflowValueChange = useCallback(
        (projectWorkflowId: number) => {
            const newSearchParams = new URLSearchParams(searchParams.toString());

            newSearchParams.delete('fromSubflow');
            newSearchParams.delete('parentChain');
            newSearchParams.delete('parentProjectWorkflowId');
            newSearchParams.delete('restoreExecutionPanel');

            if (useWorkflowEditorStore.getState().workflowIsRunning) {
                void navigate(
                    `/automation/projects/${projectId}/project-workflows/${projectWorkflowId}?${newSearchParams}`
                );

                return;
            }

            setWorkflowTestExecution(undefined);
            useWorkflowEditorStore.getState().resetWorkflowTestNodeStates(undefined);
            setCurrentNode(undefined);

            void navigate(
                `/automation/projects/${projectId}/project-workflows/${projectWorkflowId}?${newSearchParams}`
            );

            setShowBottomPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }
        },
        [
            bottomResizablePanelRef,
            navigate,
            projectId,
            searchParams,
            setCurrentNode,
            setShowBottomPanelOpen,
            setWorkflowTestExecution,
        ]
    );

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
                startChatTestSession();
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
        setShowBottomPanelOpen,
        setStreamRequest,
        setWorkflowIsRunning,
        setWorkflowTestExecution,
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

        if (endChatTestSession() && bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(0);
        }

        if (!useWorkflowEditorStore.getState().workflowTestExecution) {
            setShowBottomPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }
        }
    }, [
        bottomResizablePanelRef,
        closeWorkflowTestStream,
        getPersistedJobId,
        jobId,
        persistJobId,
        setShowBottomPanelOpen,
        setStreamRequest,
        setWorkflowIsRunning,
    ]);

    // Stop the workflow execution when:
    // - A chat test session is active (`chatTriggerName`) and the chat panel is not open (`!workflowTestChatPanelOpen`)
    useEffect(() => {
        if (chatTriggerName && !workflowTestChatPanelOpen) {
            handleStopClick();
        }
    }, [chatTriggerName, handleStopClick, workflowTestChatPanelOpen]);

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

    // Store projectName
    useEffect(() => {
        if (!project?.name) {
            return;
        }

        setProjectName(project.name);
    }, [project?.name, setProjectName]);

    return {
        handleProjectWorkflowValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        hasUnpublishedChanges,
        project,
        projectWorkflows,
        publishProjectMutationIsPending: publishProjectMutation.isPending,
    };
};
