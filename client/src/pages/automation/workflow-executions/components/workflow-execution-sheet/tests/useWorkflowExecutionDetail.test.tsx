import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import useWorkflowExecutionDetail from '../hooks/useWorkflowExecutionDetail';

const {executionQueryMock, invalidateQueriesMock, taskExecutionQueryMock} = vi.hoisted(() => ({
    executionQueryMock: vi.fn(),
    invalidateQueriesMock: vi.fn(),
    taskExecutionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    WorkflowExecutionKeys: {
        workflowExecutionTaskExecutionChildrenOfJob: (id: number) => [
            'workflowExecutions',
            id,
            'taskExecutionChildren',
        ],
    },
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
    useGetWorkflowExecutionTaskExecutionQuery: taskExecutionQueryMock,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
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
        invalidateQueriesMock.mockReset();
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
                focusPath: [{taskExecutionId: '12'}],
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
                focusPath: [{taskExecutionId: '1'}, {iterationIndex: 2, taskExecutionId: '7'}, {taskExecutionId: '9'}],
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

        expect(taskExecutionQueryMock).toHaveBeenCalledWith({id: 3, taskExecutionId: 9}, true, undefined, false);
        expect(result.current.focusedTaskExecutionError).toBe(false);
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
        expect(result.current.lazy).toEqual({focusPath: [], refetchInterval: false, workflowExecutionId: 77});
    });

    it('polls the active job and the lazy tree only while the execution runs', () => {
        const execution = (status: string) => ({
            data: {focusPath: [], id: 3, job: {id: '3', status, taskExecutions: []}},
            isLoading: false,
        });

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue(execution('STARTED'));

        const {rerender, result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(executionQueryMock).toHaveBeenCalledWith({id: 3, lazy: true}, true, 2000, 'JOB');
        expect(result.current.lazy.refetchInterval).toBe(2000);

        executionQueryMock.mockClear();
        executionQueryMock.mockReturnValue(execution('COMPLETED'));

        rerender();

        expect(executionQueryMock).toHaveBeenCalledWith({id: 3, lazy: true}, false, 2000, 'JOB');
        expect(result.current.lazy.refetchInterval).toBe(false);
    });

    it('closes an open subflow when another execution is shown', () => {
        executionQueryMock.mockImplementation((request: {id: number}) => ({
            data: {
                focusPath: [],
                id: request.id,
                job: {id: `${request.id}`, status: 'COMPLETED', taskExecutions: []},
            },
            isLoading: false,
        }));

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 5, workflowExecutionKind: 'JOB'});

        const {rerender, result} = renderHook(({id}) => useWorkflowExecutionDetail(id, true), {
            initialProps: {id: 5},
        });

        act(() => {
            result.current.handleSeeExecutions({
                id: '77',
                label: 'Process order',
                priority: 0,
                startDate: new Date('2026-10-22T10:00:00Z'),
                status: 'COMPLETED',
            });
        });

        expect(result.current.job?.id).toBe('77');

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 6, workflowExecutionKind: 'JOB'});

        rerender({id: 6});

        expect(result.current.subflowStack).toEqual([]);
        expect(result.current.job?.id).toBe('6');
        expect(result.current.lazy.workflowExecutionId).toBe(6);
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
                focusPath: [{taskExecutionId: '1'}, {taskExecutionId: '9'}],
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

    it('keeps the applied focus while a running execution moves its focus to a newly started task', () => {
        const runningExecution = (lastTaskExecutionId: string) => ({
            data: {
                focusPath: [{taskExecutionId: '1'}, {taskExecutionId: lastTaskExecutionId}],
                id: 3,
                job: {id: '3', status: 'STARTED', taskExecutions: [{childCount: 2, id: '1', jobId: '3'}]},
            },
            isLoading: false,
        });
        const startedTaskExecution = {id: '9', jobId: '3', status: 'STARTED'};

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue(runningExecution('9'));
        taskExecutionQueryMock.mockImplementation((request: {id: number; taskExecutionId: number}) => ({
            data: request.taskExecutionId === 9 ? startedTaskExecution : undefined,
            isError: false,
        }));

        const {rerender, result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.focusedExecution?.path).toEqual(['1', '9']);

        const appliedFocusedExecution = result.current.focusedExecution;

        executionQueryMock.mockReturnValue(runningExecution('10'));

        rerender();

        expect(taskExecutionQueryMock).not.toHaveBeenCalledWith(
            expect.objectContaining({taskExecutionId: 10}),
            expect.anything()
        );
        expect(result.current.focusedExecution).toBe(appliedFocusedExecution);
        expect(result.current.focusedExecution?.path).toEqual(['1', '9']);
        expect(result.current.selectedItem?.id).toBe('9');
    });

    it('reloads the open task execution pages once when a running execution finishes', () => {
        const execution = (status: string) => ({
            data: {focusPath: [], id: 3, job: {id: '3', status, taskExecutions: []}},
            isLoading: false,
        });

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue(execution('STARTED'));

        const {rerender} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(invalidateQueriesMock).not.toHaveBeenCalled();

        executionQueryMock.mockReturnValue(execution('COMPLETED'));

        rerender();
        rerender();

        expect(invalidateQueriesMock).toHaveBeenCalledTimes(1);
        expect(invalidateQueriesMock).toHaveBeenCalledWith({
            queryKey: ['workflowExecutions', 3, 'taskExecutionChildren'],
        });
    });

    it('reports a failed root load while a subflow is open and retries both loads', () => {
        const rootRefetch = vi.fn();
        const subflowRefetch = vi.fn();

        let rootLoadFails = false;

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 5, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockImplementation((request: {id: number}) => {
            if (request.id === 77) {
                return {
                    data: {focusPath: [], id: 77, job: {id: '77', status: 'COMPLETED', taskExecutions: []}},
                    isLoading: false,
                    refetch: subflowRefetch,
                };
            }

            if (rootLoadFails) {
                return {data: undefined, isError: true, isLoading: false, refetch: rootRefetch};
            }

            return {
                data: {focusPath: [], id: 5, job: {id: '5', status: 'COMPLETED', taskExecutions: []}},
                isLoading: false,
                refetch: rootRefetch,
            };
        });

        const {rerender, result} = renderHook(() => useWorkflowExecutionDetail(5, true));

        act(() => {
            result.current.handleSeeExecutions({
                id: '77',
                label: 'Process order',
                priority: 0,
                startDate: new Date('2026-10-22T10:00:00Z'),
                status: 'COMPLETED',
            });
        });

        expect(result.current.workflowExecutionError).toBeFalsy();

        rootLoadFails = true;

        rerender();

        expect(result.current.workflowExecutionError).toBe(true);

        act(() => {
            result.current.handleWorkflowExecutionRetry();
        });

        expect(subflowRefetch).toHaveBeenCalledTimes(1);
        expect(rootRefetch).toHaveBeenCalledTimes(1);
    });

    it('keeps the loaded execution and reports a failed refetch as a live updates error', () => {
        const refetch = vi.fn();
        const workflowExecution = {
            focusPath: [],
            job: {id: '3', status: 'STARTED', taskExecutions: []},
        };

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({data: workflowExecution, isError: true, isLoading: false, refetch});

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.workflowExecutionError).toBe(false);
        expect(result.current.workflowExecutionRefetchError).toBe(true);
        expect(result.current.job).toBe(workflowExecution.job);

        act(() => {
            result.current.handleWorkflowExecutionRetry();
        });

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('does not report a live updates error when the initial load failed', () => {
        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({data: undefined, isError: true, isLoading: false, refetch: vi.fn()});

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.workflowExecutionRefetchError).toBe(false);
    });

    it('reports a failed execution load and retries it', () => {
        const refetch = vi.fn();

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({data: undefined, isError: true, isLoading: false, refetch});

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.workflowExecutionError).toBe(true);
        expect(result.current.workflowExecutionLoading).toBe(false);

        act(() => {
            result.current.handleWorkflowExecutionRetry();
        });

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('opens the tree on the focus path at its top-level task when the focused task fails to load', () => {
        const topLevelTaskExecution = {childCount: 3, id: '1', jobId: '3', status: 'FAILED'};

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: '1'}, {iterationIndex: 2, taskExecutionId: '7'}, {taskExecutionId: '9'}],
                id: 3,
                job: {id: '3', status: 'FAILED', taskExecutions: [topLevelTaskExecution]},
            },
            isLoading: false,
        });
        taskExecutionQueryMock.mockReturnValue({data: undefined, isError: true});

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.workflowExecutionLoading).toBe(false);
        expect(result.current.selectedItem).toEqual(topLevelTaskExecution);
        expect(result.current.focusedExecution?.path).toEqual(['1', '1-iteration-2', '7', '9']);
    });

    it('reports a failed focused task load and selects the focused task once a retry loads it', async () => {
        const focusedTaskExecution = {
            error: {message: 'Order rejected', stackTrace: []},
            id: '9',
            jobId: '3',
            status: 'FAILED',
            workflowTask: {name: 'httpClient_1', type: 'httpClient/v1/get'},
        };
        const refetch = vi.fn().mockResolvedValue({data: focusedTaskExecution});
        const topLevelTaskExecution = {childCount: 3, id: '1', jobId: '3', status: 'FAILED'};

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: '1'}, {iterationIndex: 2, taskExecutionId: '7'}, {taskExecutionId: '9'}],
                id: 3,
                job: {id: '3', status: 'FAILED', taskExecutions: [topLevelTaskExecution]},
            },
            isLoading: false,
        });
        taskExecutionQueryMock.mockReturnValue({data: undefined, isError: true, refetch});

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.focusedTaskExecutionError).toBe(true);
        expect(result.current.selectedItem).toEqual(topLevelTaskExecution);

        await act(async () => {
            await result.current.handleFocusedTaskExecutionRetry();
        });

        expect(refetch).toHaveBeenCalledTimes(1);
        expect(result.current.selectedItem?.id).toBe('9');
        expect(result.current.activeTab).toBe('error');
    });

    it('passes the applied focus path to the lazy tree', () => {
        const focusPath = [{taskExecutionId: '1'}, {childIndex: 120, taskExecutionId: '9'}];

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 3, workflowExecutionKind: 'JOB'});
        executionQueryMock.mockReturnValue({
            data: {focusPath, id: 3, job: {id: '3', status: 'COMPLETED', taskExecutions: []}},
            isLoading: false,
        });

        const {result} = renderHook(() => useWorkflowExecutionDetail(3, true));

        expect(result.current.lazy.focusPath).toEqual(focusPath);
    });
});
