import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExclamationTriangleIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {ChangeEvent, InputHTMLAttributes, ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyInputProps extends InputHTMLAttributes<HTMLInputElement> {
    description?: string;
    error?: boolean;
    errorMessage?: string;
    key?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name: string;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    type?: string;
    value?: string;
}

const PropertyInput = forwardRef<HTMLInputElement, PropertyInputProps>(
    (
        {
            className,
            description,
            disabled,
            error,
            errorMessage,
            id,
            key,
            label,
            leadingIcon,
            name,
            onChange,
            required,
            title,
            type = 'text',
            value,
            ...props
        },
        ref
    ) => (
        <fieldset className="w-full space-y-2">
            {label && type !== 'hidden' && (
                <div className="flex items-center">
                    <Label className={twMerge(description && 'mr-1')} htmlFor={name}>
                        {label}

                        {required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                    </Label>

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
                <div className={twMerge(leadingIcon && 'relative rounded-md', type === 'hidden' && 'border-0')}>
                    {type !== 'hidden' && leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-gray-300 bg-gray-100 px-3">
                            {leadingIcon}
                        </div>
                    )}

                    <Input
                        className={twMerge(
                            error &&
                                'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500',
                            disabled && 'bg-gray-100 text-gray-500',
                            leadingIcon && 'pl-12 leading-relaxed',
                            className
                        )}
                        disabled={disabled}
                        id={id || name}
                        key={key}
                        name={name}
                        onChange={onChange}
                        ref={ref}
                        required={required}
                        type={type}
                        value={value}
                        {...props}
                    />
                </div>

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationTriangleIcon aria-hidden="true" className="size-5 text-rose-500" />
                    </div>
                )}
            </div>

            {error && (
                <p className="mt-2 text-sm text-rose-600" id={`${name}-error`} role="alert">
                    {errorMessage || 'This field is required.'}
                </p>
            )}
        </fieldset>
    )
);

PropertyInput.displayName = 'PropertyInput';

export default PropertyInput;
