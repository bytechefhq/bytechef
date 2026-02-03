import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it} from 'vitest';

import {usePropertyCodeEditorDialogStore} from '../usePropertyCodeEditorDialogStore';

describe('usePropertyCodeEditorDialogStore', () => {
    afterEach(() => {
        act(() => {
            usePropertyCodeEditorDialogStore.getState().reset();
        });
    });

    describe('initial state', () => {
        it('has correct default values', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            expect(result.current.copilotPanelOpen).toBe(false);
            expect(result.current.dirty).toBe(false);
            expect(result.current.editorValue).toBeUndefined();
            expect(result.current.saving).toBe(false);
            expect(result.current.scriptIsRunning).toBe(false);
            expect(result.current.scriptTestExecution).toBeUndefined();
        });
    });

    describe('setCopilotPanelOpen', () => {
        it('sets copilotPanelOpen to true', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setCopilotPanelOpen(true);
            });

            expect(result.current.copilotPanelOpen).toBe(true);
        });

        it('sets copilotPanelOpen to false', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setCopilotPanelOpen(true);
            });

            act(() => {
                result.current.setCopilotPanelOpen(false);
            });

            expect(result.current.copilotPanelOpen).toBe(false);
        });
    });

    describe('setDirty', () => {
        it('sets dirty to true', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setDirty(true);
            });

            expect(result.current.dirty).toBe(true);
        });

        it('sets dirty to false', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setDirty(true);
            });

            act(() => {
                result.current.setDirty(false);
            });

            expect(result.current.dirty).toBe(false);
        });
    });

    describe('setEditorValue', () => {
        it('sets editor value to a string', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());
            const testCode = 'const x = 1;';

            act(() => {
                result.current.setEditorValue(testCode);
            });

            expect(result.current.editorValue).toBe(testCode);
        });

        it('sets editor value to undefined', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setEditorValue('some code');
            });

            act(() => {
                result.current.setEditorValue(undefined);
            });

            expect(result.current.editorValue).toBeUndefined();
        });
    });

    describe('setSaving', () => {
        it('sets saving to true', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setSaving(true);
            });

            expect(result.current.saving).toBe(true);
        });

        it('sets saving to false', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setSaving(true);
            });

            act(() => {
                result.current.setSaving(false);
            });

            expect(result.current.saving).toBe(false);
        });
    });

    describe('setScriptIsRunning', () => {
        it('sets scriptIsRunning to true', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setScriptIsRunning(true);
            });

            expect(result.current.scriptIsRunning).toBe(true);
        });

        it('sets scriptIsRunning to false', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setScriptIsRunning(true);
            });

            act(() => {
                result.current.setScriptIsRunning(false);
            });

            expect(result.current.scriptIsRunning).toBe(false);
        });
    });

    describe('setScriptTestExecution', () => {
        it('sets scriptTestExecution to an object', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());
            const execution = {output: {result: 'test'}};

            act(() => {
                result.current.setScriptTestExecution(execution);
            });

            expect(result.current.scriptTestExecution).toEqual(execution);
        });

        it('sets scriptTestExecution to undefined', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setScriptTestExecution({output: {result: 'test'}});
            });

            act(() => {
                result.current.setScriptTestExecution(undefined);
            });

            expect(result.current.scriptTestExecution).toBeUndefined();
        });
    });

    describe('reset', () => {
        it('resets all state to initial values', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setCopilotPanelOpen(true);
                result.current.setDirty(true);
                result.current.setEditorValue('some code');
                result.current.setSaving(true);
                result.current.setScriptIsRunning(true);
                result.current.setScriptTestExecution({output: {result: 'test'}});
            });

            expect(result.current.copilotPanelOpen).toBe(true);
            expect(result.current.dirty).toBe(true);
            expect(result.current.editorValue).toBe('some code');
            expect(result.current.saving).toBe(true);
            expect(result.current.scriptIsRunning).toBe(true);
            expect(result.current.scriptTestExecution).toBeDefined();

            act(() => {
                result.current.reset();
            });

            expect(result.current.copilotPanelOpen).toBe(false);
            expect(result.current.dirty).toBe(false);
            expect(result.current.editorValue).toBeUndefined();
            expect(result.current.saving).toBe(false);
            expect(result.current.scriptIsRunning).toBe(false);
            expect(result.current.scriptTestExecution).toBeUndefined();
        });
    });

    describe('store persistence across rerenders', () => {
        it('maintains state when component rerenders', () => {
            const {rerender, result} = renderHook(() => usePropertyCodeEditorDialogStore());

            act(() => {
                result.current.setEditorValue('persistent code');
                result.current.setDirty(true);
            });

            rerender();

            expect(result.current.editorValue).toBe('persistent code');
            expect(result.current.dirty).toBe(true);
        });
    });
});
