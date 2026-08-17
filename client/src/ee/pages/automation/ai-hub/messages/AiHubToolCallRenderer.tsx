import Badge from '@/components/Badge/Badge';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {ToolCallEntryI, useAiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {
    AlertCircleIcon,
    BlocksIcon,
    BotIcon,
    CheckCircle2Icon,
    ChevronDownIcon,
    ChevronRightIcon,
    CodeIcon,
    DatabaseIcon,
    FileTextIcon,
    LayersIcon,
    Loader2Icon,
    PlayIcon,
    SearchIcon,
    SparklesIcon,
    WorkflowIcon,
    WrenchIcon,
} from 'lucide-react';
import {ComponentType, ReactNode, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import type {ToolCallMessagePartProps} from '@assistant-ui/react';

const SUBAGENT_TOOL_NAMES = new Set(['dataAnalyst', 'imageGenerator', 'research', 'slideBuilder', 'workflowBuilder']);

const MEMORY_TOOL_NAMES = new Set(['createMemory', 'deleteMemory', 'getMemory', 'listMemories', 'updateMemory']);

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

interface ToolIconForNameProps {
    className?: string;
    toolName: string;
}

const ToolIconForName = ({className, toolName}: ToolIconForNameProps) => {
    const iconClass = twMerge('size-4 text-muted-foreground', className);

    if (toolName === 'runChatWorkflow') {
        return <PlayIcon className={iconClass} />;
    }

    if (toolName === 'workflowBuilder') {
        return <WorkflowIcon className={iconClass} />;
    }

    if (toolName === 'research') {
        return <SearchIcon className={iconClass} />;
    }

    if (toolName === 'imageGenerator' || toolName === 'slideBuilder') {
        return <SparklesIcon className={iconClass} />;
    }

    if (toolName === 'dataAnalyst') {
        return <DatabaseIcon className={iconClass} />;
    }

    if (MEMORY_TOOL_NAMES.has(toolName)) {
        return <FileTextIcon className={iconClass} />;
    }

    return <WrenchIcon className={iconClass} />;
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

interface SpecialRendererProps {
    entry: ToolCallEntryI;
}

const RunChatWorkflowBody = ({entry}: SpecialRendererProps) => {
    const steps = entry.progressiveOutput
        .split(/\n{2,}/)
        .map((segment) => segment.trim())
        .filter((segment) => segment.length > 0);

    return (
        <div className="flex flex-col gap-2">
            {steps.length === 0 ? (
                <div className="text-xs text-muted-foreground italic">Waiting for workflow output…</div>
            ) : (
                steps.map((step, index) => (
                    <div className="rounded-md bg-muted/60 px-2 py-1.5 text-xs" key={`step-${index}`}>
                        <div className="mb-0.5 flex items-center gap-1.5 font-semibold text-muted-foreground">
                            <StatusIcon
                                className="size-3"
                                status={index === steps.length - 1 ? entry.status : 'success'}
                            />

                            <span>Step {index + 1}</span>
                        </div>

                        <div className="whitespace-pre-wrap">{step}</div>
                    </div>
                ))
            )}

            {entry.args && Object.keys(entry.args).length > 0 && <JsonViewer label="Input" value={entry.args} />}
        </div>
    );
};

const SubagentBody = ({entry}: SpecialRendererProps) => {
    return (
        <div className="flex flex-col gap-2">
            {entry.progress.length === 0 ? (
                <div className="text-xs text-muted-foreground italic">Subagent is starting…</div>
            ) : (
                <ol className="flex flex-col gap-1.5">
                    {entry.progress.map((progress, index) => (
                        <li
                            className="flex items-start gap-2 text-xs text-muted-foreground"
                            key={`${progress.timestamp}-${index}`}
                        >
                            <ChevronRightIcon className="mt-0.5 size-3 shrink-0" />

                            <span className="break-words">{progress.text}</span>
                        </li>
                    ))}
                </ol>
            )}

            {entry.args && Object.keys(entry.args).length > 0 && <JsonViewer label="Input" value={entry.args} />}

            {entry.result !== undefined && <JsonViewer label="Result" value={entry.result} />}
        </div>
    );
};

/**
 * Renders the result of an attachChatTool call as a compact "Attached: <Component> → <Action>" affordance instead of
 * raw JSON. Falls through to the input-rendered args while the call is still in flight so the user sees what's being
 * attached before the result lands. Re-attach (replacedExistingTool=true on the hardened path) flips the headline to
 * "Reconfigured" so the chat thread doesn't read as if a new tool appeared every time the user adjusts parameters.
 */
const AttachChatToolBody = ({entry}: SpecialRendererProps) => {
    const args = entry.args ?? {};

    const componentName = typeof args.componentName === 'string' ? args.componentName : undefined;
    const actionName = typeof args.actionName === 'string' ? args.actionName : undefined;
    const connectionId = args.connectionId == null ? undefined : String(args.connectionId);

    const result = (entry.result ?? {}) as Record<string, unknown>;
    const attachedToolName = typeof result.attachedToolName === 'string' ? result.attachedToolName : undefined;
    const replacedExistingTool = result.replacedExistingTool === true;

    const isComplete = entry.status === 'success' && entry.result !== undefined;

    if (!isComplete) {
        return (
            <div className="flex flex-col gap-1 text-xs">
                <div className="text-muted-foreground italic">
                    {componentName && actionName ? `Attaching ${componentName} → ${actionName}…` : 'Attaching tool…'}
                </div>

                {entry.args && Object.keys(entry.args).length > 0 && <JsonViewer label="Input" value={entry.args} />}
            </div>
        );
    }

    const headline = replacedExistingTool ? 'Reconfigured' : 'Attached';

    return (
        <div className="flex flex-col gap-1 text-xs">
            <div className="flex items-center gap-2">
                <CheckCircle2Icon className="size-4 text-content-success-primary" />

                <span>
                    <span>{headline}: </span>

                    <span className="font-medium">{componentName ?? '?'}</span>

                    <span> → </span>

                    <span className="font-medium">{actionName ?? attachedToolName ?? '?'}</span>
                </span>
            </div>

            {connectionId && (
                <div className="text-muted-foreground">
                    Connection id: <span className="font-mono">{connectionId}</span>
                </div>
            )}
        </div>
    );
};

/**
 * Counterpart to AttachChatToolBody for removeChatTool. Surfaces removed={true|false} explicitly so the LLM-emitted
 * "no matching tool found" path reads cleanly instead of as a generic-success.
 */
const RemoveChatToolBody = ({entry}: SpecialRendererProps) => {
    const args = entry.args ?? {};

    const componentName = typeof args.componentName === 'string' ? args.componentName : undefined;
    const actionName = typeof args.actionName === 'string' ? args.actionName : undefined;

    const result = (entry.result ?? {}) as Record<string, unknown>;
    const removed = result.removed === true;
    const message = typeof result.message === 'string' ? result.message : undefined;

    if (entry.status !== 'success' || entry.result === undefined) {
        return (
            <div className="flex flex-col gap-1 text-xs">
                <div className="text-muted-foreground italic">
                    {componentName && actionName ? `Detaching ${componentName} → ${actionName}…` : 'Detaching tool…'}
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-1 text-xs">
            <div className="flex items-center gap-2">
                <CheckCircle2Icon
                    className={twMerge('size-4', removed ? 'text-content-success-primary' : 'text-muted-foreground')}
                />

                <span>
                    <span>{removed ? 'Detached' : 'Not attached'}: </span>

                    <span className="font-medium">{componentName ?? '?'}</span>

                    <span> → </span>

                    <span className="font-medium">{actionName ?? '?'}</span>
                </span>
            </div>

            {message && <div className="text-muted-foreground">{message}</div>}
        </div>
    );
};

const MemoryBody = ({entry}: SpecialRendererProps) => {
    const args = entry.args ?? {};
    const memoryName = typeof args.name === 'string' ? args.name : typeof args.id === 'string' ? args.id : undefined;
    const memoryType = typeof args.type === 'string' ? args.type : undefined;

    return (
        <div className="flex flex-col gap-1 text-xs">
            {memoryName && (
                <div className="flex items-center gap-2">
                    <span className="font-semibold text-muted-foreground">Name:</span>

                    <span className="break-all">{memoryName}</span>
                </div>
            )}

            {memoryType && (
                <div>
                    <Badge
                        className="bg-purple-100 text-[10px] text-purple-700 dark:bg-purple-950 dark:text-purple-300"
                        styleType="outline-outline"
                    >
                        {memoryType}
                    </Badge>
                </div>
            )}

            {entry.args && Object.keys(entry.args).length > 0 && <JsonViewer label="Input" value={entry.args} />}

            {entry.result !== undefined && <JsonViewer label="Result" value={entry.result} />}
        </div>
    );
};

const DefaultBody = ({entry}: SpecialRendererProps) => {
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

const SPECIAL_BODIES: Record<string, ComponentType<SpecialRendererProps>> = {
    attachChatTool: AttachChatToolBody,
    createMemory: MemoryBody,
    dataAnalyst: SubagentBody,
    deleteMemory: MemoryBody,
    getMemory: MemoryBody,
    imageGenerator: SubagentBody,
    listMemories: MemoryBody,
    removeChatTool: RemoveChatToolBody,
    research: SubagentBody,
    runChatWorkflow: RunChatWorkflowBody,
    slideBuilder: SubagentBody,
    updateMemory: MemoryBody,
    workflowBuilder: SubagentBody,
};

// Tool calls that open a resource in the right panel. Instead of the raw-JSON collapsible card, these render
// as a compact clickable artifact link (name + kind) that re-opens the tab on click — the in-chat equivalent
// of an artifact row in the left sidebar.
const ARTIFACT_OPEN_META: Record<string, {icon: ComponentType<{className?: string}>; kind: string}> = {
    openAiAgentTab: {icon: BotIcon, kind: 'AI Agent'},
    openCodeWorkflowTab: {icon: CodeIcon, kind: 'Code Workflow'},
    openCustomComponentTab: {icon: BlocksIcon, kind: 'Custom Component'},
    openDataTableTab: {icon: DatabaseIcon, kind: 'Data Table'},
    openFileTab: {icon: FileTextIcon, kind: 'File'},
    openKnowledgeBaseTab: {icon: LayersIcon, kind: 'Knowledge Base'},
    openWorkflowTab: {icon: WorkflowIcon, kind: 'Workflow'},
};

// Exported so history rehydration (useSwitchChat) can skip these — their link cards are rebuilt from the
// durable artifact rows instead, which also covers chats recorded before tool events were persisted.
export const ARTIFACT_OPEN_TOOL_NAMES: ReadonlySet<string> = new Set(Object.keys(ARTIFACT_OPEN_META));

/**
 * Live openCodeWorkflowTab tool calls carry `language` directly, but REHYDRATED cards (see
 * useSwitchChat's artifactToOpenToolCall) only carry `{name, projectId}` — the artifact row never stashed
 * the language (the server-side recorder only stores projectId + name). Resolve it the same way the
 * sidebar's quick-open does (AiHubChatsSidebar.openCodeWorkflowArtifact): fetch the project and read its
 * `codeWorkflowLanguage`. A missing language means the project is no longer code-backed (e.g. converted
 * back to a visual workflow) — surface that as a toast instead of opening a tab with a bogus language.
 */
const openCodeWorkflowArtifact = async (projectId: string, name: string): Promise<void> => {
    try {
        const project = await new ProjectApi().getProject({id: Number(projectId)});

        if (!project.codeWorkflowLanguage) {
            toast.error(`"${name}" is no longer a code workflow.`);

            return;
        }

        aiHubTabsStore.getState().openCodeWorkflowTab(projectId, project.codeWorkflowLanguage, name);
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);

        toast.error(`Failed to open "${name}": ${message}`);
    }
};

const openArtifactTab = (toolName: string, args: Record<string, unknown> | undefined) => {
    const name = typeof args?.name === 'string' && args.name ? args.name : 'Untitled';
    const tabsStore = aiHubTabsStore.getState();

    if (toolName === 'openFileTab' && typeof args?.fileId === 'string') {
        tabsStore.openFileTab(args.fileId, name);
    } else if (
        toolName === 'openWorkflowTab' &&
        typeof args?.workflowId === 'string' &&
        typeof args?.projectId === 'string' &&
        args?.projectWorkflowId != null
    ) {
        tabsStore.openWorkflowTab(args.workflowId, args.projectId, Number(args.projectWorkflowId), name);
    } else if (toolName === 'openDataTableTab' && args?.dataTableId != null) {
        tabsStore.openDataTableTab(String(args.dataTableId), name);
    } else if (toolName === 'openKnowledgeBaseTab' && args?.knowledgeBaseId != null) {
        tabsStore.openKnowledgeBaseTab(String(args.knowledgeBaseId), name);
    } else if (toolName === 'openCustomComponentTab' && args?.customComponentId != null) {
        tabsStore.openCustomComponentTab(String(args.customComponentId), name);
    } else if (toolName === 'openCodeWorkflowTab' && typeof args?.projectId === 'string') {
        if (typeof args?.language === 'string' && args.language) {
            tabsStore.openCodeWorkflowTab(args.projectId, args.language, name);
        } else {
            void openCodeWorkflowArtifact(args.projectId, name);
        }
    } else if (toolName === 'openAiAgentTab' && args?.aiAgentId != null) {
        tabsStore.openAiAgentTab(String(args.aiAgentId), name);
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
    const isSubagent = SUBAGENT_TOOL_NAMES.has(toolName);
    const isMemory = MEMORY_TOOL_NAMES.has(toolName);

    const [expanded, setExpanded] = useState(isSubagent || toolName === 'runChatWorkflow');

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

    // Resource-open tool calls render as a clickable artifact link rather than a JSON card.
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

    const Body = SPECIAL_BODIES[toolName] ?? DefaultBody;

    const argsSummary = mergedArgs ? summarizeArgs(mergedArgs) : argsText ? truncate(argsText, 80) : '';

    const handleToggle = () => setExpanded((prev) => !prev);

    const header = (
        <div className="flex min-w-0 flex-1 items-center gap-2">
            <ToolIconForName toolName={toolName} />

            <span className="truncate font-medium text-foreground">{toolName}</span>

            {argsSummary && <span className="truncate text-xs text-muted-foreground">— {argsSummary}</span>}

            <span className="ml-auto flex items-center gap-1">
                {isSubagent && (
                    <Badge className="bg-amber-100 text-[10px] text-amber-700" styleType="outline-outline">
                        subagent
                    </Badge>
                )}

                {isMemory && (
                    <Badge
                        className="bg-purple-100 text-[10px] text-purple-700 dark:bg-purple-950 dark:text-purple-300"
                        styleType="outline-outline"
                    >
                        memory
                    </Badge>
                )}

                <StatusIcon status={status} />
            </span>
        </div>
    );

    return (
        <CollapsibleCard expanded={expanded} header={header} onToggle={handleToggle}>
            <Body entry={entry} />
        </CollapsibleCard>
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

    // Artifact link cards are deliverables, not tool noise — always visible. Everything else honors the
    // header toggle (hidden by default).
    if (!showToolCalls && !ARTIFACT_OPEN_META[toolName]) {
        return null;
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
