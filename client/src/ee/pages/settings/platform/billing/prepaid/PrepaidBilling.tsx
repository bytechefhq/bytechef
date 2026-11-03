import {useState} from 'react';

import SubscriptionBilling from '../components/SubscriptionBilling';
import BuyJobExecutionsPackageDialog from './components/BuyJobExecutionsPackageDialog';
import ExtraUnitsCard from './components/ExtraUnitsCard';
import PrepaidPlanCard from './components/PrepaidPlanCard';

const ADDITIONAL_WORKFLOW_EXECUTIONS_PRICE = '+$5 per 1000 additional workflow executions';

const PREPAID_PLAN_USAGE_FEATURES = {
    growth: ['Price for 15000 workflow executions per month', ADDITIONAL_WORKFLOW_EXECUTIONS_PRICE],
    starter: ['Price for 5000 workflow executions per month', ADDITIONAL_WORKFLOW_EXECUTIONS_PRICE],
};

const PrepaidBilling = () => {
    const [buyPackageOpen, setBuyPackageOpen] = useState(false);

    return (
        <>
            <SubscriptionBilling
                planUsageFeatures={PREPAID_PLAN_USAGE_FEATURES}
                pollPackagePurchase
                renderPaidPlanExtras={(paidSubscription) => (
                    <ExtraUnitsCard
                        disabled={(paidSubscription.cancelAtPeriodEnd ?? false) || paidSubscription.status !== 'ACTIVE'}
                        extraUnitTotal={paidSubscription.extraUnitTotal ?? 0}
                        extraUnitUsed={paidSubscription.extraUnitUsed ?? 0}
                        onBuy={() => setBuyPackageOpen(true)}
                    />
                )}
                renderPlanCard={(planCardProps, paidSubscription) => (
                    <PrepaidPlanCard
                        {...planCardProps}
                        planUnitTotal={paidSubscription?.planUnitTotal ?? 0}
                        planUnitUsed={paidSubscription?.planUnitUsed ?? 0}
                    />
                )}
            />

            <BuyJobExecutionsPackageDialog onClose={() => setBuyPackageOpen(false)} open={buyPackageOpen} />
        </>
    );
};

export default PrepaidBilling;
