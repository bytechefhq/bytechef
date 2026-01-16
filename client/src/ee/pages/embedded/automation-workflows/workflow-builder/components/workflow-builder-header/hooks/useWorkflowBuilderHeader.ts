import {usePublishConnectedUserProjectWorkflowMutation} from '@/ee/shared/mutations/embedded/connectedUserProjectWorkflows.mutations';
import {ConnectedUserProjectWorkflowKeys} from '@/ee/shared/queries/embedded/connectedUserProjectWorkflows.queries';
import {useToast} from '@/hooks/use-toast';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseProjectHeaderProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
    projectId: number;
}

export const useWorkflowBuilderHeader = ({bottomResizablePanelRef, chatTrigger, projectId}: UseProjectHeaderProps) => {
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

    const {captureProjectPublished, captureProjectWorkflowTested} = useAnalytics();
    const navigate = useNavigate();
    const {workflowUuid} = useParams();
    const [searchParams] = useSearchParams();

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishConnectedUserProjectWorkflowMutation = usePublishConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectPublished();

            if (workflowUuid) {
                queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }

            toast({
                description: 'The workflow has been published.',
            });
        },
    });

    const handleProjectWorkflowValueChange = (projectWorkflowId: number) => {
        setWorkflowTestExecution(undefined);
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
            if (bottomResizablePanelRef.current && bottomResizablePanelRef.current.getSize() === 0) {
                bottomResizablePanelRef.current.resize(35);
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
            bottomResizablePanelRef.current.resize(!showBottomPanel ? 35 : 0);
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
        jobId,
        persistJobId,
        setStreamRequest,
        setWorkflowIsRunning,
        setWorkflowTestChatPanelOpen,
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
    // - We are in chat mode (`chatTrigger` is true) and the chat panel is not open (`!workflowTestChatPanelOpen`)
    useEffect(() => {
        if (chatTrigger && !workflowTestChatPanelOpen) {
            handleStopClick();
        }
    }, [chatTrigger, handleStopClick, workflowTestChatPanelOpen]);

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
