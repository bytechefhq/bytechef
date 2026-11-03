import {ReactNode} from 'react';

interface PlanUsagePropsI {
    bar: ReactNode;
    description: string;
    leftLabel: string;
    rightLabel: string;
    value: number;
}

const PlanUsage = ({bar, description, leftLabel, rightLabel, value}: PlanUsagePropsI) => (
    <>
        <div className="mb-2">
            <span className="text-xl font-bold">{value.toLocaleString()}</span>

            <span className="text-xl text-muted-foreground">{` ${description}`}</span>
        </div>

        <div className="mb-2 flex justify-between text-sm font-medium">
            <span>{leftLabel}</span>

            <span>{rightLabel}</span>
        </div>

        <div className="flex h-2 w-full overflow-hidden rounded-full bg-muted">{bar}</div>
    </>
);

export default PlanUsage;
