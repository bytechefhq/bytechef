import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import {useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpIntegrationWorkflow} from '@/shared/middleware/graphql';
import {BoltIcon, PencilIcon, XIcon} from 'lucide-react';

import McpIntegrationWorkflowPropertiesPopover from './McpIntegrationWorkflowPropertiesPopover';
import useMcpIntegrationWorkflowListItem from './hooks/useMcpIntegrationWorkflowListItem';

interface McpIntegrationWorkflowListItemProps {
    componentName: string;
    mcpIntegrationWorkflow: McpIntegrationWorkflow;
}

const McpIntegrationWorkflowListItem = ({
    componentName,
    mcpIntegrationWorkflow,
}: McpIntegrationWorkflowListItemProps) => {
    const {
        handleCloseEditDialog,
        handleConfirmDelete,
        integrationInstanceConfigurationWorkflow,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    } = useMcpIntegrationWorkflowListItem(mcpIntegrationWorkflow);

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const popoverId = `integration-workflow-${mcpIntegrationWorkflow.id}`;
    const isPopoverOpen = activePopoverId === popoverId;
    const workflowLabel = mcpIntegrationWorkflow.workflow?.label || 'Unnamed Workflow';

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <PopoverAnchor asChild>
                    <Badge className="max-w-96 gap-1 py-1 pl-2.5 pr-1" styleType="secondary-filled">
                        <span className="truncate text-sm">{workflowLabel}</span>

                        <span className="ml-auto flex shrink-0 items-center gap-0.5">
                            <Button
                                aria-label="Configure"
                                className="rounded p-0.5 hover:bg-foreground/10"
                                icon={<BoltIcon className="size-3" />}
                                onClick={() => openPopover(popoverId)}
                                size="iconXxs"
                                title="Configure"
                                variant="ghost"
                            />

                            <Button
                                aria-label="Edit"
                                className="rounded p-0.5 hover:bg-foreground/10"
                                icon={<PencilIcon className="size-3" />}
                                onClick={() => setShowEditWorkflowDialog(true)}
                                size="iconXxs"
                                title="Edit"
                                variant="ghost"
                            />

                            <Button
                                aria-label="Delete"
                                className="rounded p-0.5 hover:bg-destructive/10 hover:text-destructive"
                                icon={<XIcon className="size-3" />}
                                onClick={() => setShowDeleteDialog(true)}
                                size="iconXxs"
                                title="Delete"
                                variant="ghost"
                            />
                        </span>
                    </Badge>
                </PopoverAnchor>

                {isPopoverOpen && (
                    <McpIntegrationWorkflowPropertiesPopover
                        mcpIntegrationWorkflow={mcpIntegrationWorkflow}
                        onClose={closePopover}
                    />
                )}
            </Popover>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={handleConfirmDelete}
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

export default McpIntegrationWorkflowListItem;
