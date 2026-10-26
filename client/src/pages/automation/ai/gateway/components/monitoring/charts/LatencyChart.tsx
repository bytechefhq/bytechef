import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

interface LatencyDataPointI {
    p50: number;
    p95: number;
    p99: number;
    time: string;
}

interface LatencyChartProps {
    data: LatencyDataPointI[];
}

const LatencyChart = ({data}: LatencyChartProps) => {
    if (data.length === 0) {
        return <p className="flex h-[250px] items-center justify-center text-sm text-muted-foreground">No data</p>;
    }

    return (
        <ResponsiveContainer height={250} width="100%">
            <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />

                <XAxis dataKey="time" />

                <YAxis />

                <Tooltip />

                <Line dataKey="p50" stroke="hsl(var(--primary))" strokeWidth={2} type="monotone" />

                <Line
                    dataKey="p95"
                    stroke="hsl(var(--chart-2))"
                    strokeDasharray="5 5"
                    strokeWidth={2}
                    type="monotone"
                />

                <Line
                    dataKey="p99"
                    stroke="hsl(var(--chart-3))"
                    strokeDasharray="3 3"
                    strokeWidth={2}
                    type="monotone"
                />
            </LineChart>
        </ResponsiveContainer>
    );
};

export default LatencyChart;
