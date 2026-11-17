import {describe, expect, it} from 'vitest';

import {getEnabledWorkflowTasks} from './getEnabledWorkflowTasks';

const tasks = [{name: 'task_1'}, {name: 'task_2'}];

describe('getEnabledWorkflowTasks', () => {
    it('should return every task when the workflow has no definition', () => {
        expect(getEnabledWorkflowTasks({tasks})).toEqual(tasks);
    });

    it('should return every task when the definition is not valid JSON', () => {
        expect(getEnabledWorkflowTasks({definition: '{', tasks})).toEqual(tasks);
    });

    it('should return no tasks when the workflow has none', () => {
        expect(getEnabledWorkflowTasks({definition: JSON.stringify({tasks: []})})).toEqual([]);
    });

    it('should drop tasks the definition disables', () => {
        expect(
            getEnabledWorkflowTasks({
                definition: JSON.stringify({tasks: [{name: 'task_1'}, {disabled: true, name: 'task_2'}]}),
                tasks,
            })
        ).toEqual([{name: 'task_1'}]);
    });
});
