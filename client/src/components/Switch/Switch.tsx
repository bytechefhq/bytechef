import {Switch as ShadcnSwitch} from '@/components/ui/switch';
import * as React from 'react';
import {twMerge} from 'tailwind-merge';

interface BaseSwitchProps extends Omit<
    React.ComponentPropsWithoutRef<typeof ShadcnSwitch>,
    'checked' | 'onCheckedChange'
> {
    variant?: VariantType;
    checked?: boolean;
    onCheckedChange?: (checked: boolean) => void;
}

interface LabeledSwitchProps extends BaseSwitchProps {
    label: React.ReactNode;
    description?: React.ReactNode;
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
    default: 'flex w-[228px] items-start gap-2',
    // eslint-disable-next-line sort-keys
    box: 'flex w-fit items-start gap-2 rounded-lg border border-stroke-neutral-secondary p-3',
    small: 'flex w-[98px] items-center gap-1',
};

interface TextBlockProps {
    label: React.ReactNode;
    description?: React.ReactNode;
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

const Switch = React.forwardRef<React.ElementRef<typeof ShadcnSwitch>, SwitchPropsType>(
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
            'data-[state=checked]:bg-surface-brand-primary data-[state=unchecked]:bg-surface-neutral-secondary',
            'focus-visible:ring-2 focus-visible:ring-stroke-brand-focus focus-visible:ring-offset-0',
            '[&>span]:bg-surface-neutral-primary',
            '[&>span]:shadow-[0_0_8px_rgba(0,0,0,0.15)]',
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
                <div className={wrapperClasses} data-testid="switch-wrapper">
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
                </div>
            );
        }

        const contentClasses = twMerge('flex items-start gap-2', alignment === 'start' && 'flex-row-reverse');

        return (
            <div className={wrapperClasses} data-testid="switch-wrapper">
                <div className={contentClasses}>
                    <TextBlock description={description} label={label} variant={variant} />

                    {switchElement}
                </div>
            </div>
        );
    }
);

Switch.displayName = 'Switch';

export default Switch;
export type {SwitchPropsType as SwitchProps};
