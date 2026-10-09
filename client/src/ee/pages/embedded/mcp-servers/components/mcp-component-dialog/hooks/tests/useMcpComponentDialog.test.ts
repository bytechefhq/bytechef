import {McpComponent} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialog from '../useMcpComponentDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateEmbeddedMcpComponentMutation: () => ({mutate: hoisted.createMutate}),
    useMcpToolsByComponentIdQuery: () => ({data: undefined}),
    useUpdateEmbeddedMcpComponentMutation: () => ({mutate: hoisted.updateMutate}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

describe('useMcpComponentDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockClear();
        hoisted.updateMutate.mockClear();
    });

    it('creates the component through the embedded mutation', () => {
        const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        act(() => result.current.handleComponentSelect({name: 'gmail', title: 'Gmail', version: 1} as never));

        act(() => result.current.handleSave());

        expect(hoisted.createMutate).toHaveBeenCalledWith(
            {input: {componentName: 'gmail', componentVersion: 1, mcpServerId: '1', tools: []}},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });

    it('updates the component through the embedded mutation', () => {
        const mcpComponent = {
            componentName: 'gmail',
            componentVersion: 1,
            id: '7',
            title: 'Gmail',
            version: 2,
        } as McpComponent;

        const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        act(() => result.current.handleSave());

        expect(hoisted.updateMutate).toHaveBeenCalledWith(
            {
                id: '7',
                input: {componentName: 'gmail', componentVersion: 1, mcpServerId: '1', tools: [], version: 2},
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(hoisted.createMutate).not.toHaveBeenCalled();
    });
});
