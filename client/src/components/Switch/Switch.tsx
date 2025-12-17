import {Switch as SwitchPrimitives} from 'radix-ui';
import * as React from 'react';
import {twMerge} from 'tailwind-merge';

interface BaseSwitchProps extends Omit<
    React.ComponentPropsWithoutRef<typeof SwitchPrimitives.Root>,
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

const baseTrackClasses =
    'peer inline-flex shrink-0 cursor-pointer items-center rounded-full transition-colors ' +
    'focus-visible:outline-none focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-50';

const baseThumbClasses =
    'pointer-events-none block rounded-full ring-0 transition-transform shadow-[0_0_8px_rgba(0,0,0,0.15)]';

const variantConfig: Record<VariantType, {track: string; thumb: string; translate: string}> = {
    default: {
        thumb: 'w-4 h-4',
        track: 'h-5 w-9 px-0.5',
        translate: 'translate-x-4',
    },
    // eslint-disable-next-line sort-keys
    box: {
        thumb: 'w-4 h-4',
        track: 'h-5 w-9 px-0.5',
        translate: 'translate-x-4',
    },
    small: {
        thumb: 'w-3 h-3',
        track: 'h-[14px] w-[26px] px-[1px]',
        translate: '',
    },
};

const wrapperStyles: Record<VariantType, string> = {
    default: 'flex w-[228px] items-start gap-2',
    // eslint-disable-next-line sort-keys
    box: 'w-fit rounded-lg border border-stroke-neutral-secondary p-3',
    small: 'flex w-[98px] items-center gap-1',
};

function TextBlock({
    description,
    label,
    variant,
}: {
    label: React.ReactNode;
    description?: React.ReactNode;
    variant: VariantType;
}) {
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

const Switch = React.forwardRef<React.ElementRef<typeof SwitchPrimitives.Root>, SwitchPropsType>(
    ({alignment = 'start', checked = false, className, description, id, label, variant = 'default', ...props}, ref) => {
        const config = variantConfig[variant] ?? variantConfig.default;

        const trackClasses = twMerge(
            baseTrackClasses,
            config.track,
            checked ? 'bg-surface-brand-primary' : 'bg-surface-neutral-secondary',
            'focus-visible:ring-stroke-brand-focus focus-visible:ring-offset-0',
            variant === 'small' && checked ? 'justify-end' : '',
            className
        );

        const thumbClasses = twMerge(
            baseThumbClasses,
            config.thumb,
            'bg-surface-neutral-primary',
            'my-auto',
            variant === 'small' ? 'self-center' : '',
            variant === 'small' ? '' : checked ? config.translate : 'translate-x-0'
        );

        const ariaLabel = props['aria-label'] ?? (typeof label === 'string' ? label : 'switch');

        const switchElement = (
            <SwitchPrimitives.Root
                aria-label={ariaLabel}
                checked={checked}
                className={trackClasses}
                id={id}
                ref={ref}
                role="switch"
                {...props}
            >
                <SwitchPrimitives.Thumb className={thumbClasses} />
            </SwitchPrimitives.Root>
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
                    <div className="flex items-start gap-2">
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
                </div>
            );
        }

        const contentClasses = twMerge('flex items-start gap-2', alignment === 'start' ? 'flex-row-reverse' : '');

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
