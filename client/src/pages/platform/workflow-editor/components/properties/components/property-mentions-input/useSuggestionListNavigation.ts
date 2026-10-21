import {ForwardedRef, RefObject, useEffect, useImperativeHandle, useState} from 'react';

import {SuggestionListRefType} from './suggestionPopupRenderer';

/**
 * Arrow-key and Enter navigation shared by the editor's suggestion lists. Owns the highlighted index,
 * resets it whenever the filtered item set changes, scrolls the highlighted row into view, and publishes the
 * `onKeyDown` handler that the tippy popup renderer reaches through the forwarded ref.
 */
export function useSuggestionListNavigation(
    items: unknown[],
    ref: ForwardedRef<SuggestionListRefType>,
    selectItem: (index: number) => void,
    listRef: RefObject<HTMLUListElement | null>
): number {
    const [selectedIndex, setSelectedIndex] = useState(0);

    useImperativeHandle(ref, () => ({
        onKeyDown: ({event}: {event: KeyboardEvent}) => {
            if (event.key === 'ArrowUp') {
                setSelectedIndex((selectedIndex + items.length - 1) % items.length);

                return true;
            }

            if (event.key === 'ArrowDown') {
                setSelectedIndex((selectedIndex + 1) % items.length);

                return true;
            }

            if (event.key === 'Enter') {
                selectItem(selectedIndex);

                return true;
            }

            return false;
        },
    }));

    useEffect(() => setSelectedIndex(0), [items]);

    useEffect(() => {
        listRef.current?.children[selectedIndex]?.scrollIntoView({block: 'nearest'});
    }, [listRef, selectedIndex]);

    return selectedIndex;
}
