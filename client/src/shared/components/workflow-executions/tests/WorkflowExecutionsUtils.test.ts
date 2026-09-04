import {describe, expect, it} from 'vitest';

import {getDisplayValue, getFilteredOutput, hasValue} from '../WorkflowExecutionsUtils';

const triggerExecution = {
    id: '77',
    input: {raw: 'body'},
    output: {orderId: '42'},
    workflowTrigger: {name: 'trigger_1'},
};

describe('WorkflowExecutionsUtils', () => {
    describe('getDisplayValue', () => {
        it('has nothing to show without a selected item', () => {
            expect(getDisplayValue({selectedItem: undefined, tab: 'output'})).toBeUndefined();
        });

        it('shows the selected item input on the input tab', () => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect(getDisplayValue({selectedItem: triggerExecution as any, tab: 'input'})).toEqual({raw: 'body'});
        });

        it('shows the selected item error on the error tab', () => {
            const failed = {error: {message: 'Signature check failed'}, id: '77'};

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect(getDisplayValue({selectedItem: failed as any, tab: 'error'})).toEqual({
                message: 'Signature check failed',
            });
        });

        it('shows what the trigger handed the job when the run produced one', () => {
            const displayValue = getDisplayValue({
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                job: {inputs: {trigger_1: {orderId: '42', signature: 'abc'}}} as any,
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                selectedItem: triggerExecution as any,
                tab: 'output',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                triggerExecution: triggerExecution as any,
            });

            expect(displayValue).toEqual({orderId: '42', signature: 'abc'});
        });

        it('falls back to the trigger output for a trigger row that never produced a job', () => {
            const displayValue = getDisplayValue({
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                selectedItem: triggerExecution as any,
                tab: 'output',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                triggerExecution: triggerExecution as any,
            });

            expect(displayValue).toEqual({orderId: '42'});
        });
    });

    describe('getFilteredOutput', () => {
        it('keeps the raw output when the job carries no input for the trigger', () => {
            expect(getFilteredOutput({orderId: '42'}, {other_trigger: {}}, 'trigger_1')).toEqual({orderId: '42'});
        });
    });

    describe('hasValue', () => {
        it('treats an empty object, an empty array, null and undefined as no value', () => {
            expect(hasValue({})).toBe(false);
            expect(hasValue([])).toBe(false);
            expect(hasValue(null)).toBe(false);
            expect(hasValue(undefined)).toBe(false);
        });

        it('treats a populated object, a populated array and a primitive as a value', () => {
            expect(hasValue({orderId: '42'})).toBe(true);
            expect(hasValue(['a'])).toBe(true);
            expect(hasValue('text')).toBe(true);
        });
    });
});
