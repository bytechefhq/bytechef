import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {InfoIcon, PlusIcon, ShieldCheckIcon} from 'lucide-react';

import AiAgentTool from './AiAgentTool';
import useAiAgentTools from './hooks/useAiAgentTools';

export default function AiAgentTools() {
    const {configuredConnectionKeys, rootWorkflowNodeName, toolGroups, tools} = useAiAgentTools();

    return (
        <div className="space-y-2">
            <div className="mb-3 flex items-center justify-between">
                <div className="flex items-center gap-1">
                    <h2 className="flex-1">Tools this agent can use:</h2>

                    <Tooltip>
                        <TooltipTrigger>
                            <InfoIcon className="size-4 text-muted-foreground" />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-xs">
                            Your agent can use these tools whenever it&apos;s needed during an execution. It will
                            intelligently decide when each tool is helpful based on your instructions.
                        </TooltipContent>
                    </Tooltip>
                </div>

                {rootWorkflowNodeName && (
                    <WorkflowNodesPopoverMenu
                        clusterElementType="tools"
                        hideActionComponents
                        hideTaskDispatchers
                        hideTriggerComponents
                        multipleClusterElementsNode
                        sourceNodeId={rootWorkflowNodeName}
                    >
                        <Button icon={<PlusIcon />} label="Add Tool" size="sm" variant="outline" />
                    </WorkflowNodesPopoverMenu>
                )}
            </div>

            {tools.length === 0 && toolGroups.length === 0 && (
                <p className="text-xs text-muted-foreground">
                    No tools added yet. Click &quot;Add tool&quot; to give your agent capabilities.
                </p>
            )}

            {tools.map((tool, toolIndex) => (
                <AiAgentTool
                    configuredConnectionKeys={configuredConnectionKeys}
                    key={`${tool.name}-${toolIndex}`}
                    tool={tool}
                />
            ))}

            {toolGroups.map((toolGroup) => (
                <fieldset className="space-y-2 rounded border border-stroke-neutral-secondary p-2" key={toolGroup.name}>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1">
                            <ShieldCheckIcon className="size-3.5 text-amber-600" />

                            <span className="text-xs font-medium">{toolGroup.label}</span>

                            <span className="text-xs text-muted-foreground">requires approval</span>
                        </div>

                        <WorkflowNodesPopoverMenu
                            clusterElementType="tools"
                            hideActionComponents
                            hideTaskDispatchers
                            hideTriggerComponents
                            multipleClusterElementsNode
                            sourceNodeId={toolGroup.name}
                        >
                            <Button icon={<PlusIcon />} label="Add Tool" size="sm" variant="ghost" />
                        </WorkflowNodesPopoverMenu>
                    </div>

                    {toolGroup.tools.length === 0 && (
                        <p className="text-xs text-muted-foreground">No tools gated yet.</p>
                    )}

                    {toolGroup.tools.map((tool, toolIndex) => (
                        <AiAgentTool
                            configuredConnectionKeys={configuredConnectionKeys}
                            key={`${tool.name}-${toolIndex}`}
                            tool={tool}
                        />
                    ))}

                    <div className="flex items-center justify-between">
                        <span className="text-xs text-muted-foreground">
                            {toolGroup.channelLabels.length
                                ? `Channels: ${toolGroup.channelLabels.join(', ')}`
                                : 'Channels: chat (default)'}
                        </span>

                        <WorkflowNodesPopoverMenu
                            clusterElementType="approvalChannels"
                            hideActionComponents
                            hideTaskDispatchers
                            hideTriggerComponents
                            multipleClusterElementsNode
                            sourceNodeId={toolGroup.name}
                        >
                            <Button icon={<PlusIcon />} label="Add Channel" size="sm" variant="ghost" />
                        </WorkflowNodesPopoverMenu>
                    </div>
                </fieldset>
            ))}
        </div>
    );
}
