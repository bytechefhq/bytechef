import {Label} from '@/components/ui/label';
import {Textarea, TextareaProps} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExclamationTriangleIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {ChangeEvent, ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyTextAreaProps extends TextareaProps {
    description?: string;
    error: boolean;
    label?: string;
    leadingIcon?: ReactNode;
    name: string;
    onChange?: (event: ChangeEvent<HTMLTextAreaElement>) => void;
    value?: string;
}

const PropertyTextArea = forwardRef<HTMLTextAreaElement, PropertyTextAreaProps>(
    (
        {className, description, disabled, error, label, leadingIcon, name, onChange, required, title, value, ...props},
        ref
    ) => (
        <fieldset className="mb-3 w-full">
            {label && (
                <div className="flex items-center">
                    <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
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

            <div className={twMerge([label && 'mt-1', leadingIcon && 'relative'])} title={title}>
                <div className={twMerge(leadingIcon && 'relative rounded-md')}>
                    {leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-gray-300 bg-gray-100 px-3">
                            {leadingIcon}
                        </div>
                    )}

                    <Textarea
                        className={twMerge(
                            error &&
                                'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500',
                            disabled && 'bg-gray-100 text-gray-500',
                            leadingIcon && 'pl-12 leading-relaxed',
                            className
                        )}
                        id={name}
                        name={name}
                        onChange={onChange}
                        ref={ref}
                        rows={5}
                        value={value}
                        {...props}
                    />

                    {error && (
                        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <ExclamationTriangleIcon aria-hidden="true" className="size-5 text-red-500" />
                        </div>
                    )}
                </div>
            </div>

            {error && (
                <p className="mt-2 text-sm text-destructive" id={`${name}-error`} role="alert">
                    This field is required
                </p>
            )}
        </fieldset>
    )
);

PropertyTextArea.displayName = 'PropertyTextArea';

export default PropertyTextArea;
