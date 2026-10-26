import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {ToolCallEntryI, useAiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {
    AlertCircleIcon,
    BotIcon,
    CheckCircle2Icon,
    ChevronDownIcon,
    ChevronRightIcon,
    Loader2Icon,
    WorkflowIcon,
    WrenchIcon,
} from 'lucide-react';
import {ComponentType, ReactNode, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import type {ToolCallMessagePartProps} from '@assistant-ui/react';

interface StatusIconProps {
    className?: string;
    status: ToolCallEntryI['status'];
}

const StatusIcon = ({className, status}: StatusIconProps) => {
    if (status === 'running') {
        return <Loader2Icon className={twMerge('size-4 animate-spin text-muted-foreground', className)} />;
    }

    if (status === 'error') {
        return <AlertCircleIcon className={twMerge('size-4 text-content-destructive-primary', className)} />;
    }

    return <CheckCircle2Icon className={twMerge('size-4 text-content-success-primary', className)} />;
};

const truncate = (value: string, maxLength: number): string => {
    if (value.length <= maxLength) {
        return value;
    }

    return `${value.slice(0, maxLength).trimEnd()}…`;
};

const summarizeArgs = (args: Record<string, unknown> | undefined): string => {
    if (!args) {
        return '';
    }

    const entries = Object.entries(args);

    if (entries.length === 0) {
        return '';
    }

    const summary = entries
        .map(([key, value]) => {
            const stringValue = typeof value === 'string' ? value : JSON.stringify(value);

            return `${key}: ${truncate(stringValue ?? '', 40)}`;
        })
        .join(', ');

    return truncate(summary, 80);
};

interface CollapsibleCardProps {
    children: ReactNode;
    expanded: boolean;
    header: ReactNode;
    onToggle: () => void;
}

const CollapsibleCard = ({children, expanded, header, onToggle}: CollapsibleCardProps) => {
    return (
        <div className="mt-2 flex w-full flex-col rounded-lg border border-border bg-surface-neutral-secondary text-sm">
            <button
                aria-expanded={expanded}
                className="flex w-full items-center gap-2 px-3 py-2 text-left hover:bg-accent/50"
                onClick={onToggle}
                type="button"
            >
                {expanded ? (
                    <ChevronDownIcon className="size-3.5 shrink-0 text-muted-foreground" />
                ) : (
                    <ChevronRightIcon className="size-3.5 shrink-0 text-muted-foreground" />
                )}

                {header}
            </button>

            {expanded && <div className="border-t border-border px-3 py-2">{children}</div>}
        </div>
    );
};

interface JsonViewerProps {
    label: string;
    value: unknown;
}

const JsonViewer = ({label, value}: JsonViewerProps) => {
    if (value === undefined || value === null) {
        return null;
    }

    const display = typeof value === 'string' ? value : JSON.stringify(value, null, 2);

    return (
        <div className="mt-2 first:mt-0">
            <div className="mb-1 text-xs font-semibold text-muted-foreground">{label}</div>

            <pre className="max-h-60 overflow-auto rounded-md bg-muted px-2 py-1.5 text-xs leading-snug whitespace-pre-wrap">
                {display}
            </pre>
        </div>
    );
};

interface ToolCallBodyProps {
    entry: ToolCallEntryI;
}

const DefaultBody = ({entry}: ToolCallBodyProps) => {
    return (
        <div className="flex flex-col gap-1">
            {entry.args && Object.keys(entry.args).length > 0 && <JsonViewer label="Input" value={entry.args} />}

            {entry.result !== undefined && <JsonViewer label="Result" value={entry.result} />}

            {!entry.args && entry.result === undefined && (
                <div className="text-xs text-muted-foreground italic">No input or result.</div>
            )}
        </div>
    );
};

/**
 * Tool calls that open a resource in the right panel. They render as a compact clickable link (name + kind) that
 * re-opens the tab, instead of the raw-JSON card.
 */
const ARTIFACT_OPEN_META: Record<string, {icon: ComponentType<{className?: string}>; kind: string}> = {
    openAiAgentTab: {icon: BotIcon, kind: 'AI Agent'},
    openWorkflowTab: {icon: WorkflowIcon, kind: 'Workflow'},
};

/** Exported so history rehydration skips these; their link cards are rebuilt from the durable artifact rows. */
export const ARTIFACT_OPEN_TOOL_NAMES: ReadonlySet<string> = new Set(Object.keys(ARTIFACT_OPEN_META));

const openArtifactTab = (toolName: string, args: Record<string, unknown> | undefined) => {
    const name = typeof args?.name === 'string' && args.name ? args.name : 'Untitled';

    if (toolName === 'openAiAgentTab' && typeof args?.aiAgentId === 'string' && args.aiAgentId) {
        aiHubTabsStore.getState().openAiAgentTab({aiAgentId: args.aiAgentId, name});
    } else if (
        toolName === 'openWorkflowTab' &&
        typeof args?.workflowId === 'string' &&
        args?.projectId != null &&
        args.projectId !== '' &&
        args?.projectWorkflowId != null
    ) {
        aiHubTabsStore.getState().openWorkflowTab({
            name,
            projectId: String(args.projectId),
            projectWorkflowId: Number(args.projectWorkflowId),
            workflowId: args.workflowId,
        });
    }
};

interface ArtifactLinkProps {
    args: Record<string, unknown> | undefined;
    toolName: string;
}

const ArtifactLink = ({args, toolName}: ArtifactLinkProps) => {
    const meta = ARTIFACT_OPEN_META[toolName];
    const Icon = meta.icon;
    const name = typeof args?.name === 'string' && args.name ? args.name : 'Untitled';

    return (
        <button
            className="mt-2 flex w-full items-center gap-2.5 rounded-lg border border-border bg-surface-neutral-secondary px-3 py-2 text-left hover:bg-accent/50"
            onClick={() => openArtifactTab(toolName, args)}
            type="button"
        >
            <Icon className="size-4 shrink-0 text-muted-foreground" />

            <span className="flex min-w-0 flex-col">
                <span className="truncate text-sm font-medium text-content-brand-primary group-hover:underline">
                    {name}
                </span>

                <span className="text-xs text-muted-foreground">{meta.kind}</span>
            </span>
        </button>
    );
};

interface AiHubToolCallRendererProps {
    args?: Record<string, unknown>;
    argsText?: string;
    isError?: boolean;
    result?: unknown;
    toolCallId?: string;
    toolName: string;
}

const AiHubToolCallRenderer = ({args, argsText, isError, result, toolCallId, toolName}: AiHubToolCallRendererProps) => {
    const [expanded, setExpanded] = useState(false);

    const liveEntry = useAiChatToolCallStore((state) => (toolCallId ? state.toolCalls[toolCallId] : undefined));

    const status: ToolCallEntryI['status'] = liveEntry
        ? liveEntry.status
        : result !== undefined
          ? isError
              ? 'error'
              : 'success'
          : 'running';

    const mergedArgs = liveEntry?.args ?? args;
    const mergedResult = liveEntry?.result ?? result;

    if (ARTIFACT_OPEN_META[toolName]) {
        return <ArtifactLink args={mergedArgs} toolName={toolName} />;
    }

    const entry: ToolCallEntryI = {
        args: mergedArgs,
        messageIndex: liveEntry?.messageIndex ?? -1,
        progress: liveEntry?.progress ?? [],
        progressiveOutput: liveEntry?.progressiveOutput ?? '',
        result: mergedResult,
        status,
        toolCallId: toolCallId ?? '',
        toolName,
    };

    const argsSummary = mergedArgs ? summarizeArgs(mergedArgs) : argsText ? truncate(argsText, 80) : '';

    const header = (
        <div className="flex min-w-0 flex-1 items-center gap-2">
            <WrenchIcon className="size-4 text-muted-foreground" />

            <span className="truncate font-medium text-foreground">{toolName}</span>

            {argsSummary && <span className="truncate text-xs text-muted-foreground">- {argsSummary}</span>}

            <span className="ml-auto flex items-center gap-1">
                <StatusIcon status={status} />
            </span>
        </div>
    );

    return (
        <CollapsibleCard
            expanded={expanded}
            header={header}
            onToggle={() => setExpanded((previousExpanded) => !previousExpanded)}
        >
            <DefaultBody entry={entry} />
        </CollapsibleCard>
    );
};

const RUNNING_TOOL_CALL_LABELS: Record<string, string> = {
    buildWorkflow: 'Building workflow...',
    importWorkflow: 'Importing workflow...',
};

interface RunningToolCallRowProps {
    toolCallId?: string;
    toolName: string;
}

const RunningToolCallRow = ({toolCallId, toolName}: RunningToolCallRowProps) => {
    const isRunning = useAiChatToolCallStore((state) =>
        toolCallId ? state.toolCalls[toolCallId]?.status === 'running' : false
    );

    if (!isRunning) {
        return null;
    }

    return (
        <div className="mt-2 flex items-center gap-2 text-sm text-muted-foreground" role="status">
            <Loader2Icon className="size-4 animate-spin" />

            <span>{RUNNING_TOOL_CALL_LABELS[toolName] ?? `Running ${toolName}...`}</span>
        </div>
    );
};

export const AiHubToolCallFallback = ({
    args,
    argsText,
    isError,
    result,
    toolCallId,
    toolName,
}: ToolCallMessagePartProps) => {
    const showToolCalls = useAiHubSettingsStore((state) => state.showToolCalls);

    if (!showToolCalls && !ARTIFACT_OPEN_META[toolName]) {
        return <RunningToolCallRow toolCallId={toolCallId} toolName={toolName} />;
    }

    return (
        <AiHubToolCallRenderer
            args={args}
            argsText={argsText}
            isError={isError}
            result={result}
            toolCallId={toolCallId}
            toolName={toolName}
        />
    );
};

export default AiHubToolCallRenderer;
