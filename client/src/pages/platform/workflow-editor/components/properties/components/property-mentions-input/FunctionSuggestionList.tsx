import './PropertyMentionsInputEditorSuggestionList.css';

import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {SuggestionKeyDownProps, SuggestionProps} from '@tiptap/suggestion';
import {forwardRef, useEffect, useImperativeHandle, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {formatFunctionSignature} from './functionSuggestionUtils';

export type FunctionSuggestionListRefType = {
    onKeyDown: (props: SuggestionKeyDownProps) => boolean;
};

type FunctionSuggestionListPropsType = SuggestionProps<EvaluatorFunctionDefinition>;

const FunctionSuggestionList = forwardRef<FunctionSuggestionListRefType, FunctionSuggestionListPropsType>(
    ({command, items}, ref) => {
        const [selectedIndex, setSelectedIndex] = useState(0);

        const selectItem = (index: number) => {
            const item = items[index];

            if (item) {
                command(item);
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
                    items.map((item, index) => (
                        <li key={item.name}>
                            <button
                                className={twMerge('flex-col items-start', index === selectedIndex && 'is-selected')}
                                onClick={() => selectItem(index)}
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
