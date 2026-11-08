import {usePublishConnectedUserProjectWorkflowMutation} from '@/ee/shared/mutations/embedded/connectedUserProjectWorkflows.mutations';
import {ConnectedUserProjectWorkflowKeys} from '@/ee/shared/queries/embedded/connectedUserProjectWorkflows.queries';
import useChatTestSession from '@/pages/platform/workflow-editor/hooks/useChatTestSession';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import getSelectedTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getSelectedTestRunTriggerName';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseProjectHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    projectId: number;
}

export const useWorkflowBuilderHeader = ({bottomResizablePanelRef, chatTrigger, projectId}: UseProjectHeaderProps) => {
    const [jobId, setJobId] = useState<string | null>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const {setShowBottomPanelOpen, setWorkflowIsRunning, setWorkflowTestExecution, showBottomPanel, workflowIsRunning} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                setShowBottomPanelOpen: state.setShowBottomPanelOpen,
                setWorkflowIsRunning: state.setWorkflowIsRunning,
                setWorkflowTestExecution: state.setWorkflowTestExecution,
                showBottomPanel: state.showBottomPanel,
                workflowIsRunning: state.workflowIsRunning,
            }))
        );
    const setCurrentNode = useWorkflowNodeDetailsPanelStore((state) => state.setCurrentNode);

    const {captureProjectPublished, captureProjectWorkflowTested} = useAnalytics();

    const {chatSessionRef, endChatTestSession, startChatTestSession, workflowTestChatPanelOpen} =
        useChatTestSession(chatTrigger);
    const navigate = useNavigate();
    const {workflowUuid} = useParams();
    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const publishConnectedUserProjectWorkflowMutation = usePublishConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectPublished();

            if (workflowUuid) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }

            toast('The workflow has been published.');
        },
    });

    const handleProjectWorkflowValueChange = (projectWorkflowId: number) => {
        setWorkflowTestExecution(undefined);
        useWorkflowEditorStore.getState().resetWorkflowTestNodeStates(undefined);
        setCurrentNode(undefined);

        navigate(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}?${searchParams}`);
    };

    const handlePublishProjectSubmit = ({description, onSuccess}: {description?: string; onSuccess: () => void}) => {
        publishConnectedUserProjectWorkflowMutation.mutate(
            {
                publishConnectedUserProjectWorkflowRequest: {
                    description,
                },
                workflowUuid: workflowUuid!,
            },
            {
                onSuccess: onSuccess,
            }
        );
    };

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

    const handleRunClick = useCallback(() => {
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(350);
        }

        if (workflow.id) {
            captureProjectWorkflowTested();

            if (chatTrigger) {
                startChatTestSession(workflow);
            } else {
                setWorkflowIsRunning(true);
                setJobId(null);
                persistJobId(null);

                const request = getTestWorkflowStreamPostRequest({
                    environmentId: currentEnvironmentId,
                    id: workflow.id,
                    testWorkflowRequest: {triggerName: getSelectedTestRunTriggerName(workflow)},
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
        startChatTestSession,
        workflow,
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

        if (jobId) {
            workflowTestApi.stopWorkflowTest({jobId}, {keepalive: true}).finally(() => {
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
        endChatTestSession,
        jobId,
        persistJobId,
        setShowBottomPanelOpen,
        setStreamRequest,
        setWorkflowIsRunning,
    ]);

    // On mount: try to restore an ongoing run using jobId persisted in localStorage.
    // Attach-first approach: immediately call attach with the exact jobId string.
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

    // Stop the workflow execution when:
    // - A chat test session is active (`chatSessionRef`) and the chat panel is not open (`!workflowTestChatPanelOpen`)
    useEffect(() => {
        if (!chatSessionRef.current || workflowTestChatPanelOpen) {
            return;
        }

        if (workflowIsRunning) {
            handleStopClick();
        } else {
            chatSessionRef.current = false;
        }
    }, [chatSessionRef, handleStopClick, workflowIsRunning, workflowTestChatPanelOpen]);

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
        publishProjectMutationIsPending: publishConnectedUserProjectWorkflowMutation.isPending,
    };
};
