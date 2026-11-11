import {describe, expect, it} from 'vitest';

import {toFormParameters, toStoredParameters, toStoredWorkflowParameters} from '../toolParameterConversion';

describe('toFormParameters', () => {
    it('turns null values into empty strings at every object level', () => {
        expect(toFormParameters({channel: null, nested: {deeper: {value: null}, kept: 1}, text: 'Hi'})).toEqual({
            channel: '',
            nested: {deeper: {value: ''}, kept: 1},
            text: 'Hi',
        });
    });

    it('leaves arrays and other values untouched', () => {
        expect(toFormParameters({count: 0, enabled: false, items: [null, 'a'], missing: undefined})).toEqual({
            count: 0,
            enabled: false,
            items: [null, 'a'],
            missing: undefined,
        });
    });
});

describe('toStoredParameters', () => {
    it('turns empty strings into null at every object level', () => {
        expect(toStoredParameters({channel: '', nested: {deeper: {value: ''}, kept: 'x'}})).toEqual({
            channel: null,
            nested: {deeper: {value: null}, kept: 'x'},
        });
    });

    it('leaves blank strings inside arrays untouched', () => {
        expect(toStoredParameters({items: ['', {name: ''}]})).toEqual({items: ['', {name: ''}]});
    });

    it('reverses toFormParameters for objects without arrays', () => {
        const stored = {channel: null, nested: {value: null}, text: 'Hi'};

        expect(toStoredParameters(toFormParameters(stored))).toEqual(stored);
    });
});

describe('toStoredWorkflowParameters', () => {
    it('turns empty strings into null inside objects and arrays', () => {
        expect(
            toStoredWorkflowParameters({
                items: [{name: ''}, '', 'kept', ['']],
                message: '',
                nested: {list: ['', 1]},
            })
        ).toEqual({
            items: [{name: null}, null, 'kept', [null]],
            message: null,
            nested: {list: [null, 1]},
        });
    });

    it('keeps null, numbers and booleans as they are', () => {
        expect(toStoredWorkflowParameters({count: 0, enabled: false, value: null})).toEqual({
            count: 0,
            enabled: false,
            value: null,
        });
    });
});
