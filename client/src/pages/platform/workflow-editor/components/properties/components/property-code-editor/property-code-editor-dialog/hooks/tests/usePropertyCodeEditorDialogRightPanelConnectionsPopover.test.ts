import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockComponentDefinitions: [
            {name: 'component1', title: 'Component 1', version: 1},
            {name: 'component2', title: 'Component 2', version: 1},
        ],
        mockFormState: {
            errors: {},
            getValues: vi.fn(),
            handleSubmit: vi.fn(),
            register: vi.fn(),
            reset: vi.fn(),
            setValue: vi.fn(),
            watch: vi.fn(),
        },
    };
});

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        useGetComponentDefinitionsQuery: () => ({
            data: hoisted.mockComponentDefinitions,
        }),
    }),
}));

vi.mock('@hookform/resolvers/zod', () => ({
    zodResolver: () => vi.fn(),
}));

vi.mock('react-hook-form', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-hook-form')>();

    return {
        ...actual,
        useForm: () => hoisted.mockFormState,
    };
});

vi.mock('../PropertyCodeEditorDialogRightPanelConnectionsPopover', () => ({
    connectionFormSchema: {
        parse: vi.fn(),
    },
}));

import usePropertyCodeEditorDialogRightPanelConnectionsPopover from '../usePropertyCodeEditorDialogRightPanelConnectionsPopover';

describe('usePropertyCodeEditorDialogRightPanelConnectionsPopover', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('should return open as false initially', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsPopover());

            expect(result.current.open).toBe(false);
        });

        it('should return form object', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsPopover());

            expect(result.current.form).toBeDefined();
        });

        it('should return component definitions from query', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsPopover());

            expect(result.current.componentDefinitions).toEqual(hoisted.mockComponentDefinitions);
        });
    });

    describe('setOpen', () => {
        it('should update open state to true', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsPopover());

            act(() => {
                result.current.setOpen(true);
            });

            expect(result.current.open).toBe(true);
        });

        it('should update open state to false', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsPopover());

            act(() => {
                result.current.setOpen(true);
            });

            act(() => {
                result.current.setOpen(false);
            });

            expect(result.current.open).toBe(false);
        });
    });

    describe('componentDefinitions', () => {
        it('should contain expected component definitions', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsPopover());

            expect(result.current.componentDefinitions).toHaveLength(2);
            expect(result.current.componentDefinitions?.[0].name).toBe('component1');
            expect(result.current.componentDefinitions?.[1].name).toBe('component2');
        });
    });
});
