import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExclamationTriangleIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {ChangeEvent, DetailedHTMLProps, InputHTMLAttributes, ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

type InputProps = {
    description?: string;
    fieldsetClassName?: string;
    error?: boolean;
    label?: string;
    labelClassName?: string;
    leadingIcon?: ReactNode;
    name: string;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    trailing?: ReactNode;
    type?: string;
    value?: string;
} & DetailedHTMLProps<InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>;

const Input = forwardRef<HTMLInputElement, InputProps>(
    (
        {
            className,
            description,
            disabled,
            error,
            fieldsetClassName,
            id,
            label,
            labelClassName,
            leadingIcon,
            name,
            onChange,
            required,
            title,
            trailing,
            type = 'text',
            value,
            ...props
        },
        ref
    ) => (
        <fieldset className={twMerge(type !== 'hidden' && 'mb-3', fieldsetClassName)}>
            {label && type !== 'hidden' && (
                <div className="flex items-center">
                    <label
                        className={twMerge(
                            'block text-sm font-medium capitalize text-gray-700',
                            description && 'mr-1',
                            labelClassName
                        )}
                        htmlFor={name}
                    >
                        {label}
                    </label>

                    {required && <span className="px-1 leading-3 text-red-500">*</span>}

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <QuestionMarkCircledIcon />
                            </TooltipTrigger>

                            <TooltipContent>{description}</TooltipContent>
                        </Tooltip>
                    )}
                </div>
            )}

            <div className={twMerge([label && type !== 'hidden' && 'mt-1', leadingIcon && 'relative'])} title={title}>
                <div
                    className={twMerge(
                        trailing && 'flex flex-grow items-stretch focus-within:z-10',
                        leadingIcon && 'relative rounded-md border border-gray-300',
                        type === 'hidden' && 'border-0'
                    )}
                >
                    {type !== 'hidden' && leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-3">
                            {leadingIcon}
                        </div>
                    )}

                    <input
                        className={twMerge(
                            'block w-full rounded-md border p-2 focus:outline-none focus:ring-1 sm:text-sm',
                            error
                                ? 'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500'
                                : 'border-gray-300 placeholder:text-gray-400 focus:border-transparent focus:ring focus:ring-blue-500',
                            disabled && 'cursor-not-allowed bg-gray-100 text-gray-500',
                            leadingIcon && 'border-0 pl-12 leading-relaxed',
                            trailing &&
                                'rounded-none rounded-l-md bg-gray-50 text-gray-700 outline-0 focus:border-gray-300 focus:ring-0',
                            className
                        )}
                        disabled={disabled}
                        id={id || name}
                        name={name}
                        onChange={onChange}
                        ref={ref}
                        required={required}
                        type={type}
                        value={value}
                        {...props}
                    />

                    {trailing}
                </div>

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationTriangleIcon aria-hidden="true" className="h-5 w-5 text-rose-500" />
                    </div>
                )}
            </div>

            {error && (
                <p className="mt-2 text-sm text-rose-600" id={`${name}-error`} role="alert">
                    This field is required
                </p>
            )}
        </fieldset>
    )
);

Input.displayName = 'Input';

export default Input;
