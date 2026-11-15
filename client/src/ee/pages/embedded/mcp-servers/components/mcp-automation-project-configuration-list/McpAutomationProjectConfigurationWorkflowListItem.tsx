import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Switch from '@/components/Switch/Switch';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import useAutomationExecutionsSheetStore from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
import McpToolActionsMenu from '@/shared/components/mcp-server/McpToolActionsMenu';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import WorkflowToolRowLabel from '@/shared/components/mcp-server/WorkflowToolRowLabel';

import McpAutomationProjectConfigurationWorkflowPropertiesPopover from './McpAutomationProjectConfigurationWorkflowPropertiesPopover';
import {McpAutomationProjectConfigurationWorkflowItemType} from './hooks/useMcpAutomationProjectConfigurationList';
import useMcpAutomationProjectConfigurationWorkflowListItem from './hooks/useMcpAutomationProjectConfigurationWorkflowListItem';

interface McpAutomationProjectConfigurationWorkflowListItemProps {
    environmentId?: string;
    projectName: string;
    workflow: McpAutomationProjectConfigurationWorkflowItemType;
}

const McpAutomationProjectConfigurationWorkflowListItem = ({
    environmentId,
    projectName,
    workflow,
}: McpAutomationProjectConfigurationWorkflowListItemProps) => {
    const openAutomationExecutionsSheet = useAutomationExecutionsSheetStore(
        (state) => state.openAutomationExecutionsSheet
    );

    const {
        handleConfirmDelete,
        handleEnabledChange,
        isDeletePending,
        isEnablePending,
        setShowDeleteDialog,
        setShowPropertiesPopover,
        showDeleteDialog,
        showPropertiesPopover,
    } = useMcpAutomationProjectConfigurationWorkflowListItem(workflow);

    const label = workflow.label || workflow.workflowUuid;
    const workflowId = workflow.workflowId;

    const canOpenExecutions = environmentId != null && workflowId != null;

    const handleWorkflowClick = () => {
        if (environmentId != null && workflowId != null) {
            openAutomationExecutionsSheet({
                environmentId: Number(environmentId),
                projectName,
                workflow: {
                    label: workflow.label,
                    workflowId,
                    workflowUuid: workflow.workflowUuid,
                },
            });
        }
    };

    return (
        <>
            <Popover onOpenChange={setShowPropertiesPopover} open={showPropertiesPopover}>
                <PopoverAnchor asChild>
                    <div
                        className="flex min-h-10 items-center gap-2 text-sm"
                        data-available={String(workflow.available)}
                    >
                        <WorkflowToolRowLabel
                            canOpenExecutions={canOpenExecutions}
                            filteredComponentNames={[]}
                            label={label}
                            onWorkflowClick={handleWorkflowClick}
                            workflowComponentDefinitions={{}}
                            workflowTaskDispatcherDefinitions={{}}
                        />

                        {!workflow.available && (
                            <Badge label="Not available" styleType="secondary-filled" weight="semibold" />
                        )}

                        <McpToolConfigureButton label={label} onClick={() => setShowPropertiesPopover(true)} />

                        <Switch
                            aria-label={`Enable ${label}`}
                            checked={workflow.enabled}
                            className="mr-1"
                            disabled={isEnablePending}
                            onCheckedChange={handleEnabledChange}
                        />

                        <McpToolActionsMenu label={label} onDeleteClick={() => setShowDeleteDialog(true)} />
                    </div>
                </PopoverAnchor>

                {showPropertiesPopover && (
                    <McpAutomationProjectConfigurationWorkflowPropertiesPopover
                        onClose={() => setShowPropertiesPopover(false)}
                        workflow={workflow}
                    />
                )}
            </Popover>

            <AlertDialog
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpAutomationProjectConfigurationWorkflowListItem;
