import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import handleDeleteTask from './handleDeleteTask';
import {clearAllWorkflowMutations, isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

// ── Store mocks ──────────────────────────────────────────────────────

const mockSetWorkflow = vi.fn();
const mockReset = vi.fn();
const mockSetWorkflowTestChatPanelOpen = vi.fn();

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: {
        getState: () => ({
            setWorkflow: mockSetWorkflow,
        }),
    },
}));

vi.mock('../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {
        getState: () => ({
            reset: mockReset,
            setWorkflowNodeDetailsPanelOpen: vi.fn(),
        }),
    },
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore', () => ({
    default: {
        getState: () => ({
            setWorkflowTestChatPanelOpen: mockSetWorkflowTestChatPanelOpen,
        }),
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    environmentStore: {
        getState: () => ({
            currentEnvironmentId: 'env-1',
        }),
    },
}));

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    WorkflowNodeOutputKeys: {
        filteredPreviousWorkflowNodeOutputs: vi.fn().mockReturnValue(['mock-key']),
    },
}));

// ── Helpers ──────────────────────────────────────────────────────────

function makeTask(name: string, parameters?: Record<string, unknown>): WorkflowTask {
    return {
        name,
        parameters,
        type: `test/${name}`,
    } as WorkflowTask;
}

function makeWorkflow(tasks: WorkflowTask[]) {
    const definition = JSON.stringify({tasks}, null, SPACE);

    return {
        definition,
        id: 'workflow-1',
        nodeNames: tasks.map((task) => task.name),
        tasks,
        version: 1,
    };
}

function makeMockMutation() {
    return {
        mutate: vi.fn(),
    } as unknown as Parameters<typeof handleDeleteTask>[0]['updateWorkflowMutation'];
}

function makeQueryClient() {
    return {
        invalidateQueries: vi.fn(),
    } as unknown as QueryClient;
}

// ── Tests ────────────────────────────────────────────────────────────

