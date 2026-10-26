import {Cell, Pie, PieChart, ResponsiveContainer, Tooltip} from 'recharts';

const COLORS = ['hsl(var(--primary))', 'hsl(var(--chart-2))', 'hsl(var(--chart-3))', 'hsl(var(--chart-4))'];

interface CostDataPointI {
    name: string;
    value: number;
}

interface CostBreakdownChartProps {
    data: CostDataPointI[];
}

const CostBreakdownChart = ({data}: CostBreakdownChartProps) => {
    if (data.length === 0) {
        return <p className="flex h-[250px] items-center justify-center text-sm text-muted-foreground">No data</p>;
    }

    return (
        <ResponsiveContainer height={250} width="100%">
            <PieChart>
                <Pie cx="50%" cy="50%" data={data} dataKey="value" innerRadius={60} nameKey="name" outerRadius={100}>
                    {data.map((entry, index) => (
                        <Cell fill={COLORS[index % COLORS.length]} key={entry.name} />
                    ))}
                </Pie>

                <Tooltip />
            </PieChart>
        </ResponsiveContainer>
    );
};

export default CostBreakdownChart;
