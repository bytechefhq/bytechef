import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockSetContext: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockSetSaving: vi.fn(),
        mockSetScriptIsRunning: vi.fn(),
        mockSetScriptTestExecution: vi.fn(),
        mockTestClusterElementScript: vi.fn(),
        mockTestWorkflowNodeScript: vi.fn(),
        storeState: {
            ai: {copilot: {enabled: true}},
            currentEnvironmentId: 1,
            currentNode: undefined as {clusterElementType?: string; name: string} | undefined,
            dirty: false,
            editorValue: 'const x = 1;',
            ff_1570: true,
            rootClusterElementNodeData: undefined as {workflowNodeName: string} | undefined,
            saving: false,
            scriptIsRunning: false,
        },
    };
});

vi.mock('../../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            dirty: hoisted.storeState.dirty,
            editorValue: hoisted.storeState.editorValue,
            saving: hoisted.storeState.saving,
            scriptIsRunning: hoisted.storeState.scriptIsRunning,
            setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen,
            setSaving: hoisted.mockSetSaving,
            setScriptIsRunning: hoisted.mockSetScriptIsRunning,
            setScriptTestExecution: hoisted.mockSetScriptTestExecution,
        }),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', () => ({
    MODE: {ASK: 'ASK'},
    Source: {CODE_EDITOR: 'CODE_EDITOR'},
    useCopilotStore: Object.assign(
        (selector: (state: unknown) => unknown) =>
            selector({
                setContext: hoisted.mockSetContext,
            }),
        {
            getState: () => ({
                context: {existingProp: 'value'},
            }),
        }
    ),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({
            ai: hoisted.storeState.ai,
        }),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: unknown) => unknown) =>
        selector({
            currentEnvironmentId: hoisted.storeState.currentEnvironmentId,
        }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => hoisted.storeState.ff_1570,
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowNodeScriptApi: class {
        testWorkflowNodeScript = hoisted.mockTestWorkflowNodeScript;
    },
}));

vi.mock('@/shared/mutations/platform/clusterElementScript.mutations', () => ({
    testClusterElementScript: hoisted.mockTestClusterElementScript,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) =>
        selector({
            currentNode: hoisted.storeState.currentNode,
        }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: unknown) => unknown) =>
        selector({
            rootClusterElementNodeData: hoisted.storeState.rootClusterElementNodeData,
        }),
}));

