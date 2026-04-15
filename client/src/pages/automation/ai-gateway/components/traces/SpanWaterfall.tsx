import {AiObservabilitySpanType as AiObservabilitySpanTypeEnum} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiObservabilitySpanType} from '../../types';

interface SpanWaterfallProps {
    spans: ReadonlyArray<AiObservabilitySpanType>;
}

interface WaterfallRowI {
    depth: number;
    duration: number;
    endOffsetMs: number;
    span: AiObservabilitySpanType;
    startOffsetMs: number;
}

const SPAN_TYPE_COLORS: Record<string, string> = {
    [AiObservabilitySpanTypeEnum.Event]: 'bg-purple-400',
    [AiObservabilitySpanTypeEnum.Generation]: 'bg-blue-500',
    [AiObservabilitySpanTypeEnum.Span]: 'bg-gray-400',
    [AiObservabilitySpanTypeEnum.ToolCall]: 'bg-orange-500',
};

const ERROR_COLOR = 'bg-red-500';

/**
 * Flattens a span tree into a depth-first ordered list so the waterfall mirrors the parent/child nesting of the tree
 * view. Each row knows its indentation depth plus start/end offsets relative to the earliest observed start time,
 * so bars can be laid out on a shared timeline without assuming all spans share a trace-root start.
 */
function flattenSpans(spans: ReadonlyArray<AiObservabilitySpanType>): WaterfallRowI[] {
    if (spans.length === 0) {
        return [];
    }

    const byId = new Map<string, AiObservabilitySpanType>();

    for (const span of spans) {
        byId.set(span.id, span);
    }

    const childrenByParent = new Map<string | null, AiObservabilitySpanType[]>();

    for (const span of spans) {
        const parentKey = span.parentSpanId != null ? String(span.parentSpanId) : null;
        const list = childrenByParent.get(parentKey) ?? [];

        list.push(span);

        childrenByParent.set(parentKey, list);
    }

    const traceStart = spans.reduce<number>((min, span) => {
        const startRaw = span.startTime;

        if (startRaw == null) {
            return min;
        }

        const ms = Number(startRaw);

        return Number.isFinite(ms) && ms < min ? ms : min;
    }, Number.POSITIVE_INFINITY);

    const rows: WaterfallRowI[] = [];

    const visit = (span: AiObservabilitySpanType, depth: number) => {
        const startRaw = span.startTime;
        const endRaw = span.endTime;

        const startMs = startRaw != null ? Number(startRaw) : traceStart;
        const endMs = endRaw != null ? Number(endRaw) : span.latencyMs != null ? startMs + span.latencyMs : startMs;

        rows.push({
            depth,
            duration: Math.max(endMs - startMs, 0),
            endOffsetMs: endMs - traceStart,
            span,
            startOffsetMs: Math.max(startMs - traceStart, 0),
        });

        const children = childrenByParent.get(span.id) ?? [];

        for (const child of children) {
            visit(child, depth + 1);
        }
    };

    const roots = childrenByParent.get(null) ?? [];

    for (const root of roots) {
        visit(root, 0);
    }

    return rows;
}

const SpanWaterfall = ({spans}: SpanWaterfallProps) => {
    const rows = useMemo(() => flattenSpans(spans), [spans]);

    const totalMs = useMemo(() => rows.reduce((max, row) => Math.max(max, row.endOffsetMs), 0), [rows]);

    if (rows.length === 0) {
        return null;
    }

    return (
        <div className="w-full overflow-x-auto rounded-md border text-xs">
            <table className="w-full">
                <thead>
                    <tr className="border-b bg-muted/50">
                        <th className="w-1/3 px-2 py-1 text-left font-medium">Name</th>

                        <th className="px-2 py-1 text-right font-medium">Duration</th>

                        <th className="px-2 py-1 text-left font-medium">Timeline ({totalMs}ms)</th>
                    </tr>
                </thead>

                <tbody>
                    {rows.map((row) => {
                        const widthPct = totalMs > 0 ? (row.duration / totalMs) * 100 : 0;
                        const offsetPct = totalMs > 0 ? (row.startOffsetMs / totalMs) * 100 : 0;

                        const isError = row.span.status === 'ERROR';

                        const color = isError
                            ? ERROR_COLOR
                            : (row.span.type && SPAN_TYPE_COLORS[row.span.type]) || 'bg-gray-400';

                        return (
                            <tr className="border-b" key={row.span.id}>
                                <td
                                    className="whitespace-nowrap px-2 py-1"
                                    style={{paddingLeft: `${8 + row.depth * 12}px`}}
                                >
                                    {row.span.name || '(unnamed span)'}
                                </td>

                                <td className="px-2 py-1 text-right text-muted-foreground">
                                    {row.duration > 0 ? `${Math.round(row.duration)}ms` : '-'}
                                </td>

                                <td className="px-2 py-1">
                                    <div className="relative h-3 w-full rounded-sm bg-muted/50">
                                        <div
                                            className={twMerge('absolute h-3 rounded-sm', color)}
                                            style={{
                                                left: `${offsetPct}%`,
                                                minWidth: '2px',
                                                width: `${Math.max(widthPct, 0.5)}%`,
                                            }}
                                            title={`${row.span.name ?? ''}: ${Math.round(row.duration)}ms`}
                                        />
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};

export default SpanWaterfall;
