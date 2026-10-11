import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {BoltIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

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

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label={`${workflowLabel} actions`}
                                    className="shrink-0"
                                    icon={<EllipsisVerticalIcon />}
                                    size="iconSm"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" onCloseAutoFocus={(event) => event.preventDefault()}>
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onSelect={() => openPopover(popoverId)}
                                >
                                    <BoltIcon /> Configure
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onSelect={() => setShowEditWorkflowDialog(true)}
                                >
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onSelect={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
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
