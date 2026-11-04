import {describe, expect, it} from 'vitest';

import {getExecutionSelectionKey, isSameExecution} from '../executionSelectionKey';

const triggerExecution = {id: '34', workflowTrigger: {name: 'trigger_1'}};
const taskExecution = {id: '34', jobId: '34', workflowTask: {name: 'var_1'}};
const idlessTriggerExecution = {id: undefined, workflowTrigger: {name: 'trigger_1'}};
const callExecution = {clusterElementCallId: '34', id: '34', jobId: '34', workflowTask: {name: 'httpClient_1'}};

describe('executionSelectionKey', () => {
    it('keys an execution by its kind and id', () => {
        expect(getExecutionSelectionKey(triggerExecution)).toBe('trigger:34');
        expect(getExecutionSelectionKey(taskExecution)).toBe('task:34');
        expect(getExecutionSelectionKey(callExecution)).toBe('call:34');
    });

    it('keys a missing execution, or one without an id, as empty', () => {
        expect(getExecutionSelectionKey(undefined)).toBe('');
        expect(getExecutionSelectionKey(idlessTriggerExecution)).toBe('');
    });

    it('tells a trigger execution apart from a task execution with the same id', () => {
        expect(isSameExecution(taskExecution, triggerExecution)).toBe(false);
        expect(isSameExecution(callExecution, taskExecution)).toBe(false);
        expect(isSameExecution(triggerExecution, {...triggerExecution})).toBe(true);
        expect(isSameExecution(taskExecution, {...taskExecution})).toBe(true);
    });

    it('never matches executions without an id', () => {
        expect(isSameExecution(undefined, undefined)).toBe(false);
        expect(isSameExecution({}, {})).toBe(false);
    });
});
