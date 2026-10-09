import {McpServer} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useMcpServerList from '../useMcpServerList';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    urlMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateEmbeddedMcpServerUrlMutation: () => ({mutate: hoisted.urlMutate}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

describe('useMcpServerList', () => {
    it('regenerates the server url through the embedded mutation', () => {
        const {result} = renderHook(() => useMcpServerList([{id: '5', name: 'Server'} as McpServer]));

        act(() => result.current.createHandleRefresh('5')());

        expect(hoisted.urlMutate).toHaveBeenCalledWith(
            {id: '5'},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );

        hoisted.urlMutate.mock.calls[0][1].onSuccess();

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
    });
});
