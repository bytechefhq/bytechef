import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

interface RequestVolumeDataPointI {
    requests: number;
    time: string;
}

interface RequestVolumeChartProps {
    data: RequestVolumeDataPointI[];
}

const RequestVolumeChart = ({data}: RequestVolumeChartProps) => {
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

                <Line dataKey="requests" stroke="hsl(var(--primary))" strokeWidth={2} type="monotone" />
            </LineChart>
        </ResponsiveContainer>
    );
};

export default RequestVolumeChart;
