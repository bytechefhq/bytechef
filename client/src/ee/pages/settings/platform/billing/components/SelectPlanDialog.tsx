import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {BillingApi, CheckoutSessionRequestPlanNameEnum} from '@/shared/middleware/platform/billing';
import {useUpgradeSubscriptionMutation} from '@/shared/mutations/platform/billing.mutations';
import {ArrowUpRightIcon, XIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

import DowngradeConfirmationDialog from './DowngradeConfirmationDialog';
import PlanTierCard from './PlanTierCard';
import UpgradeConfirmationDialog from './UpgradeConfirmationDialog';

interface PendingPlanChangeI {
    name: string;
    nameEnum: CheckoutSessionRequestPlanNameEnum;
}

interface SelectPlanDialogPropsI {
    currentPlanName?: string;
    hasActiveSubscription: boolean;
    onClose: () => void;
    onUpgradeSuccess?: (isUpgrade: boolean, newPlanName: string) => void;
    open: boolean;
}

const PLAN_TIERS: Record<string, number> = {
    growth: 2,
    starter: 1,
};

const PLANS = [
    {
        ctaLabel: 'Get Started',
        description: 'Perfect for individuals and small businesses.',
        features: ['5000 tasks / month, then $1/1000 tasks', '1 workspace', '1 user', '30-days long retention'],
        highlighted: false,
        name: 'Starter',
        planNameEnum: CheckoutSessionRequestPlanNameEnum.Starter,
        price: '$29',
    },
    {
        ctaLabel: 'Get Started',
        description: 'Perfect for growing business.',
        features: [
            'Everything in Starter',
            '5000 tasks / month, then $1/1000 tasks',
            '3 workspaces',
            'Unlimited users',
            'Role-based access controls',
            '30-days long retention',
        ],
        highlighted: true,
        name: 'Growth',
        planNameEnum: CheckoutSessionRequestPlanNameEnum.Growth,
        price: '$169',
    },
    {
        ctaLabel: 'Contact Us',
        description: 'For Enterprise-scale usage and flexible hosting.',
        features: [
            'Everything in Growth',
            'Custom amount of tasks',
            'Unlimited workspace',
            'Self-hosting options',
            'API Platform',
            'Environments & Version control using Git',
        ],
        highlighted: false,
        name: 'Enterprise',
        planNameEnum: null,
        price: null,
    },
];

const getPlanTier = (planName: string | undefined): number => PLAN_TIERS[planName?.toLowerCase() ?? ''] ?? 0;

const SelectPlanDialog = ({
    currentPlanName,
    hasActiveSubscription,
    onClose,
    onUpgradeSuccess,
    open,
}: SelectPlanDialogPropsI) => {
    const [downgradeConfirmOpen, setDowngradeConfirmOpen] = useState(false);
    const [loadingPlan, setLoadingPlan] = useState<string | null>(null);
    const [pendingDowngrade, setPendingDowngrade] = useState<PendingPlanChangeI | null>(null);
    const [pendingUpgrade, setPendingUpgrade] = useState<PendingPlanChangeI | null>(null);
    const [upgradeConfirmOpen, setUpgradeConfirmOpen] = useState(false);

    const {isPending: isUpgradeMutationPending, mutate: upgradeSubscription} = useUpgradeSubscriptionMutation();

    const closeDowngradeConfirm = () => {
        setPendingDowngrade(null);
        setDowngradeConfirmOpen(false);
    };

    const closeUpgradeConfirm = () => {
        setPendingUpgrade(null);
        setUpgradeConfirmOpen(false);
    };

    const handleSubscriptionChange = (
        planName: string,
        planNameEnum: CheckoutSessionRequestPlanNameEnum,
        closeConfirm: () => void
    ) => {
        const planIsUpgrade = getPlanTier(planName) > getPlanTier(currentPlanName);

        if (!planIsUpgrade) {
            setLoadingPlan(planName);
        }

        upgradeSubscription(planNameEnum, {
            onError: () => {
                toast.error('Failed to change plan. Please try again.');

                setLoadingPlan(null);
                closeConfirm();
            },
            onSuccess: () => {
                setLoadingPlan(null);
                closeConfirm();
                onUpgradeSuccess?.(planIsUpgrade, planName);
                onClose();
            },
        });
    };

    const handlePlanSelect = async (planName: string, planNameEnum: CheckoutSessionRequestPlanNameEnum | null) => {
        if (planNameEnum === null) {
            window.open('https://bytechef.io/contact', '_blank');

            return;
        }

        if (!hasActiveSubscription) {
            setLoadingPlan(planName);

            try {
                const session = await new BillingApi().createCheckoutSession({
                    checkoutSessionRequest: {planName: planNameEnum},
                });

                if (session.checkoutUrl) {
                    window.location.href = session.checkoutUrl;
                } else {
                    toast.error('Failed to start checkout. Please try again.');

                    setLoadingPlan(null);
                }
            } catch {
                toast.error('Failed to start checkout. Please try again.');

                setLoadingPlan(null);
            }

            return;
        }

        const planIsUpgrade = getPlanTier(planName) > getPlanTier(currentPlanName);

        if (planIsUpgrade) {
            setPendingUpgrade({name: planName, nameEnum: planNameEnum});
            setUpgradeConfirmOpen(true);
        } else {
            setPendingDowngrade({name: planName, nameEnum: planNameEnum});
            setDowngradeConfirmOpen(true);
        }
    };

    return (
        <>
            <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
                <DialogContent className="gap-6 sm:max-w-[960px]">
                    <DialogHeader className="flex-row items-start justify-between space-y-0">
                        <div className="flex flex-col gap-1">
                            <DialogTitle>Select a plan</DialogTitle>

                            <DialogDescription>You can upgrade, downgrade, or cancel at any time.</DialogDescription>
                        </div>

                        <div className="flex items-center gap-4">
                            <button
                                className="flex h-9 items-center gap-2 rounded-md border border-slate-200 bg-white px-4 text-sm font-medium text-foreground hover:bg-slate-50"
                                onClick={() => window.open('https://bytechef.io/pricing', '_blank')}
                                type="button"
                            >
                                Compare plans
                                <ArrowUpRightIcon className="size-4" />
                            </button>

                            <DialogClose className="opacity-50 hover:opacity-100">
                                <XIcon className="size-4" />

                                <span className="sr-only">Close</span>
                            </DialogClose>
                        </div>
                    </DialogHeader>

                    <div className="flex gap-4">
                        {PLANS.map((plan) => (
                            <PlanTierCard
                                ctaLabel={loadingPlan === plan.name ? 'Loading…' : plan.ctaLabel}
                                description={plan.description}
                                features={plan.features}
                                highlighted={plan.highlighted}
                                isCurrent={plan.name?.toLocaleLowerCase() === currentPlanName?.toLocaleLowerCase()}
                                key={plan.name}
                                name={plan.name}
                                onSelect={() => handlePlanSelect(plan.name, plan.planNameEnum)}
                                price={plan.price}
                            />
                        ))}
                    </div>
                </DialogContent>
            </Dialog>

            <DowngradeConfirmationDialog
                currentPlanName={currentPlanName}
                isPending={isUpgradeMutationPending}
                newPlanName={pendingDowngrade?.name ?? ''}
                onClose={closeDowngradeConfirm}
                onConfirm={() => {
                    if (pendingDowngrade) {
                        handleSubscriptionChange(
                            pendingDowngrade.name,
                            pendingDowngrade.nameEnum,
                            closeDowngradeConfirm
                        );
                    }
                }}
                open={downgradeConfirmOpen}
            />

            <UpgradeConfirmationDialog
                currentPlanName={currentPlanName}
                isPending={isUpgradeMutationPending}
                newPlanName={pendingUpgrade?.name ?? ''}
                onClose={closeUpgradeConfirm}
                onConfirm={() => {
                    if (pendingUpgrade) {
                        handleSubscriptionChange(pendingUpgrade.name, pendingUpgrade.nameEnum, closeUpgradeConfirm);
                    }
                }}
                open={upgradeConfirmOpen}
            />
        </>
    );
};

export default SelectPlanDialog;
