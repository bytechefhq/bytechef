import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import useWorkflowInputOptions from './useWorkflowInputOptions';
import {optionsCacheKey} from './utils';

describe('useWorkflowInputOptions', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('does not fetch when integrationInstanceId is missing', () => {
        const apiFetch = vi.fn().mockResolvedValue([]);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, undefined));

        act(() => result.current.loadOptions('slack', 1, 'channel', 'channelId', {}));

        expect(apiFetch).not.toHaveBeenCalled();
    });

    it('flushes a request queued before integrationInstanceId resolves', async () => {
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);

        const {rerender, result} = renderHook(
            ({id}: {id: number | undefined}) => useWorkflowInputOptions(apiFetch, id),
            {initialProps: {id: undefined as number | undefined}}
        );

        act(() => result.current.loadOptions('slack', 1, 'channel', 'channel', {}));

        expect(apiFetch).not.toHaveBeenCalled();

        await act(async () => {
            rerender({id: 55});
        });

        expect(apiFetch).toHaveBeenCalledWith('/api/embedded/v1/integration-instances/55/component-input-options', {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {},
                propertyName: 'channel',
            },
            method: 'POST',
        });
    });

    it('does not fetch when apiFetch is missing', () => {
        const {result} = renderHook(() => useWorkflowInputOptions(undefined, 7));

        act(() => result.current.loadOptions('slack', 1, 'channel', 'channelId', {}));

        expect(result.current.optionsByKey).toEqual({});
    });

    it('posts the option request and stores the result under the cache key', async () => {
        const options = [
            {label: 'General', value: 'C1'},
            {label: 'Random', value: 'C2'},
        ];
        const apiFetch = vi.fn().mockResolvedValue(options);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        await act(async () => {
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {workspace: 'W1'});
        });

        expect(apiFetch).toHaveBeenCalledWith('/api/embedded/v1/integration-instances/7/component-input-options', {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {workspace: 'W1'},
                propertyName: 'channelId',
            },
            method: 'POST',
        });

        const cacheKey = optionsCacheKey('slack', 1, 'channel', 'channelId', {workspace: 'W1'});

        expect(result.current.optionsByKey[cacheKey]).toEqual(options);
    });

    it('does not fetch again for an already cached key', async () => {
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        await act(async () => {
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {});
        });

        await act(async () => {
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {});
        });

        expect(apiFetch).toHaveBeenCalledTimes(1);
    });

    it('deduplicates concurrent in-flight requests for the same key', async () => {
        let resolveFetch: (value: unknown) => void = () => {};
        const apiFetch = vi.fn().mockReturnValue(
            new Promise((resolve) => {
                resolveFetch = resolve;
            })
        );

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        act(() => {
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {});
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {});
        });

        expect(apiFetch).toHaveBeenCalledTimes(1);

        await act(async () => {
            resolveFetch([]);
        });
    });

    it('isolates cached options per integration instance so switching does not leak the previous options', async () => {
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);

        const {rerender, result} = renderHook(({id}: {id: number}) => useWorkflowInputOptions(apiFetch, id), {
            initialProps: {id: 7},
        });

        await act(async () => {
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {});
        });

        const cacheKey = optionsCacheKey('slack', 1, 'channel', 'channelId', {});

        expect(result.current.optionsByKey[cacheKey]).toEqual([{label: 'General', value: 'C1'}]);

        await act(async () => {
            rerender({id: 8});
        });

        // The new instance starts from an empty slice rather than reusing instance 7's options.
        expect(result.current.optionsByKey[cacheKey]).toBeUndefined();

        await act(async () => {
            rerender({id: 7});
        });

        // Switching back surfaces the previously fetched options without a refetch.
        expect(result.current.optionsByKey[cacheKey]).toEqual([{label: 'General', value: 'C1'}]);
        expect(apiFetch).toHaveBeenCalledTimes(1);
    });

    it('clears the cache on resetOptions', async () => {
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);

        const {result} = renderHook(() => useWorkflowInputOptions(apiFetch, 7));

        await act(async () => {
            result.current.loadOptions('slack', 1, 'channel', 'channelId', {});
        });

        act(() => result.current.resetOptions());

        expect(result.current.optionsByKey).toEqual({});
    });
});
