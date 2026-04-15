import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiObservabilityTraceSource,
    AiObservabilityTraceStatus,
    useAiGatewayTagsQuery,
    useAiObservabilityTracesQuery,
} from '@/shared/middleware/graphql';
import {ActivityIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface TimeRangeOptionI {
    label: string;
    ms: number;
}

const TIME_RANGE_OPTIONS: TimeRangeOptionI[] = [
    {label: '1h', ms: 60 * 60 * 1000},
    {label: '6h', ms: 6 * 60 * 60 * 1000},
    {label: '24h', ms: 24 * 60 * 60 * 1000},
    {label: '7d', ms: 7 * 24 * 60 * 60 * 1000},
    {label: '30d', ms: 30 * 24 * 60 * 60 * 1000},
];

type StatusOptionType = 'ALL' | AiObservabilityTraceStatus;
type SourceOptionType = 'ALL' | AiObservabilityTraceSource;

const STATUS_OPTIONS: StatusOptionType[] = ['ALL', ...Object.values(AiObservabilityTraceStatus)];
const SOURCE_OPTIONS: SourceOptionType[] = ['ALL', ...Object.values(AiObservabilityTraceSource)];

const DEBOUNCE_DELAY_MS = 300;

interface AiObservabilityTracesProps {
    onSelectTrace: (traceId: string) => void;
}

const STATUS_CLASSES: Record<string, string> = {
    ACTIVE: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    ERROR: 'bg-red-100 text-red-800',
};

const AiObservabilityTraces = ({onSelectTrace}: AiObservabilityTracesProps) => {
    const [selectedRangeMs, setSelectedRangeMs] = useState<number>(TIME_RANGE_OPTIONS[2].ms);
    const [statusFilter, setStatusFilter] = useState<string>('ALL');
    const [sourceFilter, setSourceFilter] = useState<string>('ALL');
    const [userIdFilter, setUserIdFilter] = useState<string>('');
    const [debouncedUserIdFilter, setDebouncedUserIdFilter] = useState<string>('');
    const [modelFilter, setModelFilter] = useState<string>('');
    const [debouncedModelFilter, setDebouncedModelFilter] = useState<string>('');
    const [tagIdFilter, setTagIdFilter] = useState<string>('');

    const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const handleUserIdChange = useCallback((value: string) => {
        setUserIdFilter(value);

        if (debounceTimerRef.current) {
            clearTimeout(debounceTimerRef.current);
        }

        debounceTimerRef.current = setTimeout(() => {
            setDebouncedUserIdFilter(value);
        }, DEBOUNCE_DELAY_MS);
    }, []);

    const modelDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const handleModelChange = useCallback((value: string) => {
        setModelFilter(value);

        if (modelDebounceRef.current) {
            clearTimeout(modelDebounceRef.current);
        }

        modelDebounceRef.current = setTimeout(() => {
            setDebouncedModelFilter(value);
        }, DEBOUNCE_DELAY_MS);
    }, []);

    const {endDate, startDate} = useMemo(() => {
        const now = Date.now();

        return {endDate: now, startDate: now - selectedRangeMs};
    }, [selectedRangeMs]);

    const {
        data: tracesData,
        isError: tracesIsError,
        isLoading: tracesIsLoading,
    } = useAiObservabilityTracesQuery(
        {
            endDate,
            model: debouncedModelFilter || undefined,
            source: sourceFilter === 'ALL' ? undefined : (sourceFilter as AiObservabilityTraceSource),
            startDate,
            status: statusFilter === 'ALL' ? undefined : (statusFilter as AiObservabilityTraceStatus),
            tagId: tagIdFilter || undefined,
            userId: debouncedUserIdFilter || undefined,
            workspaceId: String(currentWorkspaceId ?? ''),
        },
        {enabled: currentWorkspaceId != null}
    );

    const {data: tagsData} = useAiGatewayTagsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const availableTags = useMemo(
        () => (tagsData?.aiGatewayTags ?? []).filter((tag): tag is NonNullable<typeof tag> => tag != null),
        [tagsData?.aiGatewayTags]
    );

    const traces = useMemo(
        () => (tracesData?.aiObservabilityTraces ?? []).filter((trace) => trace != null),
        [tracesData?.aiObservabilityTraces]
    );

    useEffect(() => {
        return () => {
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }
        };
    }, []);

    if (currentWorkspaceId == null || tracesIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (tracesIsError) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <p className="text-destructive">Failed to load traces. Please try again.</p>
            </div>
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between py-4">
                <div className="flex gap-1">
                    {TIME_RANGE_OPTIONS.map((option) => (
                        <button
                            className={twMerge(
                                'rounded px-3 py-1 text-sm font-medium',
                                selectedRangeMs === option.ms
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            )}
                            key={option.label}
                            onClick={() => setSelectedRangeMs(option.ms)}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Model and tags filters will be added when server-side filtering supports these dimensions */}

            <div className="mb-4 flex flex-wrap items-center gap-3">
                <select
                    className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground"
                    onChange={(event) => setStatusFilter(event.target.value)}
                    value={statusFilter}
                >
                    {STATUS_OPTIONS.map((option) => (
                        <option key={option} value={option}>
                            {option === 'ALL' ? 'All Statuses' : option}
                        </option>
                    ))}
                </select>

                <select
                    className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground"
                    onChange={(event) => setSourceFilter(event.target.value)}
                    value={sourceFilter}
                >
                    {SOURCE_OPTIONS.map((option) => (
                        <option key={option} value={option}>
                            {option === 'ALL' ? 'All Sources' : option}
                        </option>
                    ))}
                </select>

                <input
                    className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground placeholder:text-muted-foreground/60"
                    onChange={(event) => handleUserIdChange(event.target.value)}
                    placeholder="Filter by User ID..."
                    type="text"
                    value={userIdFilter}
                />

                <input
                    className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground placeholder:text-muted-foreground/60"
                    onChange={(event) => handleModelChange(event.target.value)}
                    placeholder="Filter by Model..."
                    type="text"
                    value={modelFilter}
                />

                {availableTags.length > 0 && (
                    <select
                        className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground"
                        onChange={(event) => setTagIdFilter(event.target.value)}
                        value={tagIdFilter}
                    >
                        <option value="">All Tags</option>

                        {availableTags.map((tag) => (
                            <option key={tag.id} value={tag.id}>
                                {tag.name}
                            </option>
                        ))}
                    </select>
                )}
            </div>

            {traces.length === 0 ? (
                <EmptyList
                    icon={<ActivityIcon className="size-12 text-muted-foreground" />}
                    message="No traces found for the selected time range."
                    title="No Traces"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="pb-2 font-medium">Time</th>

                                <th className="pb-2 font-medium">Name</th>

                                <th className="pb-2 font-medium">User</th>

                                <th className="pb-2 font-medium">Status</th>

                                <th className="pb-2 font-medium">Latency</th>

                                <th className="pb-2 font-medium">Tokens In</th>

                                <th className="pb-2 font-medium">Tokens Out</th>

                                <th className="pb-2 font-medium">Cost</th>
                            </tr>
                        </thead>

                        <tbody>
                            {traces.map((trace) =>
                                trace ? (
                                    <tr
                                        className="cursor-pointer border-b hover:bg-muted/50"
                                        key={trace.id}
                                        onClick={() => onSelectTrace(trace.id)}
                                    >
                                        <td className="py-3 text-muted-foreground">
                                            {trace.createdDate ? new Date(trace.createdDate).toLocaleString() : '—'}
                                        </td>

                                        <td className="py-3 font-medium">{trace.name || '—'}</td>

                                        <td className="py-3 text-muted-foreground">{trace.userId || '—'}</td>

                                        <td className="py-3">
                                            <span
                                                className={twMerge(
                                                    'rounded-full px-2 py-0.5 text-xs font-medium',
                                                    STATUS_CLASSES[trace.status] || 'bg-gray-100 text-gray-800'
                                                )}
                                            >
                                                {trace.status}
                                            </span>
                                        </td>

                                        <td className="py-3">
                                            {trace.totalLatencyMs != null ? `${trace.totalLatencyMs}ms` : '—'}
                                        </td>

                                        <td className="py-3">{trace.totalInputTokens ?? '—'}</td>

                                        <td className="py-3">{trace.totalOutputTokens ?? '—'}</td>

                                        <td className="py-3">
                                            {trace.totalCost != null ? `$${trace.totalCost.toFixed(6)}` : '—'}
                                        </td>
                                    </tr>
                                ) : null
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default AiObservabilityTraces;
