import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {updateTaskPositions} from './saveWorkflowNodesPosition';

describe('updateTaskPositions', () => {
    it('writes a node position onto a task inside a graph node list', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'graph_1',
                parameters: {nodes: [{name: 'a', type: 't'}]},
                type: 'graph/v1',
            } as WorkflowTask,
        ];

        const result = updateTaskPositions(tasks, {a: {x: 10, y: 20}});

        const updatedNodes = result[0].parameters?.nodes as Array<WorkflowTask>;

        expect(updatedNodes[0].metadata?.ui.nodePosition).toEqual({x: 10, y: 20});
    });
});
