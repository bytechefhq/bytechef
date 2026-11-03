import PlanSummaryCard, {type PlanSummaryCardPropsI} from '../../components/PlanSummaryCard';
import PlanUsage from '../../components/PlanUsage';
import TrialUsage from '../../components/TrialUsage';
import UsageZoneBar from '../../components/UsageZoneBar';

interface PlanCardPropsI extends PlanSummaryCardPropsI {
    jobsExecuted: number;
}

const PlanCard = ({jobsExecuted, ...planSummaryCardProps}: PlanCardPropsI) => {
    const {productUnitLimit, trialDaysRemaining} = planSummaryCardProps;

    const overageJobExecutionsCount = Math.max(0, jobsExecuted - productUnitLimit);

    return (
        <PlanSummaryCard {...planSummaryCardProps}>
            {trialDaysRemaining !== undefined ? (
                <TrialUsage jobsExecuted={jobsExecuted} productUnitLimit={productUnitLimit} />
            ) : (
                <>
                    <PlanUsage
                        bar={<UsageZoneBar jobsExecuted={jobsExecuted} productUnitLimit={productUnitLimit} />}
                        description="Job executions used this period"
                        leftLabel={`${(overageJobExecutionsCount > 0 ? productUnitLimit : jobsExecuted).toLocaleString()} flat rate`}
                        rightLabel={
                            overageJobExecutionsCount > 0
                                ? `${overageJobExecutionsCount.toLocaleString()} usage billed`
                                : `${productUnitLimit.toLocaleString()} included`
                        }
                        value={jobsExecuted}
                    />

                    <div className="mt-2 flex gap-4">
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                            <div className="size-2 rounded-full bg-blue-500" />

                            <span>Flat rate (included)</span>
                        </div>

                        {overageJobExecutionsCount > 0 && (
                            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                                <div className="size-2 rounded-full bg-orange-500" />

                                <span>Usage billed ($1 / 1,000 job executions)</span>
                            </div>
                        )}
                    </div>
                </>
            )}
        </PlanSummaryCard>
    );
};

export default PlanCard;
