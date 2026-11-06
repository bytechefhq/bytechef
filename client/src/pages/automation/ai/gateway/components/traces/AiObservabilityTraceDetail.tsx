import PageLoader from '@/components/PageLoader';
import {
    AiObservabilitySpanType as AiObservabilitySpanTypeEnum,
    useAiObservabilityTraceQuery,
} from '@/shared/middleware/graphql';
import {ArrowLeftIcon, ChevronDownIcon, ChevronRightIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiObservabilitySpanType} from '../../types';
import SpanWaterfall from './SpanWaterfall';

interface AiObservabilityTraceDetailProps {
    onBack: () => void;
    traceId: string;
}

interface SpanTreeNodeI {
    children: SpanTreeNodeI[];
    span: AiObservabilitySpanType;
}

const SPAN_TYPE_CLASSES: Record<string, string> = {
    [AiObservabilitySpanTypeEnum.Event]: 'bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300',
    [AiObservabilitySpanTypeEnum.Generation]: 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300',
    [AiObservabilitySpanTypeEnum.Span]: 'bg-gray-100 text-gray-800 dark:bg-gray-950 dark:text-gray-300',
    [AiObservabilitySpanTypeEnum.ToolCall]: 'bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300',
};

const TRACE_STATUS_CLASSES: Record<string, string> = {
    ACTIVE: 'bg-surface-brand-secondary text-content-brand-primary',
    COMPLETED: 'bg-surface-success-secondary text-content-success-primary',
    ERROR: 'bg-surface-destructive-secondary text-content-destructive',
};

function buildSpanTree(spans: AiObservabilitySpanType[]): SpanTreeNodeI[] {
    const nodeMap = new Map<string, SpanTreeNodeI>();

    for (const span of spans) {
        nodeMap.set(span.id, {children: [], span});
    }

    const roots: SpanTreeNodeI[] = [];

    for (const span of spans) {
        const node = nodeMap.get(span.id);

        if (!node) {
            continue;
        }

        if (span.parentSpanId) {
            const parentNode = nodeMap.get(span.parentSpanId);

            if (parentNode) {
                parentNode.children.push(node);
            } else {
                roots.push(node);
            }
        } else {
            roots.push(node);
        }
    }

    return roots;
}

interface SpanNodeProps {
    depth: number;
    expandedSpanIds: Set<string>;
    node: SpanTreeNodeI;
    onToggleExpand: (spanId: string) => void;
}

