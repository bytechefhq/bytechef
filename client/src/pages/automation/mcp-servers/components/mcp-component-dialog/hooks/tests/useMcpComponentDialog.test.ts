import {McpComponent} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialog from '../useMcpComponentDialog';

const hoisted = vi.hoisted(() => ({
    authorities: undefined as undefined | string[],
    createMutate: vi.fn(),
    createOnSuccess: undefined as undefined | (() => void),
    invalidateQueries: vi.fn(),
    updateMutate: vi.fn(),
    updateOnSuccess: undefined as undefined | (() => void),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAuthoritiesQuery: () => ({
        data: hoisted.authorities ? {authorities: hoisted.authorities} : undefined,
        isLoading: false,
    }),
    useCreateMcpComponentWithToolsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.createOnSuccess = onSuccess;

        return {mutate: hoisted.createMutate};
    },
    useMcpToolsByComponentIdQuery: () => ({data: undefined}),
    useUpdateMcpComponentWithToolsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.updateOnSuccess = onSuccess;

        return {mutate: hoisted.updateMutate};
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const existingMcpComponent = {
    componentName: 'slack',
    componentVersion: 1,
    id: '5',
    requiredAuthorities: ['ROLE_EDITOR'],
    title: 'Slack',
    version: 3,
} as unknown as McpComponent;

const selectableComponent = {name: 'github', title: 'GitHub', version: 2} as ComponentDefinitionBasic;

const runMutationSuccess = (mutate: ReturnType<typeof vi.fn>) => {
    const [, options] = mutate.mock.calls[mutate.mock.calls.length - 1] as [unknown, {onSuccess: () => void}];

    act(() => options.onSuccess());
};

const invalidatedKeys = () => hoisted.invalidateQueries.mock.calls.map(([argument]) => argument.queryKey[0]);

describe('useMcpComponentDialog', () => {
    beforeEach(() => {
        hoisted.authorities = undefined;
        hoisted.createMutate.mockReset();
        hoisted.invalidateQueries.mockClear();
        hoisted.updateMutate.mockReset();
    });

    // The server list is served by useWorkspaceMcpServersQuery and the per-server component count is read off that
    // payload, so a save that only invalidated 'mcpServers' left the count stale until a page reload.
    it('invalidates the workspace servers query after creating a component', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        hoisted.createOnSuccess?.();

        expect(invalidatedKeys()).toContain('workspaceMcpServers');
    });

    it('invalidates the workspace servers query after updating a component', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        hoisted.updateOnSuccess?.();

        expect(invalidatedKeys()).toContain('workspaceMcpServers');
    });

    describe('authorityOptions', () => {
        it('merges the server authorities with the component ones, without duplicates, sorted by label', () => {
            hoisted.authorities = ['ROLE_VIEWER', 'ROLE_ADMIN', 'ROLE_EDITOR'];

            const {result} = renderHook(() =>
                useMcpComponentDialog({
                    mcpComponent: {
                        ...existingMcpComponent,
                        requiredAuthorities: ['ROLE_EDITOR', 'ROLE_AUDITOR'],
                    } as McpComponent,
                    mcpServerId: '1',
                    open: true,
                })
            );

            expect(result.current.authorityOptions).toEqual([
                {label: 'Admin', value: 'ROLE_ADMIN'},
                {label: 'Auditor', value: 'ROLE_AUDITOR'},
                {label: 'Editor', value: 'ROLE_EDITOR'},
                {label: 'Viewer', value: 'ROLE_VIEWER'},
            ]);
        });

        it('lists no authorities when neither the server nor the component provides any', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            expect(result.current.authorityOptions).toEqual([]);
            expect(result.current.requiredAuthorities).toEqual([]);
        });
    });

    describe('requiredAuthorities reset', () => {
        it('restores the component authorities when the dialog is closed', () => {
            const onOpenChange = vi.fn();

            const {result} = renderHook(() =>
                useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', onOpenChange, open: true})
            );

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleClose());

            expect(onOpenChange).toHaveBeenCalledWith(false);
            expect(result.current.requiredAuthorities).toEqual(['ROLE_EDITOR']);
        });

        it('clears the authorities of a new component when the dialog is closed', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleClose());

            expect(result.current.requiredAuthorities).toEqual([]);
        });

        it('restores the component authorities when the dialog open state turns off', () => {
            const {result} = renderHook(() =>
                useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', open: true})
            );

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleOpenChange(true));

            expect(result.current.requiredAuthorities).toEqual(['ROLE_ADMIN']);

            act(() => result.current.handleOpenChange(false));

            expect(result.current.requiredAuthorities).toEqual(['ROLE_EDITOR']);
        });

        it('clears the authorities of a new component when the dialog open state turns off', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleOpenChange(false));

            expect(result.current.requiredAuthorities).toEqual([]);
        });
    });

    describe('handleSave', () => {
        it('updates an existing component with the chosen authorities and restores them after success', () => {
            const {result} = renderHook(() =>
                useMcpComponentDialog({mcpComponent: existingMcpComponent, mcpServerId: '1', open: true})
            );

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleSave());

            expect(hoisted.updateMutate).toHaveBeenCalledWith(
                {
                    id: '5',
                    input: expect.objectContaining({
                        componentName: 'slack',
                        mcpServerId: '1',
                        requiredAuthorities: ['ROLE_ADMIN'],
                        version: 3,
                    }),
                },
                expect.anything()
            );

            runMutationSuccess(hoisted.updateMutate);

            expect(result.current.requiredAuthorities).toEqual(['ROLE_EDITOR']);
        });

        it('creates a new component with the chosen authorities and clears them after success', () => {
            const {result} = renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

            act(() => result.current.handleComponentSelect(selectableComponent));

            act(() => result.current.setRequiredAuthorities(['ROLE_ADMIN']));

            act(() => result.current.handleSave());

            expect(hoisted.createMutate).toHaveBeenCalledWith(
                {
                    input: expect.objectContaining({
                        componentName: 'github',
                        mcpServerId: '1',
                        requiredAuthorities: ['ROLE_ADMIN'],
                    }),
                },
                expect.anything()
            );

            runMutationSuccess(hoisted.createMutate);

            expect(result.current.requiredAuthorities).toEqual([]);
            expect(result.current.selectedComponent).toBeNull();
        });
    });
});
