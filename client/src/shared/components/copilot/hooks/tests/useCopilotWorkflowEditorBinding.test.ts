import useCopilotWorkflowEditorBinding from '@/shared/components/copilot/hooks/useCopilotWorkflowEditorBinding';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useCopilotWorkflowEditorBinding', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            conversationId: 'workflow-a-conversation',
            messages: [{content: 'build workflow A', role: 'user'}],
            workflowEditorWorkflowKey: undefined,
        });
    });

    it('keeps the conversation when the editor first binds a workflow', () => {
        renderHook(() => useCopilotWorkflowEditorBinding('project:1:2'));

        const state = useCopilotStore.getState();

        expect(state.context.mode).toBe(MODE.BUILD);
        expect(state.conversationId).toBe('workflow-a-conversation');
        expect(state.messages).toHaveLength(1);
    });

    it('keeps the conversation when the editor binds the same workflow again', () => {
        useCopilotStore.setState({workflowEditorWorkflowKey: 'project:1:2'});

        renderHook(() => useCopilotWorkflowEditorBinding('project:1:2'));

        expect(useCopilotStore.getState().context.mode).toBe(MODE.BUILD);
        expect(useCopilotStore.getState().messages).toHaveLength(1);
    });

    it('starts a new Ask conversation when the editor switches to another workflow', () => {
        const {rerender} = renderHook(({workflowKey}) => useCopilotWorkflowEditorBinding(workflowKey), {
            initialProps: {workflowKey: 'project:1:2'},
        });

        rerender({workflowKey: 'project:1:3'});

        const state = useCopilotStore.getState();

        expect(state.context.mode).toBe(MODE.ASK);
        expect(state.conversationId).not.toBe('workflow-a-conversation');
        expect(state.messages).toEqual([]);
    });
});
