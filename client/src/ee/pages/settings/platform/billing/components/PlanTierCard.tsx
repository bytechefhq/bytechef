import Button from '@/components/Button/Button';
import {CircleCheckIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface PlanTierCardPropsI {
    ctaLabel: string;
    description: string;
    features: string[];
    highlighted?: boolean;
    isCurrent?: boolean;
    name: string;
    onSelect: () => void;
    price: string | null;
}

const PlanTierCard = ({
    ctaLabel,
    description,
    features,
    highlighted = false,
    isCurrent = false,
    name,
    onSelect,
    price,
}: PlanTierCardPropsI) => (
    <div
        className={twMerge(
            'flex w-72 flex-col gap-2 overflow-hidden rounded-2xl border p-2',
            highlighted ? 'border-[#b3c9ed] bg-[#e6eef9]' : 'border-slate-200 bg-white'
        )}
    >
        <div
            className={twMerge(
                'flex h-36 flex-col justify-center gap-1 rounded-xl px-4 py-8',
                highlighted ? 'bg-white' : 'bg-slate-100'
            )}
        >
            <span className="text-xl font-bold">{name}</span>

            <span className="text-base text-muted-foreground">{description}</span>
        </div>

        <div className="flex flex-col items-center justify-center gap-4 px-4 py-8">
            {price !== null ? (
                <p className="text-[0px]">
                    <span className="text-xl font-bold text-foreground">{price}</span>

                    <span className="text-base text-muted-foreground">{' / Month'}</span>
                </p>
            ) : (
                <span className="text-xl font-bold text-foreground">{ctaLabel}</span>
            )}

            <Button
                className="w-full"
                disabled={isCurrent}
                label={isCurrent ? 'Current Plan' : ctaLabel}
                onClick={onSelect}
                variant={highlighted ? 'default' : 'outline'}
            />
        </div>

        <hr className="border-slate-200" />

        <ul className="flex flex-1 flex-col gap-3 px-4 py-5">
            {features.map((feature) => (
                <li className="flex items-start gap-3" key={feature}>
                    <CircleCheckIcon className="mt-0.5 size-4 shrink-0 text-foreground" />

                    <span className="text-base font-medium text-foreground">{feature}</span>
                </li>
            ))}
        </ul>
    </div>
);

export default PlanTierCard;
