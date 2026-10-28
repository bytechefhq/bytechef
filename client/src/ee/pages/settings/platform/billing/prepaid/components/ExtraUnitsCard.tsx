import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader} from '@/components/ui/card';

interface ExtraUnitsCardPropsI {
    disabled: boolean;
    extraUnitTotal: number;
    extraUnitUsed: number;
    onBuy: () => void;
}

const ExtraUnitsCard = ({disabled, extraUnitTotal, extraUnitUsed, onBuy}: ExtraUnitsCardPropsI) => {
    const extraUnitsAvailable = extraUnitTotal - extraUnitUsed;
    const extraUnitFill = extraUnitTotal > 0 ? Math.min(extraUnitUsed / extraUnitTotal, 1) * 100 : 0;

    return (
        <Card className="w-full max-w-3xl">
            <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <span className="font-bold">Extra units</span>

                <Button disabled={disabled} label="Buy extra units" onClick={onBuy} variant="default" />
            </CardHeader>

            <CardContent>
                <div className="rounded-lg bg-muted/50 px-4 py-6">
                    <div className="mb-2">
                        <span className="text-xl font-bold">{extraUnitsAvailable.toLocaleString()}</span>

                        <span className="text-xl text-muted-foreground"> Extra job executions available</span>
                    </div>

                    <div className="mb-2 flex justify-between text-sm font-medium">
                        <span>Used {extraUnitUsed.toLocaleString()}</span>

                        <span>Purchased {extraUnitTotal.toLocaleString()}</span>
                    </div>

                    <div className="flex h-2 w-full overflow-hidden rounded-full bg-muted">
                        <div className="h-full bg-blue-500" style={{width: `${extraUnitFill}%`}} />
                    </div>
                </div>
            </CardContent>
        </Card>
    );
};

export default ExtraUnitsCard;
