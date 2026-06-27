import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {BoltIcon, PencilIcon, Trash2Icon} from 'lucide-react';

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

                    <div className="flex shrink-0 items-center gap-0.5">
                        {/* Anchor the popover to the Configure button so it opens right-aligned to that button. */}

                        <PopoverAnchor asChild>
                            <Button
                                aria-label="Configure"
                                className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                icon={<BoltIcon className="size-4" />}
                                onClick={() => openPopover(popoverId)}
                                size="iconSm"
                                title="Configure"
                                variant="ghost"
                            />
                        </PopoverAnchor>

                        <Button
                            aria-label="Edit"
                            className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                            icon={<PencilIcon className="size-4" />}
                            onClick={() => setShowEditWorkflowDialog(true)}
                            size="iconSm"
                            title="Edit"
                            variant="ghost"
                        />

                        <Button
                            aria-label="Delete"
                            className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                            icon={<Trash2Icon className="size-4" />}
                            onClick={() => setShowDeleteDialog(true)}
                            size="iconSm"
                            title="Delete"
                            variant="ghost"
                        />
                    </div>
                </div>

                {isPopoverOpen && (
                    <McpProjectWorkflowPropertiesPopover
                        mcpProjectWorkflow={mcpProjectWorkflow}
                        onClose={closePopover}
                    />
                )}
            </Popover>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={handleConfirmDelete}
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
