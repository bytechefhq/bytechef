import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {updateTaskPositions} from './saveWorkflowNodesPosition';

function makeTask(name: string, nodePosition?: {x: number; y: number}): WorkflowTask {
    return {
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        type: `test/${name}`,
    } as WorkflowTask;
}

describe('updateTaskPositions', () => {
    it('should save position for a matching task', () => {
        const tasks = [makeTask('task_1')];
        const nodePositions = {task_1: {x: 100, y: 200}};

        const result = updateTaskPositions(tasks, nodePositions);

        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 200});
    });

    it('should not modify tasks not in nodePositions', () => {
        const tasks = [makeTask('task_1'), makeTask('task_2')];
        const nodePositions = {task_1: {x: 100, y: 200}};

        const result = updateTaskPositions(tasks, nodePositions);

        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 200});
        expect(result[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should clear position for tasks in clearPositionNodeIds', () => {
        const tasks = [makeTask('task_1', {x: 50, y: 60})];
        const clearPositionNodeIds = new Set(['task_1']);

        const result = updateTaskPositions(tasks, {}, clearPositionNodeIds);

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should save dispatcher and delta-shifted child positions together', () => {
        // Simulates: dispatcher dragged, child had saved position,
        // handleNodeDragStop computed child's new position = oldPos + delta
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1'),
            parameters: {
                iteratee: [makeTask('child_1', {x: 300, y: 400})],
            },
        };
        const tasks = [loopTask];

        // Dispatcher moved to (200, 300), child updated by delta to (350, 450)
        const nodePositions = {
            child_1: {x: 350, y: 450},
            loop_1: {x: 200, y: 300},
        };

        const result = updateTaskPositions(tasks, nodePositions);

        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 200, y: 300});

        const childTasks = result[0].parameters?.iteratee as WorkflowTask[];

        expect(childTasks[0].metadata?.ui?.nodePosition).toEqual({x: 350, y: 450});
    });

    it('should save some children and clear others in the same dispatcher', () => {
        // child_1 has saved position (gets delta-shifted)
        // child_2 has no saved position (gets cleared)
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1'),
            parameters: {
                iteratee: [makeTask('child_1', {x: 300, y: 400}), makeTask('child_2', {x: 500, y: 600})],
            },
        };
        const tasks = [loopTask];

        const nodePositions = {
            child_1: {x: 350, y: 450},
            loop_1: {x: 200, y: 300},
        };
        const clearPositionNodeIds = new Set(['child_2']);

        const result = updateTaskPositions(tasks, nodePositions, clearPositionNodeIds);
        const childTasks = result[0].parameters?.iteratee as WorkflowTask[];

        // child_1: position updated
        expect(childTasks[0].metadata?.ui?.nodePosition).toEqual({x: 350, y: 450});
        // child_2: position cleared
        expect(childTasks[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should update positions inside condition caseTrue and caseFalse', () => {
        const conditionTask: WorkflowTask = {
            ...makeTask('condition_1'),
            parameters: {
                caseFalse: [makeTask('false_child', {x: 100, y: 100})],
                caseTrue: [makeTask('true_child', {x: 200, y: 200})],
            },
        };
        const tasks = [conditionTask];

        const nodePositions = {
            condition_1: {x: 50, y: 50},
            false_child: {x: 150, y: 150},
            true_child: {x: 250, y: 250},
        };

        const result = updateTaskPositions(tasks, nodePositions);
        const trueTasks = result[0].parameters?.caseTrue as WorkflowTask[];
        const falseTasks = result[0].parameters?.caseFalse as WorkflowTask[];

        expect(trueTasks[0].metadata?.ui?.nodePosition).toEqual({x: 250, y: 250});
        expect(falseTasks[0].metadata?.ui?.nodePosition).toEqual({x: 150, y: 150});
    });

    it('should update positions inside nested dispatchers', () => {
        // loop_1 > condition_1 > child_1
        const conditionTask: WorkflowTask = {
            ...makeTask('condition_1'),
            parameters: {
                caseTrue: [makeTask('child_1', {x: 500, y: 600})],
            },
        };
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1'),
            parameters: {
                iteratee: [conditionTask],
            },
        };
        const tasks = [loopTask];

        const nodePositions = {
            child_1: {x: 580, y: 680},
            condition_1: {x: 400, y: 500},
            loop_1: {x: 200, y: 300},
        };

        const result = updateTaskPositions(tasks, nodePositions);
        const iteratee = result[0].parameters?.iteratee as WorkflowTask[];
        const trueTasks = iteratee[0].parameters?.caseTrue as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 200, y: 300});
        expect(iteratee[0].metadata?.ui?.nodePosition).toEqual({x: 400, y: 500});
        expect(trueTasks[0].metadata?.ui?.nodePosition).toEqual({x: 580, y: 680});
    });

    it('should update positions inside branch cases and default', () => {
        const branchTask: WorkflowTask = {
            ...makeTask('branch_1'),
            parameters: {
                cases: [{tasks: [makeTask('case_child', {x: 100, y: 100})], value: 'A'}],
                default: [makeTask('default_child', {x: 200, y: 200})],
            },
        };
        const tasks = [branchTask];

        const nodePositions = {
            branch_1: {x: 50, y: 50},
            case_child: {x: 150, y: 150},
            default_child: {x: 250, y: 250},
        };

        const result = updateTaskPositions(tasks, nodePositions);
        const cases = result[0].parameters?.cases as Array<{tasks: WorkflowTask[]}>;
        const defaultTasks = result[0].parameters?.default as WorkflowTask[];

        expect(cases[0].tasks[0].metadata?.ui?.nodePosition).toEqual({x: 150, y: 150});
        expect(defaultTasks[0].metadata?.ui?.nodePosition).toEqual({x: 250, y: 250});
    });

    it('should handle non-array input gracefully', () => {
        const result = updateTaskPositions(null as unknown as WorkflowTask[], {task_1: {x: 1, y: 2}});

        expect(result).toBeNull();
    });
});
