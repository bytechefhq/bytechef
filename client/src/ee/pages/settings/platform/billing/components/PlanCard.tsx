import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader} from '@/components/ui/card';
import {useMemo} from 'react';

interface PlanCardPropsI {
    cancelAtPeriodEnd: boolean;
    onCancelPlan?: () => void;
    onChangePlan: () => void;
    onReactivatePlan?: () => void;
    planName: string;
    renewalDate?: string;
    scheduledPlanName?: string;
    taskLimit: number;
    tasksUsed: number;
    trialDaysRemaining?: number;
}

const PlanCard = ({
    cancelAtPeriodEnd,
    onCancelPlan,
    onChangePlan,
    onReactivatePlan,
    planName,
    renewalDate,
    scheduledPlanName,
    taskLimit,
    tasksUsed,
    trialDaysRemaining,
}: PlanCardPropsI) => {
    const isTrial = trialDaysRemaining !== undefined;
    const tasksAvailable = taskLimit - tasksUsed;
    const overageTasksCount = Math.max(0, tasksUsed - taskLimit);

    const {blueFill, blueZoneWidth, orangeFill, orangeZoneWidth} = useMemo(() => {
        const totalRange = Math.max(tasksUsed, taskLimit);

        return {
            blueFill: Math.min(tasksUsed / taskLimit, 1) * 100,
            blueZoneWidth: (taskLimit / totalRange) * 100,
            orangeFill: tasksUsed > taskLimit ? 100 : 0,
            orangeZoneWidth: (Math.max(0, tasksUsed - taskLimit) / totalRange) * 100,
        };
    }, [taskLimit, tasksUsed]);

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
                            ) : (
                                <>
                                    {'Expires in '}

                                    <span className="font-semibold text-foreground">{trialDaysRemaining} days</span>

                                    {' or after '}

                                    <span className="font-semibold text-foreground">
                                        {taskLimit.toLocaleString()} tasks
                                    </span>

                                    {' are used'}
                                </>
                            )}
                        </span>
                    )}
                </div>

                <div className="flex gap-2">
                    {cancelAtPeriodEnd ? (
                        onReactivatePlan && (
                            <Button label="Reactivate" onClick={onReactivatePlan} variant="default" />
                        )
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

            <CardContent>
                <div className="rounded-lg bg-muted/50 px-4 py-6">
                    <div className="mb-2">
                        <span className="text-xl font-bold">
                            {isTrial ? tasksAvailable.toLocaleString() : tasksUsed.toLocaleString()}
                        </span>

                        <span className="text-xl text-muted-foreground">
                            {isTrial ? ' Tasks available' : ' Tasks used this period'}
                        </span>
                    </div>

                    <div className="mb-2 flex justify-between text-sm font-medium">
                        {isTrial ? (
                            <>
                                <span>Spent {tasksUsed.toLocaleString()}</span>

                                <span>Limit {taskLimit.toLocaleString()}</span>
                            </>
                        ) : overageTasksCount > 0 ? (
                            <>
                                <span>{taskLimit.toLocaleString()} flat rate</span>

                                <span>{overageTasksCount.toLocaleString()} usage billed</span>
                            </>
                        ) : (
                            <>
                                <span>{tasksUsed.toLocaleString()} flat rate</span>

                                <span>{taskLimit.toLocaleString()} included</span>
                            </>
                        )}
                    </div>

                    <div className="flex h-2 w-full overflow-hidden rounded-full bg-muted">
                        <div className="h-full bg-muted" style={{width: `${blueZoneWidth}%`}}>
                            <div className="h-full bg-blue-500" style={{width: `${blueFill}%`}} />
                        </div>

                        {orangeZoneWidth > 0 && (
                            <div className="h-full bg-muted" style={{width: `${orangeZoneWidth}%`}}>
                                <div className="h-full bg-orange-500" style={{width: `${orangeFill}%`}} />
                            </div>
                        )}
                    </div>

                    {!isTrial && (
                        <div className="mt-2 flex gap-4">
                            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                                <div className="size-2 rounded-full bg-blue-500" />

                                <span>Flat rate (included)</span>
                            </div>

                            {overageTasksCount > 0 && (
                                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                                    <div className="size-2 rounded-full bg-orange-500" />

                                    <span>Usage billed ($1 / 1,000 tasks)</span>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
};

export default PlanCard;
