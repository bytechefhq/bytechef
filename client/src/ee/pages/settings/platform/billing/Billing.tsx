import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    useCancelSubscriptionMutation,
    useReactivateSubscriptionMutation,
} from '@/shared/mutations/platform/billing.mutations';
import {useGetCurrentSubscriptionQuery} from '@/shared/queries/platform/billing.queries';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

import CancelPlanDialog from './components/CancelPlanDialog';
import PlanCard from './components/PlanCard';
import ReactivatePlanDialog from './components/ReactivatePlanDialog';
import SelectPlanDialog from './components/SelectPlanDialog';

const TRIAL_TASK_LIMIT = 5000;
const POLL_INTERVAL_MS = 3000;
const MAX_POLL_ATTEMPTS = 5;

const Billing = () => {
    const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
    const [pendingUpgradePlanName, setPendingUpgradePlanName] = useState<string | null>(null);
    const [pollAttempts, setPollAttempts] = useState(0);
    const [reactivateDialogOpen, setReactivateDialogOpen] = useState(false);
    const [searchParams, setSearchParams] = useSearchParams();
    const [selectPlanOpen, setSelectPlanOpen] = useState(false);

    const {isPending: isCancelPending, mutate: cancelSubscription} = useCancelSubscriptionMutation();
    const {isPending: isReactivateMutationPending, mutate: reactivateSubscription} =
        useReactivateSubscriptionMutation();

    const isCheckoutSuccess = searchParams.get('checkout') === 'success';
    const isCancelPolling = searchParams.get('cancel') === 'pending';
    const isReactivatePending = searchParams.get('reactivate') === 'pending';
    const isUpgradePending = searchParams.get('upgrade') === 'pending';
    const isPolling =
        (isCheckoutSuccess || isCancelPolling || isReactivatePending || isUpgradePending) &&
        pollAttempts < MAX_POLL_ATTEMPTS;

    const {data: subscription} = useGetCurrentSubscriptionQuery({
        refetchInterval: isPolling ? POLL_INTERVAL_MS : false,
    });

    useEffect(() => {
        if (!isCheckoutSuccess) {
            return;
        }

        if (subscription) {
            setSearchParams({}, {replace: true});

            return;
        }

        if (pollAttempts >= MAX_POLL_ATTEMPTS) {
            return;
        }

        const timer = setTimeout(() => {
            setPollAttempts((prev) => prev + 1);
        }, POLL_INTERVAL_MS);

        return () => clearTimeout(timer);
    }, [isCheckoutSuccess, pollAttempts, searchParams, setSearchParams, subscription]);

    useEffect(() => {
        if (!isCancelPolling) {
            return;
        }

        if (subscription?.cancelAtPeriodEnd === true) {
            setSearchParams({}, {replace: true});

            return;
        }

        if (pollAttempts >= MAX_POLL_ATTEMPTS) {
            return;
        }

        const timer = setTimeout(() => {
            setPollAttempts((prev) => prev + 1);
        }, POLL_INTERVAL_MS);

        return () => clearTimeout(timer);
    }, [isCancelPolling, pollAttempts, setSearchParams, subscription]);

    useEffect(() => {
        if (!isReactivatePending) {
            return;
        }

        if (subscription?.cancelAtPeriodEnd === false) {
            setSearchParams({}, {replace: true});

            return;
        }

        if (pollAttempts >= MAX_POLL_ATTEMPTS) {
            return;
        }

        const timer = setTimeout(() => {
            setPollAttempts((prev) => prev + 1);
        }, POLL_INTERVAL_MS);

        return () => clearTimeout(timer);
    }, [isReactivatePending, pollAttempts, setSearchParams, subscription]);

    useEffect(() => {
        if (!isUpgradePending) {
            return;
        }

        if (
            pendingUpgradePlanName !== null &&
            subscription?.planName?.toUpperCase() === pendingUpgradePlanName.toUpperCase()
        ) {
            setPendingUpgradePlanName(null);
            setSearchParams({}, {replace: true});

            return;
        }

        if (pollAttempts >= MAX_POLL_ATTEMPTS) {
            return;
        }

        const timer = setTimeout(() => {
            setPollAttempts((prev) => prev + 1);
        }, POLL_INTERVAL_MS);

        return () => clearTimeout(timer);
    }, [isUpgradePending, pendingUpgradePlanName, pollAttempts, setSearchParams, subscription]);

    const renewalDate = subscription?.currentPeriodEnd
        ? subscription.currentPeriodEnd.toLocaleDateString('en-US', {day: 'numeric', month: 'long', year: 'numeric'})
        : undefined;

    const trialDaysRemaining =
        !subscription && !isCheckoutSuccess
            ? 30
            : subscription?.trialEnd
              ? Math.max(0, Math.ceil((subscription.trialEnd.getTime() - Date.now()) / (1000 * 60 * 60 * 24)))
              : undefined;

    const planCardProps = subscription
        ? {
              cancelAtPeriodEnd: subscription.cancelAtPeriodEnd ?? false,
              planName: subscription.planName ?? 'Active',
              renewalDate,
              scheduledPlanName: subscription.scheduledPlanName,
              taskLimit: subscription.taskLimit ?? TRIAL_TASK_LIMIT,
              tasksUsed: subscription.tasksUsed ?? 0,
              trialDaysRemaining: undefined,
          }
        : {
              cancelAtPeriodEnd: false,
              planName: 'Trial',
              renewalDate: undefined,
              scheduledPlanName: undefined,
              taskLimit: TRIAL_TASK_LIMIT,
              tasksUsed: 0,
              trialDaysRemaining,
          };

    return (
        <LayoutContainer
            header={
                <Header centerTitle description="Manage your subscription and usage." position="main" title="Billing" />
            }
            leftSidebarOpen={false}
        >
            <div className="w-full space-y-4 px-4 3xl:mx-auto 3xl:w-4/5">
                {isCheckoutSuccess && !subscription && (
                    <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                        {pollAttempts >= MAX_POLL_ATTEMPTS
                            ? "It's taking longer than expected. Please refresh the page."
                            : 'Activating your subscription, please wait…'}
                    </div>
                )}

                {isCancelPolling && (
                    <div className="rounded-lg border border-orange-200 bg-orange-50 px-4 py-3 text-sm text-orange-800">
                        {pollAttempts >= MAX_POLL_ATTEMPTS
                            ? "It's taking longer than expected. Please refresh the page."
                            : 'Scheduling cancellation, please wait…'}
                    </div>
                )}

                {isReactivatePending && (
                    <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
                        {pollAttempts >= MAX_POLL_ATTEMPTS
                            ? "It's taking longer than expected. Please refresh the page."
                            : 'Reactivating your subscription, please wait…'}
                    </div>
                )}

                {isUpgradePending && (
                    <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                        {pollAttempts >= MAX_POLL_ATTEMPTS
                            ? "It's taking longer than expected. Please refresh the page."
                            : 'Upgrading your plan, please wait…'}
                    </div>
                )}

                <Tabs defaultValue="overview">
                    <TabsList>
                        <TabsTrigger value="overview">Overview</TabsTrigger>

                        <TabsTrigger value="invoices">Invoices</TabsTrigger>
                    </TabsList>

                    <TabsContent className="mt-4 space-y-4" value="overview">
                        <PlanCard
                            {...planCardProps}
                            onCancelPlan={
                                subscription && !subscription.cancelAtPeriodEnd
                                    ? () => setCancelDialogOpen(true)
                                    : undefined
                            }
                            onChangePlan={() => setSelectPlanOpen(true)}
                            onReactivatePlan={
                                subscription?.cancelAtPeriodEnd ? () => setReactivateDialogOpen(true) : undefined
                            }
                        />
                    </TabsContent>

                    <TabsContent className="mt-4" value="invoices">
                        <p className="text-sm text-muted-foreground">No invoices yet.</p>
                    </TabsContent>
                </Tabs>

                <CancelPlanDialog
                    isPending={isCancelPending}
                    onClose={() => setCancelDialogOpen(false)}
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
                />

                <ReactivatePlanDialog
                    isPending={isReactivateMutationPending}
                    onClose={() => setReactivateDialogOpen(false)}
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
                />

                <SelectPlanDialog
                    currentPlanName={subscription?.planName ?? undefined}
                    hasActiveSubscription={!!subscription}
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
                />
            </div>
        </LayoutContainer>
    );
};

export default Billing;
