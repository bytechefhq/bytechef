import {McpServer} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpServerListItem from '../useMcpServerListItem';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    tagsMutation: {mutate: vi.fn()},
    tagsMutationOptions: undefined as {onSuccess: () => void} | undefined,
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/components/mcp-server/hooks/useMcpServerListItemClick', () => ({
    default: () => ({handleMcpServerListItemClick: vi.fn(), toolsCollapsibleTriggerRef: {current: null}}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteEmbeddedMcpServerMutation: () => ({isPending: false, mutate: hoisted.deleteMutate}),
    useUpdateEmbeddedMcpServerMutation: () => ({mutate: hoisted.updateMutate}),
    useUpdateEmbeddedMcpServerTagsMutation: (options: {onSuccess: () => void}) => {
        hoisted.tagsMutationOptions = options;

        return hoisted.tagsMutation;
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const mcpServer = {enabled: true, id: '5', name: 'Server', tags: [{id: '10', name: 'crm'}]} as unknown as McpServer;

describe('useMcpServerListItem', () => {
    beforeEach(() => {
        hoisted.deleteMutate.mockClear();
        hoisted.invalidateQueries.mockClear();
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

    it('keeps the toggle pending until the embedded server update succeeds', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        act(() => {
            void result.current.handleOnCheckedChange(true);
        });

        expect(result.current.isEnablePending).toBe(true);

        const updateOptions = hoisted.updateMutate.mock.calls[0][1];

        act(() => updateOptions.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
        expect(result.current.isEnablePending).toBe(false);
    });

    it('updates tags through the embedded tags mutation', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        expect(result.current.updateEmbeddedMcpServerTagsMutation).toBe(hoisted.tagsMutation);
        expect(result.current.mcpServerTagIds).toEqual(['10']);
    });

    it('refreshes the servers and the embedded tags after the tags are updated', () => {
        renderHook(() => useMcpServerListItem(mcpServer));

        act(() => hoisted.tagsMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpServers']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServerTags']});
        expect(hoisted.invalidateQueries).not.toHaveBeenCalledWith({queryKey: ['mcpServerTags']});
    });

    it('deletes the server and closes the delete dialog', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        act(() => result.current.setShowDeleteDialog(true));

        act(() => {
            void result.current.handleDeleteClick();
        });

        expect(hoisted.deleteMutate).toHaveBeenCalledWith(
            {mcpServerId: '5'},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );

        const deleteOptions = hoisted.deleteMutate.mock.calls[0][1];

        act(() => deleteOptions.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
        expect(result.current.showDeleteDialog).toBe(false);
    });
});
