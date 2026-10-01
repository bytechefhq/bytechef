import {Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import handleTaskDispatcherClick from './handleTaskDispatcherClick';
import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';
import saveWorkflowDefinition from './saveWorkflowDefinition';

vi.mock('./saveWorkflowDefinition', () => ({default: vi.fn()}));

vi.mock('./handleComponentAddedSuccess', () => ({
    default: vi.fn(),
    handleComponentAddedError: vi.fn(),
    openNodeDetailsPanelForNewNode: vi.fn(),
}));

const parallelTask: WorkflowTask = {
    name: 'parallel_1',
    parameters: {
        tasks: [
            {name: 'first_lane', parameters: {}, type: 'test/v1/action'},
            {name: 'second_lane', parameters: {}, type: 'test/v1/action'},
        ],
    },
    type: 'parallel/v1',
};

async function clickLoopOnAddBranchChip(sourceNodeId: string) {
    await handleTaskDispatcherClick({
        queryClient: new QueryClient(),
        sourceNodeId,
        taskDispatcherContext: {index: 0, parallelId: 'parallel_1', taskDispatcherId: 'parallel_1'},
        taskDispatcherDefinition: {
            componentVersion: 1,
            name: 'loop',
            properties: [],
            title: 'Loop',
            version: 1,
        },
        taskDispatcherName: 'loop',
        updateWorkflowMutation: {} as never,
        workflow: {tasks: [parallelTask]} as Workflow & {tasks: Array<WorkflowTask>},
    } as never);

    return vi.mocked(saveWorkflowDefinition).mock.calls[0][0];
}

describe('handleTaskDispatcherClick', () => {
    beforeEach(() => {
        vi.mocked(saveWorkflowDefinition).mockClear();

        useWorkflowDataStore.setState({
            nodes: [],
            workflow: {definition: JSON.stringify({tasks: [parallelTask]})},
        } as never);
    });

    it('should keep the placeholder id when a task dispatcher is added from a trailing branch placeholder', async () => {
        const saveArguments = await clickLoopOnAddBranchChip('parallel_1-parallel-placeholder-0');

        expect(saveArguments.placeholderId).toBe('parallel_1-parallel-placeholder-0');
    });

    it('should append the task dispatcher as the last parallel lane', async () => {
        const saveArguments = await clickLoopOnAddBranchChip('parallel_1-parallel-placeholder-0');

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: {name: saveArguments.nodeData?.workflowNodeName as string, parameters: {}, type: 'loop/v1'},
            placeholderId: saveArguments.placeholderId,
            taskDispatcherContext: saveArguments.taskDispatcherContext!,
            tasks: [structuredClone(parallelTask)],
        });

        const lanes = updatedTasks[0].parameters?.tasks as Array<WorkflowTask>;

        expect(lanes.map((lane) => lane.name)).toEqual(['first_lane', 'second_lane', 'loop_1']);
    });

    it('should drop the source node id when the context comes from an existing nested task', async () => {
        const saveArguments = await clickLoopOnAddBranchChip('parallel_1-parallel-task_1');

        expect(saveArguments.placeholderId).toBeUndefined();
    });
});
