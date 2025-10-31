import {Badge as ShadcnBadge, BadgeProps as ShadcnBadgeProps} from '@/components/ui/badge';
import React from 'react';
import {twMerge} from 'tailwind-merge';

interface BaseBadgeProps extends Omit<ShadcnBadgeProps, 'variant'> {
    className?: string;
    styleType?: StyleType;
    weight?: WeightType;
}

interface TextBadgeProps extends BaseBadgeProps {
    children: string;
    icon?: never;
    'aria-label'?: never;
}

interface IconBadgeProps extends BaseBadgeProps {
    children?: never;
    icon: React.ReactElement;
    'aria-label': string;
}

interface IconTextBadgeProps extends BaseBadgeProps {
    children: string;
    icon: React.ReactElement;
    'aria-label'?: never;
}

type BadgePropsType = TextBadgeProps | IconBadgeProps | IconTextBadgeProps;

type StyleType =
    | 'primary-filled'
    | 'primary-outline'
    | 'secondary-filled'
    | 'secondary-outline'
    | 'outline-outline'
    | 'success-filled'
    | 'success-outline'
    | 'warning-filled'
    | 'warning-outline'
    | 'destructive-filled'
    | 'destructive-outline';
type WeightType = 'regular' | 'semibold';

const basicStyles = `
  justify-center gap-1 shadow-none transition-none hover:bg-opacity-0
  [&_svg]:size-3
`;
// Using `hover:bg-opacity-0` (or any other opacity) disables hover color change without altering the badge's default color.
// Our badge background uses CSS variables while Tailwind's opacity utilities only affect colors defined as rgb()/hsl() with `var(--tw-bg-opacity)`

const size = (hasTextContent: boolean) => (hasTextContent ? 'px-2 py-0.5' : 'size-5 p-0.5');

const weightClass = (weight: WeightType) => (weight === 'semibold' ? 'font-semibold' : 'font-normal');

const variants: Record<StyleType, string> = {
    'primary-filled': `
        bg-surface-brand-primary
        text-content-onsurface-primary
        `,
    'primary-outline': `
        border-stroke-brand-primary
        bg-surface-brand-secondary
        text-content-brand-primary
       `,
    'secondary-filled': `
        bg-surface-neutral-secondary
        text-content-neutral-primary
        `,
    'secondary-outline': `
        border-stroke-neutral-secondary
        bg-surface-neutral-secondary
        text-content-neutral-primary
       `,
    // eslint-disable-next-line sort-keys
    'outline-outline': `
        border-stroke-neutral-secondary
        bg-surface-neutral-primary
        text-content-neutral-primary
        `,
    'success-filled': `
        bg-surface-success-primary
        text-content-onsurface-primary
      `,
    'success-outline': `
        border-stroke-success-primary
        bg-surface-success-secondary
        text-content-success-primary
        `,
    'warning-filled': `
        bg-surface-warning-secondary
        text-content-warning-primary
      `,
    'warning-outline': `
        border-stroke-warning-primary
        bg-surface-warning-secondary
        text-content-warning-primary
        `,
    // eslint-disable-next-line sort-keys
    'destructive-filled': `
        bg-surface-destructive-primary
        text-content-onsurface-primary
       `,
    'destructive-outline': `
        border-stroke-destructive-primary
        bg-surface-destructive-secondary
        text-content-destructive-primary
       `,
};

const Badge = ({
    children,
    className,
    icon,
    styleType = 'primary-filled',
    weight = 'regular',
    ...props
}: BadgePropsType) => {
    const hasTextContent = typeof children !== 'undefined' && children !== null;
    const spacingClass = size(hasTextContent);
    const weightClassValue = weightClass(weight);
    const variantClass = variants[styleType];

    const badgeClasses = twMerge(basicStyles, spacingClass, weightClassValue, variantClass, className);

    const safeProps = {...(props as Record<string, unknown>)} as ShadcnBadgeProps & {
        contentType?: unknown;
    };
    if ('contentType' in safeProps) {
        delete (safeProps as unknown as {contentType?: unknown}).contentType;
    }

    return (
        <ShadcnBadge className={badgeClasses} {...(safeProps as ShadcnBadgeProps)}>
            {icon}

            {children}
        </ShadcnBadge>
    );
};

Badge.displayName = 'Badge';

export default Badge;
