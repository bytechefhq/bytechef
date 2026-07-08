import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader} from '@/components/ui/card';
import {Progress} from '@/components/ui/progress';

interface PlanCardPropsI {
    cancelAtPeriodEnd: boolean;
    onCancelPlan: () => void;
    onChangePlan: () => void;
    planName: string;
    renewalDate?: string;
    taskLimit: number;
    tasksUsed: number;
    trialDaysRemaining?: number;
}

const PlanCard = ({
    cancelAtPeriodEnd,
    onCancelPlan,
    onChangePlan,
    planName,
    renewalDate,
    taskLimit,
    tasksUsed,
    trialDaysRemaining,
}: PlanCardPropsI) => {
    const tasksAvailable = taskLimit - tasksUsed;
    const progressValue = Math.min((tasksUsed / taskLimit) * 100, 100);

    return (
        <Card className="w-full max-w-3xl">
            <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div className="flex flex-col gap-1">
                    <span className="font-bold">{planName}</span>

                    {(cancelAtPeriodEnd || renewalDate || trialDaysRemaining !== undefined) && (
                        <span className="text-sm text-muted-foreground">
                            {cancelAtPeriodEnd ? (
                                'Your subscription will be cancelled at the end of the billing period.'
                            ) : renewalDate ? (
                                <>
                                    {'Your plan will automatically renew on '}

                                    <span className="font-semibold text-foreground">{renewalDate}</span>
                                </>
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
                    <Button label="Change plan" onClick={onChangePlan} variant="default" />

                    <Button label="Cancel plan" onClick={onCancelPlan} variant="destructiveGhost" />
                </div>
            </CardHeader>

            <CardContent>
                <div className="rounded-lg bg-muted/50 px-4 py-6">
                    <div className="mb-2">
                        <span className="text-xl font-bold">{tasksAvailable.toLocaleString()}</span>

                        <span className="text-xl text-muted-foreground"> Tasks available</span>
                    </div>

                    <div className="mb-2 flex justify-between text-sm font-medium">
                        <span>Spent {tasksUsed.toLocaleString()}</span>

                        <span>Limit {taskLimit.toLocaleString()}</span>
                    </div>

                    <Progress value={progressValue} />
                </div>
            </CardContent>
        </Card>
    );
};

export default PlanCard;
