import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useEffect, useState} from 'react';
import {useDebounce} from 'use-debounce';

const DEFAULT_MIN_QUERY_LENGTH = 2;
const RESOLVE_DEBOUNCE_MS = 300;

interface ResolvedChildrenI {
    children: CommandI[];
    isBelowMinimum: boolean;
}

export function useResolvedChildren(children: CommandChildrenI, query: string): ResolvedChildrenI {
    const [resolvedChildren, setResolvedChildren] = useState<CommandI[]>([]);

    const [debouncedQuery] = useDebounce(query, RESOLVE_DEBOUNCE_MS);

    const effectiveQuery = query === '' ? '' : debouncedQuery;

    const minQueryLength = children.minQueryLength ?? DEFAULT_MIN_QUERY_LENGTH;
    const isBelowMinimum = effectiveQuery.length < minQueryLength;

    useEffect(() => {
        if (isBelowMinimum) {
            setResolvedChildren([]);

            return;
        }

        const abortController = new AbortController();

        children
            .resolve(effectiveQuery, abortController.signal)
            .then((resolved) => {
                if (!abortController.signal.aborted) {
                    setResolvedChildren(resolved);
                }
            })
            .catch(() => {
                if (!abortController.signal.aborted) {
                    setResolvedChildren([]);
                }
            });

        return () => abortController.abort();
    }, [children, effectiveQuery, isBelowMinimum]);

    return {children: resolvedChildren, isBelowMinimum};
}
