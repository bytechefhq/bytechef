import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Popover, PopoverAnchor, PopoverContent} from '@/components/ui/popover';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import {XIcon} from 'lucide-react';
import {useState} from 'react';

import {McpAutomationProjectConfigurationWorkflowItemType} from './hooks/useMcpAutomationProjectConfigurationList';
import useMcpAutomationProjectConfigurationWorkflowListItem from './hooks/useMcpAutomationProjectConfigurationWorkflowListItem';
import {
    MCP_AUTOMATION_TOOL_DESCRIPTION_KEY,
    MCP_AUTOMATION_TOOL_NAME_KEY,
    buildMcpAutomationToolParameters,
    getMcpAutomationToolText,
} from './mcpAutomationToolParameters';

interface McpAutomationProjectConfigurationWorkflowListItemProps {
    workflow: McpAutomationProjectConfigurationWorkflowItemType;
}

const McpAutomationProjectConfigurationWorkflowListItem = ({
    workflow,
}: McpAutomationProjectConfigurationWorkflowListItemProps) => {
    const savedParameters = workflow.parameters as Record<string, unknown> | null;

    const [toolDescription, setToolDescription] = useState(
        getMcpAutomationToolText(savedParameters, MCP_AUTOMATION_TOOL_DESCRIPTION_KEY)
    );
    const [toolName, setToolName] = useState(getMcpAutomationToolText(savedParameters, MCP_AUTOMATION_TOOL_NAME_KEY));

    const {
        handleEnabledChange,
        handleParametersSave,
        isEnablePending,
        isParametersPending,
        setShowPropertiesPopover,
        showPropertiesPopover,
    } = useMcpAutomationProjectConfigurationWorkflowListItem(workflow);

    const label = workflow.label || workflow.workflowUuid;

    const handleSave = () => {
        const withName = buildMcpAutomationToolParameters(savedParameters, MCP_AUTOMATION_TOOL_NAME_KEY, toolName);

        handleParametersSave(
            buildMcpAutomationToolParameters(withName, MCP_AUTOMATION_TOOL_DESCRIPTION_KEY, toolDescription)
        );
    };

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
                <PopoverContent align="start" className="flex w-md flex-col gap-3 p-3" side="bottom">
                    <div className="flex items-center justify-between">
                        <h4 className="text-sm font-medium">{`Edit ${label} Tool`}</h4>

                        <Button
                            aria-label="Close"
                            icon={<XIcon className="size-4" />}
                            onClick={() => setShowPropertiesPopover(false)}
                            size="iconXs"
                            variant="ghost"
                        />
                    </div>

                    <fieldset className="space-y-3 border-0 p-0">
                        <div className="space-y-1">
                            <Label htmlFor={`tool-name-${workflow.id}`}>Tool Name</Label>

                            <Input
                                id={`tool-name-${workflow.id}`}
                                onChange={(event) => setToolName(event.target.value)}
                                placeholder="Defaults to workflow name"
                                value={toolName}
                            />
                        </div>

                        <div className="space-y-1">
                            <Label htmlFor={`tool-description-${workflow.id}`}>Tool Description</Label>

                            <Input
                                id={`tool-description-${workflow.id}`}
                                onChange={(event) => setToolDescription(event.target.value)}
                                placeholder="Defaults to workflow description"
                                value={toolDescription}
                            />
                        </div>
                    </fieldset>

                    <div className="flex justify-end gap-2">
                        <Button
                            label="Cancel"
                            onClick={() => setShowPropertiesPopover(false)}
                            type="button"
                            variant="outline"
                        />

                        <Button disabled={isParametersPending} label="Save" onClick={handleSave} type="button" />
                    </div>
                </PopoverContent>
            )}
        </Popover>
    );
};

export default McpAutomationProjectConfigurationWorkflowListItem;
