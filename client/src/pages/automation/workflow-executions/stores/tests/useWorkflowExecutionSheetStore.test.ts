import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowExecutionSheetStore from '../useWorkflowExecutionSheetStore';

describe('useWorkflowExecutionSheetStore', () => {
    beforeEach(() => {
        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: false,
        });
    });

    it('treats an execution as job-backed unless told otherwise', () => {
        useWorkflowExecutionSheetStore.getState().setWorkflowExecutionId(5);

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(5);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('JOB');
    });

    it('remembers a trigger-only execution by its kind', () => {
        useWorkflowExecutionSheetStore.getState().setWorkflowExecutionId(77, 'TRIGGER_EXECUTION');

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(77);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('TRIGGER_EXECUTION');
    });
});
