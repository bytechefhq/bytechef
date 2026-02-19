import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import PropertyCodeEditorDialog from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {CircleQuestionMarkIcon, TriangleAlertIcon} from 'lucide-react';
import {ReactNode, forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

interface PropertyCodeEditorProps {
    defaultValue?: string;
    description?: string;
    error?: boolean;
    errorMessage?: string;
    label?: string;
    language: string;
    leadingIcon?: ReactNode;
    onChange: (value: string | undefined) => void;
    required?: boolean;
    name: string;
    value: string;
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditor = forwardRef<HTMLButtonElement, PropertyCodeEditorProps>(
    (
        {
            defaultValue,
            description,
            error,
            errorMessage,
            label,
            language,
            leadingIcon,
            name,
            onChange,
            required,
            value,
            workflow,
            workflowNodeName,
        },
        ref
    ) => {
        const {setShowPropertyCodeEditorSheet, showPropertyCodeEditorSheet} = useWorkflowEditorStore(
            useShallow((state) => ({
                setShowPropertyCodeEditorSheet: state.setShowPropertyCodeEditorSheet,
                showPropertyCodeEditorSheet: state.showPropertyCodeEditorSheet,
            }))
        );

        return (
            <>
                <fieldset className="mb-3 w-full">
                    {label && (
                        <div className="flex items-center">
                            <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
                                {label}

                                {required && <RequiredMark />}
                            </Label>

                            {description && (
                                <Tooltip>
                                    <TooltipTrigger>
                                        <CircleQuestionMarkIcon className="ml-1 size-4 text-muted-foreground" />
                                    </TooltipTrigger>

                                    <TooltipContent>{description}</TooltipContent>
                                </Tooltip>
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
                                label="Open Code Editor"
                                onClick={() => setShowPropertyCodeEditorSheet(true)}
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
                            {errorMessage || ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED}
                        </p>
                    )}
                </fieldset>

                {showPropertyCodeEditorSheet && (
                    <PropertyCodeEditorDialog
                        language={language}
                        onChange={onChange}
                        onClose={() => setShowPropertyCodeEditorSheet(false)}
                        value={value || defaultValue}
                        workflow={workflow}
                        workflowNodeName={workflowNodeName}
                    />
                )}
            </>
        );
    }
);

PropertyCodeEditor.displayName = 'PropertyCodeEditor';

export default PropertyCodeEditor;
