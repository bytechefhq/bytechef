import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader} from '@/components/ui/card';
import {useMemo} from 'react';

interface PrepaidPlanCardPropsI {
    cancelAtPeriodEnd: boolean;
    jobsExecuted: number;
    onCancelPlan?: () => void;
    onChangePlan: () => void;
    onReactivatePlan?: () => void;
    planName: string;
    planUnitTotal: number;
    planUnitUsed: number;
    productUnitLimit: number;
    renewalDate?: string;
    scheduledPlanName?: string;
    trialDaysRemaining?: number;
    trialExpired?: boolean;
}

const PrepaidPlanCard = ({
    cancelAtPeriodEnd,
    jobsExecuted,
    onCancelPlan,
    onChangePlan,
    onReactivatePlan,
    planName,
    planUnitTotal,
    planUnitUsed,
    productUnitLimit,
    renewalDate,
    scheduledPlanName,
    trialDaysRemaining,
    trialExpired,
}: PrepaidPlanCardPropsI) => {
    const isTrial = trialDaysRemaining !== undefined;
    const jobExecutionsAvailable = productUnitLimit - jobsExecuted;
    const planUnitFill = planUnitTotal > 0 ? Math.min(planUnitUsed / planUnitTotal, 1) * 100 : 0;

    const {blueFill, blueZoneWidth, orangeFill, orangeZoneWidth} = useMemo(() => {
        const totalRange = Math.max(jobsExecuted, productUnitLimit);

        if (totalRange <= 0) {
            return {blueFill: 0, blueZoneWidth: 100, orangeFill: 0, orangeZoneWidth: 0};
        }

        return {
            blueFill: productUnitLimit > 0 ? Math.min(jobsExecuted / productUnitLimit, 1) * 100 : 0,
            blueZoneWidth: (productUnitLimit / totalRange) * 100,
            orangeFill: jobsExecuted > productUnitLimit ? 100 : 0,
            orangeZoneWidth: (Math.max(0, jobsExecuted - productUnitLimit) / totalRange) * 100,
        };
    }, [productUnitLimit, jobsExecuted]);

    return (
        <Card className="w-full max-w-3xl">
            <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div className="flex flex-col gap-1">
                    <span className="font-bold">{planName}</span>

                    {(cancelAtPeriodEnd || renewalDate || trialDaysRemaining !== undefined) && (
                        <span className="text-sm text-muted-foreground">
                            {cancelAtPeriodEnd ? (
                                <>
                                    {'Your subscription will be cancelled on '}

                                    <span className="font-semibold text-foreground">{renewalDate}</span>
                                </>
                            ) : renewalDate ? (
                                scheduledPlanName ? (
                                    <>
                                        {'Your plan will downgrade to '}

                                        <span className="font-semibold text-foreground">
                                            {scheduledPlanName.charAt(0).toUpperCase() +
                                                scheduledPlanName.slice(1).toLowerCase()}
                                        </span>

                                        {' on '}

                                        <span className="font-semibold text-foreground">{renewalDate}</span>
                                    </>
                                ) : (
                                    <>
                                        {'Your '}

                                        <span className="font-semibold text-foreground">
                                            {planName.charAt(0).toUpperCase() + planName.slice(1).toLowerCase()}
                                        </span>

                                        {' plan renews on '}

                                        <span className="font-semibold text-foreground">{renewalDate}</span>
                                    </>
                                )
                            ) : trialExpired ? (
                                'Your trial has expired.'
                            ) : (
                                <>
                                    {'Expires in '}

                                    <span className="font-semibold text-foreground">{trialDaysRemaining} days</span>

                                    {' or after '}

                                    <span className="font-semibold text-foreground">
                                        {productUnitLimit.toLocaleString()} job executions
                                    </span>

                                    {' are used'}
                                </>
                            )}
                        </span>
                    )}
                </div>

                <div className="flex gap-2">
                    {cancelAtPeriodEnd ? (
                        onReactivatePlan && <Button label="Reactivate" onClick={onReactivatePlan} variant="default" />
                    ) : (
                        <>
                            <Button label="Change plan" onClick={onChangePlan} variant="default" />

                            {onCancelPlan && (
                                <Button label="Cancel plan" onClick={onCancelPlan} variant="destructiveGhost" />
                            )}
                        </>
                    )}
                </div>
            </CardHeader>

            {!trialExpired && (
                <CardContent>
                    <div className="rounded-lg bg-muted/50 px-4 py-6">
                        <div className="mb-2">
                            <span className="text-xl font-bold">
                                {isTrial ? jobExecutionsAvailable.toLocaleString() : planUnitUsed.toLocaleString()}
                            </span>

                            <span className="text-xl text-muted-foreground">
                                {isTrial ? ' Job executions available' : ' Job executions used this period'}
                            </span>
                        </div>

                        <div className="mb-2 flex justify-between text-sm font-medium">
                            {isTrial ? (
                                <>
                                    <span>Spent {jobsExecuted.toLocaleString()}</span>

                                    <span>Limit {productUnitLimit.toLocaleString()}</span>
                                </>
                            ) : (
                                <>
                                    <span>Used {planUnitUsed.toLocaleString()}</span>

                                    <span>Included {planUnitTotal.toLocaleString()}</span>
                                </>
                            )}
                        </div>

                        <div className="flex h-2 w-full overflow-hidden rounded-full bg-muted">
                            {isTrial ? (
                                <>
                                    <div className="h-full bg-muted" style={{width: `${blueZoneWidth}%`}}>
                                        <div className="h-full bg-blue-500" style={{width: `${blueFill}%`}} />
                                    </div>

                                    {orangeZoneWidth > 0 && (
                                        <div className="h-full bg-muted" style={{width: `${orangeZoneWidth}%`}}>
                                            <div className="h-full bg-orange-500" style={{width: `${orangeFill}%`}} />
                                        </div>
                                    )}
                                </>
                            ) : (
                                <div className="h-full bg-blue-500" style={{width: `${planUnitFill}%`}} />
                            )}
                        </div>
                    </div>
                </CardContent>
            )}
        </Card>
    );
};

export default PrepaidPlanCard;
