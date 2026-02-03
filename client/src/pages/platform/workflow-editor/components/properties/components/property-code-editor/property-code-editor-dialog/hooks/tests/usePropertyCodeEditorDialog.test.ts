import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockReset: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockSetDirty: vi.fn(),
        mockSetEditorValue: vi.fn(),
        mockSetSaving: vi.fn(),
        storeState: {
            copilotPanelOpen: false,
            dirty: false,
            editorValue: undefined as string | undefined,
        },
    };
});

vi.mock('../../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            copilotPanelOpen: hoisted.storeState.copilotPanelOpen,
            dirty: hoisted.storeState.dirty,
            editorValue: hoisted.storeState.editorValue,
            reset: hoisted.mockReset,
            setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen,
            setDirty: hoisted.mockSetDirty,
            setEditorValue: hoisted.mockSetEditorValue,
            setSaving: hoisted.mockSetSaving,
        }),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getTask', () => ({
    getTask: ({tasks, workflowNodeName}: {tasks: Array<{name: string}>; workflowNodeName: string}) =>
        tasks.find((task) => task.name === workflowNodeName),
}));

describe('usePropertyCodeEditorDialog', () => {
    const defaultProps = {
        onClose: vi.fn(),
        value: 'const x = 1;',
        workflow: {
            id: 'workflow-1',
            tasks: [{connections: [], name: 'testNode', parameters: {}, type: 'script/script'}],
            version: 1,
        },
        workflowNodeName: 'testNode',
    };

    beforeEach(async () => {
        hoisted.storeState.copilotPanelOpen = false;
        hoisted.storeState.dirty = false;
        hoisted.storeState.editorValue = undefined;
        vi.clearAllMocks();

        // Need to reset modules to get fresh import
        vi.resetModules();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('should return copilotPanelOpen from store', async () => {
            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const {result} = renderHook(() => usePropertyCodeEditorDialog(defaultProps));

            expect(result.current.copilotPanelOpen).toBe(false);
        });

        it('should return unsavedChangesAlertDialogOpen as false initially', async () => {
            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const {result} = renderHook(() => usePropertyCodeEditorDialog(defaultProps));

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(false);
        });

        it('should return current workflow task', async () => {
            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const {result} = renderHook(() => usePropertyCodeEditorDialog(defaultProps));

            expect(result.current.currentWorkflowTask).toEqual({
                connections: [],
                name: 'testNode',
                parameters: {},
                type: 'script/script',
            });
        });
    });

    describe('handleOpenChange', () => {
        it('should call handleClose when open is false and not dirty', async () => {
            hoisted.storeState.dirty = false;

            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const mockOnClose = vi.fn();
            const {result} = renderHook(() =>
                usePropertyCodeEditorDialog({
                    ...defaultProps,
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.mockReset).toHaveBeenCalled();
            expect(mockOnClose).toHaveBeenCalled();
        });

        it('should open unsaved changes dialog when open is false and dirty', async () => {
            hoisted.storeState.dirty = true;

            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const {result} = renderHook(() => usePropertyCodeEditorDialog(defaultProps));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(true);
        });
    });

    describe('handleUnsavedChangesAlertDialogCancel', () => {
        it('should close the unsaved changes dialog', async () => {
            hoisted.storeState.dirty = true;

            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const {result} = renderHook(() => usePropertyCodeEditorDialog(defaultProps));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(true);

            act(() => {
                result.current.handleUnsavedChangesAlertDialogCancel();
            });

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(false);
        });
    });

    describe('handleUnsavedChangesAlertDialogClose', () => {
        it('should close dialog and call reset and onClose', async () => {
            hoisted.storeState.dirty = true;

            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const mockOnClose = vi.fn();
            const {result} = renderHook(() =>
                usePropertyCodeEditorDialog({
                    ...defaultProps,
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleOpenChange(false);
            });

            act(() => {
                result.current.handleUnsavedChangesAlertDialogClose();
            });

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(false);
            expect(hoisted.mockReset).toHaveBeenCalled();
            expect(mockOnClose).toHaveBeenCalled();
        });
    });

    describe('handleCopilotClose', () => {
        it('should call setCopilotPanelOpen with false', async () => {
            const {usePropertyCodeEditorDialog} = await import('../usePropertyCodeEditorDialog');
            const {result} = renderHook(() => usePropertyCodeEditorDialog(defaultProps));

            act(() => {
                result.current.handleCopilotClose();
            });

            expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(false);
        });
    });
});
