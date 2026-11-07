import {describe, expect, it} from 'vitest';

import collectConditionCaseCounts from './collectConditionCaseCounts';
import {type ConditionCaseTaskExecutionI, toConditionCaseKey} from './collectTakenEmptyConditionCases';

const conditionParameters = {
    caseFalse: [{name: 'odd_label'}],
    caseTrue: [{name: 'even_label'}],
};

const conditionExecution = (
    childName: string | undefined,
    parameters: {[key: string]: unknown} = conditionParameters,
    status = 'COMPLETED'
): ConditionCaseTaskExecutionI => ({
    children: childName ? [{status: 'COMPLETED', workflowTask: {name: childName}}] : [],
    status,
    workflowTask: {name: 'condition_1', parameters, type: 'condition/v1'},
});

const loopExecution = (iterations: ConditionCaseTaskExecutionI[][]): ConditionCaseTaskExecutionI => ({
    iterations,
    status: 'COMPLETED',
    workflowTask: {name: 'loop_1', type: 'loop/v1'},
});

describe('collectConditionCaseCounts', () => {
    it('counts every iteration of a condition in a loop toward the case its tasks belong to', () => {
        const counts = collectConditionCaseCounts([
            loopExecution([
                [conditionExecution('odd_label')],
                [conditionExecution('even_label')],
                [conditionExecution('odd_label')],
                [conditionExecution('even_label')],
                [conditionExecution('odd_label')],
            ]),
        ]);

        expect(counts.get(toConditionCaseKey('condition_1', 'caseTrue'))).toBe(2);
        expect(counts.get(toConditionCaseKey('condition_1', 'caseFalse'))).toBe(3);
    });

    it('counts a case as soon as its first task has started', () => {
        const counts = collectConditionCaseCounts([conditionExecution('even_label', conditionParameters, 'STARTED')]);

        expect(counts.get(toConditionCaseKey('condition_1', 'caseTrue'))).toBe(1);
    });

    it('counts a completed condition without children toward its only empty case', () => {
        const parameters = {caseFalse: [], caseTrue: [{name: 'even_label'}]};

        const counts = collectConditionCaseCounts([
            conditionExecution(undefined, parameters),
            conditionExecution(undefined, parameters),
            conditionExecution('even_label', parameters),
        ]);

        expect(counts.get(toConditionCaseKey('condition_1', 'caseFalse'))).toBe(2);
        expect(counts.get(toConditionCaseKey('condition_1', 'caseTrue'))).toBe(1);
    });

    it('does not guess the case of a condition whose cases are both empty', () => {
        const counts = collectConditionCaseCounts([conditionExecution(undefined, {caseFalse: [], caseTrue: []})]);

        expect(counts.size).toBe(0);
    });

    it('does not count a condition that is still evaluating or failed before entering a case', () => {
        const parameters = {caseFalse: [], caseTrue: [{name: 'even_label'}]};

        const counts = collectConditionCaseCounts([
            conditionExecution(undefined, parameters, 'STARTED'),
            conditionExecution(undefined, parameters, 'FAILED'),
        ]);

        expect(counts.size).toBe(0);
    });

    it('counts a condition nested in a case of another condition', () => {
        const innerCondition: ConditionCaseTaskExecutionI = {
            children: [{status: 'COMPLETED', workflowTask: {name: 'inner_true_task'}}],
            status: 'COMPLETED',
            workflowTask: {
                name: 'condition_2',
                parameters: {caseFalse: [], caseTrue: [{name: 'inner_true_task'}]},
                type: 'condition/v1',
            },
        };

        const counts = collectConditionCaseCounts([
            {
                children: [innerCondition],
                status: 'COMPLETED',
                workflowTask: {
                    name: 'condition_1',
                    parameters: {caseFalse: [{name: 'condition_2'}], caseTrue: []},
                    type: 'condition/v1',
                },
            },
        ]);

        expect(counts.get(toConditionCaseKey('condition_1', 'caseFalse'))).toBe(1);
        expect(counts.get(toConditionCaseKey('condition_2', 'caseTrue'))).toBe(1);
    });

    it('returns no counts without task executions', () => {
        expect(collectConditionCaseCounts(undefined).size).toBe(0);
    });
});
