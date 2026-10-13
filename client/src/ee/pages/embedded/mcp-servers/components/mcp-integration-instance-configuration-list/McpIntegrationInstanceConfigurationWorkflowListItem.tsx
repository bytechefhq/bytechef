import AlertDialog from '@/components/AlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import McpToolActionsMenu from '@/shared/components/mcp-server/McpToolActionsMenu';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';

import McpIntegrationInstanceConfigurationWorkflowPropertiesPopover from './McpIntegrationInstanceConfigurationWorkflowPropertiesPopover';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from './hooks/useMcpIntegrationInstanceConfigurationList';
import useMcpIntegrationInstanceConfigurationWorkflowListItem from './hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem';

interface McpIntegrationInstanceConfigurationWorkflowListItemProps {
    componentName: string;
    mcpIntegrationInstanceConfigurationWorkflow: McpIntegrationInstanceConfigurationWorkflowItemType;
}

const McpIntegrationInstanceConfigurationWorkflowListItem = ({
    componentName,
    mcpIntegrationInstanceConfigurationWorkflow,
}: McpIntegrationInstanceConfigurationWorkflowListItemProps) => {
    const {
        handleCloseEditDialog,
        handleConfirmDelete,
        integrationInstanceConfigurationWorkflow,
        isDeletePending,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    } = useMcpIntegrationInstanceConfigurationWorkflowListItem(mcpIntegrationInstanceConfigurationWorkflow);

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const popoverId = `integration-workflow-${mcpIntegrationInstanceConfigurationWorkflow.id}`;
    const isPopoverOpen = activePopoverId === popoverId;
    const workflowLabel = mcpIntegrationInstanceConfigurationWorkflow.workflow?.label || 'Unnamed Workflow';

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <PopoverAnchor asChild>
                    <div className="flex items-center gap-2 py-0.5">
                        <span className="min-w-0 flex-1 truncate text-sm font-medium">{workflowLabel}</span>

                        <McpToolConfigureButton label={workflowLabel} onClick={() => openPopover(popoverId)} />

                        <McpToolActionsMenu
                            label={workflowLabel}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditWorkflowDialog(true)}
                        />
                    </div>
                </PopoverAnchor>

                {isPopoverOpen && (
                    <McpIntegrationInstanceConfigurationWorkflowPropertiesPopover
                        mcpIntegrationInstanceConfigurationWorkflow={mcpIntegrationInstanceConfigurationWorkflow}
                        onClose={closePopover}
                    />
                )}
            </Popover>

            <AlertDialog
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />

            {showEditWorkflowDialog && integrationInstanceConfigurationWorkflow && workflow && (
                <IntegrationInstanceConfigurationEditWorkflowDialog
                    componentName={componentName}
                    integrationInstanceConfigurationWorkflow={integrationInstanceConfigurationWorkflow}
                    onClose={handleCloseEditDialog}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default McpIntegrationInstanceConfigurationWorkflowListItem;
