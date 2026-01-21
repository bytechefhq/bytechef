import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationVersionHistorySheet from '@/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet';
import IntegrationHeaderDeleteIntegrationAlertDialog from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderDeleteIntegrationAlertDialog';
import IntegrationHeaderHistoryButton from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderHistoryButton';
import IntegrationHeaderIntegrationDropDownMenu from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderIntegrationDropDownMenu';
import IntegrationHeaderOutputButton from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderOutputButton';
import IntegrationHeaderPublishPopover from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderPublishPopover';
import IntegrationHeaderRunButton from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderRunButton';
import IntegrationHeaderStopButton from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderStopButton';
import IntegrationHeaderTitle from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderTitle';
import IntegrationHeaderWorkflowDropDownMenu from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderWorkflowDropDownMenu';
import IntegrationHeaderWorkflowSelect from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeaderWorkflowSelect';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteIntegrationMutation} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {
    useCreateIntegrationWorkflowMutation,
    useDeleteWorkflowMutation,
} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationCategoryKeys} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

const IntegrationHeader = ({
    bottomResizablePanelRef,
    integrationId,
    integrationWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    integrationId: number;
    integrationWorkflowId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) => {
    const [jobId, setJobId] = useState<string | null>(null);
    const [showDeleteIntegrationAlertDialog, setShowDeleteIntegrationAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditIntegrationDialog, setShowEditIntegrationDialog] = useState(false);
    const [showIntegrationVersionHistorySheet, setShowIntegrationVersionHistorySheet] = useState(false);

    const {
        setShowBottomPanelOpen,
        setShowEditWorkflowDialog,
        setWorkflowIsRunning,
        setWorkflowTestExecution,
        showEditWorkflowDialog,
        workflowIsRunning,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowBottomPanelOpen: state.setShowBottomPanelOpen,
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
            setWorkflowIsRunning: state.setWorkflowIsRunning,
            setWorkflowTestExecution: state.setWorkflowTestExecution,
            showEditWorkflowDialog: state.showEditWorkflowDialog,
            workflowIsRunning: state.workflowIsRunning,
        }))
    );
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const setCurrentNode = useWorkflowNodeDetailsPanelStore((state) => state.setCurrentNode);

    const {captureIntegrationWorkflowCreated, captureIntegrationWorkflowTested} = useAnalytics();

    const queryClient = useQueryClient();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const {data: integration} = useGetIntegrationQuery(
        integrationId,
        useLoaderData() as Integration,
        !showDeleteIntegrationAlertDialog
    );

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

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (integrationWorkflowId) => {
            captureIntegrationWorkflowCreated();

            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });

            setShowBottomPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }

            navigate(`/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowId}`);
        },
    });

    const deleteIntegrationMutation = useDeleteIntegrationMutation({
        onSuccess: () => {
            navigate('/embedded/integrations');

            queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});
            queryClient.invalidateQueries({
                queryKey: IntegrationCategoryKeys.integrationCategories,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationTagKeys.integrationTags,
            });
        },
    });

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            setShowDeleteWorkflowAlertDialog(false);

            const integrationWorkflowIds = integration?.integrationWorkflowIds?.filter(
                (integrationWorkflowId) => integrationWorkflowId !== (workflow as Workflow).integrationWorkflowId
            );

            if (integrationWorkflowIds?.length) {
                navigate(
                    `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowIds[0]}?${searchParams}`
                );
            } else {
                navigate('/embedded/integrations');
            }

            queryClient.removeQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                    integrationId,
                    (workflow as Workflow).integrationWorkflowId!
                ),
            });
            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});
        },
    });

    const handleDeleteIntegrationAlertDialogClick = () => {
        if (integrationId) {
            deleteIntegrationMutation.mutate(integrationId);
        }
    };

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (integrationId && workflow.id) {
            deleteWorkflowMutation.mutate({
                id: workflow.id!,
            });
        }
    };

    const handleIntegrationWorkflowValueChange = (integrationWorkflowId: number) => {
        setWorkflowTestExecution(undefined);
        setCurrentNode(undefined);

        navigate(
            `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowId}?${searchParams.toString()}`
        );
    };

    const handleRunClick = useCallback(() => {
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
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
    }, [closeWorkflowTestStream, jobId, persistJobId, setStreamRequest, setWorkflowIsRunning]);

    useEffect(() => {
        if (!workflow.id || currentEnvironmentId === undefined) return;

        const jobId = getPersistedJobId();

        if (!jobId) {
            return;
        }

        setWorkflowIsRunning(true);
        setJobId(jobId);

        setStreamRequest(getTestWorkflowAttachRequest({jobId}));
    }, [workflow.id, currentEnvironmentId, getPersistedJobId, setWorkflowIsRunning, setJobId, setStreamRequest]);

    // On transport error (e.g., 4xx/5xx), make sure to reset the running state and clear the request to prevent retries
    useEffect(() => {
        if (workflowTestStreamError) {
            setWorkflowIsRunning(false);
            setStreamRequest(null);
            persistJobId(null);
            setJobId(null);
        }
    }, [workflowTestStreamError, persistJobId, setWorkflowIsRunning, setStreamRequest]);

    return (
        <header className="flex items-center px-3 py-2.5">
            <div className="flex flex-1">{integration && <IntegrationHeaderTitle integration={integration} />}</div>

            <div className="flex items-center space-x-12">
                <div className="flex space-x-1">
                    <IntegrationHeaderWorkflowSelect
                        integrationId={integrationId}
                        integrationWorkflowId={integrationWorkflowId}
                        onValueChange={handleIntegrationWorkflowValueChange}
                    />

                    <IntegrationHeaderWorkflowDropDownMenu
                        onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                        workflowId={workflow.id!}
                    />

                    {!!integrationId && (
                        <WorkflowDialog
                            createWorkflowMutation={createIntegrationWorkflowMutation}
                            integrationId={integrationId}
                            triggerNode={
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            className="hover:bg-background/70 [&_svg]:size-5"
                                            icon={<PlusIcon />}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Create new workflow</TooltipContent>
                                </Tooltip>
                            }
                            useGetWorkflowQuery={useGetWorkflowQuery}
                        />
                    )}

                    {workflowIsRunning ? (
                        <IntegrationHeaderStopButton onClick={handleStopClick} />
                    ) : (
                        <IntegrationHeaderRunButton onRunClick={handleRunClick} runDisabled={runDisabled} />
                    )}

                    <IntegrationHeaderOutputButton bottomResizablePanelRef={bottomResizablePanelRef} />
                </div>

                {integration && (
                    <div className="flex space-x-1">
                        <IntegrationHeaderPublishPopover integration={integration} />

                        <IntegrationHeaderHistoryButton onClick={() => setShowIntegrationVersionHistorySheet(true)} />

                        <IntegrationHeaderIntegrationDropDownMenu
                            integrationId={integrationId}
                            onDelete={() => setShowDeleteIntegrationAlertDialog(true)}
                            onEdit={() => setShowEditIntegrationDialog(true)}
                        />
                    </div>
                )}
            </div>

            {showDeleteIntegrationAlertDialog && (
                <IntegrationHeaderDeleteIntegrationAlertDialog
                    onClose={() => setShowDeleteIntegrationAlertDialog(false)}
                    onDelete={handleDeleteIntegrationAlertDialogClick}
                />
            )}

            {showDeleteWorkflowAlertDialog && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={handleDeleteWorkflowAlertDialogClick}
                />
            )}

            {showEditIntegrationDialog && integration && (
                <IntegrationDialog integration={integration} onClose={() => setShowEditIntegrationDialog(false)} />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    integrationId={integrationId}
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}

            {showIntegrationVersionHistorySheet && (
                <IntegrationVersionHistorySheet
                    integrationId={+integrationId!}
                    onClose={() => {
                        setShowIntegrationVersionHistorySheet(false);
                    }}
                />
            )}
        </header>
    );
};

export default IntegrationHeader;
