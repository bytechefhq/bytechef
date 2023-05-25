import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import React, {ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

import Tooltip from '../Tooltip/Tooltip';

type InputProps = {
    description?: string;
    fieldsetClassName?: string;
    error?: boolean;
    label?: string;
    labelClassName?: string;
    leadingIcon?: ReactNode;
    name: string;
    trailing?: ReactNode;
    type?: string;
} & React.DetailedHTMLProps<
    React.InputHTMLAttributes<HTMLInputElement>,
    HTMLInputElement
>;

const Input = forwardRef<HTMLInputElement, InputProps>(
    (
        {
            className,
            description,
            disabled,
            error,
            fieldsetClassName,
            label,
            labelClassName,
            leadingIcon,
            name,
            title,
            trailing,
            type = 'text',
            ...props
        },
        ref
    ) => (
        <fieldset
            className={twMerge(type !== 'hidden' && 'mb-3', fieldsetClassName)}
        >
            {label && type !== 'hidden' && (
                <div className="flex items-center">
                    <label
                        htmlFor={name}
                        className={twMerge(
                            'block text-sm font-medium capitalize text-gray-700 dark:text-gray-400',
                            description && 'mr-1',
                            labelClassName
                        )}
                    >
                        {label}
                    </label>

                    {description && (
                        <Tooltip text={description}>
                            <QuestionMarkCircledIcon />
                        </Tooltip>
                    )}
                </div>
            )}

            <div
                className={twMerge([
                    'relative',
                    label && type !== 'hidden' && 'mt-1',
                ])}
                title={title}
            >
                <div
                    className={twMerge(
                        'relative',
                        trailing &&
                            'flex flex-grow items-stretch focus-within:z-10',
                        leadingIcon && 'rounded-md border border-gray-300',
                        type === 'hidden' && 'border-0'
                    )}
                >
                    {type !== 'hidden' && leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-2">
                            {leadingIcon}
                        </div>
                    )}

                    <input
                        className={twMerge(
                            'block w-full rounded-md border focus:outline-none focus:ring-1 dark:bg-gray-800 sm:text-sm',
                            error
                                ? 'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500 dark:text-rose-500'
                                : 'border-gray-300 text-gray-900 placeholder:text-gray-400 focus:border-transparent focus:ring focus:ring-blue-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:placeholder:text-gray-600 dark:focus:ring-sky-500',
                            disabled &&
                                'cursor-not-allowed bg-gray-100 text-gray-500',
                            leadingIcon && 'border-0 pl-10',
                            trailing &&
                                'rounded-none rounded-l-md bg-gray-50 text-gray-700 outline-0 focus:border-gray-300 focus:ring-0',
                            className
                        )}
                        disabled={disabled}
                        id={name}
                        name={name}
                        ref={ref}
                        type={type}
                        {...props}
                    />

                    {trailing}
                </div>

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
