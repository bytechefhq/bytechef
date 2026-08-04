import {Button as ShadcnButton, ButtonProps as ShadcnButtonProps} from '@/components/ui/button';
import React from 'react';
import {twMerge} from 'tailwind-merge';

interface BasicProps extends Omit<ShadcnButtonProps, 'size' | 'variant'> {
    className?: string;
    icon?: React.ReactElement;
    variant?: VariantType;
}

interface LabelButtonProps extends BasicProps {
    children?: never;
    label: string;
    size?: TextSizeType;
}

interface CustomContentButtonProps extends BasicProps {
    children: React.ReactNode;
    label?: never;
    size?: TextSizeType;
}

interface IconButtonProps extends BasicProps {
    children?: never;
    icon: React.ReactElement;
    label?: never;
    size?: IconSizeType;
}

type ButtonPropsType = LabelButtonProps | CustomContentButtonProps | IconButtonProps;

type VariantType =
    'default' | 'secondary' | 'destructive' | 'destructiveGhost' | 'destructiveOutline' | 'outline' | 'ghost' | 'link';

type TextSizeType = 'lg' | 'default' | 'sm' | 'xs' | 'xxs';
type IconSizeType = 'icon' | 'iconSm' | 'iconXs' | 'iconXxs';

const textButtonSizes: Record<TextSizeType, string> = {
    lg: 'h-10 px-8 py-2',
    // eslint-disable-next-line sort-keys
    default: 'h-9 px-4 py-2',
    sm: 'h-8 px-3 py-2 text-xs',
    xs: 'h-6 px-2 py-1 text-xs gap-1',
    xxs: 'h-5 px-1.5 py-0.5 text-xs gap-1 [&_svg]:size-3',
};

const iconButtonSizes: Record<IconSizeType, string> = {
    icon: 'size-9 p-2.5',
    iconSm: 'size-8 p-2',
    iconXs: 'size-6 p-1',
    iconXxs: 'size-5 p-1 [&_svg]:size-3',
};

/**
 * The shadcn `outline` and `ghost` button variants carry `dark:hover:bg-input/50` and
 * `dark:hover:bg-accent/50` respectively (see `src/components/ui/button.tsx`), which compile to
 * `:is(.dark *)`-scoped selectors at specificity (0,3,0) — higher than a call site's own
 * `hover:bg-*:hover` at (0,2,0). Because those call-site classes have no `dark:` prefix,
 * tailwind-merge does not see them as the same conflict group as the base's `dark:hover:` class,
 * so both compile and the base wins dark-mode hover regardless of what a call site asks for
 * (same defect `src/components/Select/Select.tsx` fixed for `SelectTrigger`). Adding
 * `dark:hover:bg-surface-neutral-primary-hover` here — same `dark:hover:` variant, same
 * `bg-color` group as the shadcn base classes — puts tailwind-merge's own conflict detection to
 * work instead: it dedupes against the base's `dark:hover:bg-input/50` /
 * `dark:hover:bg-accent/50` and drops it from the rendered class list entirely, so there is no
 * specificity contest left to win at runtime.
 */
const variants: Record<VariantType, string> = {
    default:
        'bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-active text-content-onsurface-primary',
    secondary:
        'bg-surface-neutral-secondary hover:bg-surface-neutral-secondary-hover active:bg-surface-brand-secondary text-content-neutral-primary active:text-content-brand-primary',
    // eslint-disable-next-line sort-keys
    destructive:
        'bg-surface-destructive-primary hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active text-content-onsurface-primary',
    destructiveGhost:
        'bg-transparent hover:bg-surface-destructive-secondary-hover opacity-50 text-content-destructive-primary active:bg-surface-destructive-secondary-active hover:opacity-100',
    destructiveOutline:
        'bg-transparent border border-stroke-onsurface-primary/70 text-content-onsurface-primary hover:border-stroke-onsurface-primary hover:bg-transparent',
    outline:
        'bg-surface-neutral-primary border border-stroke-neutral-secondary text-content-neutral-primary hover:bg-surface-neutral-primary-hover hover:border-stroke-neutral-secondary hover:text-content-neutral-primary dark:hover:bg-surface-neutral-primary-hover active:bg-surface-brand-secondary active:border-stroke-brand-secondary active:text-content-brand-primary',
    // eslint-disable-next-line sort-keys
    ghost: 'bg-transparent text-content-neutral-primary hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary dark:hover:bg-surface-neutral-primary-hover active:bg-surface-brand-secondary active:text-content-brand-primary',
    link: 'bg-transparent hover:bg-transparent active:bg-transparent text-content-neutral-primary hover:text-content-neutral-primary active:text-content-brand-primary hover:underline active:underline',
};

const basicStyles = 'shadow-none hover:shadow-none active:shadow-none [&_svg]:size-4';

const ICON_SIZES: IconSizeType[] = ['icon', 'iconSm', 'iconXs', 'iconXxs'];

const isIconSize = (size?: TextSizeType | IconSizeType): size is IconSizeType =>
    !!size && ICON_SIZES.includes(size as IconSizeType);

const HOVER_TRANSPARENT_CLASS = 'hover:bg-transparent';

/**
 * A call site's plain `hover:bg-transparent` (no `dark:` prefix) is a different tailwind-merge
 * conflict group from the `outline`/`ghost` variants' own `dark:hover:bg-surface-neutral-primary-hover`
 * (see the comment on `variants` above), so it cannot dedupe it away — the call site asks for no
 * hover at all, and gets one anyway in dark mode. Rather than repeat `dark:hover:bg-transparent` at
 * every such call site (WorkflowNode.tsx, SubflowBanner.tsx, and over a dozen others were found
 * carrying this exact shape), detect the resolved override here and mirror it with the `dark:`
 * variant so tailwind-merge's own conflict detection dedupes the base class out, the same mechanism
 * the variant-level fix above relies on. This only fires when the incoming className still
 * literally resolves to `hover:bg-transparent` — a call site that conditionally swaps it out for a
 * real hover color (e.g. ComponentsFilter.tsx's active-view highlight) no longer matches, so an
 * intentional highlighted state is left alone.
 */
const hasHoverTransparentOverride = (className?: string): boolean =>
    !!className?.split(/\s+/).includes(HOVER_TRANSPARENT_CLASS);

const Button = ({
    children,
    className,
    icon,
    label,
    size = 'default',
    variant = 'default',
    ...props
}: ButtonPropsType) => {
    const content = isIconSize(size) ? null : (label ?? children);
    const sizeClass = isIconSize(size) ? iconButtonSizes[size] : textButtonSizes[size ?? 'default'];

    return (
        <ShadcnButton
            className={twMerge(
                basicStyles,
                sizeClass,
                variants[variant],
                className,
                hasHoverTransparentOverride(className) && 'dark:hover:bg-transparent'
            )}
            {...props}
        >
            {icon}

            {content}
        </ShadcnButton>
    );
};

Button.displayName = 'Button';

export default Button;
export type {ButtonPropsType as ButtonProps};
