import {ON_ERROR_WIRE_KEY_ERROR_BRANCH, ON_ERROR_WIRE_KEY_MAIN_BRANCH} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getAllTaskNames from './getAllTaskNames';

describe('getAllTaskNames', () => {
    it('returns an empty list for no tasks', () => {
        expect(getAllTaskNames([])).toEqual([]);
    });

    it('returns the names of flat tasks in order', () => {
        const tasks: WorkflowTask[] = [
            {name: 'first', type: 'http/v1/get'},
            {name: 'second', type: 'slack/v1/send'},
        ];

        expect(getAllTaskNames(tasks)).toEqual(['first', 'second']);
    });

    it('includes tasks from both condition branches', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'condition_1',
                parameters: {
                    caseFalse: [{name: 'falseTask', type: 'http/v1/get'}],
                    caseTrue: [{name: 'trueTask', type: 'http/v1/get'}],
                },
                type: 'condition/v1',
            },
        ];

        expect(getAllTaskNames(tasks)).toEqual(['condition_1', 'trueTask', 'falseTask']);
    });

    it('handles a condition without branches', () => {
        expect(getAllTaskNames([{name: 'condition_1', type: 'condition/v1'}])).toEqual(['condition_1']);
    });

    it('includes tasks from nested conditions', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'outer',
                parameters: {
                    caseTrue: [
                        {
                            name: 'inner',
                            parameters: {caseTrue: [{name: 'deep', type: 'http/v1/get'}]},
                            type: 'condition/v1',
                        },
                    ],
                },
                type: 'condition/v1',
            },
        ];

        expect(getAllTaskNames(tasks)).toEqual(['outer', 'inner', 'deep']);
    });

    it('includes tasks from both on-error branches', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'onError_1',
                parameters: {
                    [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [{name: 'handler', type: 'slack/v1/send'}],
                    [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [{name: 'risky', type: 'http/v1/get'}],
                },
                type: 'on-error/v1',
            },
        ];

        expect(getAllTaskNames(tasks)).toEqual(['onError_1', 'risky', 'handler']);
    });

    it('ignores on-error branches that are not arrays', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'onError_1',
                parameters: {[ON_ERROR_WIRE_KEY_MAIN_BRANCH]: 'not-an-array'},
                type: 'on-error/v1',
            },
            {name: 'onError_2', type: 'on-error/v1'},
        ];

        expect(getAllTaskNames(tasks)).toEqual(['onError_1', 'onError_2']);
    });

    it('does not treat on-error branch keys of other task types as branches', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'other_1',
                parameters: {[ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [{name: 'hidden', type: 'http/v1/get'}]},
                type: 'not-on-error/v1',
            },
        ];

        expect(getAllTaskNames(tasks)).toEqual(['other_1']);
    });
});
