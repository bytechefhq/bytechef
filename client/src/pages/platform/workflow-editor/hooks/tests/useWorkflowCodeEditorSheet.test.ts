import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {Workflow, WorkflowFormat} from '@/shared/middleware/platform/configuration';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowCodeEditorSheet from '../useWorkflowCodeEditorSheet';

import type {editor} from 'monaco-editor';

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

vi.mock('@/shared/hooks/usePersistJobId', () => ({
    usePersistJobId: () => ({getPersistedJobId: () => null, persistJobId: vi.fn()}),
}));

vi.mock('@/shared/hooks/useWorkflowTestStream', () => ({
    useWorkflowTestStream: () => ({close: vi.fn(), setStreamRequest: vi.fn()}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useValidateWorkflowQuery: () => ({data: undefined, refetch: vi.fn()}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

const ORIGINAL_DEFINITION = '{"tasks": []}';

const UPDATED_DEFINITION = '{"tasks": [{"name": "delay_1"}]}';

const originalContext = {mode: MODE.BUILD, parameters: {workflowId: 'w1'}, source: Source.WORKFLOW_EDITOR};

const renderWorkflowCodeEditorSheet = (workflow: Partial<Workflow> = {}) => {
    const onSheetOpenClose = vi.fn();

    const hook = renderHook(() =>
        useWorkflowCodeEditorSheet({
            invalidateWorkflowQueries: vi.fn(),
            onSheetOpenClose,
            workflow: {definition: ORIGINAL_DEFINITION, id: 'workflow-1', ...workflow} as Workflow,
        })
    );

    return {...hook, onSheetOpenClose};
};

const expectOriginalConversationRestored = () => {
    const state = useCopilotStore.getState();

    expect(state.conversationStack).toHaveLength(0);
    expect(state.context).toEqual(originalContext);
    expect(state.messages).toEqual([{content: 'editor conversation', role: 'user'}]);
};

describe('useWorkflowCodeEditorSheet', () => {
    beforeEach(() => {
        useCopilotPostTurnRegistry.setState({callbacks: {}});
        useCopilotStore.setState({
            context: originalContext,
            conversationStack: [],
            messages: [{content: 'editor conversation', role: 'user'}],
        });
    });

    describe('hasErrors', () => {
        it('reports errors when a validation marker has error severity', () => {
            const {result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleValidate([{severity: 8} as editor.IMarkerData]));

            expect(result.current.hasErrors).toBe(true);
        });

        it('does not report errors for warning markers only', () => {
            const {result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleValidate([{severity: 4} as editor.IMarkerData]));

            expect(result.current.hasErrors).toBe(false);
        });
    });

    describe('copilot conversation', () => {
        it('saves the current conversation and starts a code editor one in the workflow format', () => {
            const {result} = renderWorkflowCodeEditorSheet({format: WorkflowFormat.Yaml});

            act(() => result.current.handleCopilotClick());

            const state = useCopilotStore.getState();

            expect(result.current.copilotPanelOpen).toBe(true);
            expect(state.conversationStack).toHaveLength(1);
            expect(state.messages).toEqual([]);
            expect(state.context).toEqual({
                mode: MODE.ASK,
                parameters: {format: 'yaml'},
                source: Source.WORKFLOW_CODE_EDITOR,
            });
        });

        it('defaults the copilot format to json when the workflow has none', () => {
            const {result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleCopilotClick());

            expect(useCopilotStore.getState().context.parameters).toEqual({format: 'json'});
        });

        it('restores the saved conversation when the copilot closes', () => {
            const {result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleCopilotClick());

            act(() => result.current.handleCopilotClose());

            expect(result.current.copilotPanelOpen).toBe(false);
            expectOriginalConversationRestored();
        });

        it('restores the saved conversation when a clean sheet closes', () => {
            const {onSheetOpenClose, result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleCopilotClick());

            act(() => result.current.handleOpenChange(false));

            expect(result.current.copilotPanelOpen).toBe(false);
            expect(onSheetOpenClose).toHaveBeenCalledWith(false);
            expectOriginalConversationRestored();
        });

        it('keeps the conversation and asks for confirmation when a dirty sheet closes', () => {
            const {onSheetOpenClose, result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleCopilotClick());

            act(() => result.current.handleDefinitionChange(UPDATED_DEFINITION));

            act(() => result.current.handleOpenChange(false));

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(true);
            expect(result.current.copilotPanelOpen).toBe(true);
            expect(onSheetOpenClose).not.toHaveBeenCalled();
            expect(useCopilotStore.getState().conversationStack).toHaveLength(1);
        });

        it('restores the saved conversation when the unsaved changes are discarded', () => {
            const {onSheetOpenClose, result} = renderWorkflowCodeEditorSheet();

            act(() => result.current.handleCopilotClick());

            act(() => result.current.handleDefinitionChange(UPDATED_DEFINITION));

            act(() => result.current.handleOpenChange(false));

            act(() => result.current.handleUnsavedChangesAlertDialogClose());

            expect(result.current.unsavedChangesAlertDialogOpen).toBe(false);
            expect(result.current.copilotPanelOpen).toBe(false);
            expect(onSheetOpenClose).toHaveBeenCalledWith(false);
            expectOriginalConversationRestored();
        });
    });

    describe('copilot post-turn auto-apply', () => {
        it('applies a BUILD-mode definition reply to the editor and confirms it in the conversation', () => {
            const {result} = renderWorkflowCodeEditorSheet();

            useCopilotStore.setState({
                context: {mode: MODE.BUILD, parameters: {format: 'json'}, source: Source.WORKFLOW_CODE_EDITOR},
                messages: [
                    {content: 'add a delay', role: 'user'},
                    {content: '```json\n' + UPDATED_DEFINITION + '\n```', role: 'assistant'},
                ],
            });

            act(() => useCopilotPostTurnRegistry.getState().runFor(Source.WORKFLOW_CODE_EDITOR));

            expect(result.current.definition).toBe(UPDATED_DEFINITION);
            expect(result.current.dirty).toBe(true);
            expect(useCopilotStore.getState().messages[1]).toEqual({
                content: '✓ Applied changes to the editor.',
                role: 'assistant',
            });
        });

        it('leaves the editor untouched when the reply carries no definition', () => {
            const {result} = renderWorkflowCodeEditorSheet();

            useCopilotStore.setState({
                context: {mode: MODE.ASK, parameters: {format: 'json'}, source: Source.WORKFLOW_CODE_EDITOR},
                messages: [
                    {content: 'what does this do?', role: 'user'},
                    {content: 'It runs the tasks in order.', role: 'assistant'},
                ],
            });

            act(() => useCopilotPostTurnRegistry.getState().runFor(Source.WORKFLOW_CODE_EDITOR));

            expect(result.current.definition).toBe(ORIGINAL_DEFINITION);
            expect(result.current.dirty).toBe(false);
            expect(useCopilotStore.getState().messages[1]?.content).toBe('It runs the tasks in order.');
        });

        it('stops auto-applying once unmounted', () => {
            const {unmount} = renderWorkflowCodeEditorSheet();

            unmount();

            expect(useCopilotPostTurnRegistry.getState().callbacks[Source.WORKFLOW_CODE_EDITOR]).toBeUndefined();
        });
    });
});
