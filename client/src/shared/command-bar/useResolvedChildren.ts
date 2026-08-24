import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useEffect, useState} from 'react';
import {useDebounce} from 'use-debounce';

const DEFAULT_MIN_QUERY_LENGTH = 2;
const RESOLVE_DEBOUNCE_MS = 300;

interface ResolvedChildrenI {
    children: CommandI[];
    isBelowMinimum: boolean;
}

/**
 * Resolves a nested command's children for the current query. Each new query aborts the previous request, so a slow
 * early response cannot land after -- and overwrite -- a fast later one.
 */
export function useResolvedChildren(children: CommandChildrenI, query: string): ResolvedChildrenI {
    const [resolvedChildren, setResolvedChildren] = useState<CommandI[]>([]);

    const [debouncedQuery] = useDebounce(query, RESOLVE_DEBOUNCE_MS);

    // An empty query needs no debouncing: pushing or popping a level (or the user clearing the input by hand)
    // clears `query` synchronously, but `debouncedQuery` can lag up to RESOLVE_DEBOUNCE_MS behind holding the
    // previous level's text. Applying the empty string immediately avoids resolving against stale leftover text.
    const effectiveQuery = query === '' ? '' : debouncedQuery;

    const minQueryLength = children.minQueryLength ?? DEFAULT_MIN_QUERY_LENGTH;
    const isBelowMinimum = effectiveQuery.length < minQueryLength;

    useEffect(() => {
        if (isBelowMinimum) {
            setResolvedChildren([]);

            return;
        }

        // No manual abort of the previous controller here: React runs the previous effect's cleanup (which aborts
        // its own controller) before this effect runs, so an explicit abort at the top would be redundant.
        const abortController = new AbortController();

        children
            .resolve(effectiveQuery, abortController.signal)
            .then((resolved) => {
                if (!abortController.signal.aborted) {
                    setResolvedChildren(resolved);
                }
            })
            .catch(() => {
                // Guarded the same way as the success path: a slow FAILING request must not wipe a newer
                // successful result that already landed for a later query.
                if (!abortController.signal.aborted) {
                    setResolvedChildren([]);
                }
            });

        return () => abortController.abort();
    }, [children, effectiveQuery, isBelowMinimum]);

    return {children: resolvedChildren, isBelowMinimum};
}
