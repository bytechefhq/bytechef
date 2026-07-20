import Button from '@/components/Button/Button';
import {useGetCurrentSubscriptionQuery} from '@/shared/queries/platform/billing.queries';
import {InfoIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

export function TrialBanner() {
    const {data: subscription} = useGetCurrentSubscriptionQuery();

    const navigate = useNavigate();

    const daysRemaining = useMemo(() => {
        if (!subscription?.currentPeriodEnd) {
            return 0;
        }

        const millisecondsRemaining = subscription.currentPeriodEnd.getTime() - Date.now();

        return Math.max(0, Math.ceil(millisecondsRemaining / MILLISECONDS_PER_DAY));
    }, [subscription?.currentPeriodEnd]);

    const handleUpgradeClick = useCallback(() => navigate('/automation/settings/billing'), [navigate]);

    if (subscription?.planName !== 'TRIAL') {
        return null;
    }

    const expired = subscription.status === 'CANCELED';

    return (
        <div
            className={twMerge(
                'flex items-center gap-2 border-b px-4 py-2',
                expired
                    ? 'bg-surface-error-secondary border-stroke-destructive-secondary'
                    : 'border-stroke-warning-secondary bg-surface-warning-secondary'
            )}
        >
            <InfoIcon
                className={twMerge('size-5 shrink-0', expired ? 'text-content-destructive' : 'text-content-onwarning')}
            />

            <span className="flex-1 text-sm font-medium text-content-neutral-primary">
                {expired
                    ? 'Your trial has expired.'
                    : `Trial: ${daysRemaining} ${daysRemaining === 1 ? 'day' : 'days'} remaining · ${subscription.tasksUsed ?? 0}/${subscription.taskLimit ?? 0} tasks used.`}
            </span>

            <Button
                className="active:text-content-primary text-sm font-medium hover:bg-transparent hover:underline active:bg-transparent"
                label="Upgrade"
                onClick={handleUpgradeClick}
                size="xs"
                variant="ghost"
            />
        </div>
    );
}
