import SubscriptionBilling from '../components/SubscriptionBilling';
import PlanCard from './components/PlanCard';

const METERED_PLAN_USAGE_FEATURES = {
    growth: [],
    starter: ['1000 job executions / month, then $1/1000 job executions'],
};

const MeteredBilling = () => (
    <SubscriptionBilling
        planUsageFeatures={METERED_PLAN_USAGE_FEATURES}
        renderPlanCard={(planCardProps) => <PlanCard {...planCardProps} />}
    />
);

export default MeteredBilling;
