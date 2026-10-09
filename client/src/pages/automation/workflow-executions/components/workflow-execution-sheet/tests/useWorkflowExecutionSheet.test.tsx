import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import useWorkflowExecutionSheet from '../hooks/useWorkflowExecutionSheet';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: unknown) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: false}}}),
}));

const originalContext = {mode: MODE.BUILD, parameters: {workflowId: 'w0'}, source: Source.WORKFLOW_EDITOR};

describe('useWorkflowExecutionSheet', () => {
    beforeEach(() => {
        executionQueryMock.mockReset();
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: true,
        });

        useCopilotStore.setState({
            context: originalContext,
            conversationStack: [],
            messages: [{content: 'editor conversation', role: 'user'}],
        });
    });

    it('fetches a trigger-only row from the trigger execution endpoint', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 77, workflowExecutionKind: 'TRIGGER_EXECUTION'});

        renderHook(() => useWorkflowExecutionSheet());

        expect(executionQueryMock).toHaveBeenCalledWith({id: 77}, true, undefined, 'TRIGGER_EXECUTION');
    });

    it('fetches a job-backed row from the job endpoint', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 5, workflowExecutionKind: 'JOB'});

        const {result} = renderHook(() => useWorkflowExecutionSheet());

        expect(executionQueryMock).toHaveBeenCalledWith({id: 5}, true, undefined, 'JOB');
        expect(result.current.workflowExecutionId).toBe(5);
        expect(result.current.workflowExecutionLoading).toBe(true);
    });

    it('closes the sheet when the open state is toggled', () => {
        const {result} = renderHook(() => useWorkflowExecutionSheet());

        result.current.handleOpenChange();

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });

    describe('copilot conversation', () => {
        const openCopilot = () => {
            executionQueryMock.mockReturnValue({
                data: {job: {workflowId: 'workflow-9'}, projectDeployment: {environmentId: 2}},
                isLoading: false,
            });
            useWorkflowExecutionSheetStore.setState({workflowExecutionId: 5});

            const hook = renderHook(() => useWorkflowExecutionSheet());

            act(() => hook.result.current.handleCopilotClick());

            return hook;
        };

        it('saves the current conversation and starts an execution-scoped one when the copilot opens', () => {
            const {result} = openCopilot();

            const state = useCopilotStore.getState();

            expect(result.current.copilotPanelOpen).toBe(true);
            expect(state.conversationStack).toHaveLength(1);
            expect(state.messages).toEqual([]);
            expect(state.context).toEqual({
                mode: MODE.ASK,
                parameters: {environmentId: 2, workflowExecutionId: 5, workflowId: 'workflow-9', workspaceId: 1},
                source: Source.WORKFLOW_EXECUTION,
            });
        });

        it('restores the saved conversation when the copilot closes', () => {
            const {result} = openCopilot();

            act(() => result.current.handleCopilotClose());

            const state = useCopilotStore.getState();

            expect(result.current.copilotPanelOpen).toBe(false);
            expect(state.conversationStack).toHaveLength(0);
            expect(state.context).toEqual(originalContext);
            expect(state.messages).toEqual([{content: 'editor conversation', role: 'user'}]);
        });

        it('restores the saved conversation and closes the copilot when the sheet closes', () => {
            const {result} = openCopilot();

            act(() => result.current.handleOpenChange());

            expect(result.current.copilotPanelOpen).toBe(false);
            expect(useCopilotStore.getState().context).toEqual(originalContext);
            expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
        });

        it('leaves the conversation untouched when the sheet opens', () => {
            useWorkflowExecutionSheetStore.setState({workflowExecutionSheetOpen: false});

            useCopilotStore.getState().saveConversationState();

            const {result} = renderHook(() => useWorkflowExecutionSheet());

            act(() => result.current.handleOpenChange());

            expect(useCopilotStore.getState().conversationStack).toHaveLength(1);
            expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
        });
    });
});
