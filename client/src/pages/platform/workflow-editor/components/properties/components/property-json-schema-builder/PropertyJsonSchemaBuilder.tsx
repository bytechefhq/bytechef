import Button from '@/components/Button/Button';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/properties/components/InputTypeSwitchButton';
import PropertyJsonSchemaBuilderSheet from '@/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet';
import {CircleQuestionMarkIcon, TriangleAlertIcon} from 'lucide-react';
import {ReactNode, forwardRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyJsonSchemaBuilderProps {
    description?: string;
    error?: boolean;
    errorMessage?: string;
    handleInputTypeSwitchButtonClick?: () => void;
    label?: string;
    leadingIcon?: ReactNode;
    locale?: string;
    name: string;
    onChange?: (newSchema: SchemaRecordType) => void;
    required?: boolean;
    schema?: SchemaRecordType;
    title?: string;
}

const PropertyJsonSchemaBuilder = forwardRef<HTMLButtonElement, PropertyJsonSchemaBuilderProps>(
    (
        {
            description,
            error,
            errorMessage,
            handleInputTypeSwitchButtonClick,
            label,
            leadingIcon,
            locale,
            name,
            onChange,
            required,
            schema,
            title,
        },
        ref
    ) => {
        const [showPropertyJsonSchemaBuilder, setShowPropertyJsonSchemaBuilder] = useState(false);

        return (
            <>
                <fieldset className="w-full">
                    {label && (
                        <div className="flex w-full items-center justify-between">
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

                            {handleInputTypeSwitchButtonClick && (
                                <InputTypeSwitchButton
                                    handleClick={handleInputTypeSwitchButtonClick}
                                    mentionInput={false}
                                />
                            )}
                        </div>
                    )}

                    <div className={twMerge([label && 'mt-1', leadingIcon && 'relative'])}>
                        <div className={twMerge(leadingIcon && 'relative flex w-full rounded-md')}>
                            {leadingIcon && (
                                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-input bg-gray-100 px-3">
                                    {leadingIcon}
                                </div>
                            )}

                            <Button
                                className="ml-10 flex-1 rounded-l-none"
                                label="Open JSON Schema Builder"
                                onClick={() => setShowPropertyJsonSchemaBuilder(true)}
                                ref={ref}
                                variant="outline"
                            />

                            {error && (
                                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                                    <TriangleAlertIcon aria-hidden="true" className="size-5 text-red-500" />
                                </div>
                            )}
                        </div>
                    </div>

                    {error && (
                        <p className="mt-2 text-sm text-destructive" id={`${name}-error`} role="alert">
                            {errorMessage || 'This field is required.'}
                        </p>
                    )}
                </fieldset>

                {showPropertyJsonSchemaBuilder && (
                    <PropertyJsonSchemaBuilderSheet
                        locale={locale}
                        onChange={onChange}
                        onClose={() => setShowPropertyJsonSchemaBuilder(false)}
                        schema={schema}
                        title={title}
                    />
                )}
            </>
        );
    }
);

PropertyJsonSchemaBuilder.displayName = 'PropertyJsonSchemaBuilder';

export default PropertyJsonSchemaBuilder;
