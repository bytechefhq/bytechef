import PlanUsage from './PlanUsage';
import UsageZoneBar from './UsageZoneBar';

interface TrialUsagePropsI {
    jobsExecuted: number;
    productUnitLimit: number;
}

const TrialUsage = ({jobsExecuted, productUnitLimit}: TrialUsagePropsI) => (
    <PlanUsage
        bar={<UsageZoneBar jobsExecuted={jobsExecuted} productUnitLimit={productUnitLimit} />}
        description="Job executions available"
        leftLabel={`Spent ${jobsExecuted.toLocaleString()}`}
        rightLabel={`Limit ${productUnitLimit.toLocaleString()}`}
        value={productUnitLimit - jobsExecuted}
    />
);

export default TrialUsage;
