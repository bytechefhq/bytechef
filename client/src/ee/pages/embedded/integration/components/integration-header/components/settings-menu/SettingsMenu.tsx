import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationVersionHistorySheet from '@/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet';
import DeleteIntegrationAlertDialog from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/components/DeleteIntegrationAlertDialog';
import IntegrationTabButtons from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/components/IntegrationTabButtons';
import WorkflowTabButtons from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/components/WorkflowTabButtons';
import {useSettingsMenu} from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/hooks/useSettingsMenu';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {SettingsIcon} from 'lucide-react';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface IntegrationHeaderSettingsMenuProps {
    integration: Integration;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

const SettingsMenu = ({integration, updateWorkflowMutation, workflow}: IntegrationHeaderSettingsMenuProps) => {
    const [openDropdownMenu, setOpenDropdownMenu] = useState(false);
    const [showDeleteIntegrationAlertDialog, setShowDeleteIntegrationAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditIntegrationDialog, setShowEditIntegrationDialog] = useState(false);
    const [showIntegrationVersionHistorySheet, setShowIntegrationVersionHistorySheet] = useState(false);

    const {setShowEditWorkflowDialog, showEditWorkflowDialog} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
            showEditWorkflowDialog: state.showEditWorkflowDialog,
        }))
    );

    const {handleDeleteIntegrationAlertDialogClick, handleDeleteWorkflowAlertDialogClick, handleImportWorkflow} =
        useSettingsMenu({integration, workflow});

    return (
        <>
            <DropdownMenu onOpenChange={setOpenDropdownMenu} open={openDropdownMenu}>
                <Tooltip>
                    <DropdownMenuTrigger
                        asChild
                        className="cursor-pointer [&[data-state=open]]:bg-surface-brand-secondary [&[data-state=open]]:text-content-brand-primary"
                    >
                        <TooltipTrigger asChild>
                            <Button aria-label="Settings" icon={<SettingsIcon />} size="icon" variant="ghost" />
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>Integration and workflow settings</TooltipContent>
                </Tooltip>

                <DropdownMenuContent className="p-0">
                    <Tabs aria-label="Settings menu" defaultValue="workflow">
                        <TabsList className="rounded-none">
                            <TabsTrigger
                                aria-label="Workflow tab"
                                className="w-1/2 px-9 py-1 data-[state=active]:shadow-none"
                                value="workflow"
                            >
                                Workflow
                            </TabsTrigger>

                            <TabsTrigger
                                aria-label="Integration tab"
                                className="w-1/2 px-9 py-1 data-[state=active]:shadow-none"
                                value="integration"
                            >
                                Integration
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent className="mt-0" value="workflow">
                            <WorkflowTabButtons
                                onCloseDropdownMenu={() => setOpenDropdownMenu(false)}
                                onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                                onShowEditWorkflowDialog={() => setShowEditWorkflowDialog(true)}
                                workflowId={workflow.id!}
                            />
                        </TabsContent>

                        <TabsContent className="mt-0" value="integration">
                            <IntegrationTabButtons
                                onCloseDropdownMenuClick={() => setOpenDropdownMenu(false)}
                                onDeleteIntegrationClick={() => setShowDeleteIntegrationAlertDialog(true)}
                                onImportWorkflow={handleImportWorkflow}
                                onShowEditIntegrationDialogClick={() => setShowEditIntegrationDialog(true)}
                                onShowIntegrationVersionHistorySheet={() => setShowIntegrationVersionHistorySheet(true)}
                            />
                        </TabsContent>
                    </Tabs>
                </DropdownMenuContent>
            </DropdownMenu>

            {showDeleteIntegrationAlertDialog && (
                <DeleteIntegrationAlertDialog
                    onClose={() => setShowDeleteIntegrationAlertDialog(false)}
                    onDelete={handleDeleteIntegrationAlertDialogClick}
                />
            )}

            {showDeleteWorkflowAlertDialog && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={() => {
                        handleDeleteWorkflowAlertDialogClick();

                        setShowDeleteWorkflowAlertDialog(false);
                    }}
                />
            )}

            {showEditIntegrationDialog && (
                <IntegrationDialog integration={integration} onClose={() => setShowEditIntegrationDialog(false)} />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    integrationId={integration.id}
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}

            {showIntegrationVersionHistorySheet && (
                <IntegrationVersionHistorySheet
                    integrationId={integration.id!}
                    onClose={() => setShowIntegrationVersionHistorySheet(false)}
                />
            )}
        </>
    );
};

export default SettingsMenu;
