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
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    light: 'btn-light',
    lightBorder: 'btn-light-border',
    unstyled: 'btn-unstyled',
};

const sizes: Record<Size, string> = {
    small: 'btn-small',
    large: 'btn-large',
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
    (
        {
            children,
            className,
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
                link && 'bg-transparent text-blue-600 hover:underline',
                size && sizes[size],
                className
            )}
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
