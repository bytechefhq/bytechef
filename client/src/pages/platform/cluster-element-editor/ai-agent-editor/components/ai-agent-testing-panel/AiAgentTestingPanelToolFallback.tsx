import Button from '@/components/Button/Button';
import useAiAgentTools from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/hooks/useAiAgentTools';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {ChevronDownIcon, ChevronUpIcon, WrenchIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import type {ToolCallMessagePartComponent} from '@assistant-ui/react';

function prettyPrintJson(value: unknown): string {
    if (typeof value === 'string') {
        try {
            const parsed = JSON.parse(value);

            return JSON.stringify(parsed, null, 2);
        } catch {
            return value;
        }
    }

    return JSON.stringify(value, null, 2);
}

function toBackendToolName(componentName: string, operationName: string): string {
    let result = componentName.toUpperCase() + '_';

    for (const char of operationName) {
        if (char >= 'A' && char <= 'Z') {
            result += '_' + char;
        } else {
            result += char.toUpperCase();
        }
    }

    return result;
}

export const AiAgentTestingPanelToolFallback: ToolCallMessagePartComponent = ({argsText, result, toolName}) => {
    const [isExpanded, setIsExpanded] = useState(false);

    const {tools} = useAiAgentTools();

    const toolItem = useMemo(() => {
        return tools.find((tool) => toBackendToolName(tool.componentName, tool.operationName) === toolName);
    }, [tools, toolName]);

    const {data: clusterElementDefinition} = useGetClusterElementDefinitionQuery(
        {
            clusterElementName: toolItem?.operationName || toolName,
            componentName: toolItem?.componentName || '',
            componentVersion: toolItem?.componentVersion || 1,
        },
        !!toolItem
    );

    const toolResult: {confidence?: string; output?: unknown; reasoning?: string} =
        typeof result === 'object' && result !== null
            ? (result as {confidence?: string; output?: unknown; reasoning?: string})
            : {output: result};

    const {confidence, output, reasoning} = toolResult;

    let inputs: unknown;

    try {
        inputs = JSON.parse(argsText);
    } catch {
        inputs = argsText;
    }

    const displayIcon = clusterElementDefinition?.icon || toolItem?.icon;
    const componentTitle = toolItem?.title || toolName;
    const toolTitle = clusterElementDefinition?.title;

    return (
        <div className="my-2 overflow-hidden rounded-lg border bg-muted/30">
            <Button
                className="flex h-auto w-full items-center justify-between rounded-none px-4 py-3"
                onClick={() => setIsExpanded(!isExpanded)}
                variant="ghost"
            >
                <div className="flex items-center gap-2">
                    {displayIcon ? (
                        <InlineSVG
                            className="size-5"
                            loader={<WrenchIcon className="size-4 text-muted-foreground" />}
                            src={displayIcon}
                        />
                    ) : (
                        <WrenchIcon className="size-4 text-muted-foreground" />
                    )}

                    <span className="text-sm font-medium">
                        {componentTitle}

                        {toolTitle && `: ${toolTitle}`}
                    </span>
                </div>

                {isExpanded ? (
                    <ChevronUpIcon className="size-4 text-muted-foreground" />
                ) : (
                    <ChevronDownIcon className="size-4 text-muted-foreground" />
                )}
            </Button>

            {isExpanded && (
                <div className="space-y-3 border-t px-4 py-3">
                    {reasoning && (
                        <div>
                            <span className="text-xs font-medium text-muted-foreground">Reasoning</span>

                            <p className="mt-0.5 text-sm">{reasoning}</p>
                        </div>
                    )}

                    {confidence && (
                        <div>
                            <span className="text-xs font-medium text-muted-foreground">Confidence</span>

                            <span className="ml-2 rounded-full bg-muted px-2 py-0.5 text-xs">{confidence}</span>
                        </div>
                    )}

                    <div>
                        <span className="text-xs font-medium text-muted-foreground">Input</span>

                        <pre className="mt-1 overflow-auto rounded-md bg-background p-3 text-xs leading-relaxed">
                            {prettyPrintJson(inputs)}
                        </pre>
                    </div>

                    {output !== undefined && (
                        <div>
                            <span className="text-xs font-medium text-muted-foreground">Output</span>

                            <pre className="mt-1 max-h-64 overflow-auto rounded-md bg-background p-3 text-xs leading-relaxed">
                                {prettyPrintJson(output)}
                            </pre>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};
