import {BillingApi, CheckoutSessionRequestPlanNameEnum} from '@/shared/middleware/platform/billing';
import {useMutation} from '@tanstack/react-query';

export const useCancelSubscriptionMutation = () =>
    useMutation({
        mutationFn: () => new BillingApi().cancelSubscription(),
    });

export const useReactivateSubscriptionMutation = () =>
    useMutation({
        mutationFn: () => new BillingApi().reactivateSubscription(),
    });

export const useUpgradeSubscriptionMutation = () =>
    useMutation({
        mutationFn: (planName: CheckoutSessionRequestPlanNameEnum) =>
            new BillingApi().upgradeSubscription({checkoutSessionRequest: {planName}}),
    });
