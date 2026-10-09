import {McpComponent} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialog from '../useMcpComponentDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    createMutationOptions: undefined as {onSuccess: () => void} | undefined,
    invalidateQueries: vi.fn(),
    updateMutate: vi.fn(),
    updateMutationOptions: undefined as {onSuccess: () => void} | undefined,
    useEmbeddedMcpToolsByComponentIdQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateEmbeddedMcpComponentMutation: (options: {onSuccess: () => void}) => {
        hoisted.createMutationOptions = options;

        return {mutate: hoisted.createMutate};
    },
    useEmbeddedMcpToolsByComponentIdQuery: hoisted.useEmbeddedMcpToolsByComponentIdQuery,
    useUpdateEmbeddedMcpComponentMutation: (options: {onSuccess: () => void}) => {
        hoisted.updateMutationOptions = options;

        return {mutate: hoisted.updateMutate};
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const mcpComponent = {
    componentName: 'gmail',
    componentVersion: 1,
    id: '7',
    title: 'Gmail',
    version: 2,
} as McpComponent;

describe('useMcpComponentDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockClear();
        hoisted.invalidateQueries.mockClear();
        hoisted.updateMutate.mockClear();
        hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReset();
        hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReturnValue({data: undefined});
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

    it('loads the existing embedded tools only for a saved component while the dialog is open', () => {
        const existingTools = {embeddedMcpToolsByComponentId: [{name: 'sendEmail'}]};

        hoisted.useEmbeddedMcpToolsByComponentIdQuery.mockReturnValue({data: existingTools});

        const {result} = renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        expect(hoisted.useEmbeddedMcpToolsByComponentIdQuery).toHaveBeenCalledWith(
            {mcpComponentId: '7'},
            {enabled: true}
        );
        expect(result.current.existingTools).toBe(existingTools);

        renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: false}));

        expect(hoisted.useEmbeddedMcpToolsByComponentIdQuery).toHaveBeenLastCalledWith(
            {mcpComponentId: '7'},
            {enabled: false}
        );

        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        expect(hoisted.useEmbeddedMcpToolsByComponentIdQuery).toHaveBeenLastCalledWith(
            {mcpComponentId: ''},
            {enabled: false}
        );
    });

    it('refreshes the embedded components and servers after a component is created', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        act(() => hoisted.createMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
        expect(hoisted.invalidateQueries).not.toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
    });

    it('refreshes the embedded components and servers after a component is updated', () => {
        renderHook(() => useMcpComponentDialog({mcpComponent, mcpServerId: '1', open: true}));

        act(() => hoisted.updateMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpServers']});
    });

    it('closes the dialog and resets to the component step after a new component is saved', () => {
        const onOpenChange = vi.fn();

        const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', onOpenChange, open: true}));

        act(() => result.current.handleComponentSelect({name: 'gmail', title: 'Gmail', version: 1} as never));

        expect(result.current.currentStep).toBe('tools');

        act(() => result.current.handleSave());

        const saveOptions = hoisted.createMutate.mock.calls[0][1];

        act(() => saveOptions.onSuccess());

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(result.current.currentStep).toBe('components');
        expect(result.current.selectedComponent).toBeNull();
    });
});
