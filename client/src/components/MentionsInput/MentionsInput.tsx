import getRandomId from '@/pages/automation/project/utils/getRandomId';
import QuillMention from 'quill-mention';
import React, {useCallback, useMemo, useRef, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import './mentionsInput.css';

import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import {useNodeDetailsDialogStore} from '@/pages/automation/project/stores/useNodeDetailsDialogStore';
import {DataPillType} from '@/types/types';
import {twMerge} from 'tailwind-merge';

import MentionBlot from './MentionBlot';

Quill.register('modules/mentions', QuillMention);
Quill.register('formats/bytechef-mention', MentionBlot);

const MentionInputListItem = (item: DataPillType) => `
    <div>
        <span>${item.icon}</span>

        <span>${item.value}</span>
    </div>
`;

type MentionsInputProps = {
    data: Array<DataPillType>;
    label?: string;
    placeholder?: string;
};

const MentionsInput = ({data, label, placeholder}: MentionsInputProps) => {
    const [value, setValue] = useState('');

    const editorRef = useRef<ReactQuill>(null);

    const {focusedInput, setFocusedInput} = useNodeDetailsDialogStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();

    const elementId = useMemo(() => `mentions-input-${getRandomId()}`, []);

    const modules = {
        mention: {
            allowedChars: /^[A-Za-z\sÅÄÖåäö]*$/,
            blotName: 'mention',
            dataAttributes: ['component'],
            fixMentionsToQuill: true,
            mentionDenotationChars: ['${'],
            onSelect: useCallback(
                (
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
                []
            ),
            renderItem: useCallback(
                (item: DataPillType) => MentionInputListItem(item),
                []
            ),
            showDenotationChar: false,
            source: useCallback(
                (
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
                [data]
            ),
        },
        toolbar: false,
    };

    return (
        <fieldset className="w-full">
            {label && (
                <label
                    className="block px-2 text-sm font-medium capitalize text-gray-700"
                    htmlFor={elementId}
                >
                    {label}
                </label>
            )}

            <ReactQuill
                className={twMerge(
                    'h-full w-full mt-1',
                    focusedInput?.props.id === elementId && 'focused'
                )}
                formats={['bytechef-mention', 'mention']}
                id={elementId}
                key={elementId}
                modules={modules}
                onChange={setValue}
                onFocus={() => {
                    if (editorRef.current) {
                        setFocusedInput(editorRef.current);

                        setDataPillPanelOpen(true);
                    }
                }}
                placeholder={placeholder}
                ref={editorRef}
                value={value}
            />
        </fieldset>
    );
};

export default React.memo(MentionsInput);
