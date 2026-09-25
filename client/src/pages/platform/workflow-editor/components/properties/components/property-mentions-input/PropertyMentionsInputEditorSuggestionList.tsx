import './PropertyMentionsInputEditorSuggestionList.css';

import {resolveArrayIndexTemplate} from '@/pages/platform/workflow-editor/utils/dataPillArrayIndex';
import {DataPillType} from '@/shared/types';
import {SuggestionProps} from '@tiptap/suggestion';
import {forwardRef} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import {findDataPillMatchRange} from './dataPillSuggestionUtils';
import {SuggestionListRefType} from './suggestionPopupRenderer';
import {useSuggestionListNavigation} from './useSuggestionListNavigation';

export type PropertyMentionsInputListRefType = SuggestionListRefType;

type PropertyMentionsInputListPropsType = SuggestionProps<DataPillType>;

const DataPillSuggestionLabel = ({query, value}: {query: string; value: string}) => {
    const matchRange = findDataPillMatchRange(value, query);

    if (!matchRange) {
        return <>{value}</>;
    }

    return (
        <>
            {value.slice(0, matchRange.start)}

            <mark className="bg-transparent font-semibold text-inherit" data-testid="data-pill-suggestion-match">
                {value.slice(matchRange.start, matchRange.end)}
            </mark>

            {value.slice(matchRange.end)}
        </>
    );
};

const PropertyMentionsInputEditorSuggestionList = forwardRef<
    PropertyMentionsInputListRefType,
    PropertyMentionsInputListPropsType
>(({command, items, query}, ref) => {
    const selectItem = (index: number) => {
        const item: DataPillType = items[index];

        if (item) {
            const mentionId = resolveArrayIndexTemplate(item.value);

            command({id: mentionId});
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

                            <DataPillSuggestionLabel query={query} value={item.value} />
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
