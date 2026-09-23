import {toInputValueText} from '@/ee/pages/embedded/automation-hub/utils/inputValue';
import {describe, expect, it} from 'vitest';

describe('toInputValueText', () => {
    it('renders missing values as an empty string', () => {
        expect(toInputValueText(undefined)).toBe('');
        expect(toInputValueText(null)).toBe('');
    });

    it('keeps strings and formats numbers and booleans', () => {
        expect(toInputValueText('eu')).toBe('eu');
        expect(toInputValueText(42)).toBe('42');
        expect(toInputValueText(false)).toBe('false');
    });

    it('renders objects as JSON instead of [object Object]', () => {
        expect(toInputValueText({region: 'eu'})).toBe('{"region":"eu"}');
        expect(toInputValueText(['a', 'b'])).toBe('["a","b"]');
    });
});
