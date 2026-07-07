import RequiredMark from '@/components/RequiredMark';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import PropertyInputTypeSwitch from '@/pages/platform/workflow-editor/components/properties/components/PropertyInputTypeSwitch';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {CircleQuestionMarkIcon, TriangleAlertIcon} from 'lucide-react';
import {ChangeEvent, InputHTMLAttributes, ReactNode, forwardRef, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyInputProps extends InputHTMLAttributes<HTMLInputElement> {
    deletePropertyButton?: ReactNode;
    description?: string;
    error?: boolean;
    errorMessage?: string;
    expressionPrefix?: boolean;
    fieldsetClassName?: string;
    handleInputTypeSwitchButtonClick?: () => void;
    inputOverlay?: ReactNode;
    label?: string;
    leadingIcon?: ReactNode;
    mentionInput?: boolean;
    name: string;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    showInputTypeSwitchButton?: boolean;
    trailingAction?: ReactNode;
    type?: string;
    value?: string;
}

const PropertyInput = forwardRef<HTMLInputElement, PropertyInputProps>(
    (
        {
            className,
            deletePropertyButton,
            description,
            disabled,
            error,
            errorMessage,
            expressionPrefix = false,
            fieldsetClassName,
            handleInputTypeSwitchButtonClick,
            id,
            inputOverlay,
            label,
            leadingIcon,
            mentionInput = false,
            name,
            onChange,
            placeholder,
            required,
            showInputTypeSwitchButton,
            title,
            trailingAction,
            type = 'text',
            value,
            ...props
        },
        ref
    ) => {
        const [isFocused, setIsFocused] = useState(false);
        const [localValue, setLocalValue] = useState(value);

        const setFocusedInput = useWorkflowNodeDetailsPanelStore((state) => state.setFocusedInput);

        const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
            const rawValue = event.target.value;

            const displayValue = expressionPrefix && rawValue.startsWith('=') ? rawValue.substring(1) : rawValue;

            setLocalValue(displayValue);

            if (onChange) {
                onChange(event);
            }
        };

        useEffect(() => {
            if (!isFocused) {
                setLocalValue(value);
            }
        }, [value, isFocused]);

        return (
            <fieldset className={twMerge('w-full space-y-1', fieldsetClassName)}>
                <div className="flex w-full items-center justify-between">
                    {label && type !== 'hidden' && (
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
                    )}

                    <div className="flex items-center">
                        {showInputTypeSwitchButton && handleInputTypeSwitchButtonClick && (
                            <PropertyInputTypeSwitch
                                handleClick={handleInputTypeSwitchButtonClick}
                                mentionInput={mentionInput}
                            />
                        )}

                        {deletePropertyButton}
                    </div>
                </div>

                <div
                    className={twMerge([label && type !== 'hidden' && 'mt-1', leadingIcon && 'relative'])}
                    title={title}
                >
                    <div
                        className={twMerge(
                            'focus:ring-2 focus:ring-ring focus-visible:outline-hidden',
                            leadingIcon && 'relative rounded-md',
                            type === 'hidden' && 'border-0',
                            error && 'ring-rose-300',
                            trailingAction &&
                                'flex h-9 items-center rounded-md border border-stroke-neutral-secondary focus-within:ring-2 focus-within:ring-ring'
                        )}
                    >
                        {type !== 'hidden' && leadingIcon && (
                            <div
                                className={twMerge(
                                    'pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-r-0 border-stroke-neutral-secondary bg-surface-neutral-secondary px-3',
                                    trailingAction && 'border-y-0 border-l-0',
                                    error && 'border-rose-300 text-rose-900 placeholder-rose-300'
                                )}
                            >
                                {leadingIcon}
                            </div>
                        )}

                        {inputOverlay || (
                            <Input
                                className={twMerge(
                                    'shadow-none outline-hidden focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-hidden',
                                    error &&
                                        'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 ring-rose-300 focus-visible:ring-rose-300',
                                    disabled && 'bg-gray-100 text-content-neutral-secondary',
                                    leadingIcon && 'pl-property-input-position leading-relaxed',
                                    trailingAction &&
                                        'h-full flex-1 border-0 focus-visible:ring-0 focus-visible:ring-offset-0',
                                    className
                                )}
                                disabled={disabled}
                                id={id || name}
                                name={name}
                                onBlur={() => setIsFocused(false)}
                                onChange={handleInputChange}
                                onFocus={() => {
                                    setIsFocused(true);

                                    setFocusedInput(null);
                                }}
                                placeholder={placeholder}
                                ref={ref}
                                required={required}
                                step={type === 'time' ? 60 : 1}
                                type={type}
                                value={localValue}
                                {...props}
                            />
                        )}

                        {trailingAction}
                    </div>

                    {error && (
                        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <TriangleAlertIcon aria-hidden="true" className="size-5 text-rose-500" />
                        </div>
                    )}
                </div>

                {error && (
                    <p className="mt-2 text-sm text-rose-600" id={`${name}-error`} role="alert">
                        {errorMessage || ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED}
                    </p>
                )}
            </fieldset>
        );
    }
);

PropertyInput.displayName = 'PropertyInput';

export default PropertyInput;
