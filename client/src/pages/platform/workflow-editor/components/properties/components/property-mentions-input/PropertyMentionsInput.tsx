import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {getRandomId} from '@/shared/util/random-utils';
import {ForwardedRef, ReactNode, forwardRef, memo, useEffect, useMemo, useState} from 'react';

import './PropertyMentionsInput.css';

import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/properties/components/InputTypeSwitchButton';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import PropertyMentionsInputEditorSheet from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditorSheet';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ControlType} from '@/shared/middleware/platform/configuration';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {Editor} from '@tiptap/react';
import {twMerge} from 'tailwind-merge';

interface PropertyMentionsInputProps {
    className?: string;
    controlType?: ControlType;
    defaultValue?: string;
    deletePropertyButton?: ReactNode;
    description?: string;
    handleInputTypeSwitchButtonClick?: () => void;
    label?: string;
    leadingIcon?: ReactNode;
    path?: string;
    placeholder?: string;
    required?: boolean;
    showInputTypeSwitchButton?: boolean;
    type?: string;
    value?: string;
}

const PropertyMentionsInput = forwardRef<Editor, PropertyMentionsInputProps>(
    (
        {
            className,
            controlType,
            defaultValue,
            deletePropertyButton,
            description,
            handleInputTypeSwitchButtonClick,
            label,
            leadingIcon,
            path,
            placeholder,
            required = false,
            showInputTypeSwitchButton = false,
            type = 'STRING',
            value,
        },
        ref: ForwardedRef<Editor>
    ) => {
        const [isFocused, setIsFocused] = useState(false);

        const {componentDefinitions, dataPills, workflow} = useWorkflowDataStore();
        const {focusedInput, setFocusedInput, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
        const {setDataPillPanelOpen} = useDataPillPanelStore();

        const onFocus = (editor: Editor) => {
            setFocusedInput(editor);

            if (workflowNodeDetailsPanelOpen) {
                setDataPillPanelOpen(true);
            }
        };

        const elementId = useMemo(() => `mentions-input-${getRandomId()}`, []);

        useEffect(() => {
            if (!focusedInput) {
                return;
            }

            setIsFocused((focusedInput.view.props.attributes as {[name: string]: string}).id === elementId);
        }, [focusedInput, elementId]);

        return (
            <fieldset className={twMerge('w-full', label && 'space-y-1')}>
                {(label || description || showInputTypeSwitchButton) && (
                    <div className={twMerge('flex w-full items-center justify-between', !label && 'justify-end')}>
                        {label && (
                            <div className="flex items-center">
                                <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={elementId}>
                                    {label}

                                    {required && <RequiredMark />}
                                </Label>

                                {description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <QuestionMarkCircledIcon />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-tooltip-sm">{description}</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>
                        )}

                        <div className="flex items-center gap-1">
                            {(controlType === 'RICH_TEXT' || controlType === 'TEXT_AREA') && (
                                <PropertyMentionsInputEditorSheet
                                    componentDefinitions={componentDefinitions}
                                    controlType={controlType}
                                    dataPills={dataPills}
                                    onChange={(value) => {
                                        if (onChange) {
                                            onChange(value);
                                        }
                                    }}
                                    placeholder={placeholder}
                                    title={label ?? ''}
                                    type={type}
                                    value={value}
                                    workflow={workflow}
                                />
                            )}

                            {showInputTypeSwitchButton && handleInputTypeSwitchButtonClick && (
                                <InputTypeSwitchButton handleClick={handleInputTypeSwitchButtonClick} mentionInput />
                            )}

                            {deletePropertyButton}
                        </div>
                    </div>
                )}

                <div
                    className={twMerge(
                        'flex items-center rounded-md border-gray-200 shadow-sm',
                        isFocused && 'ring-2 ring-blue-500',
                        label && 'mt-1',
                        leadingIcon && 'relative rounded-md border'
                    )}
                    title={controlType}
                >
                    {leadingIcon && (
                        <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r bg-gray-100 px-3">
                            {leadingIcon}
                        </span>
                    )}

                    <div
                        className={twMerge(
                            'property-mentions-editor flex h-full min-h-[34px] w-full rounded-md bg-white',
                            leadingIcon && 'border-0 pl-10',
                            className
                        )}
                    >
                        <PropertyMentionsInputEditor
                            className="px-2 py-[0.44rem]"
                            componentDefinitions={componentDefinitions}
                            controlType={controlType}
                            dataPills={dataPills}
                            elementId={elementId}
                            onFocus={onFocus}
                            path={path}
                            placeholder={placeholder}
                            ref={ref}
                            type={type}
                            value={value || defaultValue}
                            workflow={workflow}
                        />
                    </div>
                </div>
            </fieldset>
        );
    }
);

PropertyMentionsInput.displayName = 'PropertyMentionsInput';

export default memo(PropertyMentionsInput);
