import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiGatewayRequestLog as AiGatewayRequestLogType,
    useAiGatewayBudgetQuery,
    useAiGatewayWorkspaceSettingsQuery,
    useWorkspaceAiGatewayRequestLogsQuery,
} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import AiGatewayRequestLog from './AiGatewayRequestLog';
import CostBreakdownChart from './charts/CostBreakdownChart';
import ErrorRateChart from './charts/ErrorRateChart';
import LatencyChart from './charts/LatencyChart';
import RequestVolumeChart from './charts/RequestVolumeChart';

type SourceFilterType = 'ALL' | 'API' | 'PLAYGROUND';

type TimeRangeType = '1h' | '24h' | '30d' | '6h' | '7d';

// Source filtering will be effective after Phase 4 adds source tracking to request logs
const SOURCE_FILTER_OPTIONS: SourceFilterType[] = ['ALL', 'API', 'PLAYGROUND'];

const SOURCE_FILTER_LABELS: Record<SourceFilterType, string> = {
    ALL: 'All Sources',
    API: 'API',
    PLAYGROUND: 'Playground',
};

const TIME_RANGE_MS: Record<TimeRangeType, number> = {
    '1h': 60 * 60 * 1000,
    '6h': 6 * 60 * 60 * 1000,
    '7d': 7 * 24 * 60 * 60 * 1000,
    '24h': 24 * 60 * 60 * 1000,
    '30d': 30 * 24 * 60 * 60 * 1000,
};

const TIME_RANGE_OPTIONS: TimeRangeType[] = ['1h', '6h', '24h', '7d', '30d'];

function computeTimeRange(timeRange: TimeRangeType): {endDate: number; startDate: number} {
    const endDate = Date.now();
    const startDate = endDate - TIME_RANGE_MS[timeRange];

    return {endDate, startDate};
}

function getHourLabel(timestamp: number): string {
    const date = new Date(timestamp);

    return `${date.getHours().toString().padStart(2, '0')}:00`;
}

function computeRequestVolumeData(logs: AiGatewayRequestLogType[], timeRange: TimeRangeType) {
    const bucketCount = timeRange === '1h' ? 6 : timeRange === '6h' ? 6 : timeRange === '24h' ? 8 : 7;
    const rangeDuration = TIME_RANGE_MS[timeRange];
    const bucketSize = rangeDuration / bucketCount;
    const now = Date.now();
    const rangeStart = now - rangeDuration;

    const buckets = Array.from({length: bucketCount}, (_, index) => ({
        requests: 0,
        time:
            timeRange === '7d' || timeRange === '30d'
                ? new Date(rangeStart + index * bucketSize).toLocaleDateString(undefined, {
                      day: 'numeric',
                      month: 'short',
                  })
                : getHourLabel(rangeStart + index * bucketSize),
    }));

    for (const log of logs) {
        if (log.createdDate == null) {
            continue;
        }

        const bucketIndex = Math.floor((log.createdDate - rangeStart) / bucketSize);

        if (bucketIndex >= 0 && bucketIndex < bucketCount) {
            buckets[bucketIndex].requests += 1;
        }
    }

    return buckets;
}

function computeLatencyData(logs: AiGatewayRequestLogType[], timeRange: TimeRangeType) {
    const bucketCount = timeRange === '1h' ? 6 : timeRange === '6h' ? 6 : timeRange === '24h' ? 8 : 7;
    const rangeDuration = TIME_RANGE_MS[timeRange];
    const bucketSize = rangeDuration / bucketCount;
    const now = Date.now();
    const rangeStart = now - rangeDuration;

    const bucketedLatencies: number[][] = Array.from({length: bucketCount}, () => []);

    for (const log of logs) {
        if (log.createdDate == null || log.latencyMs == null) {
            continue;
        }

        const bucketIndex = Math.floor((log.createdDate - rangeStart) / bucketSize);

        if (bucketIndex >= 0 && bucketIndex < bucketCount) {
            bucketedLatencies[bucketIndex].push(log.latencyMs);
        }
    }

    return bucketedLatencies.map((latencies, index) => {
        const sorted = [...latencies].sort((first, second) => first - second);
        const percentile = (percentileValue: number) =>
            sorted.length > 0 ? sorted[Math.floor(sorted.length * percentileValue)] : 0;

        return {
            p50: percentile(0.5),
            p95: percentile(0.95),
            p99: percentile(0.99),
            time:
                timeRange === '7d' || timeRange === '30d'
                    ? new Date(rangeStart + index * bucketSize).toLocaleDateString(undefined, {
                          day: 'numeric',
                          month: 'short',
                      })
                    : getHourLabel(rangeStart + index * bucketSize),
        };
    });
}

