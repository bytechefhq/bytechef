import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getRandomId from '@/utils/getRandomId';

import 'quill-mention';
import {ChangeEvent, KeyboardEvent, ReactNode, Ref, forwardRef, memo, useEffect, useMemo, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import './propertyMentionsInput.css';

import {Label} from '@/components/ui/label';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from '@/pages/automation/project/utils/saveWorkflowDefinition';
import {ComponentDataType, CurrentComponentType, DataPillType} from '@/types/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';

import PropertyMentionsInputBlot from './PropertyMentionsInputBlot';

const isAlphaNumericalKeyCode = (event: KeyboardEvent) =>
    (event.keyCode >= 48 && event.keyCode <= 57) || (event.keyCode >= 65 && event.keyCode <= 90);

Quill.register('formats/property-mention', PropertyMentionsInputBlot);

const MentionInputListItem = (item: DataPillType) => {
    const div = document.createElement('div');

    div.innerHTML = `
        <div>
            <span>${item.componentIcon}</span>

            <span>${item.value}</span>
        </div>
    `;

    return div;
};

interface PropertyMentionsInputProps {
    arrayName?: string;
    controlType?: string;
    currentComponent: CurrentComponentType;
    currentComponentData: ComponentDataType;
    dataPills?: Array<DataPillType>;
    defaultValue?: string;
    description?: string;
    fieldsetClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name?: string;
    objectName?: string;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    onKeyPress?: (event: KeyboardEvent) => void;
    placeholder?: string;
    required?: boolean;
    setValue: (value: string) => void;
    singleMention?: boolean;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
    value: string;
    workflow?: WorkflowModel;
}

const PropertyMentionsInput = forwardRef(
    (
        {
            arrayName,
            controlType,
            currentComponent,
            currentComponentData,
            dataPills,
            defaultValue,
            description,
            label,
            leadingIcon,
            name,
            objectName,
            onChange,
            onKeyPress,
            placeholder = "Show data pills using '{'",
            required,
            setValue,
            singleMention,
            updateWorkflowMutation,
            value,
            workflow,
        }: PropertyMentionsInputProps,
        ref: Ref<ReactQuill>
    ) => {
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const {copiedPropertyData, focusedInput, setFocusedInput} = useWorkflowNodeDetailsPanelStore();
        const {setDataPillPanelOpen} = useDataPillPanelStore();

        const elementId = useMemo(() => `mentions-input-${getRandomId()}`, []);

        const modules = {
            mention: {
                blotName: 'property-mention',
                dataAttributes: ['componentIcon'],
                fixMentionsToQuill: true,
                mentionDenotationChars: ['{'],
                onOpen: () => {
                    // @ts-expect-error Quill false positive
                    if (!ref?.current) {
                        return;
                    }

                    const editorContainer =
                        // @ts-expect-error Quill false positive
                        ref.current.getEditor().container;

                    const {height} = editorContainer.getBoundingClientRect();

                    const mentionListParentElement = editorContainer.querySelector('#quill-mention-list').parentNode;

                    mentionListParentElement.style.top = `${height + editorContainer.offsetTop + 10}px`;
                },
                onSelect: (
                    item: DataPillType,
                    insertItem: (data: DataPillType, programmaticInsert: boolean, overriddenOptions: object) => void
                ) => {
                    // @ts-expect-error Quill false positive
                    const editor = ref.current.getEditor();

                    const selection = editor.getSelection();

                    const [leaf, offset] = editor.getLeaf(selection?.index || 0);

                    if (leaf) {
                        editor.deleteText(0, editor.getLength());

                        editor.setText(' ');

                        leaf.deleteAt(0, offset);
                    }

                    insertItem(
                        {
                            componentIcon: item.componentIcon,
                            id: item.id,
                            value: item.value,
                        },
                        false,
                        {
                            blotName: 'property-mention',
                        }
                    );
                },
                renderItem: (item: DataPillType) => MentionInputListItem(item),
                showDenotationChar: false,
                source: (searchTerm: string, renderList: (arg1: Array<object>, arg2: string) => void) => {
                    if (!dataPills) {
                        return;
                    }

                    const formattedData = dataPills.map((dataPill) => {
                        const {componentIcon, componentName, id, value} = dataPill;

                        return {
                            componentIcon: componentIcon || '📄',
                            componentName,
                            id,
                            value,
                        };
                    });

                    if (searchTerm.length === 0) {
                        renderList(formattedData, searchTerm);
                    } else {
                        const matches = formattedData.filter(
                            (datum) => ~datum.value.toLowerCase().indexOf(searchTerm.toLowerCase())
                        );

                        renderList(matches, searchTerm);
                    }
                },
            },
            toolbar: false,
        };

        const isFocused = focusedInput?.props.id === elementId;

        const saveInputValue = useDebouncedCallback(() => {
            if (!currentComponentData || !workflow || !updateWorkflowMutation) {
                return;
            }

            const {actionName, componentName, parameters, workflowNodeName} = currentComponentData;

            let strippedValue = value;

            const dataPillValue = value.match(/data-value="([^"]+)"/)?.[1];

            if (dataPillValue && !dataPillValue.startsWith('${') && !dataPillValue.endsWith('}')) {
                strippedValue = `\${${dataPillValue}}`;
            } else {
                strippedValue = value.replace(/<[^>]*>?/gm, '');
            }

            if (arrayName && parameters) {
                const combinedArray = Object.entries(parameters)
                    .filter(([key]) => key.startsWith(`${arrayName}_`))
                    .sort((a, b) => {
                        const aIndex = parseInt(a[0].split('_')[1], 10);
                        const bIndex = parseInt(b[0].split('_')[1], 10);

                        return aIndex - bIndex;
                    })
                    .map(([, value]) => value as string);

                const combinedString = combinedArray.map((item) => item.replace(/<[^>]*>?/gm, '')).join(', ');

                saveWorkflowDefinition(
                    {
                        actionName,
                        componentName,
                        name: workflowNodeName,
                        parameters: {
                            ...parameters,
                            [arrayName]: combinedString,
                        },
                    },
                    workflow,
                    updateWorkflowMutation
                );

                return;
            } else if (objectName && parameters && name) {
                if (parameters![objectName]?.[name] === strippedValue) {
                    return;
                }

                saveWorkflowDefinition(
                    {
                        actionName,
                        componentName,
                        name: workflowNodeName,
                        parameters: {
                            ...parameters,
                            [objectName]: {
                                ...parameters![objectName],
                                [name as string]: strippedValue,
                            },
                        },
                    },
                    workflow,
                    updateWorkflowMutation
                );
            } else {
                saveWorkflowDefinition(
                    {
                        actionName,
                        componentName,
                        name: workflowNodeName,
                        parameters: {
                            ...parameters,
                            [name as string]: strippedValue,
                        },
                    },
                    workflow,
                    updateWorkflowMutation
                );
            }
        }, 200);

        const handleOnChange = (value: string) => {
            if (setValue) {
                setValue(value);
            }

            if (onChange) {
                onChange({
                    target: {name, value},
                } as ChangeEvent<HTMLInputElement>);
            }

            setMentionOccurences(value.match(/property-mention/g)?.length || 0);

            saveInputValue();
        };

        const handleOnFocus = () => {
            // @ts-expect-error Quill false positive
            if (ref?.current) {
                // @ts-expect-error Quill false positive
                setFocusedInput(ref.current!);

                setDataPillPanelOpen(true);
            }
        };

        const handleOnKeyDown = (event: KeyboardEvent<Element>) => {
            if (mentionOccurences && isAlphaNumericalKeyCode(event)) {
                // @ts-expect-error Quill false positive
                const editor = ref.current.getEditor();

                const selection = editor.getSelection();

                const [leaf] = editor.getLeaf(selection?.index || 0);

                if (leaf) {
                    const length = editor.getLength();

                    editor.deleteText(0, length);

                    editor.insertText(0, '');

                    editor.setSelection(length);
                }
            }

            if (singleMention && mentionOccurences) {
                event.preventDefault();
            }

            if (event.key === 'v' && (event.metaKey || event.ctrlKey)) {
                event.preventDefault();

                if (copiedPropertyData) {
                    if (currentComponent?.icon) {
                        copiedPropertyData.componentIcon = currentComponent.icon;
                    }

                    const mentionInput = focusedInput?.getEditor().getModule('mention');

                    if (!mentionInput) {
                        return;
                    }

                    mentionInput.insertItem(copiedPropertyData, true, {blotName: 'property-mention'});
                }
            }
        };

        useEffect(() => {
            // @ts-expect-error Quill false positive
            if (!ref?.current) {
                return;
            }

            // @ts-expect-error Quill false positive
            const keyboard = ref?.current.getEditor().getModule('keyboard');

            delete keyboard.bindings[9];
        }, [ref]);

        return (
            <fieldset className="w-full space-y-2">
                {label && (
                    <div className="flex items-center">
                        <Label className={twMerge(description && 'mr-1')} htmlFor={elementId}>
                            {label}

                            {required && <span className="leading-3 text-red-500">*</span>}
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

                <div
                    className={twMerge(
                        'flex items-center shadow-sm',
                        isFocused && 'ring-2 ring-blue-500',
                        leadingIcon && 'relative rounded-md border'
                    )}
                    title={controlType}
                >
                    {leadingIcon && (
                        <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r bg-gray-100 px-3">
                            {leadingIcon}
                        </span>
                    )}

                    <ReactQuill
                        className={twMerge('h-full w-full bg-white rounded-md', leadingIcon && 'border-0 pl-10')}
                        defaultValue={defaultValue}
                        formats={['property-mention', 'mention']}
                        id={elementId}
                        key={elementId}
                        // eslint-disable-next-line react-hooks/exhaustive-deps -- put data as dependency and it will render empty editor, but it will update available datapills
                        modules={useMemo(() => modules, [])}
                        onChange={handleOnChange}
                        onFocus={handleOnFocus}
                        onKeyDown={handleOnKeyDown}
                        onKeyPress={onKeyPress}
                        placeholder={placeholder}
                        ref={ref}
                        value={value}
                    />
                </div>
            </fieldset>
        );
    }
);

PropertyMentionsInput.displayName = 'PropertyMentionsInput';

export default memo(PropertyMentionsInput);
