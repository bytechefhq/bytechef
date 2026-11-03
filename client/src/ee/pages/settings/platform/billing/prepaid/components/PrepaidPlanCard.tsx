import PlanSummaryCard, {type PlanSummaryCardPropsI} from '../../components/PlanSummaryCard';
import PlanUsage from '../../components/PlanUsage';
import TrialUsage from '../../components/TrialUsage';

interface PrepaidPlanCardPropsI extends PlanSummaryCardPropsI {
    jobsExecuted: number;
    planUnitTotal: number;
    planUnitUsed: number;
}

const PrepaidPlanCard = ({
    jobsExecuted,
    planUnitTotal,
    planUnitUsed,
    ...planSummaryCardProps
}: PrepaidPlanCardPropsI) => {
    const {productUnitLimit, trialDaysRemaining} = planSummaryCardProps;

    const planUnitFill = planUnitTotal > 0 ? Math.min(planUnitUsed / planUnitTotal, 1) * 100 : 0;

    return (
        <PlanSummaryCard {...planSummaryCardProps}>
            {trialDaysRemaining !== undefined ? (
                <TrialUsage jobsExecuted={jobsExecuted} productUnitLimit={productUnitLimit} />
            ) : (
                <PlanUsage
                    bar={<div className="h-full bg-blue-500" style={{width: `${planUnitFill}%`}} />}
                    description="Job executions used this period"
                    leftLabel={`Used ${planUnitUsed.toLocaleString()}`}
                    rightLabel={`Included ${planUnitTotal.toLocaleString()}`}
                    value={planUnitUsed}
                />
            )}
        </PlanSummaryCard>
    );
};

export default PrepaidPlanCard;
