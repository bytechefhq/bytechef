import {McpTool} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpProjectComponentToolDropdownMenu from '../useMcpProjectComponentToolDropdownMenu';

const hoisted = vi.hoisted(() => ({
    deleteEmbeddedMutate: vi.fn(),
    deleteMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteEmbeddedMcpToolMutation: () => ({isPending: false, mutate: hoisted.deleteEmbeddedMutate}),
    useDeleteMcpToolMutation: () => ({isPending: false, mutate: hoisted.deleteMutate}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

const mcpTool = {id: '42', name: 'createOpportunity'} as McpTool;

describe('useMcpProjectComponentToolDropdownMenu', () => {
    beforeEach(() => {
        hoisted.deleteEmbeddedMutate.mockClear();
        hoisted.deleteMutate.mockClear();
    });

    it('deletes an automation tool through the shared mutation', () => {
        const {result} = renderHook(() => useMcpProjectComponentToolDropdownMenu({mcpTool}));

        act(() => result.current.handleConfirmDelete());

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '42'});
        expect(hoisted.deleteEmbeddedMutate).not.toHaveBeenCalled();
    });

    it('deletes an embedded tool through the embedded mutation', () => {
        const {result} = renderHook(() => useMcpProjectComponentToolDropdownMenu({embedded: true, mcpTool}));

        act(() => result.current.handleConfirmDelete());

        expect(hoisted.deleteEmbeddedMutate).toHaveBeenCalledWith({id: '42'});
        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
    });
});
