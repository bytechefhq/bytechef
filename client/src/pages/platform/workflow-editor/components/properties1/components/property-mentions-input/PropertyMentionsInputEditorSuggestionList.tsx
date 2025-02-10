import './PropertyMentionsInputEditorSuggestionList.css';

import {DataPillType} from '@/shared/types';
import {SuggestionKeyDownProps, SuggestionProps} from '@tiptap/suggestion';
import React, {forwardRef, useEffect, useImperativeHandle, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

export type PropertyMentionsInputListRefType = {
    onKeyDown: (props: SuggestionKeyDownProps) => boolean;
};

type PropertyMentionsInputListPropsType = SuggestionProps<DataPillType>;

const PropertyMentionsInputEditorSuggestionList = forwardRef<
    PropertyMentionsInputListRefType,
    PropertyMentionsInputListPropsType
>((props, ref) => {
    const [selectedIndex, setSelectedIndex] = useState(0);

    const selectItem = (index: number) => {
        const item: DataPillType = props.items[index];

        if (item) {
            props.command({id: item.value.replace('[index]', '[0]')});
        }
    };

    const upHandler = () => {
        setSelectedIndex((selectedIndex + props.items.length - 1) % props.items.length);
    };

    const downHandler = () => {
        setSelectedIndex((selectedIndex + 1) % props.items.length);
    };

    const enterHandler = () => {
        selectItem(selectedIndex);
    };

    useEffect(() => setSelectedIndex(0), [props.items]);

    useImperativeHandle(ref, () => ({
        onKeyDown: ({event}: {event: KeyboardEvent}) => {
            if (event.key === 'ArrowUp') {
                upHandler();

                return true;
            }

            if (event.key === 'ArrowDown') {
                downHandler();

                return true;
            }

            if (event.key === 'Enter') {
                enterHandler();

                return true;
            }

            return false;
        },
    }));

    return (
        <div className="property-mentions-suggestion-menu max-h-96 overflow-y-auto">
            {props.items.length ? (
                props.items.map((item: DataPillType, index: number) => (
                    <button
                        className={index === selectedIndex ? 'is-selected' : ''}
                        key={index}
                        onClick={() => selectItem(index)}
                    >
                        <InlineSVG className="mr-2 size-4 flex-none" src={item.componentIcon!} />

                        {item.value}
                    </button>
                ))
            ) : (
                <p className="text-sm">No data pills found.</p>
            )}
        </div>
    );
});

PropertyMentionsInputEditorSuggestionList.displayName = 'PropertyMentionsInputList';

export default PropertyMentionsInputEditorSuggestionList;
