import {McpTool} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpProjectComponentToolDropdownMenu from '../useMcpProjectComponentToolDropdownMenu';

const hoisted = vi.hoisted(() => ({
    deleteEmbeddedMutate: vi.fn(),
    deleteEmbeddedMutationOptions: undefined as {onSuccess: () => void} | undefined,
    deleteEmbeddedPending: false,
    deleteMutate: vi.fn(),
    deleteMutationOptions: undefined as {onSuccess: () => void} | undefined,
    deletePending: false,
    invalidateQueries: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteEmbeddedMcpToolMutation: (options: {onSuccess: () => void}) => {
        hoisted.deleteEmbeddedMutationOptions = options;

        return {isPending: hoisted.deleteEmbeddedPending, mutate: hoisted.deleteEmbeddedMutate};
    },
    useDeleteMcpToolMutation: (options: {onSuccess: () => void}) => {
        hoisted.deleteMutationOptions = options;

        return {isPending: hoisted.deletePending, mutate: hoisted.deleteMutate};
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const mcpTool = {id: '42', name: 'createOpportunity'} as McpTool;

describe('useMcpProjectComponentToolDropdownMenu', () => {
    beforeEach(() => {
        hoisted.deleteEmbeddedMutate.mockClear();
        hoisted.deleteMutate.mockClear();
        hoisted.invalidateQueries.mockClear();

        hoisted.deleteEmbeddedPending = false;
        hoisted.deletePending = false;
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

    it('refreshes the automation components and closes the dialog after an automation tool is deleted', () => {
        const {result} = renderHook(() => useMcpProjectComponentToolDropdownMenu({mcpTool}));

        act(() => result.current.setShowDeleteDialog(true));

        expect(result.current.showDeleteDialog).toBe(true);

        act(() => hoisted.deleteMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
        expect(result.current.showDeleteDialog).toBe(false);
    });

    it('refreshes the embedded components and closes the dialog after an embedded tool is deleted', () => {
        const {result} = renderHook(() => useMcpProjectComponentToolDropdownMenu({embedded: true, mcpTool}));

        act(() => result.current.setShowDeleteDialog(true));

        act(() => hoisted.deleteEmbeddedMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
        expect(result.current.showDeleteDialog).toBe(false);
    });

    it('reports the pending state of the mutation that matches the tool kind', () => {
        hoisted.deleteEmbeddedPending = true;

        const {result: automationResult} = renderHook(() => useMcpProjectComponentToolDropdownMenu({mcpTool}));
        const {result: embeddedResult} = renderHook(() =>
            useMcpProjectComponentToolDropdownMenu({embedded: true, mcpTool})
        );

        expect(automationResult.current.isDeletePending).toBe(false);
        expect(embeddedResult.current.isDeletePending).toBe(true);
    });
});
