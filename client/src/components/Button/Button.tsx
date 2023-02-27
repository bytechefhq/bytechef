import {forwardRef, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';
import './button.css';

type Size = 'small' | 'large';
type DisplayType =
    | 'danger'
    | 'icon'
    | 'primary'
    | 'secondary'
    | 'light'
    | 'lightBorder'
    | 'unstyled';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    displayType?: DisplayType;
    icon?: ReactNode;
    iconPosition?: 'left' | 'right';
    label?: string;
    onClick?: () => void;
    size?: Size;
}

const sizes: Record<Size, string> = {
    small: 'btn-small',
    large: 'btn-large',
};

const displayTypes: Record<DisplayType, string> = {
    danger: 'btn-danger',
    icon: 'btn-icon',
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    light: 'btn-light',
    lightBorder: 'btn-light-border',
    unstyled: 'btn-unstyled',
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
