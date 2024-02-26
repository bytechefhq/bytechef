import {Label} from '@/components/ui/label';
import {Textarea, TextareaProps} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExclamationTriangleIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyTextAreaProps extends TextareaProps {
    description?: string;
    error?: string | undefined;
    label?: string;
    labelClassName?: string;
    name: string;
}

const PropertyTextArea = forwardRef<HTMLTextAreaElement, PropertyTextAreaProps>(
    ({description, error, label, labelClassName, name, required, ...props}, ref) => (
        <fieldset className="mb-3 w-full">
            {label && (
                <div className="flex items-center">
                    <Label
                        className={twMerge(
                            'block text-sm font-medium capitalize text-gray-700',
                            description && 'mr-1',
                            labelClassName
                        )}
                        htmlFor={name}
                    >
                        {label}

                        {required && <span className="leading-3 text-red-500">*</span>}
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

            <div className={twMerge([label && 'mt-1'])}>
                <Textarea id={name} name={name} ref={ref} rows={5} {...props} />

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationTriangleIcon aria-hidden="true" className="size-5 text-red-500" />
                    </div>
                )}
            </div>

            {error && (
                <p className="mt-2 text-sm text-red-600" id={`${name}-error`} role="alert">
                    This field is required
                </p>
            )}
        </fieldset>
    )
);

PropertyTextArea.displayName = 'PropertyTextArea';

export default PropertyTextArea;
