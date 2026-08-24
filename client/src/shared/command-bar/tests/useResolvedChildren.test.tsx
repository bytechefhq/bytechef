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

        staleDeferred.resolve([command('abc')]);

        await act(async () => {
            await vi.advanceTimersByTimeAsync(0);
        });

        expect(result.current.children.map((child) => child.id)).toEqual(['abcd']);
    });

    it('discards a stale FAILING resolve that settles after a newer one has already landed', async () => {
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

        staleDeferred.promise.catch(() => {});
        staleDeferred.reject(new Error('stale request failed'));

        await act(async () => {
            await vi.advanceTimersByTimeAsync(0);
        });

        expect(result.current.children.map((child) => child.id)).toEqual(['abcd']);
    });
});
