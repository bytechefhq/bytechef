import {BillingApi, BillingSubscription} from '@/shared/middleware/platform/billing';
import {useQuery} from '@tanstack/react-query';

export const BillingSubscriptionKeys = {
    subscription: ['billing', 'subscription'] as const,
};

export const useGetCurrentSubscriptionQuery = (options?: {enabled?: boolean; refetchInterval?: number | false}) =>
    useQuery<BillingSubscription | null>({
        enabled: options?.enabled ?? true,
        queryFn: async () => {
            const result = await new BillingApi().getCurrentSubscription();

            return result ?? null;
        },
        queryKey: BillingSubscriptionKeys.subscription,
        refetchInterval: options?.refetchInterval,
    });
