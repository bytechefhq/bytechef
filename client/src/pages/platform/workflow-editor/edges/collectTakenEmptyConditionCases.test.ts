import {describe, expect, it} from 'vitest';

import collectTakenEmptyConditionCases, {
    type ConditionCaseTaskExecutionI,
    toConditionCaseKey,
} from './collectTakenEmptyConditionCases';

function conditionExecution(
    name: string,
    caseTrue: unknown[],
    caseFalse: unknown[],
    overrides: Partial<ConditionCaseTaskExecutionI> = {}
): ConditionCaseTaskExecutionI {
    return {
        status: 'COMPLETED',
        workflowTask: {name, parameters: {caseFalse, caseTrue}, type: 'condition/v1'},
        ...overrides,
    };
}

const loggerTask = {name: 'logger_1', type: 'logger/v1/info'};

describe('collectTakenEmptyConditionCases', () => {
    it('records the empty case a childless visit took when the other case has tasks', () => {
        const takenCases = collectTakenEmptyConditionCases([conditionExecution('condition_1', [], [loggerTask])]);

        expect([...takenCases]).toEqual([toConditionCaseKey('condition_1', 'caseTrue')]);
    });

    it('records nothing for a visit that ran a child, since that branch was not the empty one', () => {
        const takenCases = collectTakenEmptyConditionCases([
            conditionExecution('condition_1', [], [loggerTask], {
                children: [{status: 'COMPLETED', workflowTask: {name: 'logger_1'}}],
            }),
        ]);

        expect(takenCases.size).toBe(0);
    });

    it('records nothing when both cases are empty, because the visit cannot say which one it took', () => {
        const takenCases = collectTakenEmptyConditionCases([conditionExecution('condition_2', [], [])]);

        expect(takenCases.size).toBe(0);
    });

    it('records nothing for a visit that did not complete', () => {
        const takenCases = collectTakenEmptyConditionCases([
            conditionExecution('condition_1', [loggerTask], [], {status: 'FAILED'}),
        ]);

        expect(takenCases.size).toBe(0);
    });

    it('finds conditions nested in loop iterations and in other conditions', () => {
        const takenCases = collectTakenEmptyConditionCases([
            {
                iterations: [
                    [
                        conditionExecution('condition_1', [{name: 'condition_2'}], [loggerTask], {
                            children: [conditionExecution('condition_2', [loggerTask], [])],
                        }),
                    ],
                ],
                status: 'COMPLETED',
                workflowTask: {name: 'loop_1', type: 'loop/v1'},
            },
        ]);

        expect([...takenCases]).toEqual([toConditionCaseKey('condition_2', 'caseFalse')]);
    });

    it('ignores a non-condition task that happens to have no children', () => {
        const takenCases = collectTakenEmptyConditionCases([
            {status: 'COMPLETED', workflowTask: {name: 'logger_1', parameters: {caseTrue: []}, type: 'logger/v1'}},
        ]);

        expect(takenCases.size).toBe(0);
    });
});
