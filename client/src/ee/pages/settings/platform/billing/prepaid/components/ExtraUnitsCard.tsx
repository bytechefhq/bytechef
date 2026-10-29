import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader} from '@/components/ui/card';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {ChevronDownIcon} from 'lucide-react';
import {useState} from 'react';

interface ExtraUnitsCardPropsI {
    disabled: boolean;
    extraUnitTotal: number;
    extraUnitUsed: number;
    onBuy: () => void;
}

const ExtraUnitsCard = ({disabled, extraUnitTotal, extraUnitUsed, onBuy}: ExtraUnitsCardPropsI) => {
    const [open, setOpen] = useState(false);

    const hasExtraUnits = extraUnitTotal > 0 || extraUnitUsed > 0;
    const extraUnitsAvailable = extraUnitTotal - extraUnitUsed;
    const extraUnitFill = extraUnitTotal > 0 ? Math.min(extraUnitUsed / extraUnitTotal, 1) * 100 : 0;

    return (
        <Card className="w-full max-w-3xl">
            <Collapsible onOpenChange={setOpen} open={hasExtraUnits || open}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0">
                    {hasExtraUnits ? (
                        <span className="font-bold">Extra units</span>
                    ) : (
                        <CollapsibleTrigger className="group flex items-center gap-1 font-bold">
                            Extra units
                            <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                        </CollapsibleTrigger>
                    )}

                    <Button disabled={disabled} label="Buy extra units" onClick={onBuy} variant="default" />
                </CardHeader>

                <CollapsibleContent>
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
                </CollapsibleContent>
            </Collapsible>
        </Card>
    );
};

export default ExtraUnitsCard;
