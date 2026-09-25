import {describe, expect, it} from 'vitest';

import {
    fromFormulaValue,
    getPropertyInputMode,
    isSingleDataPill,
    shouldIncludeInMetadata,
    toFormulaValue,
} from './propertyInputMode';

describe('getPropertyInputMode', () => {
    it.each([
        {
            controlType: 'INTEGER',
            expected: {legacyMixed: false, mode: 'text', renderer: 'native', singlePill: false},
            name: 'empty number renders the native control',
            value: '',
        },
        {
            controlType: 'INTEGER',
            expected: {legacyMixed: false, mode: 'text', renderer: 'native', singlePill: false},
            name: 'constant number renders the native control',
            value: 5,
        },
        {
            controlType: 'SELECT',
            expected: {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: true},
            name: 'a lone pill on a select renders the one-pill editor',
            value: '${trigger_1.id}',
        },
        {
            controlType: 'NUMBER',
            expected: {legacyMixed: true, mode: 'text', renderer: 'mentions', singlePill: false},
            name: 'text around a pill on a non-string is legacy mixed',
            value: '${trigger_1.id} ms',
        },
        {
            controlType: 'DATE',
            expected: {legacyMixed: true, mode: 'text', renderer: 'mentions', singlePill: false},
            name: 'two pills on a non-string are legacy mixed',
            value: '${a.b}${c.d}',
        },
        {
            controlType: 'TEXT',
            expected: {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: false},
            name: 'a text-like control always mixes text and pills',
            value: 'hello ${a.b}',
        },
        {
            controlType: 'INTEGER',
            expected: {legacyMixed: false, mode: 'formula', renderer: 'mentions', singlePill: false},
            name: 'an = value is formula',
            value: '=1 + 2',
        },
        {
            controlType: 'BOOLEAN',
            expected: {legacyMixed: false, mode: 'formula', renderer: 'mentions', singlePill: false},
            name: 'a customised fromAi value that is not flagged fromAi is formula',
            value: "=fromAi('x', 'BOOLEAN', {'required': true, 'note': 'edited'})",
        },
    ])('$name', ({controlType, expected, value}) => {
        expect(getPropertyInputMode({controlType, formulaMode: false, isFromAi: false, value})).toEqual(expected);
    });

    it('formula state wins over an empty value', () => {
        expect(getPropertyInputMode({controlType: 'INTEGER', formulaMode: true, isFromAi: false, value: ''})).toEqual({
            legacyMixed: false,
            mode: 'formula',
            renderer: 'mentions',
            singlePill: false,
        });
    });

    it('FORMULA_MODE control type is always formula', () => {
        expect(
            getPropertyInputMode({controlType: 'FORMULA_MODE', formulaMode: false, isFromAi: false, value: ''}).mode
        ).toBe('formula');
    });

    it('fromAi wins over an = value and never reports formula', () => {
        expect(
            getPropertyInputMode({
                controlType: 'INTEGER',
                formulaMode: true,
                isFromAi: true,
                value: "=fromAi('count', 'INTEGER', {'required': false})",
            })
        ).toEqual({legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: false});
    });

    it('pill entry shows the one-pill editor on an empty value', () => {
        expect(
            getPropertyInputMode({
                controlType: 'INTEGER',
                formulaMode: false,
                isFromAi: false,
                pillEntry: true,
                value: '',
            })
        ).toEqual({legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: true});
    });

    it('a controlled text-like field is a native input', () => {
        expect(
            getPropertyInputMode({
                controlType: 'TEXT',
                formulaMode: false,
                hasControl: true,
                isFromAi: false,
                value: 'x',
            }).renderer
        ).toBe('native');
    });

    it('an uncontrolled FILE_ENTRY is a mentions input, a controlled one is not', () => {
        expect(
            getPropertyInputMode({controlType: 'FILE_ENTRY', formulaMode: false, isFromAi: false, value: ''}).renderer
        ).toBe('mentions');
        expect(
            getPropertyInputMode({
                controlType: 'FILE_ENTRY',
                formulaMode: false,
                hasControl: true,
                isFromAi: false,
                value: '',
            }).renderer
        ).toBe('native');
    });
});

describe('toFormulaValue', () => {
    it.each([
        {expected: undefined, type: 'INTEGER', value: ''},
        {expected: undefined, type: 'INTEGER', value: null},
        {expected: '=5', type: 'INTEGER', value: 5},
        {expected: '=5', type: 'INTEGER', value: '5'},
        {expected: '=true', type: 'BOOLEAN', value: true},
        {expected: '=${a.b}', type: 'NUMBER', value: '${a.b}'},
        {expected: "='CURRENT_EXECUTION'", type: 'STRING', value: 'CURRENT_EXECUTION'},
        {expected: "='123'", type: 'STRING', value: '123'},
        {expected: "='it''s'", type: 'STRING', value: "it's"},
        {expected: undefined, type: 'STRING', value: 'hello ${a.b}'},
        {expected: '=1+1', type: 'INTEGER', value: '=1+1'},
        {expected: undefined, type: 'OBJECT', value: {key: 'value'}},
    ])('$value ($type) → $expected', ({expected, type, value}) => {
        expect(toFormulaValue(value, type)).toBe(expected);
    });
});

describe('fromFormulaValue', () => {
    it.each([
        {expected: '${a.b}', type: 'NUMBER', value: '=${a.b}'},
        {expected: 5, type: 'INTEGER', value: '=5'},
        {expected: 2.5, type: 'NUMBER', value: '=2.5'},
        {expected: undefined, type: 'INTEGER', value: '=2.5'},
        {expected: true, type: 'BOOLEAN', value: '=true'},
        {expected: 'x', type: 'STRING', value: "='x'"},
        {expected: "it's", type: 'STRING', value: "='it''s'"},
        {expected: undefined, type: 'NUMBER', value: "='x'"},
        {expected: undefined, type: 'NUMBER', value: '=concat(a, b)'},
        {expected: undefined, type: 'STRING', value: '='},
        {expected: undefined, type: 'STRING', value: 'plain'},
    ])('$value ($type) → $expected', ({expected, type, value}) => {
        expect(fromFormulaValue(value, type)).toBe(expected);
    });
});

describe('helpers', () => {
    it('recognises exactly one pill', () => {
        expect(isSingleDataPill('${a.b[0]}')).toBe(true);
        expect(isSingleDataPill('${a}${b}')).toBe(false);
        expect(isSingleDataPill(' ${a}')).toBe(false);
        expect(isSingleDataPill(5)).toBe(false);
    });

    it('records metadata for expressions and pills, and falls back to custom for constants', () => {
        expect(shouldIncludeInMetadata('=1', false)).toBe(true);
        expect(shouldIncludeInMetadata('${a.b}', false)).toBe(true);
        expect(shouldIncludeInMetadata(5, false)).toBe(false);
        expect(shouldIncludeInMetadata(5, true)).toBe(true);
    });
});
