import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getRandomId from '@/utils/getRandomId';

import 'quill-mention';
import {ReactNode, memo, useEffect, useMemo, useRef, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import './mentionsInput.css';

import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import {useNodeDetailsDialogStore} from '@/pages/automation/project/stores/useNodeDetailsDialogStore';
import {DataPillType} from '@/types/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {twMerge} from 'tailwind-merge';

import MentionBlot from './MentionBlot';

Quill.register('formats/bytechef-mention', MentionBlot);

const MentionInputListItem = (item: DataPillType) => {
    const div = document.createElement('div');

    div.innerHTML = `
        <div>
            <span>${item.icon}</span>

            <span>${item.value}</span>
        </div>
    `;

    return div;
};

type MentionsInputProps = {
    controlType?: string;
    data: Array<DataPillType>;
    description?: string;
    fieldsetClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    onKeyPress?: (event: KeyboardEvent) => void;
    placeholder?: string;
};

const MentionsInput = ({
    controlType,
    data,
    description,
    fieldsetClassName,
    label,
    leadingIcon,
    onKeyPress,
    placeholder = "Mention datapills using '{'",
}: MentionsInputProps) => {
    const [value, setValue] = useState('');

    const editorRef = useRef<ReactQuill>(null);

    const {focusedInput, setFocusedInput} = useNodeDetailsDialogStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();

    const elementId = useMemo(() => `mentions-input-${getRandomId()}`, []);

    const modules = {
        mention: {
            blotName: 'bytechef-mention',
            dataAttributes: ['component'],
            fixMentionsToQuill: true,
            mentionDenotationChars: ['{'],
            onOpen: () => {
                if (!editorRef.current) {
                    return;
                }

                const editorContainer =
                    // @ts-expect-error Quill false positive
                    editorRef.current.getEditor().container;

                const {height} = editorContainer.getBoundingClientRect();

                const mentionListParentElement = editorContainer.querySelector(
                    '#quill-mention-list'
                ).parentNode;

                mentionListParentElement.style.top = `${
                    height + editorContainer.offsetTop + 10
                }px`;
            },
            onSelect: (
                item: DataPillType,
                insertItem: (
                    data: DataPillType,
                    programmaticInsert: boolean,
                    overriddenOptions: object
                ) => void
            ) => {
                const component = JSON.parse(item.component as string);

                insertItem(
                    {
                        component,
                        icon: component.icon,
                        id: item.id,
                        value: item.value,
                    },
                    false,
                    {
                        blotName: 'bytechef-mention',
                    }
                );
            },
            renderItem: (item: DataPillType) => MentionInputListItem(item),
            showDenotationChar: false,
            source: (
                searchTerm: string,
                renderList: (arg1: Array<object>, arg2: string) => void
            ) => {
                const formattedData = data.map((datum) => ({
                    ...datum,
                    icon: JSON.parse(datum.component as string).icon,
                }));

                if (searchTerm.length === 0) {
                    renderList(formattedData, searchTerm);
                } else {
                    const matches = formattedData.filter(
                        (datum) =>
                            ~datum.value
                                .toLowerCase()
                                .indexOf(searchTerm.toLowerCase())
                    );

                    renderList(matches, searchTerm);
                }
            },
        },
        toolbar: false,
    };

    const isFocused = focusedInput?.props.id === elementId;

    useEffect(() => {
        if (!editorRef.current) {
            return;
        }

        const keyboard = editorRef.current.getEditor().getModule('keyboard');

        delete keyboard.bindings[9];
    }, [editorRef]);

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
                    </label>

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <QuestionMarkCircledIcon />
                            </TooltipTrigger>

                            <TooltipContent className="max-w-tooltip-sm">
                                {description}
                            </TooltipContent>
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
                    className={twMerge(
                        'h-full w-full bg-white rounded-md',
                        leadingIcon && 'border-0 pl-10'
                    )}
                    formats={['bytechef-mention', 'mention']}
                    id={elementId}
                    key={elementId}
                    // eslint-disable-next-line react-hooks/exhaustive-deps -- put data as dependency and it will render empty editor, but it will update available datapills
                    modules={useMemo(() => modules, [])}
                    onChange={setValue}
                    onFocus={() => {
                        if (editorRef.current) {
                            setFocusedInput(editorRef.current);

                            setDataPillPanelOpen(true);
                        }
                    }}
                    onKeyPress={onKeyPress}
                    placeholder={placeholder}
                    ref={editorRef}
                    value={value}
                />
            </div>
        </fieldset>
    );
};

export default memo(MentionsInput);
