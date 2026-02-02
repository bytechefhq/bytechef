import './PropertyMentionsInputEditorSuggestionList.css';

import {DataPillType} from '@/shared/types';
import {SuggestionKeyDownProps, SuggestionProps} from '@tiptap/suggestion';
import {forwardRef, useEffect, useImperativeHandle, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

export type PropertyMentionsInputListRefType = {
    onKeyDown: (props: SuggestionKeyDownProps) => boolean;
};

type PropertyMentionsInputListPropsType = SuggestionProps<DataPillType>;

const PropertyMentionsInputEditorSuggestionList = forwardRef<
    PropertyMentionsInputListRefType,
    PropertyMentionsInputListPropsType
>(({command, items}, ref) => {
    const [selectedIndex, setSelectedIndex] = useState(0);

    const selectItem = (index: number) => {
        const item: DataPillType = items[index];

        if (item) {
            command({id: item.value.replace('[index]', '[0]')});
        }
    };

    const upHandler = () => setSelectedIndex((selectedIndex + items.length - 1) % items.length);

    const downHandler = () => setSelectedIndex((selectedIndex + 1) % items.length);

    const enterHandler = () => selectItem(selectedIndex);

    useEffect(() => setSelectedIndex(0), [items]);

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
        <ul className="property-mentions-suggestion-menu max-h-96 gap-y-1 overflow-y-auto">
            {items.length ? (
                items.map((item: DataPillType, index: number) => (
                    <li key={item.value}>
                        <button
                            className={twMerge(index === selectedIndex && 'is-selected')}
                            onClick={() => selectItem(index)}
                        >
                            <InlineSVG className="mr-2 size-4 flex-none" src={item.componentIcon!} />

                            {item.value}
                        </button>
                    </li>
                ))
            ) : (
                <span className="text-sm">No data pills found.</span>
            )}
        </ul>
    );
});

PropertyMentionsInputEditorSuggestionList.displayName = 'PropertyMentionsInputList';

export default PropertyMentionsInputEditorSuggestionList;
