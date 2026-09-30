import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import useWorkflowExecutionDetail from '../hooks/useWorkflowExecutionDetail';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
}));

const failedTriggerExecution = {
    error: {message: 'Signature check failed', stackTrace: []},
    id: '77',
    startDate: new Date('2026-09-04T10:00:00Z'),
    status: 'FAILED',
    workflowTrigger: {label: 'Webhook', name: 'trigger_1'},
};

describe('useWorkflowExecutionDetail', () => {
    beforeEach(() => {
        executionQueryMock.mockReset();
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 0, workflowExecutionKind: 'JOB'});
    });

    it('asks for the trigger execution detail when the sheet holds a trigger-only row', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 77, workflowExecutionKind: 'TRIGGER_EXECUTION'});
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        renderHook(() => useWorkflowExecutionDetail(77, true));

        expect(executionQueryMock).toHaveBeenCalledWith({id: 77}, true, undefined, 'TRIGGER_EXECUTION');
    });

    it('selects the trigger and opens its error for a trigger-only execution', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 77, workflowExecutionKind: 'TRIGGER_EXECUTION'});
        executionQueryMock.mockReturnValue({
            data: {id: 77, triggerExecution: failedTriggerExecution, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        const {result} = renderHook(() => useWorkflowExecutionDetail(77, true));

        expect(result.current.job).toBeUndefined();
        expect(result.current.selectedItem).toEqual(failedTriggerExecution);
        expect(result.current.isTriggerExecution).toBe(true);
        expect(result.current.activeTab).toBe('error');
        expect(result.current.taskExecutionAttempts).toEqual([]);
    });

    it('keeps asking for the job detail for a job-backed row', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 5, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        renderHook(() => useWorkflowExecutionDetail(5, true));

        expect(executionQueryMock).toHaveBeenCalledWith({id: 5}, true, undefined, 'JOB');
    });

    it('shows the latest attempt of a restarted task and selects its failure', () => {
        const dataTableTaskExecution = {
            endDate: new Date('2026-09-07T18:47:00.030Z'),
            id: '10',
            jobId: '7',
            startDate: new Date('2026-09-07T18:47:00Z'),
            status: 'COMPLETED',
            workflowTask: {name: 'dataTable_1', type: 'dataTable/v1/getRecord'},
        };
        const firstConditionTaskExecution = {
            error: {message: 'first failure', stackTrace: []},
            id: '11',
            jobId: '7',
            startDate: new Date('2026-09-07T18:47:00Z'),
            status: 'FAILED',
            workflowTask: {name: 'condition_1', type: 'condition/v1'},
        };
        const restartedConditionTaskExecution = {
            error: {message: 'restart failure', stackTrace: []},
            id: '12',
            jobId: '7',
            startDate: new Date('2026-09-30T21:19:20Z'),
            status: 'FAILED',
            workflowTask: {name: 'condition_1', type: 'condition/v1'},
        };

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 7, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {
                id: 7,
                job: {
                    id: '7',
                    status: 'FAILED',
                    taskExecutions: [
                        dataTableTaskExecution,
                        firstConditionTaskExecution,
                        restartedConditionTaskExecution,
                    ],
                },
            },
            isLoading: false,
        });

        const {result} = renderHook(() => useWorkflowExecutionDetail(7, true));

        expect(result.current.taskExecutionAttempts).toEqual([
            {latestTaskExecution: dataTableTaskExecution, previousTaskExecutions: []},
            {
                latestTaskExecution: restartedConditionTaskExecution,
                previousTaskExecutions: [firstConditionTaskExecution],
            },
        ]);
        expect(result.current.selectedItem).toEqual(restartedConditionTaskExecution);
        expect(result.current.activeTab).toBe('error');
    });
});
