import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import useWorkflowExecutionDetail from '../hooks/useWorkflowExecutionDetail';

const {executionQueryMock, taskExecutionQueryMock} = vi.hoisted(() => ({
    executionQueryMock: vi.fn(),
    taskExecutionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
    useGetWorkflowExecutionTaskExecutionQuery: taskExecutionQueryMock,
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
        taskExecutionQueryMock.mockReset();
        taskExecutionQueryMock.mockReturnValue({data: undefined, isError: false});
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 0, workflowExecutionKind: 'JOB'});
    });

    it('asks for the trigger execution detail when the sheet holds a trigger-only row', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 77, workflowExecutionKind: 'TRIGGER_EXECUTION'});
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        renderHook(() => useWorkflowExecutionDetail(77, true));

        expect(executionQueryMock).toHaveBeenCalledWith({id: 77, lazy: true}, true, undefined, 'TRIGGER_EXECUTION');
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

        expect(executionQueryMock).toHaveBeenCalledWith({id: 5, lazy: true}, true, undefined, 'JOB');
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
        taskExecutionQueryMock.mockReturnValue({data: restartedConditionTaskExecution, isError: false});
        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: 12}],
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

    it('requestsTheRootLazily', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3291, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        renderHook(() => useWorkflowExecutionDetail(3291, true));

        expect(executionQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({id: 3291, lazy: true}),
            true,
            undefined,
            'JOB'
        );
        expect(executionQueryMock).not.toHaveBeenCalledWith(
            expect.not.objectContaining({lazy: true}),
            expect.anything(),
            expect.anything(),
            expect.anything()
        );
    });

    it('selectsTheFocusedTaskFromTheFocusPath', () => {
        const focusedTaskExecution = {
            error: {message: 'Order rejected', stackTrace: []},
            id: '9',
            jobId: '3',
            status: 'FAILED',
            workflowTask: {name: 'httpClient_1', type: 'httpClient/v1/get'},
        };

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: 1}, {iterationIndex: 2, taskExecutionId: 7}, {taskExecutionId: 9}],
                id: 3,
                job: {
                    id: '3',
                    status: 'FAILED',
                    taskExecutions: [{childCount: 3, id: '1', jobId: '3', status: 'FAILED'}],
                },
            },
            isLoading: false,
        });
        taskExecutionQueryMock.mockImplementation((request: {id: number; taskExecutionId: number}) => ({
            data: request.taskExecutionId === 9 ? focusedTaskExecution : undefined,
            isError: false,
        }));

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(taskExecutionQueryMock).toHaveBeenCalledWith({id: 3, taskExecutionId: 9}, true);
        expect(result.current.selectedItem?.id).toBe('9');
        expect(result.current.focusedExecution?.path).toEqual(['1', '1-iteration-2', '7', '9']);
        expect(result.current.activeTab).toBe('error');
    });

    it('opensASubflowByJobId', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 5, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockImplementation((request: {id: number}) => ({
            data:
                request.id === 77
                    ? {
                          focusPath: [],
                          id: 77,
                          job: {id: '77', label: 'Subflow', status: 'COMPLETED', taskExecutions: []},
                      }
                    : {focusPath: [], id: 5, job: {id: '5', label: 'Root', status: 'COMPLETED', taskExecutions: []}},
            isLoading: false,
        }));

        const {result} = renderHook(() => useWorkflowExecutionDetail(5, true));

        act(() => {
            result.current.handleSeeExecutions({
                id: '77',
                label: 'Process order',
                priority: 0,
                startDate: new Date('2026-10-22T10:00:00Z'),
                status: 'COMPLETED',
            });
        });

        expect(executionQueryMock).toHaveBeenCalledWith({id: 77, lazy: true}, true, undefined, 'JOB');
        expect(result.current.job?.id).toBe('77');
        expect(result.current.rootJob?.id).toBe('5');
        expect(result.current.subflowStack).toEqual([{jobId: 77, label: 'Process order'}]);
        expect(result.current.lazy).toEqual({refetchInterval: false, workflowExecutionId: 77});
    });

    it('exposesTaskExecutionCount', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3291, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [],
                id: 3291,
                job: {id: '3291', status: 'COMPLETED', taskExecutions: []},
                taskExecutionCount: 112464,
            },
            isLoading: false,
        });

        const {result} = renderHook(() => useWorkflowExecutionDetail(3291, true));

        expect(result.current.taskExecutionCount).toBe(112464);
    });

    it('stays loading until the focused task has loaded so the tree opens on the focus path', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: 1}, {taskExecutionId: 9}],
                id: 3,
                job: {id: '3', status: 'FAILED', taskExecutions: [{childCount: 1, id: '1', jobId: '3'}]},
            },
            isLoading: false,
        });

        const {rerender, result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.workflowExecutionLoading).toBe(true);
        expect(result.current.selectedItem).toBeUndefined();

        taskExecutionQueryMock.mockReturnValue({
            data: {error: {message: 'Order rejected', stackTrace: []}, id: '9', jobId: '3', status: 'FAILED'},
            isError: false,
        });

        rerender();

        expect(result.current.workflowExecutionLoading).toBe(false);
        expect(result.current.selectedItem?.id).toBe('9');
    });
});
