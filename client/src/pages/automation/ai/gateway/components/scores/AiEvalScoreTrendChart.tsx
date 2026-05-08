import {useAiEvalScoreTrendQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

interface AiEvalScoreTrendChartProps {
    endDate: number;
    name: string;
    startDate: number;
    workspaceId: string;
}

/**
 * Day-bucketed average score over time. Null averages (days with only non-NUMERIC rows) are skipped; recharts joins
 * the remaining points with a line — which is visually truthful about the real sampling cadence rather than drawing
 * through fabricated zeroes.
 */
const AiEvalScoreTrendChart = ({endDate, name, startDate, workspaceId}: AiEvalScoreTrendChartProps) => {
    const {data, isLoading} = useAiEvalScoreTrendQuery({endDate, name, startDate, workspaceId});

    const chartData = useMemo(() => {
        const points = (data?.aiEvalScoreTrend ?? []).filter(
            (point): point is NonNullable<typeof point> => point != null
        );

        return points.map((point) => ({
            average: point.average,
            day: new Date(Number(point.day)).toLocaleDateString(),
        }));
    }, [data?.aiEvalScoreTrend]);

    if (isLoading || chartData.length === 0) {
        return null;
    }

    return (
        <div className="mt-4">
            <div className="mb-1 text-xs text-muted-foreground">Trend (daily average)</div>

            <ResponsiveContainer height={160} width="100%">
                <LineChart data={chartData} margin={{bottom: 0, left: 0, right: 0, top: 5}}>
                    <CartesianGrid stroke="#eee" strokeDasharray="3 3" />

                    <XAxis dataKey="day" tick={{fontSize: 10}} />

                    <YAxis allowDecimals={true} tick={{fontSize: 10}} />

                    <Tooltip />

                    <Line connectNulls={true} dataKey="average" dot={false} stroke="#3b82f6" type="monotone" />
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
};

export default AiEvalScoreTrendChart;
