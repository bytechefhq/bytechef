import {Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

interface ErrorRateDataPointI {
    errorRate: number;
    provider: string;
}

interface ErrorRateChartProps {
    data: ErrorRateDataPointI[];
}

const ErrorRateChart = ({data}: ErrorRateChartProps) => {
    if (data.length === 0) {
        return <p className="flex h-[250px] items-center justify-center text-sm text-muted-foreground">No data</p>;
    }

    return (
        <ResponsiveContainer height={250} width="100%">
            <BarChart data={data}>
                <XAxis dataKey="provider" />

                <YAxis />

                <Tooltip />

                <Bar dataKey="errorRate" fill="hsl(var(--destructive))" radius={[4, 4, 0, 0]} />
            </BarChart>
        </ResponsiveContainer>
    );
};

export default ErrorRateChart;
