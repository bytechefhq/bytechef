import './PropertyMentionsInputEditorSuggestionList.css';

import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {SuggestionProps} from '@tiptap/suggestion';
import {forwardRef, useMemo, useRef} from 'react';
import {twMerge} from 'tailwind-merge';

import {formatFunctionSignature, groupFunctionOverloads} from './functionSuggestionUtils';
import {SuggestionListRefType} from './suggestionPopupRenderer';
import {useSuggestionListNavigation} from './useSuggestionListNavigation';

export type FunctionSuggestionListRefType = SuggestionListRefType;

type FunctionSuggestionListPropsType = SuggestionProps<EvaluatorFunctionDefinition>;

const FunctionSuggestionList = forwardRef<FunctionSuggestionListRefType, FunctionSuggestionListPropsType>(
    ({command, items}, ref) => {
        const listRef = useRef<HTMLUListElement>(null);

        const groups = useMemo(() => groupFunctionOverloads(items), [items]);

        const selectItem = (index: number) => {
            const group = groups[index];

            if (group) {
                command(group.overloads[0]);
            }
        };

        const selectedIndex = useSuggestionListNavigation(groups, ref, selectItem, listRef);

        return (
            <ul className="property-mentions-suggestion-menu max-h-96 gap-y-1 overflow-y-auto" ref={listRef}>
                {groups.length ? (
                    groups.map((group, index) => {
                        const [firstOverload] = group.overloads;
                        const hasVariations = group.overloads.length > 1;

                        return (
                            <li key={group.name}>
                                <button
                                    className={twMerge(
                                        'flex-col items-start',
                                        index === selectedIndex && 'is-selected'
                                    )}
                                    onClick={() => selectItem(index)}
                                    type="button"
                                >
                                    <span className="flex w-full items-baseline gap-2">
                                        <span className="font-mono">
                                            <span className="text-primary">{group.name}</span>

                                            {!hasVariations && (
                                                <span className="text-muted-foreground">
                                                    {formatFunctionSignature(firstOverload)}
                                                </span>
                                            )}
                                        </span>

                                        {hasVariations && (
                                            <span className="ml-auto shrink-0 text-xs text-muted-foreground">
                                                {group.overloads.length} variations
                                            </span>
                                        )}
                                    </span>

                                    {index === selectedIndex && (
                                        <span className="mt-1 text-xs whitespace-normal text-muted-foreground">
                                            {hasVariations ? (
                                                group.overloads.map((overload) => (
                                                    <span
                                                        className="mt-3 block border-l border-border pl-2 first:mt-0"
                                                        key={formatFunctionSignature(overload)}
                                                    >
                                                        <span className="block font-mono text-foreground">
                                                            {formatFunctionSignature(overload)}
                                                        </span>

                                                        {overload.description}

                                                        {overload.example && (
                                                            <span className="mt-0.5 block font-mono text-primary">
                                                                {overload.example}
                                                            </span>
                                                        )}
                                                    </span>
                                                ))
                                            ) : (
                                                <>
                                                    {firstOverload.description}

                                                    {firstOverload.example && (
                                                        <span className="mt-0.5 block font-mono text-primary">
                                                            {firstOverload.example}
                                                        </span>
                                                    )}
                                                </>
                                            )}
                                        </span>
                                    )}
                                </button>
                            </li>
                        );
                    })
                ) : (
                    <span className="text-sm">No functions found.</span>
                )}
            </ul>
        );
    }
);

FunctionSuggestionList.displayName = 'FunctionSuggestionList';

export default FunctionSuggestionList;
