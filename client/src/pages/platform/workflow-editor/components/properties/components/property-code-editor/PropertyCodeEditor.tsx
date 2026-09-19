import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import PropertyCodeEditorDialog from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialog';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {CircleQuestionMarkIcon} from 'lucide-react';
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

        const readOnly = useWorkflowEditorReadOnly();

        return (
            <>
                <fieldset className="mb-3 w-full">
                    {label && (
                        <div className="flex items-center">
                            <Label className={twMerge(description && 'mr-1', 'gap-0 leading-normal')} htmlFor={name}>
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

                            {readOnly ? (
                                <span
                                    className="ml-10 inline-flex h-9 flex-1 cursor-pointer items-center justify-center rounded-md rounded-l-none border border-input bg-background px-4 text-sm font-medium hover:bg-surface-neutral-primary-hover"
                                    onClick={() => setShowPropertyCodeEditorSheet(true)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            setShowPropertyCodeEditorSheet(true);
                                        }
                                    }}
                                    role="button"
                                    tabIndex={0}
                                >
                                    View Code
                                </span>
                            ) : (
                                <Button
                                    className="ml-10 flex-1 rounded-l-none"
                                    label="Open Code Editor"
                                    onClick={() => setShowPropertyCodeEditorSheet(true)}
                                    ref={ref}
                                    variant="outline"
                                />
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
