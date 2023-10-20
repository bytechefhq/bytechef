import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import React from 'react';
import {twMerge} from 'tailwind-merge';

export interface ISelectOption {
    label: string;
    value: string;
}

type NativeSelectProps = {
    className?: string;
    name: string;
    fieldsetClassName?: string;
    error?: boolean;
    label?: string;
    options: ISelectOption[];
    placeholder?: string;
} & React.DetailedHTMLProps<
    React.SelectHTMLAttributes<HTMLSelectElement>,
    HTMLSelectElement
>;

const NativeSelect = React.forwardRef<HTMLSelectElement, NativeSelectProps>(
    (
        {className, fieldsetClassName, label, name, error, options, ...props},
        ref
    ): JSX.Element => (
        <fieldset className={twMerge(label ? 'mb-3' : '', fieldsetClassName)}>
            {label && (
                <label
                    htmlFor={name}
                    className="block text-sm font-medium text-gray-700 dark:text-gray-400"
                >
                    {label}
                </label>
            )}

            <div className={twMerge([label && 'mt-1'])}>
                <select
                    className={twMerge(
                        'block w-full rounded-md border-gray-300 text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 dark:focus:ring-sky-500 sm:text-sm',
                        className
                    )}
                    id={name}
                    name={name}
                    ref={ref}
                    {...props}
                >
                    {options?.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>

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

NativeSelect.displayName = 'NativeSelect';

export default NativeSelect;
