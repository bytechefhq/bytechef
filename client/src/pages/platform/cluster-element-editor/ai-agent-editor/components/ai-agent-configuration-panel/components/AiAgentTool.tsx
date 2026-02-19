import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import AiAgentToolDropdownMenu from './AiAgentToolDropdownMenu';
import {ToolItemI} from './hooks/useAiAgentTools';

interface AiAgentToolPropsI {
    configuredConnectionKeys: Set<string>;
    tool: ToolItemI;
}

export default function AiAgentTool({configuredConnectionKeys, tool}: AiAgentToolPropsI) {
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

            <AiAgentToolDropdownMenu tool={tool} />
        </div>
    );
}
