import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {CircleQuestionMarkIcon, TriangleAlertIcon} from 'lucide-react';
import {ChangeEvent, ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyTextAreaProps {
    className?: string;
    deletePropertyButton?: ReactNode;
    description?: string;
    disabled?: boolean;
    error: boolean;
    errorMessage?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name: string;
    onChange?: (event: ChangeEvent<HTMLTextAreaElement>) => void;
    required?: boolean;
    title?: string;
    trailingAction?: ReactNode;
    value?: string;
}

const PropertyTextArea = forwardRef<HTMLTextAreaElement, PropertyTextAreaProps>(
    (
        {
            className,
            deletePropertyButton,
            description,
            disabled,
            error,
            errorMessage,
            label,
            leadingIcon,
            name,
            onChange,
            required,
            title,
            trailingAction,
            value,
            ...props
        },
        ref
    ) => (
        <fieldset className="mb-3 w-full">
            {label && (
                <div className="flex items-center justify-between">
                    <div className="flex items-center">
                        <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
                            {label}

                            {required && <RequiredMark />}
                        </Label>

                        {description && (
                            <Tooltip>
                                <TooltipTrigger>
                                    <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent>{description}</TooltipContent>
                            </Tooltip>
                        )}
                    </div>

                    {deletePropertyButton}
                </div>
            )}

            <div className={twMerge([label && 'mt-1', leadingIcon && 'relative'])} title={title}>
                <div
                    className={twMerge(
                        leadingIcon && 'relative rounded-md',
                        trailingAction &&
                            'flex rounded-md border border-input shadow-sm focus-within:ring-2 focus-within:ring-blue-500'
                    )}
                >
                    {leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-gray-200 bg-gray-100 px-3">
                            {leadingIcon}
                        </div>
                    )}

                    <Textarea
                        className={twMerge(
                            error &&
                                'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 focus:border-rose-500 focus:ring-rose-500',
                            disabled && 'bg-gray-100 text-gray-500',
                            leadingIcon && 'pl-12 leading-relaxed',
                            trailingAction &&
                                'flex-1 border-0 shadow-none focus-visible:ring-0 focus-visible:ring-offset-0',
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

                    {trailingAction && <div className="flex items-start pr-1 pt-1">{trailingAction}</div>}

                    {error && (
                        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <TriangleAlertIcon aria-hidden="true" className="size-5 text-red-500" />
                        </div>
                    )}
                </div>
            </div>

            {error && (
                <p className="mt-2 text-sm text-destructive" id={`${name}-error`} role="alert">
                    {errorMessage || ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED}
                </p>
            )}
        </fieldset>
    )
);

PropertyTextArea.displayName = 'PropertyTextArea';

export default PropertyTextArea;
