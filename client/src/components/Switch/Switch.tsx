import {Switch as ShadcnSwitch} from '@/components/ui/switch';
import {type ComponentPropsWithoutRef, type ComponentRef, type ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

interface BaseSwitchProps extends Omit<ComponentPropsWithoutRef<typeof ShadcnSwitch>, 'checked' | 'onCheckedChange'> {
    variant?: VariantType;
    checked?: boolean;
    onCheckedChange?: (checked: boolean) => void;
}

interface LabeledSwitchProps extends BaseSwitchProps {
    label: ReactNode;
    description?: ReactNode;
    alignment?: AlignmentType;
}

interface PlainSwitchProps extends BaseSwitchProps {
    label?: never;
    description?: never;
    alignment?: never;
}

type VariantType = 'default' | 'box' | 'small';
type AlignmentType = 'start' | 'end';

type SwitchPropsType = LabeledSwitchProps | PlainSwitchProps;

const variantConfig: Record<VariantType, {track: string; thumbOverrides: string}> = {
    default: {
        thumbOverrides: '[&>span]:size-4 [&>span]:data-[state=checked]:translate-x-4',
        track: 'h-5 w-9 px-0.5 rounded-full border-0',
    },
    // eslint-disable-next-line sort-keys
    box: {
        thumbOverrides: '[&>span]:size-4 [&>span]:data-[state=checked]:translate-x-4',
        track: 'h-5 w-9 px-0.5 rounded-full border-0',
    },
    small: {
        thumbOverrides: '[&>span]:size-3 [&>span]:data-[state=checked]:translate-x-3',
        track: 'h-[14px] w-[26px] px-[1px] rounded-[7px] border-0',
    },
};

const wrapperStyles: Record<VariantType, string> = {
    default: 'flex w-fit items-start gap-2 cursor-pointer',
    // eslint-disable-next-line sort-keys
    box: 'flex w-fit items-start gap-2 rounded-lg border border-stroke-neutral-secondary p-3 cursor-pointer',
    small: 'flex w-fit max-w-[200px] items-center gap-1 cursor-pointer',
};

interface TextBlockProps {
    label: ReactNode;
    description?: ReactNode;
    variant: VariantType;
}

function TextBlock({description, label, variant}: TextBlockProps) {
    const isSmall = variant === 'small';

    return (
        <div className="flex flex-col gap-0.5">
            <span
                className={twMerge(
                    'font-medium text-content-neutral-primary',
                    isSmall ? 'text-xs leading-4' : 'text-sm leading-5'
                )}
            >
                {label}
            </span>

            {!isSmall && description && (
                <span className="text-sm font-normal leading-5 text-content-neutral-secondary">{description}</span>
            )}
        </div>
    );
}

const Switch = forwardRef<ComponentRef<typeof ShadcnSwitch>, SwitchPropsType>(
    (
        {
            alignment = 'start',
            checked = false,
            className,
            description,
            id,
            label,
            onCheckedChange,
            variant = 'default',
            ...props
        },
        ref
    ) => {
        const config = variantConfig[variant];

        const switchClasses = twMerge(
            config.track,
            config.thumbOverrides,
            'shadow-none [&_.shadow-lg]:shadow-none',
            'data-[state=checked]:bg-surface-brand-primary data-[state=unchecked]:bg-surface-neutral-tertiary',
            'focus-visible:ring-2 focus-visible:ring-stroke-brand-focus focus-visible:ring-offset-0',
            '[&>span]:bg-surface-neutral-primary',
            className
        );

        const ariaLabel = props['aria-label'] ?? (typeof label === 'string' ? label : 'switch');

        const switchElement = (
            <ShadcnSwitch
                aria-label={ariaLabel}
                checked={checked}
                className={switchClasses}
                id={id}
                onCheckedChange={onCheckedChange}
                ref={ref}
                {...props}
            />
        );

        if (!label) {
            return switchElement;
        }

        const isBoxVariant = variant === 'box';

        const wrapperClasses = twMerge(
            wrapperStyles[variant],
            isBoxVariant && checked ? 'bg-surface-brand-secondary border-stroke-brand-secondary' : ''
        );

        if (isBoxVariant) {
            return (
                <label className={wrapperClasses} data-testid="switch-wrapper">
                    {alignment === 'start' ? (
                        <>
                            {switchElement}
                            <TextBlock description={description} label={label} variant={variant} />
                        </>
                    ) : (
                        <>
                            <TextBlock description={description} label={label} variant={variant} />
                            {switchElement}
                        </>
                    )}
                </label>
            );
        }

        const contentClasses = twMerge('flex items-start gap-2', alignment === 'start' && 'flex-row-reverse');

        return (
            <label className={wrapperClasses} data-testid="switch-wrapper">
                <div className={contentClasses}>
                    <TextBlock description={description} label={label} variant={variant} />

                    {switchElement}
                </div>
            </label>
        );
    }
);

Switch.displayName = 'Switch';

export default Switch;
export type {SwitchPropsType as SwitchProps};
