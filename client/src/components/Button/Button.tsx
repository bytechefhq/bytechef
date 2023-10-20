import React from 'react';

type Props = Omit<React.ComponentPropsWithRef<'button'>, 'className'>;

const Button = React.forwardRef<HTMLButtonElement, Props>(
    ({onClick, children, ...props}, ref) => (
        <button
            ref={ref}
            {...props}
            className="inline-flex items-center rounded-md border border-transparent bg-gray-900 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
            onClick={onClick}
        >
            {children}
        </button>
    )
);

Button.displayName = 'Button';

export default Button;
