import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {InfoIcon, PlusIcon} from 'lucide-react';

import AiAgentTool from './AiAgentTool';
import useAiAgentTools from './hooks/useAiAgentTools';

export default function AiAgentTools() {
    const {configuredConnectionKeys, rootWorkflowNodeName, tools} = useAiAgentTools();

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

            {tools.length === 0 && (
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
        </div>
    );
}
