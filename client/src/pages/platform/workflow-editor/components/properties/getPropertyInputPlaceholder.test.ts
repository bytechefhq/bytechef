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

    // An empty optional field is left unset, so the placeholder is the only place to say what the default is.
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

    // A required property with a default has that default saved into the workflow, so the field is never left
    // empty because of a missing value.
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

    it('ignores an empty default', () => {
        expect(getPropertyInputPlaceholder({defaultValue: ''})).toBe('Type something...');
        expect(getPropertyInputPlaceholder({defaultValue: null})).toBe('Type something...');
    });
});
