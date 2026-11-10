import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getAvailableTaskNamesByTaskName from './getAvailableTaskNamesByTaskName';

function task(name: string, parameters?: Record<string, unknown>, type = 'component/v1/action'): WorkflowTask {
    return {name, parameters, type} as WorkflowTask;
}

describe('getAvailableTaskNamesByTaskName', () => {
    it('makes only earlier tasks available in a flat list', () => {
        const availableTaskNamesByTaskName = getAvailableTaskNamesByTaskName([task('a'), task('b'), task('c')]);

        expect([...availableTaskNamesByTaskName.get('b')!]).toEqual(['a']);
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
});
