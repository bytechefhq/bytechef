import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useResolvedChildren} from '@/shared/command-bar/useResolvedChildren';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

const command = (id: string): CommandI => ({actions: [{to: `/${id}`, type: 'navigate'}], id, title: id});

interface DeferredI<T> {
    promise: Promise<T>;
    reject: (reason: unknown) => void;
    resolve: (value: T) => void;
}

function createDeferred<T>(): DeferredI<T> {
    let resolve!: (value: T) => void;
    let reject!: (reason: unknown) => void;

    const promise = new Promise<T>((res, rej) => {
        resolve = res;
        reject = rej;
    });

    return {promise, reject, resolve};
}

describe('useResolvedChildren', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('reports when the query is below the minimum length', () => {
        const children: CommandChildrenI = {minQueryLength: 2, placeholder: 'Search...', resolve: vi.fn()};

        const {result} = renderHook(() => useResolvedChildren(children, 'a'));

        expect(result.current.isBelowMinimum).toBe(true);
        expect(children.resolve).not.toHaveBeenCalled();
    });

    it('resolves children once the query is long enough', async () => {
        const children: CommandChildrenI = {
            placeholder: 'Search...',
            resolve: async () => [command('a')],
        };

        const {result} = renderHook(() => useResolvedChildren(children, 'abc'));

        await waitFor(() => {
            expect(result.current.children.map((child) => child.id)).toEqual(['a']);
        });
    });

    it('resolves immediately when minQueryLength is zero', async () => {
        const children: CommandChildrenI = {
            minQueryLength: 0,
            placeholder: 'Search...',
            resolve: async () => [command('a')],
        };

        const {result} = renderHook(() => useResolvedChildren(children, ''));

        await waitFor(() => {
            expect(result.current.children).toHaveLength(1);
        });
    });

    it('applies an empty query immediately instead of waiting out the debounce', async () => {
        const resolveCalls: string[] = [];

        const children: CommandChildrenI = {
            minQueryLength: 0,
            placeholder: 'Search...',
            resolve: async (query) => {
                resolveCalls.push(query);

                return [command(query || 'root')];
            },
        };

        const {rerender} = renderHook(({query}) => useResolvedChildren(children, query), {
            initialProps: {query: 'abc'},
        });

        rerender({query: ''});

        // If the empty query were debounced like any other value, this would still read ['abc'] here -- the fix
        // is exactly that '' bypasses the 300ms wait, so it must show up without any waitFor timeout budget.
        await waitFor(
            () => {
                expect(resolveCalls).toContain('');
            },
            {timeout: 100}
        );
    });

    it('aborts the previous resolve when the query changes', async () => {
        const abortedSignals: boolean[] = [];

        const children: CommandChildrenI = {
            placeholder: 'Search...',
            resolve: async (query, signal) => {
                signal.addEventListener('abort', () => abortedSignals.push(true));

                return [command(query)];
            },
        };

        const {rerender} = renderHook(({query}) => useResolvedChildren(children, query), {
            initialProps: {query: 'abc'},
        });

        rerender({query: 'abcd'});

        await waitFor(() => {
            expect(abortedSignals).toHaveLength(1);
        });
    });

    it('discards a stale resolve that settles after a newer one has already landed', async () => {
        // Fake timers give this test exact control over the 300ms debounce, and act()-wrapped
        // advanceTimersByTimeAsync flushes the resulting promise chains deterministically -- real timers cannot
        // prove a NEGATIVE (nothing changes) without an arbitrary wall-clock wait, which is exactly what this
        // needs to avoid. The stale ('abc') query's resolve is held open by a deferred promise; it settles only
        // after the current ('abcd') query's resolve has already committed its result, so if the effect applied
        // every resolve unconditionally, the stale result would land last and overwrite the correct one.
        vi.useFakeTimers();

        const staleDeferred = createDeferred<CommandI[]>();

        const children: CommandChildrenI = {
            minQueryLength: 0,
            placeholder: 'Search...',
            resolve: async (query) => {
                if (query === 'abc') {
                    return staleDeferred.promise;
                }

                return [command(query)];
            },
        };

        const {rerender, result} = renderHook(({query}) => useResolvedChildren(children, query), {
            initialProps: {query: 'abc'},
        });

        await act(async () => {
            await vi.advanceTimersByTimeAsync(300);
        });

        rerender({query: 'abcd'});

        await act(async () => {
            await vi.advanceTimersByTimeAsync(300);
        });

        expect(result.current.children.map((child) => child.id)).toEqual(['abcd']);

        // The stale resolve now settles, well after the current one already landed.
        staleDeferred.resolve([command('abc')]);

        await act(async () => {
            await vi.advanceTimersByTimeAsync(0);
        });

        expect(result.current.children.map((child) => child.id)).toEqual(['abcd']);
    });

    it('discards a stale FAILING resolve that settles after a newer one has already landed', async () => {
        // Same shape as the success-path test above, but the stale query's resolve REJECTS instead of resolving.
        // The .catch handler must check the abort signal exactly like the success handler does -- otherwise a
        // slow failing request for an old query wipes out a newer query's already-landed successful result.
        vi.useFakeTimers();

        const staleDeferred = createDeferred<CommandI[]>();

        const children: CommandChildrenI = {
            minQueryLength: 0,
            placeholder: 'Search...',
            resolve: async (query) => {
                if (query === 'abc') {
                    return staleDeferred.promise;
                }

                return [command(query)];
            },
        };

        const {rerender, result} = renderHook(({query}) => useResolvedChildren(children, query), {
            initialProps: {query: 'abc'},
        });

        await act(async () => {
            await vi.advanceTimersByTimeAsync(300);
        });

        rerender({query: 'abcd'});

        await act(async () => {
            await vi.advanceTimersByTimeAsync(300);
        });

        expect(result.current.children.map((child) => child.id)).toEqual(['abcd']);

        // The stale request now rejects, well after the current one already landed. Swallow the rejection here too
        // so vitest does not report it as an unhandled rejection alongside the hook's own .catch.
        staleDeferred.promise.catch(() => {});
        staleDeferred.reject(new Error('stale request failed'));

        await act(async () => {
            await vi.advanceTimersByTimeAsync(0);
        });

        expect(result.current.children.map((child) => child.id)).toEqual(['abcd']);
    });
});
