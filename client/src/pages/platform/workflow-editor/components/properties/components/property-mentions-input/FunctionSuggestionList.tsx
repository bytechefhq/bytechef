import './PropertyMentionsInputEditorSuggestionList.css';

import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {SuggestionProps} from '@tiptap/suggestion';
import {forwardRef} from 'react';
import {twMerge} from 'tailwind-merge';

import {formatFunctionSignature} from './functionSuggestionUtils';
import {SuggestionListRefType} from './suggestionPopupRenderer';
import {useSuggestionListNavigation} from './useSuggestionListNavigation';

export type FunctionSuggestionListRefType = SuggestionListRefType;

type FunctionSuggestionListPropsType = SuggestionProps<EvaluatorFunctionDefinition>;

const FunctionSuggestionList = forwardRef<FunctionSuggestionListRefType, FunctionSuggestionListPropsType>(
    ({command, items}, ref) => {
        const selectItem = (index: number) => {
            const item = items[index];

            if (item) {
                command(item);
            }
        };

        const selectedIndex = useSuggestionListNavigation(items, ref, selectItem);

        return (
            <ul className="property-mentions-suggestion-menu max-h-96 gap-y-1 overflow-y-auto">
                {items.length ? (
                    items.map((item, index) => (
                        <li key={item.name}>
                            <button
                                className={twMerge('flex-col items-start', index === selectedIndex && 'is-selected')}
                                onClick={() => selectItem(index)}
                                type="button"
                            >
                                <span className="font-mono">
                                    <span className="text-primary">{item.name}</span>

                                    <span className="text-muted-foreground">{formatFunctionSignature(item)}</span>
                                </span>

                                {index === selectedIndex && (
                                    <span className="mt-1 text-xs whitespace-normal text-muted-foreground">
                                        {item.description}

                                        {item.example && <span className="mt-0.5 block font-mono">{item.example}</span>}
                                    </span>
                                )}
                            </button>
                        </li>
                    ))
                ) : (
                    <span className="text-sm">No functions found.</span>
                )}
            </ul>
        );
    }
);

FunctionSuggestionList.displayName = 'FunctionSuggestionList';

export default FunctionSuggestionList;
