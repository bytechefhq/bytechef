import AlertDialog from '@/components/AlertDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {BillingSubscription} from '@/shared/middleware/platform/billing';
import {
    useCancelSubscriptionMutation,
    useReactivateSubscriptionMutation,
} from '@/shared/mutations/platform/billing.mutations';
import {useGetCurrentSubscriptionQuery} from '@/shared/queries/platform/billing.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {ReactNode, useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import ManageBillingCard from './ManageBillingCard';
import {type PlanSummaryCardPropsI} from './PlanSummaryCard';
import SelectPlanDialog, {type PlanUsageFeaturesI} from './SelectPlanDialog';

const POLL_INTERVAL_MS = 3000;
const MAX_POLL_ATTEMPTS = 5;

export interface SubscriptionPlanCardPropsI extends PlanSummaryCardPropsI {
    jobsExecuted: number;
}

interface SubscriptionBillingPropsI {
    planUsageFeatures: PlanUsageFeaturesI;
    pollPackagePurchase?: boolean;
    renderPaidPlanExtras?: (paidSubscription: BillingSubscription) => ReactNode;
    renderPlanCard: (
        planCardProps: SubscriptionPlanCardPropsI,
        paidSubscription: BillingSubscription | undefined
    ) => ReactNode;
}

const SubscriptionBilling = ({
    planUsageFeatures,
    pollPackagePurchase = false,
    renderPaidPlanExtras,
    renderPlanCard,
}: SubscriptionBillingPropsI) => {
    const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
    const [pendingUpgradePlanName, setPendingUpgradePlanName] = useState<string | null>(null);
    const [pollAttempts, setPollAttempts] = useState(0);
    const [reactivateDialogOpen, setReactivateDialogOpen] = useState(false);
    const [searchParams, setSearchParams] = useSearchParams();
    const [selectPlanOpen, setSelectPlanOpen] = useState(false);

    const customerPortalUrl = useApplicationInfoStore((state) => state.billing.customerPortalUrl);

    const {isPending: isCancelPending, mutate: cancelSubscription} = useCancelSubscriptionMutation();
    const {isPending: isReactivateMutationPending, mutate: reactivateSubscription} =
        useReactivateSubscriptionMutation();

    const isCheckoutSuccess = searchParams.get('checkout') === 'success';
    const isCancelPolling = searchParams.get('cancel') === 'pending';
    const isReactivatePending = searchParams.get('reactivate') === 'pending';
    const isUpgradePending = searchParams.get('upgrade') === 'pending';
    const isPackageSuccess = pollPackagePurchase && searchParams.get('package') === 'success';
    const isPolling =
        (isCheckoutSuccess || isCancelPolling || isReactivatePending || isUpgradePending || isPackageSuccess) &&
        pollAttempts < MAX_POLL_ATTEMPTS;

    const {data: subscription, error: subscriptionError} = useGetCurrentSubscriptionQuery({
        refetchInterval: isPolling ? POLL_INTERVAL_MS : false,
    });

    const isTrialSubscription = subscription?.planName?.toUpperCase() === 'TRIAL';

    const isAwaitingChange =
        isCheckoutSuccess || isCancelPolling || isReactivatePending || isUpgradePending || isPackageSuccess;

    const isChangeApplied =
        (isCheckoutSuccess && !!subscription && !isTrialSubscription) ||
        (isCancelPolling && subscription?.cancelAtPeriodEnd === true) ||
        (isReactivatePending && subscription?.cancelAtPeriodEnd === false) ||
        (isUpgradePending &&
            pendingUpgradePlanName !== null &&
            subscription?.planName?.toUpperCase() === pendingUpgradePlanName.toUpperCase());

    const hasPollingTimedOut = pollAttempts >= MAX_POLL_ATTEMPTS;

    const pollingBanners = [
        {
            className: 'border-stroke-brand-secondary bg-surface-brand-secondary text-content-brand-primary',
            message: 'Activating your subscription, please wait…',
            visible: isCheckoutSuccess && (!subscription || isTrialSubscription),
        },
        {
            className: 'border-stroke-warning-secondary bg-surface-warning-secondary text-content-warning-primary',
            message: 'Scheduling cancellation, please wait…',
            visible: isCancelPolling,
        },
        {
            className: 'border-stroke-success-secondary bg-surface-success-secondary text-content-success-primary',
            message: 'Reactivating your subscription, please wait…',
            visible: isReactivatePending,
        },
        {
            className: 'border-stroke-brand-secondary bg-surface-brand-secondary text-content-brand-primary',
            message: 'Upgrading your plan, please wait…',
            visible: isUpgradePending,
        },
    ];

    useEffect(() => {
        if (!isAwaitingChange) {
            return;
        }

        if (isChangeApplied || (isPackageSuccess && hasPollingTimedOut)) {
            setPendingUpgradePlanName(null);
            setSearchParams({}, {replace: true});

            return;
        }

        if (hasPollingTimedOut) {
            return;
        }

        const timer = setTimeout(() => {
            setPollAttempts((previousPollAttempts) => previousPollAttempts + 1);
        }, POLL_INTERVAL_MS);

        return () => clearTimeout(timer);
    }, [hasPollingTimedOut, isAwaitingChange, isChangeApplied, isPackageSuccess, pollAttempts, setSearchParams]);

    const renewalDate = subscription?.currentPeriodEnd
        ? subscription.currentPeriodEnd.toLocaleDateString('en-US', {day: 'numeric', month: 'long', year: 'numeric'})
        : undefined;

    const trialDaysRemaining =
        isTrialSubscription && subscription?.currentPeriodEnd
            ? Math.max(0, Math.ceil((subscription.currentPeriodEnd.getTime() - Date.now()) / (1000 * 60 * 60 * 24)))
            : undefined;

    const isTrialExpired = isTrialSubscription && subscription?.status === 'CANCELED';

    const paidSubscription = subscription && !isTrialSubscription ? subscription : undefined;

    const planCardProps: Omit<SubscriptionPlanCardPropsI, 'onChangePlan'> =
        subscription && !isTrialSubscription
            ? {
                  cancelAtPeriodEnd: subscription.cancelAtPeriodEnd ?? false,
                  jobsExecuted: subscription.jobsExecuted ?? 0,
                  planName: subscription.planName ?? 'Active',
                  productUnitLimit: subscription.productUnitLimit ?? 0,
                  renewalDate,
                  scheduledPlanName: subscription.scheduledPlanName,
                  trialDaysRemaining: undefined,
                  trialExpired: undefined,
              }
            : {
                  cancelAtPeriodEnd: false,
                  jobsExecuted: subscription?.jobsExecuted ?? 0,
                  planName: 'Trial',
                  productUnitLimit: subscription?.productUnitLimit ?? 0,
                  renewalDate: undefined,
                  scheduledPlanName: undefined,
                  trialDaysRemaining,
                  trialExpired: isTrialExpired,
              };

    return (
        <LayoutContainer
            header={
                <Header centerTitle description="Manage your subscription and usage." position="main" title="Billing" />
            }
            leftSidebarOpen={false}
        >
            {subscriptionError ? (
                <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                    <div className="rounded-lg border border-stroke-destructive-secondary bg-surface-destructive-secondary px-4 py-3 text-sm text-content-destructive">
                        Unable to load subscription. Please refresh the page or contact support if the problem persists.
                    </div>
                </div>
            ) : (
                <div className="w-full space-y-4 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                    {pollingBanners
                        .filter((pollingBanner) => pollingBanner.visible)
                        .map((pollingBanner) => (
                            <div
                                className={twMerge('rounded-lg border px-4 py-3 text-sm', pollingBanner.className)}
                                key={pollingBanner.message}
                            >
                                {hasPollingTimedOut
                                    ? "It's taking longer than expected. Please refresh the page."
                                    : pollingBanner.message}
                            </div>
                        ))}

                    {isPackageSuccess && (
                        <div className="rounded-lg border border-stroke-brand-secondary bg-surface-brand-secondary px-4 py-3 text-sm text-content-brand-primary">
                            Adding job executions, please wait…
                        </div>
                    )}

                    {renderPlanCard(
                        {
                            ...planCardProps,
                            onCancelPlan:
                                subscription && !isTrialSubscription && !subscription.cancelAtPeriodEnd
                                    ? () => setCancelDialogOpen(true)
                                    : undefined,
                            onChangePlan: () => setSelectPlanOpen(true),
                            onReactivatePlan: subscription?.cancelAtPeriodEnd
                                ? () => setReactivateDialogOpen(true)
                                : undefined,
                        },
                        paidSubscription
                    )}

                    {paidSubscription && renderPaidPlanExtras?.(paidSubscription)}

                    {customerPortalUrl && <ManageBillingCard customerPortalUrl={customerPortalUrl} />}

                    <AlertDialog
                        cancelLabel="Keep plan"
                        confirmLabel={isCancelPending ? 'Cancelling…' : 'Cancel subscription'}
                        description="Your subscription will be cancelled at the end of the current billing period. You will retain access until then and will not be charged again."
                        isPending={isCancelPending}
                        onCancel={() => setCancelDialogOpen(false)}
                        onConfirm={() =>
                            cancelSubscription(undefined, {
                                onSuccess: () => {
                                    setCancelDialogOpen(false);
                                    setPollAttempts(0);
                                    setSearchParams({cancel: 'pending'}, {replace: true});
                                },
                            })
                        }
                        open={cancelDialogOpen}
                        title="Cancel subscription?"
                    />

                    <AlertDialog
                        cancelLabel="Keep cancelled"
                        confirmButtonVariant="default"
                        confirmLabel={isReactivateMutationPending ? 'Reactivating…' : 'Reactivate subscription'}
                        description="Your subscription will continue and you will be charged at the next billing cycle. Your plan will no longer be cancelled."
                        isPending={isReactivateMutationPending}
                        onCancel={() => setReactivateDialogOpen(false)}
                        onConfirm={() =>
                            reactivateSubscription(undefined, {
                                onSuccess: () => {
                                    setReactivateDialogOpen(false);
                                    setPollAttempts(0);
                                    setSearchParams({reactivate: 'pending'}, {replace: true});
                                },
                            })
                        }
                        open={reactivateDialogOpen}
                        title="Reactivate subscription?"
                    />

                    <SelectPlanDialog
                        currentPlanName={subscription?.planName ?? undefined}
                        hasActiveSubscription={!!subscription && !isTrialSubscription}
                        onClose={() => setSelectPlanOpen(false)}
                        onUpgradeSuccess={(isUpgrade, newPlanName) => {
                            setPollAttempts(0);

                            if (isUpgrade) {
                                setPendingUpgradePlanName(newPlanName);
                                setSearchParams({upgrade: 'pending'}, {replace: true});
                            } else {
                                toast('Your plan will be downgraded at the end of the current billing period.');
                            }
                        }}
                        open={selectPlanOpen}
                        planUsageFeatures={planUsageFeatures}
                    />
                </div>
            )}
        </LayoutContainer>
    );
};

export default SubscriptionBilling;