describe('handleDeleteTask', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('should delete a top-level task', () => {
        const tasks = [makeTask('task_1'), makeTask('task_2'), makeTask('task_3')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {componentName: 'test', name: 'task_2'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        expect(updatedDefinition.tasks).toHaveLength(2);
        expect(updatedDefinition.tasks.map((task: WorkflowTask) => task.name)).toEqual(['task_1', 'task_3']);
    });

    it('should delete a task from condition caseTrue', () => {
        const conditionTask = makeTask('condition_1', {
            caseFalse: [makeTask('false_child')],
            caseTrue: [makeTask('true_child_1'), makeTask('true_child_2')],
        });
        const workflow = makeWorkflow([conditionTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1'},
                name: 'true_child_1',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);
        const updatedCondition = updatedDefinition.tasks[0];

        expect(updatedCondition.parameters.caseTrue).toHaveLength(1);
        expect(updatedCondition.parameters.caseTrue[0].name).toBe('true_child_2');
        // caseFalse should be unchanged
        expect(updatedCondition.parameters.caseFalse).toHaveLength(1);
    });

    it('should delete a task from condition caseFalse', () => {
        const conditionTask = makeTask('condition_1', {
            caseFalse: [makeTask('false_child')],
            caseTrue: [makeTask('true_child')],
        });
        const workflow = makeWorkflow([conditionTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_1'},
                name: 'false_child',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        expect(updatedDefinition.tasks[0].parameters.caseFalse).toHaveLength(0);
        expect(updatedDefinition.tasks[0].parameters.caseTrue).toHaveLength(1);
    });

    it('should delete a task from loop iteratee', () => {
        const loopTask = makeTask('loop_1', {
            iteratee: [makeTask('child_1'), makeTask('child_2')],
        });
        const workflow = makeWorkflow([loopTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                loopData: {loopId: 'loop_1'},
                name: 'child_1',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        expect(updatedDefinition.tasks[0].parameters.iteratee).toHaveLength(1);
        expect(updatedDefinition.tasks[0].parameters.iteratee[0].name).toBe('child_2');
    });

    it('should delete a task from branch case', () => {
        const branchTask = makeTask('branch_1', {
            cases: [
                {key: 'case_0', tasks: [makeTask('case_child_1'), makeTask('case_child_2')]},
                {key: 'case_1', tasks: [makeTask('other_child')]},
            ],
            default: [makeTask('default_child')],
        });
        const workflow = makeWorkflow([branchTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                branchData: {branchId: 'branch_1', caseKey: 'case_0'},
                componentName: 'test',
                name: 'case_child_1',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        expect(updatedDefinition.tasks[0].parameters.cases[0].tasks).toHaveLength(1);
        expect(updatedDefinition.tasks[0].parameters.cases[0].tasks[0].name).toBe('case_child_2');
        // Other case and default unchanged
        expect(updatedDefinition.tasks[0].parameters.cases[1].tasks).toHaveLength(1);
        expect(updatedDefinition.tasks[0].parameters.default).toHaveLength(1);
    });

    it('should delete a task from branch default', () => {
        const branchTask = makeTask('branch_1', {
            cases: [{key: 'case_0', tasks: [makeTask('case_child')]}],
            default: [makeTask('default_child_1'), makeTask('default_child_2')],
        });
        const workflow = makeWorkflow([branchTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                branchData: {branchId: 'branch_1', caseKey: 'default'},
                componentName: 'test',
                name: 'default_child_1',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        expect(updatedDefinition.tasks[0].parameters.default).toHaveLength(1);
        expect(updatedDefinition.tasks[0].parameters.default[0].name).toBe('default_child_2');
    });

    it('should delete a task from parallel tasks', () => {
        const parallelTask = makeTask('parallel_1', {
            tasks: [makeTask('par_1'), makeTask('par_2'), makeTask('par_3')],
        });
        const workflow = makeWorkflow([parallelTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                name: 'par_2',
                parallelData: {parallelId: 'parallel_1'},
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        expect(updatedDefinition.tasks[0].parameters.tasks).toHaveLength(2);
        expect(updatedDefinition.tasks[0].parameters.tasks.map((task: WorkflowTask) => task.name)).toEqual([
            'par_1',
            'par_3',
        ]);
    });

    it('should delete a task from fork-join branches', () => {
        const forkJoinTask = makeTask('fork-join_1', {
            branches: [[makeTask('branch_a_1'), makeTask('branch_a_2')], [makeTask('branch_b_1')]],
        });
        const workflow = makeWorkflow([forkJoinTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                forkJoinData: {forkJoinId: 'fork-join_1'},
                name: 'branch_a_1',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        // branch_a should still have branch_a_2
        expect(updatedDefinition.tasks[0].parameters.branches[0]).toHaveLength(1);
        expect(updatedDefinition.tasks[0].parameters.branches[0][0].name).toBe('branch_a_2');
        // branch_b unchanged
        expect(updatedDefinition.tasks[0].parameters.branches[1]).toHaveLength(1);
    });

    it('should remove empty fork-join branches after deletion', () => {
        const forkJoinTask = makeTask('fork-join_1', {
            branches: [[makeTask('branch_a_1')], [makeTask('branch_b_1')]],
        });
        const workflow = makeWorkflow([forkJoinTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                forkJoinData: {forkJoinId: 'fork-join_1'},
                name: 'branch_a_1',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        // Empty branch should be filtered out
        expect(updatedDefinition.tasks[0].parameters.branches).toHaveLength(1);
        expect(updatedDefinition.tasks[0].parameters.branches[0][0].name).toBe('branch_b_1');
    });

    it('should not mutate when workflow is already mutating', () => {
        const tasks = [makeTask('task_1')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        setWorkflowMutating('workflow-1', true);

        handleDeleteTask({
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).not.toHaveBeenCalled();
    });

    it('should return early when workflow has no definition', () => {
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow: {id: 'workflow-1', version: 1} as Parameters<typeof handleDeleteTask>[0]['workflow'],
        });

        expect(mutation.mutate).not.toHaveBeenCalled();
    });

    it('should return early when workflow has no tasks', () => {
        const mutation = makeMockMutation();
        const workflow = {
            definition: JSON.stringify({}, null, SPACE),
            id: 'workflow-1',
            nodeNames: [],
            tasks: [],
            version: 1,
        };

        handleDeleteTask({
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow: workflow as Parameters<typeof handleDeleteTask>[0]['workflow'],
        });

        expect(mutation.mutate).not.toHaveBeenCalled();
    });

    it('should perform optimistic UI update', () => {
        const tasks = [makeTask('task_1'), makeTask('task_2')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        handleDeleteTask({
            currentNode: {componentName: 'test', name: 'task_1'} as NodeDataType,
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        // setWorkflow should be called for optimistic update
        expect(mockSetWorkflow).toHaveBeenCalledOnce();

        const optimisticWorkflow = mockSetWorkflow.mock.calls[0][0];

        // Tasks should be filtered
        expect(optimisticWorkflow.tasks).toHaveLength(1);
        expect(optimisticWorkflow.tasks[0].name).toBe('task_2');
    });

    it('should close panel when deleting current node', () => {
        const tasks = [makeTask('task_1')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        handleDeleteTask({
            currentNode: {componentName: 'test', name: 'task_1'} as NodeDataType,
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mockReset).toHaveBeenCalledOnce();
        expect(mockSetWorkflowTestChatPanelOpen).toHaveBeenCalledWith(false);
    });

    it('should not close panel when deleting a different node', () => {
        const tasks = [makeTask('task_1'), makeTask('task_2')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        handleDeleteTask({
            currentNode: {componentName: 'test', name: 'task_1'} as NodeDataType,
            data: {componentName: 'test', name: 'task_2'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mockReset).not.toHaveBeenCalled();
    });

    it('should rollback on mutation error', () => {
        const tasks = [makeTask('task_1')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();
        const invalidateQueries = vi.fn();

        handleDeleteTask({
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: invalidateQueries,
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        // Simulate error callback
        const mutateCall = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0];
        const callbacks = mutateCall[1];

        callbacks.onError();

        // Should restore previous workflow
        expect(mockSetWorkflow).toHaveBeenCalledTimes(2); // once for optimistic, once for rollback
        expect(mockSetWorkflow.mock.calls[1][0]).toBe(workflow);
        expect(invalidateQueries).toHaveBeenCalled();
    });

    it('should clear mutation flag on settled', () => {
        const tasks = [makeTask('task_1')];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {componentName: 'test', name: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(isWorkflowMutating('workflow-1')).toBe(true);

        // Simulate settled callback
        const mutateCall = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0];

        mutateCall[1].onSettled();

        expect(isWorkflowMutating('workflow-1')).toBe(false);
    });

    it('should clear each data iteratee to empty object', () => {
        const eachTask = makeTask('each_1', {
            iteratee: makeTask('each_child'),
        });
        const workflow = makeWorkflow([eachTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            data: {
                componentName: 'test',
                eachData: {eachId: 'each_1'},
                name: 'each_child',
            } as unknown as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
        const updatedDefinition = JSON.parse(mutationArgs.workflow.definition);

        // Each data clears iteratee to empty object
        expect(updatedDefinition.tasks[0].parameters.iteratee).toEqual({});
    });
});
