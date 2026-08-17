import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon, SettingsIcon, TrashIcon, WorkflowIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import useAiAgentToolActions from './hooks/useAiAgentToolActions';
import {ToolItemI} from './hooks/useAiAgentTools';

interface AiAgentToolPropsI {
    configuredConnectionKeys: Set<string>;
    tool: ToolItemI;
}

export default function AiAgentTool({configuredConnectionKeys, tool}: AiAgentToolPropsI) {
    const {handleConfigureTool, handleRemoveTool} = useAiAgentToolActions();

    const {data: toolComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: tool.componentName, componentVersion: tool.componentVersion},
        !!tool.componentName
    );

    const isConnectionMissing = !!toolComponentDefinition?.connection && !configuredConnectionKeys.has(tool.name);

    return (
        <div
            className={twMerge(
                'flex items-center gap-2 rounded bg-muted/50 p-2',
                isConnectionMissing && 'border border-red-500'
            )}
        >
            {tool.icon ? (
                <InlineSVG
                    className="size-5 flex-none text-gray-700"
                    loader={<ComponentIcon className="size-5 flex-none text-gray-700" />}
                    src={tool.icon}
                />
            ) : (
                <ComponentIcon className="size-5 flex-none text-gray-700" />
            )}

            <div className="flex flex-1 items-center justify-start gap-1 truncate">
                <span className="text-xs font-medium">{tool.label}</span>

                <span className="px-1 text-xs font-medium">-</span>

                <span className="flex-1 text-xs font-medium">{tool.operationName}</span>
            </div>

            {/* Direct buttons rather than a kebab menu: configure and remove are the only two actions, so a
                menu cost an extra click to reach either. */}

            {tool.clusterRoot ? (
                // A cluster root tool holds its own cluster elements, which are only reachable from the
                // advanced canvas. The node details panel this button would open cannot configure them, so
                // the row stays view-only here rather than offering an action that leads nowhere.
                <Tooltip>
                    <TooltipTrigger
                        aria-label="Configure on the advanced canvas"
                        className="flex size-6 items-center justify-center text-muted-foreground"
                    >
                        <WorkflowIcon className="size-4" />
                    </TooltipTrigger>

                    <TooltipContent className="max-w-xs">
                        This tool has its own cluster elements. Switch to the advanced canvas to configure it.
                    </TooltipContent>
                </Tooltip>
            ) : (
                <Button
                    aria-label="Configure tool"
                    className="size-6"
                    icon={<SettingsIcon />}
                    onClick={() => handleConfigureTool(tool)}
                    size="iconSm"
                    variant="ghost"
                />
            )}

            <Button
                aria-label="Remove tool"
                className="size-6"
                icon={<TrashIcon />}
                onClick={() => handleRemoveTool(tool)}
                size="iconSm"
                variant="destructiveGhost"
            />
        </div>
    );
}
