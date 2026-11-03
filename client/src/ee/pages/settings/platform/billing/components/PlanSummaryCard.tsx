import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader} from '@/components/ui/card';
import {ReactNode} from 'react';

export interface PlanSummaryCardPropsI {
    cancelAtPeriodEnd: boolean;
    onCancelPlan?: () => void;
    onChangePlan: () => void;
    onReactivatePlan?: () => void;
    planName: string;
    productUnitLimit: number;
    renewalDate?: string;
    scheduledPlanName?: string;
    trialDaysRemaining?: number;
    trialExpired?: boolean;
}

const PlanSummaryCard = ({
    cancelAtPeriodEnd,
    children,
    onCancelPlan,
    onChangePlan,
    onReactivatePlan,
    planName,
    productUnitLimit,
    renewalDate,
    scheduledPlanName,
    trialDaysRemaining,
    trialExpired,
}: PlanSummaryCardPropsI & {children: ReactNode}) => (
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
                <div className="rounded-lg bg-muted/50 px-4 py-6">{children}</div>
            </CardContent>
        )}
    </Card>
);

export default PlanSummaryCard;
