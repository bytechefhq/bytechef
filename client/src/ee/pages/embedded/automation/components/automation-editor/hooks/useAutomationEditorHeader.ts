import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {useAutomationProjectsQuery, usePublishAutomationProjectMutation} from '@/shared/middleware/graphql';
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

interface UseAutomationEditorHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    projectId: string;
}

export const useAutomationEditorHeader = ({bottomResizablePanelRef, projectId}: UseAutomationEditorHeaderProps) => {
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

    const {captureProjectWorkflowTested} = useAnalytics();
    const navigate = useNavigate();

    const {data: projectsData} = useAutomationProjectsQuery();

    const projects = projectsData?.automationProjects ?? [];

    const project = projects.find((automationProject) => automationProject.id === projectId);

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

    const publishProjectMutation = usePublishAutomationProjectMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});

            toast('The project has been published.');
        },
    });

    const handlePublishProjectSubmit = ({description, onSuccess}: {description?: string; onSuccess: () => void}) => {
        if (project) {
            publishProjectMutation.mutate(
                {description, id: project.id},
                {
                    onSuccess: onSuccess,
                }
            );
        }
    };

    const handleWorkflowValueChange = (workflowUuid: string) => {
        void navigate('/embedded/automations/' + workflowUuid + '/editor');
    };

    const handleProjectValueChange = (selectedProjectId: string) => {
        if (selectedProjectId === projectId) {
            return;
        }

        const selectedProject = projects.find((automationProject) => automationProject.id === selectedProjectId);
        const firstWorkflowUuid = selectedProject?.workflowTemplates[0]?.workflowUuid;

        if (!firstWorkflowUuid) {
            return;
        }

        handleWorkflowValueChange(firstWorkflowUuid);
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
            void workflowTestApi.stopWorkflowTest({jobId: currentJobId}, {keepalive: true}).finally(() => {
                persistJobId(null);
                setJobId(null);
            });
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
        handleProjectValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        handleWorkflowValueChange,
        project,
        projects,
        publishProjectMutationIsPending: publishProjectMutation.isPending,
    };
};
