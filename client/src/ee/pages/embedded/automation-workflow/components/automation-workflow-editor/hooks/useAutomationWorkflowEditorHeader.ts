import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {
    useAutomationWorkflowProjectsQuery,
    usePublishAutomationWorkflowProjectMutation,
} from '@/shared/middleware/graphql';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseAutomationWorkflowEditorHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    projectId: string;
}

export const useAutomationWorkflowEditorHeader = ({
    bottomResizablePanelRef,
    projectId,
}: UseAutomationWorkflowEditorHeaderProps) => {
    const [jobId, setJobId] = useState<string | null>(null);

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
    const setCurrentNode = useWorkflowNodeDetailsPanelStore((state) => state.setCurrentNode);

    const {captureProjectWorkflowTested} = useAnalytics();
    const navigate = useNavigate();

    const {data: projectsData} = useAutomationWorkflowProjectsQuery();

    const projects = projectsData?.automationWorkflowProjects ?? [];

    const project = projects.find((automationWorkflowProject) => automationWorkflowProject.id === projectId);

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
        onStart: (startedJobId) => setJobId(startedJobId),
        workflowId: workflow.id!,
    });

    const publishProjectMutation = usePublishAutomationWorkflowProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

            toast('The project has been published.');
        },
    });

    const handlePublishProjectSubmit = ({onSuccess}: {description?: string; onSuccess: () => void}) => {
        if (project) {
            publishProjectMutation.mutate(
                {id: project.id},
                {
                    onSuccess: onSuccess,
                }
            );
        }
    };

    const handleWorkflowValueChange = (workflowUuid: string) => {
        setWorkflowTestExecution(undefined);
        setCurrentNode(undefined);

        navigate('/embedded/automation-workflows/' + workflowUuid + '/editor');
    };

    const handleRunClick = useCallback(() => {
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(350);
        }

        if (workflow.id) {
            captureProjectWorkflowTested();

            setWorkflowIsRunning(true);
            setJobId(null);
            persistJobId(null);

            const request = getTestWorkflowStreamPostRequest({
                environmentId: currentEnvironmentId,
                id: workflow.id,
            });

            setStreamRequest(request);
        }
    }, [
        bottomResizablePanelRef,
        captureProjectWorkflowTested,
        currentEnvironmentId,
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
    }, [closeWorkflowTestStream, getPersistedJobId, jobId, persistJobId, setStreamRequest, setWorkflowIsRunning]);

    // On mount: try to restore an ongoing workflow execution using jobId persisted in localStorage by calling
    // attach endpoint.
    useEffect(() => {
        if (!workflow.id || currentEnvironmentId === undefined) {
            return;
        }

        const persistedJobId = getPersistedJobId();

        if (!persistedJobId) {
            return;
        }

        setWorkflowIsRunning(true);
        setJobId(persistedJobId);

        setStreamRequest(getTestWorkflowAttachRequest({jobId: persistedJobId}));
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
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        handleWorkflowValueChange,
        project,
        publishProjectMutationIsPending: publishProjectMutation.isPending,
    };
};