const SpanNode = ({depth, expandedSpanIds, node, onToggleExpand}: SpanNodeProps) => {
    const {span} = node;
    const isExpanded = expandedSpanIds.has(span.id);

    return (
        <div style={{marginLeft: `${depth * 16}px`}}>
            <div
                className="mb-1 cursor-pointer rounded border bg-card p-3 hover:bg-muted/30"
                onClick={() => onToggleExpand(span.id)}
            >
                <div className="flex items-center gap-3">
                    {isExpanded ? (
                        <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                    ) : (
                        <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" />
                    )}

                    <span
                        className={twMerge(
                            'rounded-full px-2 py-0.5 text-xs font-medium',
                            SPAN_TYPE_CLASSES[span.type] || 'bg-surface-neutral-secondary text-content-neutral-primary'
                        )}
                    >
                        {span.type}
                    </span>

                    <span className="font-medium">{span.name || '—'}</span>

                    {span.model && <span className="text-sm text-muted-foreground">{span.model}</span>}

                    <div className="ml-auto flex items-center gap-4 text-sm text-muted-foreground">
                        {span.latencyMs != null && <span>{span.latencyMs}ms</span>}

                        {span.cost != null && <span>${span.cost.toFixed(6)}</span>}

                        <span
                            className={twMerge(
                                'rounded-full px-2 py-0.5 text-xs font-medium',
                                span.status === 'COMPLETED'
                                    ? 'bg-surface-success-secondary text-content-success-primary'
                                    : span.status === 'ERROR'
                                      ? 'bg-surface-destructive-secondary text-content-destructive'
                                      : 'bg-surface-brand-secondary text-content-brand-primary'
                            )}
                        >
                            {span.status}
                        </span>
                    </div>
                </div>

                {isExpanded && (
                    <div className="mt-3 border-t pt-3 text-sm">
                        <div className="grid grid-cols-2 gap-4">
                            {span.provider && (
                                <div>
                                    <span className="font-medium text-muted-foreground">Provider:</span> {span.provider}
                                </div>
                            )}

                            {span.inputTokens != null && (
                                <div>
                                    <span className="font-medium text-muted-foreground">{'Input Tokens: '}</span>

                                    <span>{span.inputTokens}</span>
                                </div>
                            )}

                            {span.outputTokens != null && (
                                <div>
                                    <span className="font-medium text-muted-foreground">{'Output Tokens: '}</span>

                                    <span>{span.outputTokens}</span>
                                </div>
                            )}

                            {span.startTime && (
                                <div>
                                    <span className="font-medium text-muted-foreground">{'Start: '}</span>

                                    <span>{new Date(span.startTime).toLocaleString()}</span>
                                </div>
                            )}

                            {span.endTime && (
                                <div>
                                    <span className="font-medium text-muted-foreground">{'End: '}</span>

                                    <span>{new Date(span.endTime).toLocaleString()}</span>
                                </div>
                            )}
                        </div>

                        {span.input && (
                            <div className="mt-3">
                                <span className="font-medium text-muted-foreground">Input:</span>

                                <pre className="mt-1 max-h-40 overflow-y-auto rounded bg-muted p-2 text-xs whitespace-pre-wrap">
                                    {span.input}
                                </pre>
                            </div>
                        )}

                        {span.output && (
                            <div className="mt-3">
                                <span className="font-medium text-muted-foreground">Output:</span>

                                <pre className="mt-1 max-h-40 overflow-y-auto rounded bg-muted p-2 text-xs whitespace-pre-wrap">
                                    {span.output}
                                </pre>
                            </div>
                        )}

                        {span.metadata && (
                            <div className="mt-3">
                                <span className="font-medium text-muted-foreground">Metadata:</span>

                                <pre className="mt-1 max-h-40 overflow-y-auto rounded bg-muted p-2 text-xs whitespace-pre-wrap">
                                    {span.metadata}
                                </pre>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {node.children.map((childNode) => (
                <SpanNode
                    depth={depth + 1}
                    expandedSpanIds={expandedSpanIds}
                    key={childNode.span.id}
                    node={childNode}
                    onToggleExpand={onToggleExpand}
                />
            ))}
        </div>
    );
};

function parseMetadataEntries(metadata: string): [string, string][] {
    try {
        const parsed = JSON.parse(metadata) as Record<string, string>;

        return Object.entries(parsed);
    } catch (error) {
        console.error('Failed to parse trace metadata as JSON', error);

        return [];
    }
}

interface TraceMetadataProps {
    metadata: string;
}

const TraceMetadata = ({metadata}: TraceMetadataProps) => {
    const entries = useMemo(() => parseMetadataEntries(metadata), [metadata]);

    if (entries.length === 0) {
        return null;
    }

    return (
        <div className="mt-2 flex flex-wrap gap-2">
            {entries.map(([key, value]) => (
                <span className="rounded bg-muted px-2 py-0.5 text-xs text-muted-foreground" key={key}>
                    {key}: {String(value)}
                </span>
            ))}
        </div>
    );
};

const AiObservabilityTraceDetail = ({onBack, traceId}: AiObservabilityTraceDetailProps) => {
    const [expandedSpanIds, setExpandedSpanIds] = useState<Set<string>>(new Set());
    const [spanView, setSpanView] = useState<'tree' | 'waterfall'>('tree');

    const {
        data: traceData,
        isError: traceIsError,
        isLoading: traceIsLoading,
    } = useAiObservabilityTraceQuery({id: traceId});

    const trace = traceData?.aiObservabilityTrace;

    const handleToggleExpand = useCallback((spanId: string) => {
        setExpandedSpanIds((previousExpandedIds) => {
            const nextExpandedIds = new Set(previousExpandedIds);

            if (nextExpandedIds.has(spanId)) {
                nextExpandedIds.delete(spanId);
            } else {
                nextExpandedIds.add(spanId);
            }

            return nextExpandedIds;
        });
    }, []);

    const spans = useMemo(
        () => (trace?.spans ?? []).filter((span): span is AiObservabilitySpanType => span != null),
        [trace?.spans]
    );

    const spanTree = useMemo(() => buildSpanTree(spans), [spans]);

    if (traceIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (traceIsError) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <button
                    className="mb-4 flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
                    onClick={onBack}
                >
                    <ArrowLeftIcon className="size-4" />

                    <span>Back to Traces</span>
                </button>

                <p className="text-destructive">Failed to load trace. Please try again.</p>
            </div>
        );
    }

    if (!trace) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <button
                    className="mb-4 flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
                    onClick={onBack}
                >
                    <ArrowLeftIcon className="size-4" />

                    <span>Back to Traces</span>
                </button>

                <p className="text-muted-foreground">Trace not found.</p>
            </div>
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <button
                className="mb-4 flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
                onClick={onBack}
            >
                <ArrowLeftIcon className="size-4" />

                <span>Back to Traces</span>
            </button>

            <div className="mb-6 rounded-lg border bg-card p-6">
                <div className="mb-4 flex items-center gap-3">
                    <h2 className="text-xl font-semibold">{trace.name || 'Unnamed Trace'}</h2>

                    <span
                        className={twMerge(
                            'rounded-full px-2 py-0.5 text-xs font-medium',
                            TRACE_STATUS_CLASSES[trace.status] ||
                                'bg-surface-neutral-secondary text-content-neutral-primary'
                        )}
                    >
                        {trace.status}
                    </span>
                </div>

                <div className="flex flex-wrap gap-6 text-sm text-muted-foreground">
                    {trace.userId && (
                        <div>
                            <span className="font-medium">User:</span> {trace.userId}
                        </div>
                    )}

                    {trace.totalLatencyMs != null && (
                        <div>
                            <span className="font-medium">Latency:</span> {trace.totalLatencyMs}ms
                        </div>
                    )}

                    {trace.totalCost != null && (
                        <div>
                            <span className="font-medium">Cost:</span> ${trace.totalCost.toFixed(6)}
                        </div>
                    )}

                    {trace.totalInputTokens != null && (
                        <div>
                            <span className="font-medium">Input Tokens:</span> {trace.totalInputTokens}
                        </div>
                    )}

                    {trace.totalOutputTokens != null && (
                        <div>
                            <span className="font-medium">Output Tokens:</span> {trace.totalOutputTokens}
                        </div>
                    )}
                </div>

                {trace.metadata && <TraceMetadata metadata={trace.metadata} />}
            </div>

            <div className="mb-3 flex items-center justify-between">
                <h3 className="text-lg font-semibold">Spans</h3>

                <div className="flex gap-1 text-xs">
                    <button
                        className={twMerge(
                            'rounded-md px-2 py-1',
                            spanView === 'tree'
                                ? 'bg-primary text-primary-foreground'
                                : 'bg-muted text-muted-foreground hover:bg-muted/80'
                        )}
                        onClick={() => setSpanView('tree')}
                    >
                        Tree
                    </button>

                    <button
                        className={twMerge(
                            'rounded-md px-2 py-1',
                            spanView === 'waterfall'
                                ? 'bg-primary text-primary-foreground'
                                : 'bg-muted text-muted-foreground hover:bg-muted/80'
                        )}
                        onClick={() => setSpanView('waterfall')}
                    >
                        Waterfall
                    </button>
                </div>
            </div>

            {spanTree.length === 0 ? (
                <p className="text-muted-foreground">No spans recorded for this trace.</p>
            ) : spanView === 'waterfall' ? (
                <SpanWaterfall spans={spans} />
            ) : (
                <div>
                    {spanTree.map((rootNode) => (
                        <SpanNode
                            depth={0}
                            expandedSpanIds={expandedSpanIds}
                            key={rootNode.span.id}
                            node={rootNode}
                            onToggleExpand={handleToggleExpand}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

export default AiObservabilityTraceDetail;
