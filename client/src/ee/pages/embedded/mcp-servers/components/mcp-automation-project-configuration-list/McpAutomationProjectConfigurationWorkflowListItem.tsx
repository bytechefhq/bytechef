import Badge from '@/components/Badge/Badge';
import Switch from '@/components/Switch/Switch';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';

import McpAutomationProjectConfigurationWorkflowPropertiesPopover from './McpAutomationProjectConfigurationWorkflowPropertiesPopover';
import {McpAutomationProjectConfigurationWorkflowItemType} from './hooks/useMcpAutomationProjectConfigurationList';
import useMcpAutomationProjectConfigurationWorkflowListItem from './hooks/useMcpAutomationProjectConfigurationWorkflowListItem';

interface McpAutomationProjectConfigurationWorkflowListItemProps {
    workflow: McpAutomationProjectConfigurationWorkflowItemType;
}

const McpAutomationProjectConfigurationWorkflowListItem = ({
    workflow,
}: McpAutomationProjectConfigurationWorkflowListItemProps) => {
    const {handleEnabledChange, isEnablePending, setShowPropertiesPopover, showPropertiesPopover} =
        useMcpAutomationProjectConfigurationWorkflowListItem(workflow);

    const label = workflow.label || workflow.workflowUuid;

    return (
        <Popover onOpenChange={setShowPropertiesPopover} open={showPropertiesPopover}>
            <PopoverAnchor asChild>
                <div className="flex min-h-10 items-center gap-2 text-sm" data-available={String(workflow.available)}>
                    <span className="min-w-0 flex-1 truncate">{label}</span>

                    {!workflow.available && (
                        <Badge label="Not available" styleType="secondary-filled" weight="semibold" />
                    )}

                    <McpToolConfigureButton label={label} onClick={() => setShowPropertiesPopover(true)} />

                    <Switch
                        aria-label={`Enable ${label}`}
                        checked={workflow.enabled}
                        disabled={isEnablePending}
                        onCheckedChange={handleEnabledChange}
                    />
                </div>
            </PopoverAnchor>

            {showPropertiesPopover && (
                <McpAutomationProjectConfigurationWorkflowPropertiesPopover
                    onClose={() => setShowPropertiesPopover(false)}
                    workflow={workflow}
                />
            )}
        </Popover>
    );
};

export default McpAutomationProjectConfigurationWorkflowListItem;
