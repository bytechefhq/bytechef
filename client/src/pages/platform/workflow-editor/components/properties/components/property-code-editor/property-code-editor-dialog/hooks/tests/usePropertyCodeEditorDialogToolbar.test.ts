import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockGenerateConversationId: vi.fn(),
        mockMutateAsyncClusterElement: vi.fn(),
        mockMutateAsyncWorkflowNode: vi.fn(),
        mockResetMessages: vi.fn(),
        mockSaveConversationState: vi.fn(),
        mockSetContext: vi.fn(),
        mockSetConversationToken: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockSetSaving: vi.fn(),
        mockSetScriptIsRunning: vi.fn(),
        mockSetScriptTestExecution: vi.fn(),
        mockStopScriptTestMutate: vi.fn(),
        storeState: {
            ai: {copilot: {enabled: true}},
            currentEnvironmentId: 1,
            currentNode: undefined as {clusterElementType?: string; name: string} | undefined,
            dirty: false,
            editorValue: 'const x = 1;',
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
            setConversationToken: hoisted.mockSetConversationToken,
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
                generateConversationId: hoisted.mockGenerateConversationId,
                resetMessages: hoisted.mockResetMessages,
                saveConversationState: hoisted.mockSaveConversationState,
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

vi.mock('@/shared/middleware/graphql', () => ({
    useStopScriptTestMutation: () => ({
        mutate: hoisted.mockStopScriptTestMutate,
    }),
    useTestClusterElementScriptMutation: () => ({
        mutateAsync: hoisted.mockMutateAsyncClusterElement,
    }),
    useTestWorkflowNodeScriptMutation: () => ({
        mutateAsync: hoisted.mockMutateAsyncWorkflowNode,
    }),
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
        hoisted.storeState.currentNode = undefined;
        hoisted.storeState.rootClusterElementNodeData = undefined;
        hoisted.mockMutateAsyncWorkflowNode.mockResolvedValue({
            testWorkflowNodeScript: {output: 'test result'},
        });
        hoisted.mockMutateAsyncClusterElement.mockResolvedValue({
            testClusterElementScript: {output: 'cluster element result'},
        });
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
        it('should be true when ai.copilot.enabled is true', async () => {
            hoisted.storeState.ai = {copilot: {enabled: true}};

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            expect(result.current.copilotEnabled).toBe(true);
        });

        it('should be false when ai.copilot.enabled is false', async () => {
            hoisted.storeState.ai = {copilot: {enabled: false}};

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

        it('should call testWorkflowNodeScript mutation with correct params', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            expect(hoisted.mockMutateAsyncWorkflowNode).toHaveBeenCalledWith({
                environmentId: 1,
                executionId: expect.any(String),
                workflowId: 'workflow-1',
                workflowNodeName: 'testNode',
            });
        });

        it('should set script test execution on success', async () => {
            hoisted.mockMutateAsyncWorkflowNode.mockResolvedValue({
                testWorkflowNodeScript: {output: 'success'},
            });

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
            hoisted.mockMutateAsyncWorkflowNode.mockRejectedValue(new Error('Test error'));

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
            it('should call testClusterElementScript mutation when currentNode has clusterElementType', async () => {
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

                expect(hoisted.mockMutateAsyncClusterElement).toHaveBeenCalledWith({
                    clusterElementType: 'TOOL',
                    clusterElementWorkflowNodeName: 'tool_1',
                    environmentId: 1,
                    executionId: expect.any(String),
                    workflowId: 'workflow-1',
                    workflowNodeName: 'aiAgent_1',
                });
                expect(hoisted.mockMutateAsyncWorkflowNode).not.toHaveBeenCalled();
            });

            it('should set script test execution on cluster element script success', async () => {
                hoisted.storeState.currentNode = {
                    clusterElementType: 'SYSTEM_PROMPT',
                    name: 'systemPrompt_1',
                };
                hoisted.storeState.rootClusterElementNodeData = {
                    workflowNodeName: 'aiAgent_1',
                };
                hoisted.mockMutateAsyncClusterElement.mockResolvedValue({
                    testClusterElementScript: {output: 'cluster success'},
                });

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
                hoisted.mockMutateAsyncClusterElement.mockRejectedValue(new Error('Cluster error'));

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

                expect(hoisted.mockMutateAsyncWorkflowNode).toHaveBeenCalled();
                expect(hoisted.mockMutateAsyncClusterElement).not.toHaveBeenCalled();
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

        it('should save conversation state, reset messages, and generate new conversation ID', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleCopilotClick();
            });

            expect(hoisted.mockSaveConversationState).toHaveBeenCalledOnce();
            expect(hoisted.mockResetMessages).toHaveBeenCalledOnce();
            expect(hoisted.mockGenerateConversationId).toHaveBeenCalledOnce();
        });

        it('should store the token the save returned so the dialog can restore', async () => {
            hoisted.mockSaveConversationState.mockReturnValue('token-1');

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleCopilotClick();
            });

            expect(hoisted.mockSetConversationToken).toHaveBeenCalledWith('token-1');
        });

        it('should save conversation state before resetting messages', async () => {
            const callOrder: string[] = [];

            hoisted.mockSaveConversationState.mockImplementation(() => callOrder.push('save'));
            hoisted.mockResetMessages.mockImplementation(() => callOrder.push('reset'));
            hoisted.mockGenerateConversationId.mockImplementation(() => callOrder.push('generate'));

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleCopilotClick();
            });

            expect(callOrder).toEqual(['save', 'reset', 'generate']);
        });
    });

    describe('handleStopClick', () => {
        it('should do nothing before a script was run', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleStopClick();
            });

            expect(hoisted.mockStopScriptTestMutate).not.toHaveBeenCalled();
        });

        it('should stop the running script with the execution id the run sent', async () => {
            hoisted.mockMutateAsyncWorkflowNode.mockReturnValue(new Promise(() => {}));

            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            act(() => {
                result.current.handleStopClick();
            });

            const [{executionId}] = hoisted.mockMutateAsyncWorkflowNode.mock.calls[0];

            expect(executionId).toEqual(expect.any(String));
            expect(hoisted.mockStopScriptTestMutate).toHaveBeenCalledWith({executionId});
        });

        it('should send a new execution id for every run', async () => {
            const {usePropertyCodeEditorDialogToolbar} = await import('../usePropertyCodeEditorDialogToolbar');
            const {result} = renderHook(() => usePropertyCodeEditorDialogToolbar(defaultProps));

            act(() => {
                result.current.handleRunClick();
            });

            act(() => {
                result.current.handleRunClick();
            });

            const executionIds = hoisted.mockMutateAsyncWorkflowNode.mock.calls.map(
                ([variables]) => variables.executionId
            );

            expect(new Set(executionIds).size).toBe(2);
        });
    });
});
