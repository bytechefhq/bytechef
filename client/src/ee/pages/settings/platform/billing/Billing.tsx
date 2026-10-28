import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {BillingSubscriptionSubscriptionModelEnum} from '@/shared/middleware/platform/billing';
import {useGetCurrentSubscriptionQuery} from '@/shared/queries/platform/billing.queries';

import MeteredBilling from './metered/MeteredBilling';
import PrepaidBilling from './prepaid/PrepaidBilling';

const Billing = () => {
    const {data: subscription, error: subscriptionError, isPending} = useGetCurrentSubscriptionQuery();

    if (subscriptionError) {
        return (
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description="Manage your subscription and usage."
                        position="main"
                        title="Billing"
                    />
                }
                leftSidebarOpen={false}
            >
                <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                    <div className="rounded-lg border border-stroke-destructive-secondary bg-surface-destructive-secondary px-4 py-3 text-sm text-content-destructive">
                        Unable to load subscription. Please refresh the page or contact support if the problem persists.
                    </div>
                </div>
            </LayoutContainer>
        );
    }

    if (isPending) {
        return null;
    }

    if (subscription?.subscriptionModel === BillingSubscriptionSubscriptionModelEnum.Metered) {
        return <MeteredBilling />;
    }

    return <PrepaidBilling />;
};

export default Billing;
