import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {IntegrationModel} from '@/middleware/embedded/configuration';
import {WorkflowTestApi} from '@/middleware/platform/workflow/test';
import {
    useCreateIntegrationWorkflowMutation,
    useDeleteIntegrationMutation,
} from '@/mutations/embedded/integrations.mutations';
import {useDeleteWorkflowMutation} from '@/mutations/embedded/workflows.mutations';
import IntegrationHeaderDeleteIntegrationAlertDialog from '@/pages/embedded/integration/components/IntegrationHeaderDeleteIntegrationAlertDialog';
import IntegrationHeaderDeleteWorkflowAlertDialog from '@/pages/embedded/integration/components/IntegrationHeaderDeleteWorkflowAlertDialog';
import IntegrationHeaderIntegrationDropDownMenu from '@/pages/embedded/integration/components/IntegrationHeaderIntegrationDropDownMenu';
import IntegrationHeaderIntegrationVersionBadge from '@/pages/embedded/integration/components/IntegrationHeaderIntegrationVersionBadge';
import IntegrationHeaderOutputButton from '@/pages/embedded/integration/components/IntegrationHeaderOutputButton';
import IntegrationHeaderRunButton from '@/pages/embedded/integration/components/IntegrationHeaderRunButton';
import IntegrationHeaderStopButton from '@/pages/embedded/integration/components/IntegrationHeaderStopButton';
import IntegrationHeaderWorkflowDropDownMenu from '@/pages/embedded/integration/components/IntegrationHeaderWorkflowDropDownMenu';
import IntegrationHeaderWorkflowSelect from '@/pages/embedded/integration/components/IntegrationHeaderWorkflowSelect';
import IntegrationPublishPopover from '@/pages/embedded/integration/components/IntegrationPublishPopover';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {IntegrationCategoryKeys} from '@/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/queries/embedded/integrationTags.quries';
import {IntegrationWorkflowKeys} from '@/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/queries/embedded/integrations.queries';
import {useGetWorkflowQuery} from '@/queries/embedded/workflows.queries';
import {UpdateWorkflowMutationType} from '@/types/types';
import {PlusIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate} from 'react-router-dom';

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
    const {setWorkflow, workflow} = useWorkflowDataStore();

    const navigate = useNavigate();

    const {componentNames, nodeNames} = workflow;

    const {data: integration} = useGetIntegrationQuery(
        integrationId,
        useLoaderData() as IntegrationModel,
        !showDeleteIntegrationAlertDialog
    );

    const queryClient = useQueryClient();

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });

            setShowBottomPanelOpen(false);
            setWorkflow({...workflow, componentNames, nodeNames});

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }

            navigate(`/embedded/integrations/${integrationId}/integration-workflows/${workflow.integrationWorkflowId}`);
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

            navigate('/embedded/integrations');

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

        navigate(`/embedded/integrations/${integrationId}/integration-workflows/${integrationWorkflowId}`);
    };

    const handleRunClick = () => {
        setShowBottomPanelOpen(true);
        setWorkflowIsRunning(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow.id) {
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

                    // queryClient.invalidateQueries({
                    //     queryKey: WorkflowKeys.integrationWorkflows(integrationId),
                    // });
                });
        }
    };

    return (
        <header className="flex items-center border-b py-2 pl-3 pr-2.5">
            <div className="flex flex-1 items-center space-x-2">
                <h1>{integration?.componentName}</h1>

                {integration && <IntegrationHeaderIntegrationVersionBadge integration={integration} />}
            </div>

            <div className="flex items-center space-x-6">
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
                                <Button className="hover:bg-gray-200" size="icon" variant="ghost">
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
                        <IntegrationPublishPopover integration={integration} />

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
