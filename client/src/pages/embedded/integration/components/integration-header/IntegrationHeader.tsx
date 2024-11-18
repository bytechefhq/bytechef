import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationHeaderDeleteIntegrationAlertDialog from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderDeleteIntegrationAlertDialog';
import IntegrationHeaderDeleteWorkflowAlertDialog from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderDeleteWorkflowAlertDialog';
import IntegrationHeaderIntegrationDropDownMenu from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderIntegrationDropDownMenu';
import IntegrationHeaderOutputButton from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderOutputButton';
import IntegrationHeaderPublishPopover from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderPublishPopover';
import IntegrationHeaderRunButton from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderRunButton';
import IntegrationHeaderStopButton from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderStopButton';
import IntegrationHeaderTitle from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderTitle';
import IntegrationHeaderWorkflowDropDownMenu from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderWorkflowDropDownMenu';
import IntegrationHeaderWorkflowSelect from '@/pages/embedded/integration/components/integration-header/IntegrationHeaderWorkflowSelect';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Integration, Workflow} from '@/shared/middleware/embedded/configuration';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useDeleteIntegrationMutation} from '@/shared/mutations/embedded/integrations.mutations';
import {
    useCreateIntegrationWorkflowMutation,
    useDeleteWorkflowMutation,
} from '@/shared/mutations/embedded/workflows.mutations';
import {IntegrationCategoryKeys} from '@/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/shared/queries/embedded/integrationTags.quries';
import {IntegrationWorkflowKeys} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/shared/queries/embedded/integrations.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/shared/queries/embedded/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {PlusIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';

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
    const [showDeleteIntegrationAlertDialog, setShowDeleteIntegrationAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditIntegrationDialog, setShowEditIntegrationDialog] = useState(false);

    const {
        setShowBottomPanelOpen,
        setShowEditWorkflowDialog,
        setWorkflowIsRunning,
        setWorkflowTestExecution,
        showEditWorkflowDialog,
        workflowIsRunning,
    } = useWorkflowEditorStore();
    const {workflow} = useWorkflowDataStore();
    const {setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const {captureIntegrationWorkflowCreated, captureIntegrationWorkflowTested} = useAnalytics();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const {data: integration} = useGetIntegrationQuery(
        integrationId,
        useLoaderData() as Integration,
        !showDeleteIntegrationAlertDialog
    );

    const queryClient = useQueryClient();

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

            navigate(
                `/embedded/integrations/${integrationId}/integration-workflows/${integration?.integrationWorkflowIds?.filter((integrationWorkflowId) => integrationWorkflowId !== (workflow as Workflow).integrationWorkflowId)[0]}?${searchParams}`
            );

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
            `/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowId}?${searchParams}`
        );
    };

    const handleRunClick = () => {
        setShowBottomPanelOpen(true);
        setWorkflowIsRunning(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow.id) {
            captureIntegrationWorkflowTested();

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
    };

    return (
        <header className="flex items-center border-b border-b-border/50 py-2.5 pl-3 pr-2.5">
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
                            parentId={integrationId}
                            triggerNode={
                                <Button className="hover:bg-background/70" size="icon" variant="ghost">
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <PlusIcon className="mx-2 size-5" />
                                        </TooltipTrigger>

                                        <TooltipContent>Create new workflow</TooltipContent>
                                    </Tooltip>
                                </Button>
                            }
                            useGetWorkflowQuery={useGetWorkflowQuery}
                        />
                    )}

                    {workflowIsRunning ? (
                        <IntegrationHeaderStopButton />
                    ) : (
                        <IntegrationHeaderRunButton onRunClick={handleRunClick} runDisabled={runDisabled} />
                    )}

                    <IntegrationHeaderOutputButton bottomResizablePanelRef={bottomResizablePanelRef} />
                </div>

                {integration && (
                    <div className="flex space-x-1">
                        <IntegrationHeaderPublishPopover integration={integration} />

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
                <IntegrationHeaderDeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={handleDeleteWorkflowAlertDialogClick}
                />
            )}

            {showEditIntegrationDialog && integration && (
                <IntegrationDialog integration={integration} onClose={() => setShowEditIntegrationDialog(false)} />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </header>
    );
};

export default IntegrationHeader;
