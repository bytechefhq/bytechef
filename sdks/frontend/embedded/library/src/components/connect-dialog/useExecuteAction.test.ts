import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useExecuteAction from './useExecuteAction';

describe('useExecuteAction', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('posts to the action endpoint with the instance header and returns result', async () => {
        const apiFetch = vi.fn().mockResolvedValue({result: [{name: 'Contacts'}]});

        const {result} = renderHook(() => useExecuteAction(apiFetch, 'user-1', 7));

        let actionResult: unknown;

        await act(async () => {
            actionResult = await result.current('hubspot', 1, 'listObjects', {q: 'x'});
        });

        expect(apiFetch).toHaveBeenCalledWith(
            '/api/embedded/v1/user-1/components/hubspot/versions/1/actions/listObjects',
            {body: {input: {q: 'x'}}, headers: {'X-Instance-Id': '7'}, method: 'POST'}
        );
        expect(actionResult).toEqual([{name: 'Contacts'}]);
    });

    it('returns an empty array when externalUserId is missing', async () => {
        const apiFetch = vi.fn();

        const {result} = renderHook(() => useExecuteAction(apiFetch, undefined, 7));

        let actionResult: unknown;

        await act(async () => {
            actionResult = await result.current('hubspot', 1, 'listObjects', {});
        });

        expect(apiFetch).not.toHaveBeenCalled();
        expect(actionResult).toEqual([]);
    });
});
