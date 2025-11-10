import {Badge as ShadcnBadge, BadgeProps as ShadcnBadgeProps} from '@/components/ui/badge';
import React from 'react';
import {twMerge} from 'tailwind-merge';

interface BaseBadgeProps extends Omit<ShadcnBadgeProps, 'variant'> {
    className?: string;
    styleType?: StyleType;
    weight?: WeightType;
}

interface TextBadgeProps extends BaseBadgeProps {
    label: string;
    children?: never;
    icon?: never;
    'aria-label'?: never;
}

interface IconBadgeProps extends BaseBadgeProps {
    children?: never;
    label?: never;
    icon: React.ReactElement;
    'aria-label': string;
}

interface IconTextBadgeProps extends BaseBadgeProps {
    label: string;
    children?: never;
    icon: React.ReactElement;
    'aria-label'?: never;
}

interface CustomBadgeProps extends BaseBadgeProps {
    children: React.ReactNode;
    label?: never;
    icon?: React.ReactElement;
    'aria-label'?: string;
}

type BadgePropsType = TextBadgeProps | IconBadgeProps | IconTextBadgeProps | CustomBadgeProps;

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

const basicStyles = 'justify-center gap-1 shadow-none transition-none hover:bg-opacity-0 [&_svg]:size-3';

const weightClass: Record<WeightType, string> = {
    regular: 'font-normal',
    semibold: 'font-semibold',
};

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
    label,
    styleType = 'primary-filled',
    weight = 'regular',
    ...props
}: BadgePropsType) => {
    const size = (label ?? children) !== undefined ? 'px-2 py-0.5' : 'size-5 p-0.5';

    const badgeClasses = twMerge(basicStyles, size, weightClass[weight], variants[styleType], className);

    return (
        <ShadcnBadge className={badgeClasses} {...props}>
            {icon}

            {label ?? children}
        </ShadcnBadge>
    );
};

Badge.displayName = 'Badge';

export default Badge;
