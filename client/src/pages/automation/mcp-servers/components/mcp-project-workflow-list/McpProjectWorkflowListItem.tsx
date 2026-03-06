import Badge from '@/components/Badge/Badge';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import {useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpProjectWorkflow} from '@/shared/middleware/graphql';
import {BoltIcon, PencilIcon, XIcon} from 'lucide-react';

import McpProjectWorkflowPropertiesPopover from './McpProjectWorkflowPropertiesPopover';
import useMcpProjectWorkflowBadge from './hooks/useMcpProjectWorkflowBadge';

interface McpProjectWorkflowListItemProps {
    mcpProjectWorkflow: McpProjectWorkflow;
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

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <PopoverAnchor asChild>
                    <Badge className="max-w-96 gap-1 py-1 pl-2.5 pr-1" styleType="secondary-filled">
                        <span className="truncate text-sm">{workflowLabel}</span>

                        <span className="ml-auto flex shrink-0 items-center gap-0.5">
                            <button
                                aria-label="Configure"
                                className="rounded p-0.5 hover:bg-foreground/10"
                                onClick={() => openPopover(popoverId)}
                                title="Configure"
                                type="button"
                            >
                                <BoltIcon className="size-3" />
                            </button>

                            <button
                                aria-label="Edit"
                                className="rounded p-0.5 hover:bg-foreground/10"
                                onClick={() => setShowEditWorkflowDialog(true)}
                                title="Edit"
                                type="button"
                            >
                                <PencilIcon className="size-3" />
                            </button>

                            <button
                                aria-label="Delete"
                                className="rounded p-0.5 hover:bg-destructive/10 hover:text-destructive"
                                onClick={() => setShowDeleteDialog(true)}
                                title="Delete"
                                type="button"
                            >
                                <XIcon className="size-3" />
                            </button>
                        </span>
                    </Badge>
                </PopoverAnchor>

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
