import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getAvailableTaskNamesByTaskName from './getAvailableTaskNamesByTaskName';

function task(name: string, parameters?: Record<string, unknown>, type = 'var/v1/set'): WorkflowTask {
    return {name, parameters, type} as WorkflowTask;
}

function graph(nodes: Array<WorkflowTask>, transitions: Array<{from: string; to: string}>): WorkflowTask {
    return {name: 'graph_1', parameters: {nodes, transitions}, type: 'graph/v1'} as WorkflowTask;
}

describe('getAvailableTaskNamesByTaskName', () => {
    it('offers tasks listed before a task outside a graph, and none after it', () => {
        const available = getAvailableTaskNamesByTaskName([task('task_1'), task('task_2'), task('task_3')]);

        expect([...available.get('task_2')!]).toEqual(['task_1']);
    });

    it('offers a graph member that a loop runs before it, even when it is listed later', () => {
        const available = getAvailableTaskNamesByTaskName([
            task('trigger_task'),
            graph(
                [task('attempts'), task('draft'), task('judge'), task('feedback'), task('strong')],
                [
                    {from: 'attempts', to: 'draft'},
                    {from: 'draft', to: 'judge'},
                    {from: 'judge', to: 'feedback'},
                    {from: 'feedback', to: 'attempts'},
                    {from: 'feedback', to: 'strong'},
                ]
            ),
        ]);

        expect(available.get('attempts')).toEqual(
            new Set(['trigger_task', 'graph_1', 'attempts', 'draft', 'judge', 'feedback'])
        );
        expect(available.get('strong')).toEqual(
            new Set(['trigger_task', 'graph_1', 'attempts', 'draft', 'judge', 'feedback'])
        );
    });

    it('leaves out a graph member on a sibling branch that never runs before it', () => {
        const available = getAvailableTaskNamesByTaskName([
            graph(
                [task('start'), task('left'), task('right')],
                [
                    {from: 'start', to: 'left'},
                    {from: 'start', to: 'right'},
                ]
            ),
        ]);

        expect(available.get('right')).toEqual(new Set(['graph_1', 'start']));
    });

    it('makes every task of a loop body available to each task inside it, itself included', () => {
        const loopTask = task('loop_1', {iteratee: [task('counter'), task('later')]}, 'loop/v1');

        const availableTaskNamesByTaskName = getAvailableTaskNamesByTaskName([task('before'), loopTask, task('after')]);

        expect(availableTaskNamesByTaskName.get('counter')).toEqual(new Set(['before', 'loop_1', 'counter', 'later']));
        expect(availableTaskNamesByTaskName.get('later')).toEqual(new Set(['before', 'loop_1', 'counter', 'later']));
        expect(availableTaskNamesByTaskName.get('before')).toEqual(new Set());
    });

    it('keeps the tasks of a condition case positional', () => {
        const conditionTask = task('condition_1', {caseFalse: [], caseTrue: [task('x'), task('y')]}, 'condition/v1');

        const availableTaskNamesByTaskName = getAvailableTaskNamesByTaskName([conditionTask]);

        expect(availableTaskNamesByTaskName.get('x')).toEqual(new Set(['condition_1']));
    });

    it('offers every graph member, itself included, when any transition targets an expression', () => {
        const available = getAvailableTaskNamesByTaskName([
            graph(
                [task('classify'), task('escalate'), task('resolve')],
                [{from: 'classify', to: '=${classify.decision}'}]
            ),
        ]);

        expect(available.get('classify')).toEqual(new Set(['graph_1', 'classify', 'escalate', 'resolve']));
    });

    it('offers a task nested in a graph member the members that run before that member', () => {
        const available = getAvailableTaskNamesByTaskName([
            graph(
                [task('review', {iteratee: [task('inner')]}), task('draft'), task('publish')],
                [
                    {from: 'draft', to: 'review'},
                    {from: 'review', to: 'publish'},
                ]
            ),
        ]);

        expect(available.get('inner')).toEqual(new Set(['graph_1', 'draft', 'review']));
    });
});
