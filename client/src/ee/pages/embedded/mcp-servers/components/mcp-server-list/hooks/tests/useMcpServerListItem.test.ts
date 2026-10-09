import {McpServer} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpServerListItem from '../useMcpServerListItem';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    tagsMutation: {mutate: vi.fn()},
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/components/mcp-server/hooks/useMcpServerListItemClick', () => ({
    default: () => ({handleMcpServerListItemClick: vi.fn(), toolsCollapsibleTriggerRef: {current: null}}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteEmbeddedMcpServerMutation: () => ({isPending: false, mutate: hoisted.deleteMutate}),
    useUpdateEmbeddedMcpServerMutation: () => ({mutate: hoisted.updateMutate}),
    useUpdateEmbeddedMcpServerTagsMutation: () => hoisted.tagsMutation,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

const mcpServer = {enabled: true, id: '5', name: 'Server', tags: []} as unknown as McpServer;

describe('useMcpServerListItem', () => {
    beforeEach(() => {
        hoisted.updateMutate.mockClear();
    });

    it('toggles the server through the embedded update mutation', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        act(() => {
            void result.current.handleOnCheckedChange(false);
        });

        expect(hoisted.updateMutate).toHaveBeenCalledWith(
            {id: '5', input: {enabled: false}},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });

    it('updates tags through the embedded tags mutation', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        expect(result.current.updateEmbeddedMcpServerTagsMutation).toBe(hoisted.tagsMutation);
    });
});
