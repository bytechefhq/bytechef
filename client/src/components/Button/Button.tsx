/* eslint-disable sort-keys */
import {Button as ShadcnButton, ButtonProps as ShadcnButtonProps} from '@/components/ui/button';
import * as React from 'react';
import {twMerge} from 'tailwind-merge';

type ButtonVariantType =
    | 'default'
    | 'secondary'
    | 'destructive'
    | 'destructiveGhost'
    | 'destructiveOutline'
    | 'outline'
    | 'ghost'
    | 'link';

type TextButtonSizeType = 'lg' | 'default' | 'sm' | 'xs' | 'xxs';
type IconButtonSizeType = 'icon' | 'iconSm' | 'iconXs' | 'iconXxs';

const ICON_SIZES: IconButtonSizeType[] = ['icon', 'iconSm', 'iconXs', 'iconXxs'];

const isIconSizeButton = (size?: TextButtonSizeType | IconButtonSizeType): size is IconButtonSizeType =>
    !!size && ICON_SIZES.includes(size as IconButtonSizeType);

const baseStyles = 'shadow-none hover:shadow-none active:shadow-none [&_svg]:size-4';

const textButtonSize: Record<TextButtonSizeType, string> = {
    lg: 'h-10 px-8 py-2',
    default: 'h-9 px-4 py-2',
    sm: 'h-8 px-3 py-2 text-xs',
    xs: 'h-6 px-2 py-1 text-xs gap-1',
    xxs: 'h-5 px-1.5 py-0.5 text-xs gap-1 [&_svg]:size-3',
};

const iconButtonSize: Record<IconButtonSizeType, string> = {
    icon: 'size-9 p-2.5',
    iconSm: 'size-8 p-2',
    iconXs: 'size-6 p-1',
    iconXxs: 'size-5 p-1 [&_svg]:size-3',
};

const buttonVariant: Record<ButtonVariantType, string> = {
    default:
        'bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-active text-content-onsurface-primary',
    secondary:
        'bg-surface-neutral-secondary hover:bg-surface-neutral-secondary-hover active:bg-surface-brand-secondary text-content-neutral-primary active:text-content-brand-primary',
    destructive:
        'bg-surface-destructive-primary hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active text-content-onsurface-primary',
    destructiveGhost:
        'bg-transparent hover:bg-surface-destructive-secondary-hover opacity-50 text-content-destructive-primary active:bg-surface-destructive-secondary-active hover:opacity-100',
    destructiveOutline:
        'bg-transparent border border-stroke-onsurface-primary/70 text-content-onsurface-primary hover:border-stroke-onsurface-primary/100 hover:bg-transparent',
    outline:
        'bg-surface-neutral-primary border border-stroke-neutral-secondary text-content-neutral-primary hover:bg-surface-neutral-primary-hover hover:border-stroke-neutral-secondary hover:text-content-neutral-primary active:bg-surface-brand-secondary active:border-stroke-brand-secondary active:text-content-brand-primary',
    ghost: 'bg-transparent text-content-neutral-primary hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary active:bg-surface-brand-secondary active:text-content-brand-primary',
    link: 'bg-transparent hover:bg-transparent active:bg-transparent text-content-neutral-primary hover:text-content-neutral-primary active:text-content-brand-primary hover:underline active:underline',
};

interface ButtonProps extends Omit<ShadcnButtonProps, 'size' | 'variant'> {
    icon?: React.ReactElement;
    className?: string;
    variant?: ButtonVariantType;
}

interface ButtonWithLabelProps extends ButtonProps {
    children?: never;
    label: string;
    size?: TextButtonSizeType;
}

interface ButtonWithCustomContentProps extends ButtonProps {
    children: React.ReactNode;
    label?: never;
    size?: TextButtonSizeType;
}

interface IconButtonProps extends ButtonProps {
    icon: React.ReactElement;
    label?: never;
    size?: IconButtonSizeType;
    children?: never;
}

type ButtonPropsType = ButtonWithLabelProps | ButtonWithCustomContentProps | IconButtonProps;

const Button = React.forwardRef<HTMLButtonElement, ButtonPropsType>(
    ({children, className, icon, label, size = 'default', variant = 'default', ...props}, ref) => {
        const content = isIconSizeButton(size) ? null : (label ?? children);
        const sizeClass = isIconSizeButton(size) ? iconButtonSize[size] : textButtonSize[size ?? 'default'];

        return (
            <ShadcnButton
                className={twMerge(baseStyles, sizeClass, buttonVariant[variant], className)}
                ref={ref}
                {...props}
            >
                {icon}

                {content}
            </ShadcnButton>
        );
    }
);

Button.displayName = 'Button';

export default Button;
