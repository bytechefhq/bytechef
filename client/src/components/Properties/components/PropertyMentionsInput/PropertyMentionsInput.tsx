import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getRandomId from '@/utils/getRandomId';

import 'quill-mention';
import {KeyboardEvent, ReactNode, Ref, forwardRef, memo, useEffect, useMemo, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import './propertyMentionsInput.css';

import InputTypeSwitchButton from '@/components/Properties/components/InputTypeSwitchButton';
import {Label} from '@/components/ui/label';
import {
    UpdateWorkflowNodeParameter200ResponseModel,
    UpdateWorkflowNodeParameterRequest,
} from '@/middleware/platform/configuration';
import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/automation/project/utils/saveProperty';
import {ComponentType, CurrentComponentDefinitionType, DataPillType} from '@/types/types';
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
    arrayIndex?: number;
    arrayName?: string;
    controlType?: string;
    currentComponentDefinition: CurrentComponentDefinitionType;
    currentComponent: ComponentType;
    dataPills?: Array<DataPillType>;
    defaultValue?: string;
    description?: string;
    fieldsetClassName?: string;
    handleInputTypeSwitchButtonClick: () => void;
    inputTypeSwitchButtonClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name?: string;
    objectName?: string;
    onChange: (value: string) => void;
    onKeyPress?: (event: KeyboardEvent) => void;
    otherComponents: Array<ComponentType>;
    path?: string;
    placeholder?: string;
    required?: boolean;
    singleMention?: boolean;
    showInputTypeSwitchButton: boolean;
    updateWorkflowNodeParameterMutation?: UseMutationResult<
        UpdateWorkflowNodeParameter200ResponseModel,
        Error,
        UpdateWorkflowNodeParameterRequest,
        unknown
    >;
    value: string;
}

const PropertyMentionsInput = forwardRef(
    (
        {
            arrayIndex,
            arrayName,
            controlType,
            currentComponent,
            currentComponentDefinition,
            dataPills,
            defaultValue,
            description,
            handleInputTypeSwitchButtonClick,
            inputTypeSwitchButtonClassName,
            label,
            leadingIcon,
            name,
            objectName,
            onChange,
            onKeyPress,
            otherComponents,
            path,
            placeholder = "Show data pills using '{'",
            required,
            showInputTypeSwitchButton,
            singleMention,
            updateWorkflowNodeParameterMutation,
            value,
        }: PropertyMentionsInputProps,
        ref: Ref<ReactQuill>
    ) => {
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const {copiedPropertyData, focusedInput, setFocusedInput} = useWorkflowNodeDetailsPanelStore();
        const {setComponents, workflow} = useWorkflowDataStore();
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
            if (!currentComponent || !workflow || !updateWorkflowNodeParameterMutation || !name) {
                return;
            }

            const {parameters} = currentComponent;

            if (!parameters) {
                return;
            }

            let strippedValue = value;

            const dataPillValue = value.match(/data-value="([^"]+)"/)?.[1];

            if (dataPillValue && !dataPillValue.startsWith('${') && !dataPillValue.endsWith('}')) {
                strippedValue = `\${${dataPillValue.replace(/\//g, '.')}}`;
            } else {
                strippedValue = value.replace(/<[^>]*>?/gm, '');
            }

            let data = parameters;
            let currentValue;

            if (arrayName && arrayIndex !== undefined) {
                if (path?.includes('parameters')) {
                    if (objectName) {
                        data = {
                            ...parameters,
                            [arrayName]: [
                                ...(parameters?.[arrayName] ?? []).slice(0, arrayIndex),
                                {
                                    ...(parameters?.[arrayName]?.[arrayIndex] ?? {}),
                                    [name]: strippedValue,
                                },
                                ...(parameters?.[arrayName] ?? []).slice(arrayIndex + 1),
                            ],
                        };
                    } else {
                        data = {
                            ...parameters,
                            [arrayName]: [
                                ...(parameters?.[arrayName] ?? []).slice(0, arrayIndex),
                                strippedValue,
                                ...(parameters?.[arrayName] ?? []).slice(arrayIndex + 1),
                            ],
                        };
                    }
                } else {
                    const combinedArray = Object.entries(parameters)
                        .filter(([key]) => key.startsWith(`${arrayName}_`))
                        .sort((a, b) => {
                            const aIndex = parseInt(a[0].split('_')[1], 10);
                            const bIndex = parseInt(b[0].split('_')[1], 10);

                            return aIndex - bIndex;
                        })
                        .map(([, value]) => value as string);

                    const combinedString = combinedArray?.map((item) => item.replace(/<[^>]*>?/gm, '')).join(', ');

                    currentValue = parameters[arrayName];

                    data = {
                        ...parameters,
                        [arrayName]: combinedString,
                    };
                }
            } else if (objectName && parameters && path) {
                const matchingObject = path.split('.').reduce((acc, key) => {
                    if (key !== 'parameters') {
                        if (acc && acc[key] === undefined) {
                            acc[key] = {};
                        }

                        return acc && acc[key];
                    } else {
                        return acc;
                    }
                }, data);

                if (matchingObject) {
                    currentValue = matchingObject[name as string];

                    matchingObject[name as string] = strippedValue;
                }
            } else {
                currentValue = parameters[name as string];

                data = {
                    ...parameters,
                    [name as string]: strippedValue,
                };
            }

            if (!data) {
                return;
            }

            if (currentValue === strippedValue) {
                return;
            }

            saveProperty(
                name,
                path ?? '',
                true,
                currentComponent,
                otherComponents,
                setComponents,
                updateWorkflowNodeParameterMutation,
                workflow,
                strippedValue,
                arrayIndex
            );
        }, 200);

        const handleOnChange = (value: string) => {
            if (onChange) {
                onChange(value);
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

                if (copiedPropertyData.value) {
                    if (currentComponentDefinition?.icon) {
                        copiedPropertyData.componentIcon = currentComponentDefinition.icon;
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
            <fieldset className={twMerge('w-full', arrayIndex === undefined && 'space-y-2')}>
                {(label || description || showInputTypeSwitchButton) && (
                    <div className={twMerge('flex w-full items-center justify-between', !label && 'justify-end')}>
                        {label && (
                            <div className="flex items-center">
                                <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={elementId}>
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

                        {showInputTypeSwitchButton && (
                            <InputTypeSwitchButton
                                className={inputTypeSwitchButtonClassName}
                                handleClick={handleInputTypeSwitchButtonClick}
                                mentionInput={true}
                            />
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
                        onChange={(newValue) => handleOnChange(newValue)}
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
