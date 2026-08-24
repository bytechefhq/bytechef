import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getEffectivelyDisabledTaskNames} from './getEffectivelyDisabledTaskNames';

function task(
    name: string,
    options: {disabled?: boolean; parameters?: Record<string, unknown>; type?: string} = {}
): WorkflowTask {
    const {disabled, parameters = {}, type = 'test/v1/action'} = options;

    return {disabled, name, parameters, type};
}

describe('getEffectivelyDisabledTaskNames', () => {
    it('marks only the explicitly disabled task in a flat list', () => {
        const tasks = [task('a'), task('b', {disabled: true}), task('c')];

        expect(getEffectivelyDisabledTaskNames(tasks)).toEqual(new Set(['b']));
    });

    it('marks a disabled condition task and its caseTrue subtask', () => {
        const conditionTask = task('condition_1', {
            disabled: true,
            parameters: {caseTrue: [task('true_1')]},
            type: 'condition/v1',
        });

        expect(getEffectivelyDisabledTaskNames([conditionTask])).toEqual(new Set(['condition_1', 'true_1']));
    });

    it('marks only the disabled iteratee subtask when the loop itself is enabled', () => {
        const loopTask = task('loop_1', {
            parameters: {iteratee: [task('inner_1', {disabled: true}), task('inner_2')]},
            type: 'loop/v1',
        });

        expect(getEffectivelyDisabledTaskNames([loopTask])).toEqual(new Set(['inner_1']));
    });

    it('propagates ancestor disablement through the cases[].tasks shape', () => {
        const branchTask = task('branch_1', {
            disabled: true,
            parameters: {
                cases: [{key: 'case_a', tasks: [task('case_a_1')]}],
                default: [task('default_1')],
            },
            type: 'branch/v1',
        });

        expect(getEffectivelyDisabledTaskNames([branchTask])).toEqual(new Set(['branch_1', 'case_a_1', 'default_1']));
    });

    it('collects a disabled task nested in a pre/post/finalize hook list', () => {
        const parentTask = {
            ...task('parent_1'),
            finalize: [task('finalize_1', {disabled: true})],
            post: [task('post_1', {disabled: true})],
            pre: [task('pre_1', {disabled: true})],
        };

        expect(getEffectivelyDisabledTaskNames([parentTask])).toEqual(new Set(['pre_1', 'post_1', 'finalize_1']));
    });

    it('propagates ancestor disablement into pre/post/finalize hook lists', () => {
        const parentTask = {
            ...task('parent_1', {disabled: true}),
            post: [task('post_1')],
        };

        expect(getEffectivelyDisabledTaskNames([parentTask])).toEqual(new Set(['parent_1', 'post_1']));
    });

    it('returns an empty set when nothing is disabled', () => {
        const tasks = [task('a'), task('b', {parameters: {iteratee: [task('inner_1')]}, type: 'loop/v1'})];

        expect(getEffectivelyDisabledTaskNames(tasks)).toEqual(new Set());
    });
});
