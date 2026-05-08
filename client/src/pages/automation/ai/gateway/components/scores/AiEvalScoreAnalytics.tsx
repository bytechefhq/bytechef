import Badge from '@/components/Badge/Badge';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiEvalScoreAnalyticsQuery} from '@/shared/middleware/graphql';
import {StarIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

import AiEvalScoreTrendChart from './AiEvalScoreTrendChart';

type TimeRangeType = '1h' | '6h' | '24h' | '7d' | '30d';

const TIME_RANGES: {label: string; value: TimeRangeType}[] = [
    {label: '1h', value: '1h'},
    {label: '6h', value: '6h'},
    {label: '24h', value: '24h'},
    {label: '7d', value: '7d'},
    {label: '30d', value: '30d'},
];

const PIE_COLORS = ['#22c55e', '#ef4444', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899'];

const getRangeMillis = (range: TimeRangeType): number => {
    switch (range) {
        case '1h':
            return 60 * 60 * 1000;
        case '6h':
            return 6 * 60 * 60 * 1000;
        case '24h':
            return 24 * 60 * 60 * 1000;
        case '7d':
            return 7 * 24 * 60 * 60 * 1000;
        case '30d':
            return 30 * 24 * 60 * 60 * 1000;
    }
};

const AiEvalScoreAnalytics = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [timeRange, setTimeRange] = useState<TimeRangeType>('24h');

    const {endDate, startDate} = useMemo(() => {
        const now = Date.now();

        return {endDate: now, startDate: now - getRangeMillis(timeRange)};
    }, [timeRange]);

    const {data: analyticsData, isLoading: analyticsIsLoading} = useAiEvalScoreAnalyticsQuery({
        endDate,
        startDate,
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const analytics = useMemo(
        () =>
            (analyticsData?.aiEvalScoreAnalytics ?? []).filter(
                (item): item is NonNullable<typeof item> => item != null
            ),
        [analyticsData?.aiEvalScoreAnalytics]
    );

    return (
        <div className="space-y-4">
            <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">Time range:</span>

                <div className="flex gap-1">
                    {TIME_RANGES.map((range) => (
                        <button
                            className={`rounded-md px-3 py-1 text-xs font-medium ${
                                timeRange === range.value
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/70'
                            }`}
                            key={range.value}
                            onClick={() => setTimeRange(range.value)}
                        >
                            {range.label}
                        </button>
                    ))}
                </div>
            </div>

            {analyticsIsLoading ? (
                <PageLoader loading={true} />
            ) : analytics.length === 0 ? (
                <EmptyList
                    icon={<StarIcon className="size-12 text-muted-foreground" />}
                    message="Scores will appear here once traces are evaluated in this time range."
                    title="No Scores Yet"
                />
            ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    {analytics.map((item) => {
                        const distribution = (item.distribution ?? []).filter(
                            (entry): entry is NonNullable<typeof entry> => entry != null
                        );

                        const chartData = distribution.map((entry) => ({
                            count: entry.count ?? 0,
                            value: entry.value ?? 'unknown',
                        }));

                        return (
                            <div className="rounded-lg border p-4" key={item.name ?? ''}>
                                <div className="mb-3 flex items-center justify-between">
                                    <div className="text-sm font-semibold">{item.name}</div>

                                    <Badge label={item.dataType ?? ''} styleType="secondary-outline" />
                                </div>

                                <div className="mb-3 text-xs text-muted-foreground">{item.count} scores recorded</div>

                                {item.dataType === 'NUMERIC' && (
                                    <div className="grid grid-cols-3 gap-2 text-center">
                                        <div>
                                            <div className="text-xs text-muted-foreground">Avg</div>

                                            <div className="text-lg font-bold">
                                                {item.average != null ? item.average.toFixed(2) : '-'}
                                            </div>
                                        </div>

                                        <div>
                                            <div className="text-xs text-muted-foreground">Min</div>

                                            <div className="text-lg font-bold">
                                                {item.min != null ? item.min.toFixed(2) : '-'}
                                            </div>
                                        </div>

                                        <div>
                                            <div className="text-xs text-muted-foreground">Max</div>

                                            <div className="text-lg font-bold">
                                                {item.max != null ? item.max.toFixed(2) : '-'}
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {item.dataType === 'BOOLEAN' && chartData.length > 0 && (
                                    <ResponsiveContainer height={200} width="100%">
                                        <PieChart>
                                            <Pie
                                                data={chartData}
                                                dataKey="count"
                                                label={(entry) => entry.value}
                                                nameKey="value"
                                                outerRadius={70}
                                            >
                                                {chartData.map((entry, index) => (
                                                    <Cell
                                                        fill={PIE_COLORS[index % PIE_COLORS.length]}
                                                        key={entry.value}
                                                    />
                                                ))}
                                            </Pie>

                                            <Tooltip />
                                        </PieChart>
                                    </ResponsiveContainer>
                                )}

                                {item.dataType === 'CATEGORICAL' && chartData.length > 0 && (
                                    <ResponsiveContainer height={200} width="100%">
                                        <BarChart data={chartData}>
                                            <XAxis dataKey="value" />

                                            <YAxis allowDecimals={false} />

                                            <Tooltip />

                                            <Bar dataKey="count" fill="#3b82f6" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                )}

                                {item.dataType === 'NUMERIC' && item.name && currentWorkspaceId != null && (
                                    <AiEvalScoreTrendChart
                                        endDate={endDate}
                                        name={item.name}
                                        startDate={startDate}
                                        workspaceId={String(currentWorkspaceId)}
                                    />
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default AiEvalScoreAnalytics;
