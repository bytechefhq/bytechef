import {describe, expect, it} from 'vitest';

import getPropertyInputPlaceholder from './getPropertyInputPlaceholder';

describe('getPropertyInputPlaceholder', () => {
    it('falls back to a generic prompt', () => {
        expect(getPropertyInputPlaceholder({})).toBe('Type something...');
        expect(getPropertyInputPlaceholder({isNumericalInput: true})).toBe('Type a number...');
    });

    it('uses the property placeholder', () => {
        expect(getPropertyInputPlaceholder({placeholder: 'https://example.com'})).toBe('https://example.com');
    });

    it('shows the range of a numerical input', () => {
        expect(getPropertyInputPlaceholder({isNumericalInput: true, maxValue: 128000, minValue: 1})).toBe(
            'From 1 to 128000'
        );
    });

    it('shows a range with a zero bound', () => {
        expect(getPropertyInputPlaceholder({isNumericalInput: true, maxValue: 1, minValue: 0})).toBe('From 0 to 1');
    });

    it('shows the default of an optional property', () => {
        expect(getPropertyInputPlaceholder({defaultValue: 'gpt-4o', placeholder: 'Model name'})).toBe(
            'Default: gpt-4o'
        );
    });

    it('shows the default and the range of an optional numerical property', () => {
        expect(
            getPropertyInputPlaceholder({defaultValue: 16000, isNumericalInput: true, maxValue: 128000, minValue: 1})
        ).toBe('Default: 16000 · From 1 to 128000');
    });

    it('shows a falsy default that is set', () => {
        expect(getPropertyInputPlaceholder({defaultValue: 0, isNumericalInput: true})).toBe('Default: 0');
    });

    it('does not show the default of a required property', () => {
        expect(
            getPropertyInputPlaceholder({
                defaultValue: 16000,
                isNumericalInput: true,
                maxValue: 128000,
                minValue: 1,
                required: true,
            })
        ).toBe('From 1 to 128000');
    });

    it('ignores a default that is not a primitive', () => {
        expect(getPropertyInputPlaceholder({defaultValue: {key: 'value'}})).toBe('Type something...');
        expect(getPropertyInputPlaceholder({defaultValue: ['a', 'b']})).toBe('Type something...');
    });

    it('shows a boolean default', () => {
        expect(getPropertyInputPlaceholder({defaultValue: false})).toBe('Default: false');
    });

    it('ignores an empty default', () => {
        expect(getPropertyInputPlaceholder({defaultValue: ''})).toBe('Type something...');
        expect(getPropertyInputPlaceholder({defaultValue: null})).toBe('Type something...');
    });
});
