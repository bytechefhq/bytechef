import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {BoltIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

import McpProjectWorkflowPropertiesPopover from './McpProjectWorkflowPropertiesPopover';
import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';
import useMcpProjectWorkflowBadge from './hooks/useMcpProjectWorkflowBadge';

interface McpProjectWorkflowListItemProps {
    mcpProjectWorkflow: McpProjectWorkflowItemType;
}

const McpProjectWorkflowListItem = ({mcpProjectWorkflow}: McpProjectWorkflowListItemProps) => {
    const {
        handleCloseEditDialog,
        handleConfirmDelete,
        handleEnabledChange,
        isDeletePending,
        isEnablePending,
        projectDeploymentWorkflow,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    } = useMcpProjectWorkflowBadge(mcpProjectWorkflow);

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const popoverId = `project-workflow-${mcpProjectWorkflow.id}`;
    const isPopoverOpen = activePopoverId === popoverId;
    const workflowLabel = mcpProjectWorkflow.workflow?.label || 'Unnamed Workflow';

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <div className="flex items-center gap-2 py-0.5">
                    <span className="min-w-0 flex-1 truncate text-sm font-medium">{workflowLabel}</span>

                    <div className="flex shrink-0 items-center gap-2">
                        <Switch
                            aria-label={`Enable ${workflowLabel}`}
                            checked={mcpProjectWorkflow.enabled}
                            className="mr-2"
                            disabled={isEnablePending}
                            onCheckedChange={handleEnabledChange}
                        />

                        <DropdownMenu>
                            <PopoverAnchor asChild>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        aria-label={`${workflowLabel} actions`}
                                        icon={<EllipsisVerticalIcon />}
                                        size="iconSm"
                                        variant="ghost"
                                    />
                                </DropdownMenuTrigger>
                            </PopoverAnchor>

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
                </div>

                {isPopoverOpen && (
                    <McpProjectWorkflowPropertiesPopover
                        mcpProjectWorkflow={mcpProjectWorkflow}
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

            {showEditWorkflowDialog && projectDeploymentWorkflow && workflow && (
                <ProjectDeploymentEditWorkflowDialog
                    onClose={handleCloseEditDialog}
                    projectDeploymentWorkflow={projectDeploymentWorkflow}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default McpProjectWorkflowListItem;
