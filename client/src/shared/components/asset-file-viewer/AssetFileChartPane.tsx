import {
    type ChartConfig as ChartConfigType,
    ChartContainer,
    ChartLegend,
    ChartLegendContent,
    ChartTooltip,
    ChartTooltipContent,
} from '@/components/ui/chart';
import {type ComponentType, useMemo} from 'react';
import {
    Area,
    AreaChart,
    Bar,
    BarChart,
    CartesianGrid,
    Cell,
    Line,
    LineChart,
    Pie,
    PieChart,
    XAxis,
    YAxis,
} from 'recharts';

interface ChartSpecCommonI {
    title?: string;
    type: 'area' | 'bar' | 'line' | 'pie';
}

interface ChartSpecAxisI extends ChartSpecCommonI {
    data: Array<Record<string, unknown>>;
    type: 'area' | 'bar' | 'line';
    xKey: string;
    yKeys: string[];
}

interface ChartSpecPieI extends ChartSpecCommonI {
    data: Array<Record<string, unknown>>;
    labelKey: string;
    type: 'pie';
    valueKey: string;
}

type ChartSpecType = ChartSpecAxisI | ChartSpecPieI;

interface AssetFileChartPanePropsI {
    /**
     * Either the metadataJson value (preferred — chart generator duplicates the spec there) or the raw file
     * content. We accept both so the viewer can fall back to file content if the metadata column is empty for
     * legacy rows.
     */
    spec: string;
}

/**
 * Color palette tuned to match ByteChef's existing chart styling. Picking a fixed palette rather than sampling
 * tailwind tokens keeps this component free of theme-context plumbing — the viewer renders inside a generic DOM
 * tree where the tailwind variables are not directly available without prop-drilling.
 */
const CHART_COLORS = ['#2563eb', '#16a34a', '#dc2626', '#ca8a04', '#7c3aed', '#0891b2', '#db2777', '#ea580c'];

/**
 * Renders a chart from the validated server-side spec via shadcn's chart primitives. Trade-off vs server-side
 * raster: client-side sharpens at any zoom and supports interactive tooltips/legend; loses static thumbnail and
 * download-as-PNG. Both can layer on later via a print-style export without changing this component.
 *
 * <p>
 * Defensive parsing: server-side validation already pinned the spec shape, but `metadataJson` is a string at this
 * layer so a hand-edited row could land here malformed. Catch + render an inline error rather than letting the
 * exception bubble — the user keeps the rest of the file viewer (download, edit JSON) and gets a clear "spec is
 * malformed" hint instead of a blank pane.
 */
const AssetFileChartPane = ({spec}: AssetFileChartPanePropsI) => {
    const parsed = useMemo<{error?: string; value?: ChartSpecType}>(() => {
        try {
            const value = JSON.parse(spec) as ChartSpecType;

            if (!value || typeof value !== 'object') {
                return {error: 'Chart spec must be a JSON object'};
            }

            return {value};
        } catch (parseError) {
            return {
                error: `Chart spec is not valid JSON: ${parseError instanceof Error ? parseError.message : String(parseError)}`,
            };
        }
    }, [spec]);

    const config = useMemo<ChartConfigType>(() => {
        const value = parsed.value;

        if (!value) {
            return {};
        }

        if (value.type === 'pie') {
            const result: ChartConfigType = {};

            value.data.forEach((row, index) => {
                const label = String(row[value.labelKey]);

                result[label] = {color: CHART_COLORS[index % CHART_COLORS.length], label};
            });

            return result;
        }

        const result: ChartConfigType = {};

        value.yKeys.forEach((key, index) => {
            result[key] = {color: CHART_COLORS[index % CHART_COLORS.length], label: key};
        });

        return result;
    }, [parsed]);

    if (parsed.error) {
        return (
            <div className="flex size-full flex-col items-center justify-center gap-2 p-8 text-center text-muted-foreground">
                <p className="text-sm font-medium">Chart spec is malformed.</p>

                <p className="text-xs">{parsed.error}</p>
            </div>
        );
    }

    const value = parsed.value!;

    if (value.type === 'pie') {
        return (
            <div className="flex size-full flex-col">
                {value.title && <h3 className="px-4 pt-4 text-sm font-semibold">{value.title}</h3>}

                <ChartContainer className="[aspect-ratio:auto] size-full max-h-full flex-1" config={config}>
                    <PieChart>
                        <Pie
                            cx="50%"
                            cy="50%"
                            data={value.data}
                            dataKey={value.valueKey}
                            label
                            nameKey={value.labelKey}
                            outerRadius="80%"
                        >
                            {value.data.map((_entry, index) => (
                                <Cell fill={CHART_COLORS[index % CHART_COLORS.length]} key={`cell-${index}`} />
                            ))}
                        </Pie>

                        <ChartTooltip content={<ChartTooltipContent nameKey={value.labelKey} />} />

                        <ChartLegend content={<ChartLegendContent nameKey={value.labelKey} />} />
                    </PieChart>
                </ChartContainer>
            </div>
        );
    }

    const ChartComponent = value.type === 'bar' ? BarChart : value.type === 'line' ? LineChart : AreaChart;
    const SeriesComponent = (value.type === 'bar' ? Bar : value.type === 'line' ? Line : Area) as ComponentType<{
        dataKey: string;
        fill: string;
        name: string;
        stroke: string;
        type?: 'monotone';
    }>;

    return (
        <div className="flex size-full flex-col">
            {value.title && <h3 className="px-4 pt-4 text-sm font-semibold">{value.title}</h3>}

            <ChartContainer className="[aspect-ratio:auto] size-full max-h-full flex-1" config={config}>
                <ChartComponent data={value.data}>
                    <CartesianGrid strokeDasharray="3 3" />

                    <XAxis dataKey={value.xKey} />

                    <YAxis />

                    <ChartTooltip content={<ChartTooltipContent />} />

                    <ChartLegend content={<ChartLegendContent />} />

                    {value.yKeys.map((key, index) => (
                        <SeriesComponent
                            dataKey={key}
                            fill={CHART_COLORS[index % CHART_COLORS.length]}
                            key={key}
                            name={key}
                            stroke={CHART_COLORS[index % CHART_COLORS.length]}
                            type="monotone"
                        />
                    ))}
                </ChartComponent>
            </ChartContainer>
        </div>
    );
};

export default AssetFileChartPane;
