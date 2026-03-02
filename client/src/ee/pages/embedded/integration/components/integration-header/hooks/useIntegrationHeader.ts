import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {usePublishIntegrationMutation} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {IntegrationVersionKeys} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import {useGetIntegrationWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

interface UseIntegrationHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    integrationId: number;
}

export const useIntegrationHeader = ({bottomResizablePanelRef, integrationId}: UseIntegrationHeaderProps) => {
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

    const {captureIntegrationPublished, captureIntegrationWorkflowTested} = useAnalytics();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {data: integration} = useGetIntegrationQuery(integrationId, useLoaderData() as Integration);

    const {data: integrationWorkflows} = useGetIntegrationWorkflowsQuery(integrationId, !!integrationId);

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

    const publishIntegrationMutation = usePublishIntegrationMutation({
        onSuccess: () => {
            captureIntegrationPublished();

            if (integration) {
                queryClient.invalidateQueries({
                    queryKey: IntegrationKeys.integration(integration.id!),
                });

                queryClient.invalidateQueries({
                    queryKey: IntegrationVersionKeys.integrationIntegrationVersions(integration.id!),
                });
            }

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.filteredIntegrations({}),
            });

            toast('The integration has been published.');
        },
    });

    const handleIntegrationWorkflowValueChange = (integrationWorkflowId: number) => {
        setWorkflowTestExecution(undefined);
        setCurrentNode(undefined);

        navigate(
            `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowId}?${searchParams}`
        );
    };

    const handlePublishIntegrationSubmit = ({
        description,
        onSuccess,
    }: {
        description?: string;
        onSuccess: () => void;
    }) => {
        if (integration) {
            publishIntegrationMutation.mutate(
                {
                    id: integration.id!,
                    publishIntegrationRequest: {
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
            captureIntegrationWorkflowTested();

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
        captureIntegrationWorkflowTested,
        currentEnvironmentId,
        bottomResizablePanelRef,
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
        handleIntegrationWorkflowValueChange,
        handlePublishIntegrationSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        integration,
        integrationWorkflows,
        publishIntegrationMutationIsPending: publishIntegrationMutation.isPending,
    };
};
