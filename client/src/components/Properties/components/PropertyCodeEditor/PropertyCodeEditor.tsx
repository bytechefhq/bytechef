import PropertyCodeEditorSheet from '@/components/Properties/components/PropertyCodeEditor/PropertyCodeEditorSheet';
import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowModel} from '@/middleware/platform/configuration';
import {ExclamationTriangleIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {Label} from '@radix-ui/react-label';
import {ReactNode, forwardRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface PropertyCodeEditorProps {
    description?: string;
    error?: string | undefined;
    label?: string;
    leadingIcon?: ReactNode;
    required?: boolean;
    name: string;
}

const PropertyCodeEditor = forwardRef<HTMLButtonElement, PropertyCodeEditorProps>(
    ({description, error, label, leadingIcon, name, required}, ref) => {
        const [showPropertyCodeEditorSheet, setPropertyCodeEditorSheet] = useState(false);

        return (
            <>
                <fieldset className="mb-3 w-full">
                    {label && (
                        <div className="flex items-center">
                            <Label className={twMerge(description && 'mr-1')} htmlFor={name}>
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

                    <div className={twMerge([label && 'mt-1', leadingIcon && 'relative'])}>
                        <div className={twMerge(leadingIcon && 'relative rounded-md flex w-full')}>
                            {leadingIcon && (
                                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border border-input bg-gray-100 px-3">
                                    {leadingIcon}
                                </div>
                            )}

                            <Button
                                className="ml-10 flex-1 rounded-l-none"
                                onClick={() => setPropertyCodeEditorSheet(true)}
                                ref={ref}
                                variant="outline"
                            >
                                Open Code Editor
                            </Button>

                            {error && (
                                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                                    <ExclamationTriangleIcon aria-hidden="true" className="size-5 text-red-500" />
                                </div>
                            )}
                        </div>
                    </div>

                    {error && (
                        <p className="mt-2 text-sm text-red-600" id={`${name}-error`} role="alert">
                            This field is required
                        </p>
                    )}
                </fieldset>

                {showPropertyCodeEditorSheet && (
                    <PropertyCodeEditorSheet
                        onClose={() => {
                            setPropertyCodeEditorSheet(false);
                        }}
                        workflow={{} as WorkflowModel}
                    />
                )}
            </>
        );
    }
);

PropertyCodeEditor.displayName = 'PropertyCodeEditor';

export default PropertyCodeEditor;
