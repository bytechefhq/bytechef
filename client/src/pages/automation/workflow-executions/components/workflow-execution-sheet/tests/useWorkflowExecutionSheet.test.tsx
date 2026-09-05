import {renderHook} from '@testing-library/react';
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

vi.mock('@/shared/components/copilot/stores/useCopilotStore', () => ({
    MODE: {ASK: 'ASK'},
    Source: {WORKFLOW_EXECUTION: 'WORKFLOW_EXECUTION'},
    useCopilotStore: Object.assign((selector: (state: unknown) => unknown) => selector({setContext: vi.fn()}), {
        getState: () => ({restoreConversationState: vi.fn()}),
    }),
}));

describe('useWorkflowExecutionSheet', () => {
    beforeEach(() => {
        executionQueryMock.mockReset();
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: true,
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
});
