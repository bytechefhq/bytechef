import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getRandomId from '@/utils/getRandomId';

import 'quill-mention';
import {ChangeEvent, KeyboardEvent, ReactNode, Ref, forwardRef, memo, useEffect, useMemo, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import './mentionsInput.css';

import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {DataPillType} from '@/types/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {twMerge} from 'tailwind-merge';

import MentionBlot from './MentionBlot';

const isAlphaNumericalKeyCode = (event: KeyboardEvent) =>
    (event.keyCode >= 48 && event.keyCode <= 57) || (event.keyCode >= 65 && event.keyCode <= 90);

Quill.register('formats/property-mention', MentionBlot);

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

type MentionsInputProps = {
    controlType?: string;
    dataPills: Array<DataPillType>;
    defaultValue?: string;
    description?: string;
    fieldsetClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name?: string;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    onKeyPress?: (event: KeyboardEvent) => void;
    placeholder?: string;
    required?: boolean;
    singleMention?: boolean;
};

const MentionsInput = forwardRef(
    (
        {
            controlType,
            dataPills,
            defaultValue,
            description,
            fieldsetClassName,
            label,
            leadingIcon,
            name,
            onChange,
            onKeyPress,
            placeholder = "Show data pills using '{'",
            required,
            singleMention,
        }: MentionsInputProps,
        ref: Ref<ReactQuill>
    ) => {
        const [value, setValue] = useState('');
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const {focusedInput, setFocusedInput} = useWorkflowNodeDetailsPanelStore();
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
                    const formattedData = dataPills.map((dataPill) => {
                        if (!dataPill.componentDefinition) {
                            return {
                                componentAlias: dataPill.componentAlias,
                                componentIcon: '📄',
                                id: dataPill.id,
                                value: dataPill.value,
                            };
                        }

                        return {
                            componentAlias: dataPill.componentAlias,
                            componentIcon: JSON.parse(dataPill.componentDefinition as string).icon,
                            id: dataPill.id,
                            value: dataPill.value,
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
            <fieldset className={twMerge('w-full', fieldsetClassName)}>
                {label && (
                    <div className="flex items-center">
                        <label
                            className={twMerge(
                                'block text-sm font-medium capitalize text-gray-700',
                                description && 'mr-1'
                            )}
                            htmlFor={elementId}
                        >
                            {label}

                            {required && <span className="leading-3 text-red-500">*</span>}
                        </label>

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
                        'flex items-center',
                        isFocused && 'ring ring-blue-500 shadow-lg shadow-blue-200',
                        label && 'mt-1',
                        leadingIcon && 'relative rounded-md border border-gray-300'
                    )}
                    title={controlType}
                >
                    {leadingIcon && (
                        <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-3">
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
                        onChange={(value) => {
                            if (onChange) {
                                onChange({
                                    target: {name, value},
                                } as ChangeEvent<HTMLInputElement>);
                            }

                            setValue(value);

                            setMentionOccurences(value.match(/property-mention/g)?.length || 0);
                        }}
                        onFocus={() => {
                            // @ts-expect-error Quill false positive
                            if (ref?.current) {
                                // @ts-expect-error Quill false positive
                                setFocusedInput(ref.current!);

                                setDataPillPanelOpen(true);
                            }
                        }}
                        onKeyDown={(event) => {
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
                        }}
                        onKeyPress={onKeyPress}
                        placeholder={placeholder}
                        ref={ref}
                        value={defaultValue || value}
                    />
                </div>
            </fieldset>
        );
    }
);

MentionsInput.displayName = 'MentionsInput';

export default memo(MentionsInput);