function computeErrorRateData(logs: AiGatewayRequestLogType[]) {
    const providerStats = new Map<string, {errors: number; total: number}>();

    for (const log of logs) {
        const provider = log.routedProvider || 'Unknown';
        const stats = providerStats.get(provider) || {errors: 0, total: 0};

        stats.total += 1;

        if (log.status != null && (log.status < 200 || log.status >= 300)) {
            stats.errors += 1;
        }

        providerStats.set(provider, stats);
    }

    return Array.from(providerStats.entries()).map(([provider, stats]) => ({
        errorRate: stats.total > 0 ? Number(((stats.errors / stats.total) * 100).toFixed(1)) : 0,
        provider,
    }));
}

function computeCostBreakdownData(logs: AiGatewayRequestLogType[]) {
    const providerCosts = new Map<string, number>();

    for (const log of logs) {
        const provider = log.routedProvider || 'Unknown';
        const currentCost = providerCosts.get(provider) || 0;

        providerCosts.set(provider, currentCost + parseFloat(log.cost ?? '0'));
    }

    return Array.from(providerCosts.entries())
        .filter(([, value]) => value > 0)
        .map(([name, value]) => ({name, value: Number(value.toFixed(4))}));
}

const AiGatewayDashboard = () => {
    const [sourceFilter, setSourceFilter] = useState<SourceFilterType>('ALL');
    const [timeRange, setTimeRange] = useState<TimeRangeType>('24h');
    const [propertyKey, setPropertyKey] = useState('');
    const [propertyValue, setPropertyValue] = useState('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {endDate, startDate} = useMemo(() => computeTimeRange(timeRange), [timeRange]);

    const {data: budgetData} = useAiGatewayBudgetQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const budget = budgetData?.aiGatewayBudget;

    const {data: settingsData} = useAiGatewayWorkspaceSettingsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const warningThresholdPct = settingsData?.aiGatewayWorkspaceSettings?.softBudgetWarningPct ?? 80;

    const {data: logsData, isLoading: logsIsLoading} = useWorkspaceAiGatewayRequestLogsQuery({
        endDate,
        propertyKey: propertyKey && propertyValue ? propertyKey : undefined,
        propertyValue: propertyKey && propertyValue ? propertyValue : undefined,
        startDate,
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const logs = useMemo(() => {
        return (logsData?.workspaceAiGatewayRequestLogs ?? []).filter(Boolean) as AiGatewayRequestLogType[];
    }, [logsData]);

    const summaryStats = useMemo(() => {
        const totalRequests = logs.length;

        const avgLatency =
            totalRequests > 0
                ? Math.round(logs.reduce((sum, log) => sum + (log.latencyMs ?? 0), 0) / totalRequests)
                : 0;

        const errorCount = logs.filter((log) => log.status != null && (log.status < 200 || log.status >= 300)).length;

        const errorRate = totalRequests > 0 ? ((errorCount / totalRequests) * 100).toFixed(1) : '0.0';

        const totalSpend = logs.reduce((sum, log) => sum + parseFloat(log.cost ?? '0'), 0);

        return {
            avgLatency: `${avgLatency}ms`,
            errorRate: `${errorRate}%`,
            totalRequests: totalRequests.toLocaleString(),
            totalSpend: `$${totalSpend.toFixed(2)}`,
            totalSpendNumeric: totalSpend,
        };
    }, [logs]);

    // Visible spend is the dashboard's selected time range; for budget percentage we use this as a proxy. Strictly
    // correct would be filtering logs to budget.period boundaries (DAILY/WEEKLY/MONTHLY) — acceptable approximation
    // for the dashboard widget; the actual budget enforcement uses the real period in the gateway.
    const spentForBudgetPeriod = summaryStats.totalSpendNumeric;

    const budgetUsagePct = budget?.amount ? (spentForBudgetPeriod / parseFloat(budget.amount)) * 100 : 0;

    const costByApiKey = useMemo(() => {
        const byKey = new Map<string, number>();

        for (const log of logs) {
            const key = log.apiKeyId != null ? `Key #${log.apiKeyId}` : 'No key';
            const cost = parseFloat(log.cost ?? '0');

            byKey.set(key, (byKey.get(key) ?? 0) + cost);
        }

        return Array.from(byKey.entries())
            .map(([apiKey, cost]) => ({apiKey, cost}))
            .sort((a, b) => b.cost - a.cost)
            .slice(0, 5);
    }, [logs]);

    const requestVolumeData = useMemo(() => computeRequestVolumeData(logs, timeRange), [logs, timeRange]);
    const costBreakdownData = useMemo(() => computeCostBreakdownData(logs), [logs]);
    const errorRateData = useMemo(() => computeErrorRateData(logs), [logs]);
    const latencyData = useMemo(() => computeLatencyData(logs, timeRange), [logs, timeRange]);

    if (logsIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full space-y-6 px-2 py-4 2xl:mx-auto 2xl:w-4/5">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium">Monitoring Dashboard</h3>

                <div className="flex items-center gap-3">
                    <select
                        className="rounded-lg border bg-background px-3 py-1.5 text-sm text-muted-foreground"
                        onChange={(event) => setSourceFilter(event.target.value as SourceFilterType)}
                        value={sourceFilter}
                    >
                        {SOURCE_FILTER_OPTIONS.map((option) => (
                            <option key={option} value={option}>
                                {SOURCE_FILTER_LABELS[option]}
                            </option>
                        ))}
                    </select>

                    <div className="flex gap-1 rounded-lg border p-1">
                        {TIME_RANGE_OPTIONS.map((range) => (
                            <button
                                className={twMerge(
                                    'rounded-md px-3 py-1 text-sm',
                                    timeRange === range
                                        ? 'bg-primary text-primary-foreground'
                                        : 'text-muted-foreground hover:bg-muted'
                                )}
                                key={range}
                                onClick={() => setTimeRange(range)}
                            >
                                {range}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <input
                        className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground placeholder:text-muted-foreground/60"
                        onChange={(event) => setPropertyKey(event.target.value)}
                        placeholder="Property key"
                        type="text"
                        value={propertyKey}
                    />

                    <input
                        className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground placeholder:text-muted-foreground/60"
                        onChange={(event) => setPropertyValue(event.target.value)}
                        placeholder="Value"
                        type="text"
                        value={propertyValue}
                    />

                    {(propertyKey || propertyValue) && (
                        <button
                            className="text-xs text-muted-foreground hover:text-foreground"
                            onClick={() => {
                                setPropertyKey('');
                                setPropertyValue('');
                            }}
                            type="button"
                        >
                            Clear
                        </button>
                    )}
                </div>
            </div>

            {/* Summary Cards */}

            <div className="grid grid-cols-4 gap-4">
                <div className="rounded-lg border p-4">
                    <p className="text-sm text-muted-foreground">Total Requests</p>

                    <p className="text-2xl font-bold">{summaryStats.totalRequests}</p>
                </div>

                <div className="rounded-lg border p-4">
                    <p className="text-sm text-muted-foreground">Avg Latency</p>

                    <p className="text-2xl font-bold">{summaryStats.avgLatency}</p>
                </div>

                <div className="rounded-lg border p-4">
                    <p className="text-sm text-muted-foreground">Error Rate</p>

                    <p className="text-2xl font-bold">{summaryStats.errorRate}</p>
                </div>

                <div className="rounded-lg border p-4">
                    <p className="text-sm text-muted-foreground">Total Spend</p>

                    <p className="text-2xl font-bold">{summaryStats.totalSpend}</p>
                </div>
            </div>

            {costByApiKey.length > 0 && (
                <div className="rounded-lg border p-4">
                    <h4 className="mb-3 font-medium">Top spend by API key</h4>

                    <table className="w-full text-sm">
                        <tbody>
                            {costByApiKey.map((row) => (
                                <tr className="border-b last:border-0" key={row.apiKey}>
                                    <td className="py-1.5 text-muted-foreground">{row.apiKey}</td>

                                    <td className="py-1.5 text-right font-mono">${row.cost.toFixed(4)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {budget && budget.amount && (
                <div className="rounded-lg border p-4">
                    <div className="mb-2 flex items-center justify-between text-sm">
                        <span className="font-medium">Budget vs. spend ({budget.period})</span>

                        <span className="text-muted-foreground">
                            ${spentForBudgetPeriod.toFixed(2)} / ${budget.amount}
                        </span>
                    </div>

                    <div className="relative h-2 w-full overflow-hidden rounded-full bg-muted">
                        <div
                            className={twMerge(
                                'absolute h-2 rounded-full transition-all',
                                budgetUsagePct >= 100
                                    ? 'bg-red-500'
                                    : budgetUsagePct >= warningThresholdPct
                                      ? 'bg-yellow-500'
                                      : 'bg-green-500'
                            )}
                            style={{width: `${Math.min(budgetUsagePct, 100)}%`}}
                        />
                    </div>

                    {budgetUsagePct >= 100 && (
                        <p className="mt-2 text-xs text-red-600">Budget exhausted — new requests return HTTP 402.</p>
                    )}
                </div>
            )}

            {/* Charts Grid */}

            <div className="grid grid-cols-2 gap-4">
                <div className="rounded-lg border p-4">
                    <h4 className="mb-4 font-medium">Request Volume</h4>

                    <RequestVolumeChart data={requestVolumeData} />
                </div>

                <div className="rounded-lg border p-4">
                    <h4 className="mb-4 font-medium">Latency (ms)</h4>

                    <LatencyChart data={latencyData} />
                </div>

                <div className="rounded-lg border p-4">
                    <h4 className="mb-4 font-medium">Error Rate by Provider</h4>

                    <ErrorRateChart data={errorRateData} />
                </div>

                <div className="rounded-lg border p-4">
                    <h4 className="mb-4 font-medium">Cost Breakdown</h4>

                    <CostBreakdownChart data={costBreakdownData} />
                </div>
            </div>

            {/* Request Log */}

            <div className="rounded-lg border p-4">
                <h4 className="mb-4 font-medium">Recent Requests</h4>

                <AiGatewayRequestLog requestLogs={logsData?.workspaceAiGatewayRequestLogs ?? []} />
            </div>
        </div>
    );
};

export default AiGatewayDashboard;
