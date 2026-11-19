import {describe, expect, it} from 'vitest';

import formatWorkflowInputTestValue from '../formatWorkflowInputTestValue';

describe('formatWorkflowInputTestValue', () => {
    it('returns an empty string for a missing value', () => {
        expect(formatWorkflowInputTestValue(undefined)).toBe('');
        expect(formatWorkflowInputTestValue(null)).toBe('');
    });

    it('formats a scalar value', () => {
        expect(formatWorkflowInputTestValue('hello')).toBe('hello');
        expect(formatWorkflowInputTestValue(42)).toBe('42');
        expect(formatWorkflowInputTestValue(true)).toBe('true');
    });

    it('joins the filled members of a component input value', () => {
        expect(formatWorkflowInputTestValue({channel: 'general', empty: '', missing: null, team: 'T1'})).toBe(
            'general, T1'
        );
    });

    it('formats a nested member as JSON instead of [object Object]', () => {
        expect(formatWorkflowInputTestValue({channel: {id: 'C1'}, team: 'T1'})).toBe('{"id":"C1"}, T1');
    });
});
