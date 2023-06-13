import {ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

import './Button.css';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    displayType?: DisplayType;
    icon?: ReactNode;
    iconPosition?: 'left' | 'right';
    label?: string;
    link?: boolean;
    onClick?: () => void;
    size?: Size;
}

type DisplayType =
    | 'danger'
    | 'icon'
    | 'primary'
    | 'secondary'
    | 'light'
    | 'lightBorder'
    | 'unstyled';

type Size = 'small' | 'large';

const displayTypes: Record<DisplayType, string> = {
    danger: 'btn-danger',
    icon: 'btn-icon',
    light: 'btn-light',
    lightBorder: 'btn-light-border',
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    unstyled: 'btn-unstyled',
};

const sizes: Record<Size, string> = {
    large: 'btn-large',
    small: 'btn-small',
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
    (
        {
            children,
            className,
            disabled,
            displayType = 'primary',
            icon,
            iconPosition,
            label,
            link,
            onClick,
            size,
            ...props
        },
        ref
    ) => (
        <button
            className={twMerge(
                'btn',
                displayTypes[displayType],
                disabled && 'cursor-not-allowed bg-gray-200 text-gray-500',
                link && 'bg-transparent text-blue-600 hover:underline',
                size && sizes[size],
                className
            )}
            disabled={disabled}
            onClick={onClick}
            ref={ref}
            type="button"
            {...props}
        >
            <>
                {iconPosition === 'left' && !!icon && (
                    <span className="mr-2">{icon}</span>
                )}

                {!iconPosition && !label && icon}

                {label}

                {children}

                {iconPosition === 'right' && !!icon && (
                    <span className="ml-2">{icon}</span>
                )}
            </>
        </button>
    )
);

Button.displayName = 'Button';

export default Button;
