import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import cx from 'classnames';
import {twMerge} from 'tailwind-merge';
import {forwardRef} from 'react';

type InputProps = {
    name: string;
    fieldsetClassName?: string;
    error?: boolean;
    label?: string;
    labelClassName?: string;
    type?: string;
} & React.DetailedHTMLProps<
    React.InputHTMLAttributes<HTMLInputElement>,
    HTMLInputElement
>;

const Input = forwardRef<HTMLInputElement, InputProps>(
    (
        {
            className,
            fieldsetClassName,
            label,
            labelClassName,
            name,
            type = 'text',
            error,
            ...props
        },
        ref
    ) => (
        <fieldset className={twMerge(cx('mb-3', fieldsetClassName))}>
            {label && (
                <label
                    htmlFor={name}
                    className={cx(
                        'block text-sm font-medium text-gray-700 dark:text-gray-400',
                        labelClassName
                    )}
                >
                    {label}
                </label>
            )}

            <div
                className={cx([
                    label && 'mt-1',
                    error ? 'relative rounded-md shadow-sm' : null,
                ])}
            >
                <input
                    className={cx([
                        'block w-full rounded-md border focus:outline-none focus:ring-1 dark:bg-gray-800',
                        error
                            ? 'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500 dark:text-rose-500 sm:text-sm'
                            : 'border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 dark:border-gray-700 dark:text-white dark:focus:border-sky-500 dark:focus:ring-sky-500 sm:text-sm',
                        className,
                    ])}
                    id={name}
                    name={name}
                    ref={ref}
                    type={type}
                    {...props}
                />

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationCircleIcon
                            className="h-5 w-5 text-rose-500"
                            aria-hidden="true"
                        />
                    </div>
                )}
            </div>

            {error && (
                <p
                    role="alert"
                    className="mt-2 text-sm text-rose-600"
                    id={`${name}-error`}
                >
                    This field is required
                </p>
            )}
        </fieldset>
    )
);

Input.displayName = 'Input';

export default Input;
