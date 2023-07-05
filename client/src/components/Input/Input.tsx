import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {ExclamationCircleIcon} from '@heroicons/react/24/outline';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {
    DetailedHTMLProps,
    InputHTMLAttributes,
    ReactNode,
    forwardRef,
} from 'react';
import {twMerge} from 'tailwind-merge';

type InputProps = {
    dataPills?: Array<string>;
    description?: string;
    fieldsetClassName?: string;
    error?: boolean;
    label?: string;
    labelClassName?: string;
    leadingIcon?: ReactNode;
    name: string;
    trailing?: ReactNode;
    type?: string;
} & DetailedHTMLProps<InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>;

const Input = forwardRef<HTMLInputElement, InputProps>(
    (
        {
            className,
            dataPills,
            description,
            disabled,
            error,
            fieldsetClassName,
            id,
            label,
            labelClassName,
            leadingIcon,
            name,
            required,
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
                            'block text-sm font-medium capitalize text-gray-700',
                            description && 'mr-1',
                            labelClassName
                        )}
                    >
                        {label}
                    </label>

                    {required && <span className="pr-1 text-red-500">*</span>}

                    {description && (
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger>
                                    <QuestionMarkCircledIcon />
                                </TooltipTrigger>

                                <TooltipContent>{description}</TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    )}
                </div>
            )}

            <div
                className={twMerge([
                    label && type !== 'hidden' && 'mt-1',
                    leadingIcon && 'relative',
                ])}
                title={title}
            >
                <div
                    className={twMerge(
                        trailing &&
                            'flex flex-grow items-stretch focus-within:z-10',
                        leadingIcon &&
                            'relative rounded-md border border-gray-300',
                        type === 'hidden' && 'border-0'
                    )}
                >
                    {type !== 'hidden' && leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-2">
                            {leadingIcon}
                        </div>
                    )}

                    {!!dataPills?.length && (
                        <div
                            className={twMerge(
                                'absolute left-0 top-1/2 -translate-y-1/2 space-x-2',
                                leadingIcon && 'left-10'
                            )}
                        >
                            {dataPills.map((pill) => (
                                <span
                                    key={pill}
                                    className="inline-flex rounded-full border bg-gray-100 px-2 py-1 text-xs"
                                >
                                    {pill}
                                </span>
                            ))}
                        </div>
                    )}

                    <input
                        className={twMerge(
                            'block w-full rounded-md border p-2 focus:outline-none focus:ring-1 sm:text-sm',
                            error
                                ? 'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500'
                                : 'border-gray-300 text-gray-900 placeholder:text-gray-400 focus:border-transparent focus:ring focus:ring-blue-500',
                            disabled &&
                                'cursor-not-allowed bg-gray-100 text-gray-500',
                            leadingIcon && 'border-0 pl-10',
                            trailing &&
                                'rounded-none rounded-l-md bg-gray-50 text-gray-700 outline-0 focus:border-gray-300 focus:ring-0',
                            className
                        )}
                        disabled={disabled}
                        id={id || name}
                        name={name}
                        ref={ref}
                        type={type}
                        required={required}
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
