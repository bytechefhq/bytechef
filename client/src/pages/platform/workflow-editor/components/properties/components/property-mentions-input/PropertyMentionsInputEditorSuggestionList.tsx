import './PropertyMentionsInputEditorSuggestionList.css';

import {DataPillType} from '@/shared/types';
import {SuggestionProps} from '@tiptap/suggestion';
import {forwardRef} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import {SuggestionListRefType} from './suggestionPopupRenderer';
import {useSuggestionListNavigation} from './useSuggestionListNavigation';

export type PropertyMentionsInputListRefType = SuggestionListRefType;

type PropertyMentionsInputListPropsType = SuggestionProps<DataPillType>;

const PropertyMentionsInputEditorSuggestionList = forwardRef<
    PropertyMentionsInputListRefType,
    PropertyMentionsInputListPropsType
>(({command, items}, ref) => {
    const selectItem = (index: number) => {
        const item: DataPillType = items[index];

        if (item) {
            command({id: item.value.replace('[index]', '[0]')});
        }
    };

    const selectedIndex = useSuggestionListNavigation(items, ref, selectItem);

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