describe('usePropertyCodeEditorDialogToolbar', () => {
    const defaultProps = {
        language: 'javascript',
        onChange: vi.fn(),
        workflowId: 'workflow-1',
        workflowNodeName: 'testNode',
    };

    beforeEach(async () => {
        hoisted.storeState.dirty = false;
        hoisted.storeState.saving = false;
        hoisted.storeState.scriptIsRunning = false;
        hoisted.storeState.editorValue = 'const x = 1;';
        hoisted.storeState.currentEnvironmentId = 1;
        hoisted.storeState.ai = {copilot: {enabled: true}};
        hoisted.storeState.ff_1570 = true;
        hoisted.storeState.currentNode = undefined;
        hoisted.storeState.rootClusterElementNodeData = undefined;
        hoisted.mockTestWorkflowNodeScript.mockResolvedValue({output: 'test result'});
        hoisted.mockTestClusterElementScript.mockResolvedValue({output: 'cluster element result'});
        vi.clearAllMocks();
        vi.resetModules();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('should return dirty from store', async () => {
            hoisted.storeState.dirty = true;

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(result.current.dirty).toBe(true);
        });

        it('should return saving from store', async () => {
            hoisted.storeState.saving = true;

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(result.current.saving).toBe(true);
        });

        it('should return scriptIsRunning from store', async () => {
            hoisted.storeState.scriptIsRunning = true;

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(result.current.scriptIsRunning).toBe(true);
        });
    });

    describe('copilotEnabled', () => {
        it('should be true when both ai.copilot.enabled and feature flag are true', async () => {
            hoisted.storeState.ai = {copilot: {enabled: true}};
            hoisted.storeState.ff_1570 = true;

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(result.current.copilotEnabled).toBe(true);
        });

        it('should be false when ai.copilot.enabled is false', async () => {
            hoisted.storeState.ai = {copilot: {enabled: false}};
            hoisted.storeState.ff_1570 = true;

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(result.current.copilotEnabled).toBe(false);
        });
    });

    describe('handleSaveClick', () => {
        it('should call setSaving with true and onChange with editor value', async () => {
            const mockOnChange = vi.fn();
            hoisted.storeState.editorValue = 'new code';

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() =>
                usePropertyCodeEditorDialogToolbar({
                    ...defaultProps,
                    onChange: mockOnChange,
                })
            );

            act(() => {
                result.current.handleSaveClick();
            });

            expect(hoisted.mockSetSaving).toHaveBeenCalledWith(true);
            expect(mockOnChange).toHaveBeenCalledWith('new code');
        });
    });

    describe('handleRunClick', () => {
        it('should call setScriptIsRunning with true', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            expect(hoisted.mockSetScriptIsRunning).toHaveBeenCalledWith(true);
        });

        it('should call testWorkflowNodeScript with correct params', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            expect(hoisted.mockTestWorkflowNodeScript).toHaveBeenCalledWith({
                environmentId: 1,
                id: 'workflow-1',
                workflowNodeName: 'testNode',
            });
        });

        it('should set script test execution on success', async () => {
            hoisted.mockTestWorkflowNodeScript.mockResolvedValue({output: 'success'});

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            await waitFor(() => {
                expect(hoisted.mockSetScriptTestExecution).toHaveBeenCalledWith({output: 'success'});
            });

            await waitFor(() => {
                expect(hoisted.mockSetScriptIsRunning).toHaveBeenCalledWith(false);
            });
        });

        it('should set scriptIsRunning to false on error', async () => {
            hoisted.mockTestWorkflowNodeScript.mockRejectedValue(new Error('Test error'));

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            await waitFor(() => {
                expect(hoisted.mockSetScriptIsRunning).toHaveBeenLastCalledWith(false);
            });
        });

        describe('cluster element script', () => {
            it('should call testClusterElementScript when currentNode has clusterElementType', async () => {
                hoisted.storeState.currentNode = {
                    clusterElementType: 'TOOL',
                    name: 'tool_1',
                };
                hoisted.storeState.rootClusterElementNodeData = {
                    workflowNodeName: 'aiAgent_1',
                };

                const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
                const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

                act(() => {
                    result.current.handleRunClick();
                });

                expect(hoisted.mockTestClusterElementScript).toHaveBeenCalledWith({
                    clusterElementType: 'TOOL',
                    clusterElementWorkflowNodeName: 'tool_1',
                    environmentId: 1,
                    workflowId: 'workflow-1',
                    workflowNodeName: 'aiAgent_1',
                });
                expect(hoisted.mockTestWorkflowNodeScript).not.toHaveBeenCalled();
            });

            it('should set script test execution on cluster element script success', async () => {
                hoisted.storeState.currentNode = {
                    clusterElementType: 'SYSTEM_PROMPT',
                    name: 'systemPrompt_1',
                };
                hoisted.storeState.rootClusterElementNodeData = {
                    workflowNodeName: 'aiAgent_1',
                };
                hoisted.mockTestClusterElementScript.mockResolvedValue({output: 'cluster success'});

                const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
                const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

                act(() => {
                    result.current.handleRunClick();
                });

                await waitFor(() => {
                    expect(hoisted.mockSetScriptTestExecution).toHaveBeenCalledWith({output: 'cluster success'});
                });

                await waitFor(() => {
                    expect(hoisted.mockSetScriptIsRunning).toHaveBeenCalledWith(false);
                });
            });

            it('should set scriptIsRunning to false on cluster element script error', async () => {
                hoisted.storeState.currentNode = {
                    clusterElementType: 'TOOL',
                    name: 'tool_1',
                };
                hoisted.storeState.rootClusterElementNodeData = {
                    workflowNodeName: 'aiAgent_1',
                };
                hoisted.mockTestClusterElementScript.mockRejectedValue(new Error('Cluster error'));

                const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
                const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

                act(() => {
                    result.current.handleRunClick();
                });

                await waitFor(() => {
                    expect(hoisted.mockSetScriptIsRunning).toHaveBeenLastCalledWith(false);
                });
            });

            it('should fall back to testWorkflowNodeScript when rootClusterElementNodeData is missing', async () => {
                hoisted.storeState.currentNode = {
                    clusterElementType: 'TOOL',
                    name: 'tool_1',
                };
                hoisted.storeState.rootClusterElementNodeData = undefined;

                const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
                const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

                act(() => {
                    result.current.handleRunClick();
                });

                expect(hoisted.mockTestWorkflowNodeScript).toHaveBeenCalled();
                expect(hoisted.mockTestClusterElementScript).not.toHaveBeenCalled();
            });
        });
    });

    describe('handleCopilotClick', () => {
        it('should call setContext with correct parameters', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleCopilotClick();
            });

            expect(hoisted.mockSetContext).toHaveBeenCalledWith({
                existingProp: 'value',
                mode: 'ASK',
                parameters: {language: 'javascript'},
                source: 'CODE_EDITOR',
            });
        });

        it('should call setCopilotPanelOpen with true', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleCopilotClick();
            });

            expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
        });
    });

    describe('handleStopClick', () => {
        it('should be a callable function', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(typeof result.current.handleStopClick).toBe('function');

            // Should not throw
            act(() => {
                result.current.handleStopClick();
            });
        });
    });
});
