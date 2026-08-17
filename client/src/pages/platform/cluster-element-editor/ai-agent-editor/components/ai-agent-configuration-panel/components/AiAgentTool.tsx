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

    const isConnectionMissing =
        !!toolComponentDefinition?.connectionRequired && !configuredConnectionKeys.has(tool.name);

    return (
        <div
            className={twMerge(
                'flex items-center gap-2 rounded bg-muted/50 p-2',
                isConnectionMissing && 'border border-red-500'
            )}
        >
            {tool.icon ? (
                <InlineSVG
                    className="size-5 flex-none text-content-neutral-subtle"
                    loader={<ComponentIcon className="size-5 flex-none text-content-neutral-subtle" />}
                    src={tool.icon}
                />
            ) : (
                <ComponentIcon className="size-5 flex-none text-content-neutral-subtle" />
            )}

            <div className="flex flex-1 items-center justify-start gap-1 truncate">
                <span className="text-xs font-medium">{tool.label}</span>

                <span className="px-1 text-xs font-medium">-</span>

                <span className="flex-1 text-xs font-medium">{tool.operationName}</span>
            </div>

            {tool.clusterRoot ? (
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
