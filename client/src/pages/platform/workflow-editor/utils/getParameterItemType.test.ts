import {describe, expect, it} from 'vitest';

import getParameterItemType from './getParameterItemType';

describe('getParameterItemType', () => {
    it.each([
        [{key: 'value'}, 'OBJECT'],
        [{}, 'OBJECT'],
        [[1, 2], 'ARRAY'],
        [[], 'ARRAY'],
        ['2024-01-31T10:20:30Z', 'DATE_TIME'],
        ['2024-01-31T10:20:30.123Z', 'DATE_TIME'],
        ['2024-01-31T10:20:30+02:00', 'STRING'],
        ['2024-01-31', 'DATE'],
        ['2024-1-31', 'STRING'],
        ['hello', 'STRING'],
        ['', 'STRING'],
        [42, 'INTEGER'],
        [0, 'INTEGER'],
        [3.14, 'NUMBER'],
        [-0.5, 'NUMBER'],
        [true, 'BOOLEAN'],
        [false, 'BOOLEAN'],
        [null, 'STRING'],
        [undefined, 'STRING'],
    ])('classifies %j as %s', (value, expected) => {
        expect(getParameterItemType(value)).toBe(expected);
    });

    it('classifies negative integers as INTEGER', () => {
        expect(getParameterItemType(-5)).toBe('INTEGER');
        expect(getParameterItemType(-1)).toBe('INTEGER');
    });

    it('classifies non-finite numbers as NUMBER', () => {
        expect(getParameterItemType(Number.NaN)).toBe('NUMBER');
        expect(getParameterItemType(Number.POSITIVE_INFINITY)).toBe('NUMBER');
    });
});
