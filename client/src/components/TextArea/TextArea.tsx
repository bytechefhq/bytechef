import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExclamationTriangleIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

type TextAreaProps = {
    description?: string;
    error?: string | undefined;
    fieldsetClassName?: string;
    label?: string;
    labelClassName?: string;
    name: string;
} & React.DetailedHTMLProps<React.TextareaHTMLAttributes<HTMLTextAreaElement>, HTMLTextAreaElement>;

const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(
    ({description, error, fieldsetClassName, label, labelClassName, name, required, ...props}, ref) => (
        <fieldset className={twMerge('mb-3', fieldsetClassName)}>
            {label && (
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

            <div className={twMerge([label && 'mt-1'])}>
                <textarea
                    className={twMerge([
                        'block w-full rounded-md border focus:outline-none focus:ring-1 sm:text-sm',
                        error
                            ? 'border-red-300 pr-10 text-red-900 placeholder-red-300 focus:border-red-500 focus:ring-red-500'
                            : 'border-gray-300 placeholder:text-gray-400 focus:border-transparent focus:ring focus:ring-blue-500',
                    ])}
                    id={name}
                    name={name}
                    ref={ref}
                    rows={5}
                    {...props}
                />

                {error && (
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                        <ExclamationTriangleIcon aria-hidden="true" className="h-5 w-5 text-red-500" />
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

TextArea.displayName = 'TextArea';

export default TextArea;
