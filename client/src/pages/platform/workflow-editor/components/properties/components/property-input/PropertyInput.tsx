import RequiredMark from '@/components/RequiredMark';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/properties/components/InputTypeSwitchButton';
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
    fieldsetClassName?: string;
    handleInputTypeSwitchButtonClick?: () => void;
    label?: string;
    leadingIcon?: ReactNode;
    name: string;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    showInputTypeSwitchButton?: boolean;
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
            fieldsetClassName,
            handleInputTypeSwitchButtonClick,
            id,
            label,
            leadingIcon,
            name,
            onChange,
            placeholder,
            required,
            showInputTypeSwitchButton,
            title,
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
            setLocalValue(event.target.value);

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
                            <InputTypeSwitchButton
                                handleClick={handleInputTypeSwitchButtonClick}
                                mentionInput={false}
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
                            'focus:ring-2 focus:ring-blue-500 focus-visible:outline-none',
                            leadingIcon && 'relative rounded-md',
                            type === 'hidden' && 'border-0',
                            error && 'ring-rose-300'
                        )}
                    >
                        {type !== 'hidden' && leadingIcon && (
                            <div
                                className={twMerge(
                                    'pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-gray-200 bg-gray-100 px-3',
                                    error && 'border-r-0 border-rose-300 text-rose-900 placeholder-rose-300'
                                )}
                            >
                                {leadingIcon}
                            </div>
                        )}

                        <Input
                            className={twMerge(
                                'bg-background outline-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500',
                                error &&
                                    'border-rose-300 pr-10 text-rose-900 placeholder-rose-300 ring-rose-300 focus-visible:ring-rose-300',
                                disabled && 'bg-gray-100 text-gray-500',
                                leadingIcon && 'pl-property-input-position leading-relaxed',
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
                            step={1}
                            type={type}
                            value={localValue}
                            {...props}
                        />
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
