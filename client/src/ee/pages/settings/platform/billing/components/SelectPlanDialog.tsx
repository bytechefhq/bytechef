import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {ArrowUpRightIcon, XIcon} from 'lucide-react';

import PlanTierCard from './PlanTierCard';

interface SelectPlanDialogPropsI {
    onClose: () => void;
    open: boolean;
}

const PLANS = [
    {
        ctaLabel: 'Get Started',
        description: 'Perfect for individuals and small businesses.',
        features: ['5000 tasks / month, then $1/1000 tasks', '1 workspace', '1 user', '30-days long retention'],
        highlighted: false,
        name: 'Starter',
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
        price: null,
    },
];

const SelectPlanDialog = ({onClose, open}: SelectPlanDialogPropsI) => (
    <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
        <DialogContent className="max-w-[960px] gap-6">
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
                        ctaLabel={plan.ctaLabel}
                        description={plan.description}
                        features={plan.features}
                        highlighted={plan.highlighted}
                        key={plan.name}
                        name={plan.name}
                        onSelect={onClose}
                        price={plan.price}
                    />
                ))}
            </div>
        </DialogContent>
    </Dialog>
);

export default SelectPlanDialog;
