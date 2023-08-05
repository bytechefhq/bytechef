import QuillMention from 'quill-mention';
import React, {useCallback, useRef, useState} from 'react';
import ReactQuill, {Quill} from 'react-quill';

import './mentionsInput.css';

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
    id: string;
    placeholder?: string;
};

const MentionsInput = ({data, id, placeholder}: MentionsInputProps) => {
    const [value, setValue] = useState('');
    const editorRef = useRef<ReactQuill>(null);

    const {focusedInput, setFocusedInput} = useNodeDetailsDialogStore();

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
            spaceAfterInsert: true,
        },
        toolbar: false,
    };

    return (
        <ReactQuill
            className={twMerge(
                'h-full w-full',
                focusedInput?.props.id === id && 'focused'
            )}
            formats={['bytechef-mention', 'mention']}
            id={id}
            key="keyBAR"
            modules={modules}
            onChange={setValue}
            onFocus={() => {
                if (editorRef.current) {
                    setFocusedInput(editorRef.current);
                }
            }}
            placeholder={placeholder}
            ref={editorRef}
            value={value}
        />
    );
};

export default React.memo(MentionsInput);
