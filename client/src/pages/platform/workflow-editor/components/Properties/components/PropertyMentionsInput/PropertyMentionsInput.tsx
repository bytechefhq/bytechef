import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {getRandomId} from '@/shared/util/random-utils';

import 'quill-mention';
import {KeyboardEvent, ReactNode, Ref, forwardRef, memo, useEffect, useMemo, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import 'quill-paste-smart';

import './propertyMentionsInput.css';

import {Label} from '@/components/ui/label';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/Properties/components/InputTypeSwitchButton';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {DataPillType} from '@/shared/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {twMerge} from 'tailwind-merge';

import PropertyMentionsInputBlot from './PropertyMentionsInputBlot';

Quill.register('formats/property-mention', PropertyMentionsInputBlot);

const isAlphaNumericalKeyCode = (event: KeyboardEvent) =>
    (event.keyCode >= 48 && event.keyCode <= 57) || (event.keyCode >= 65 && event.keyCode <= 90);

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
    controlType?: string;
    defaultValue?: string;
    description?: string;
    fieldsetClassName?: string;
    handleInputTypeSwitchButtonClick: () => void;
    inputTypeSwitchButtonClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    onChange: (value: string) => void;
    onKeyPress?: (event: KeyboardEvent) => void;
    placeholder?: string;
    required?: boolean;
    singleMention?: boolean;
    showInputTypeSwitchButton: boolean;
    value: string;
}

const PropertyMentionsInput = forwardRef(
    (
        {
            controlType,
            defaultValue,
            description,
            handleInputTypeSwitchButtonClick,
            inputTypeSwitchButtonClassName,
            label,
            leadingIcon,
            onChange,
            onKeyPress,
            placeholder,
            required,
            showInputTypeSwitchButton,
            singleMention,
            value,
        }: PropertyMentionsInputProps,
        ref: Ref<ReactQuill>
    ) => {
        const [isFocused, setIsFocused] = useState(false);
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const {dataPills} = useWorkflowDataStore();
        const {focusedInput, setFocusedInput} = useWorkflowNodeDetailsPanelStore();
        const {setDataPillPanelOpen} = useDataPillPanelStore();

        const elementId = useMemo(() => `mentions-input-${getRandomId()}`, []);

        const modules = {
            clipboard: {
                keepSelection: true,
                matchVisual: false,
            },
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

                    if (leaf && singleMention && mentionOccurences) {
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
                spaceAfterInsert: false,
            },
            toolbar: false,
        };

        const handleOnChange = (value: string) => {
            if (onChange) {
                onChange(value);
            }

            setMentionOccurences(value.match(/property-mention/g)?.length || 0);
        };

        const handleOnKeyDown = (event: KeyboardEvent) => {
            // @ts-expect-error Quill false positive
            const editor = ref.current.getEditor();

            if (!editor) {
                return;
            }

            if (mentionOccurences && isAlphaNumericalKeyCode(event) && singleMention) {
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
        };

        const handleOnFocus = () => {
            // @ts-expect-error Quill false positive
            if (ref?.current && !isFocused) {
                setTimeout(() => {
                    // @ts-expect-error Quill false positive
                    setFocusedInput(ref.current!);

                    setIsFocused(true);

                    setDataPillPanelOpen(true);
                }, 50);
            }
        };

        const handleOnBlur = () => {
            setFocusedInput(null);

            setIsFocused(false);
        };

        useEffect(() => {
            // @ts-expect-error Quill false positive
            if (!ref?.current) {
                return;
            }

            // @ts-expect-error Quill false positive
            const editor = ref.current.getEditor();

            if (!editor) {
                return;
            }

            const keyboard = editor.getModule('keyboard');

            delete keyboard.bindings[9];
        }, [ref]);

        useEffect(() => {
            if (!focusedInput) {
                return;
            }

            setIsFocused(focusedInput.props.id === elementId);
        }, [focusedInput, elementId]);

        return (
            <fieldset className={twMerge('w-full', label && 'space-y-1')}>
                {(label || description || showInputTypeSwitchButton) && (
                    <div className={twMerge('flex w-full items-center justify-between', !label && 'justify-end')}>
                        {label && (
                            <div className="flex items-center">
                                <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={elementId}>
                                    {label}

                                    {required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
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
                                mentionInput
                            />
                        )}
                    </div>
                )}

                <div
                    className={twMerge(
                        'flex items-center shadow-sm',
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

                    <ReactQuill
                        className={twMerge(
                            'h-full w-full bg-white rounded-md',
                            leadingIcon && 'border-0 pl-10',
                            controlType === 'TEXT_AREA' && 'min-h-32'
                        )}
                        defaultValue={defaultValue}
                        formats={['property-mention', 'mention']}
                        id={elementId}
                        key={elementId}
                        // eslint-disable-next-line react-hooks/exhaustive-deps -- put data as dependency and it will render empty editor, but it will update available datapills
                        modules={useMemo(() => modules, [])}
                        onBlur={handleOnBlur}
                        onChange={handleOnChange}
                        onFocus={handleOnFocus}
                        onKeyDown={handleOnKeyDown}
                        onKeyPress={onKeyPress}
                        placeholder={
                            placeholder ? `${placeholder} (Show data pills using '{')` : "Show data pills using '{'"
                        }
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
