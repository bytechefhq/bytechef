import {
    MAX_SAMPLE_ARRAY_ROWS,
    formatSampleValue,
    resolveDataPillSampleArray,
} from '@/pages/platform/workflow-editor/utils/resolveDataPillSampleArray';
import {describe, expect, it} from 'vitest';

const sampleOutputs = {
    airtable_1: {
        rows: [
            {cells: ['a0', 'a1', 'a2'], id: 'rec1'},
            {cells: ['b0', 'b1'], id: 'rec2'},
        ],
    },
    ftp_1: [{name: 'first.txt'}, {name: 'second.txt'}],
    propertyTesting_1: {
        objectDefaultValues: {key1: 'value1'},
        stringRegEx: ['^a', '^b', '^c'],
    },
    quoted_1: {
        'some key': ['x', 'y'],
    },
};

describe('resolveDataPillSampleArray', () => {
    it('resolves a top level array property', () => {
        const result = resolveDataPillSampleArray({
            mentionId: 'propertyTesting_1.stringRegEx[0]',
            occurrence: 0,
            sampleOutputs,
        });

        expect(result).toEqual({cappedItems: ['^a', '^b', '^c'], uncappedLength: 3});
    });

    it('resolves an array that is the node output itself', () => {
        const result = resolveDataPillSampleArray({mentionId: 'ftp_1[1].name', occurrence: 0, sampleOutputs});

        expect(result).toEqual({cappedItems: [{name: 'first.txt'}, {name: 'second.txt'}], uncappedLength: 2});
    });

    it('resolves a nested array against the item the outer index selected', () => {
        const result = resolveDataPillSampleArray({
            mentionId: 'airtable_1.rows[1].cells[0]',
            occurrence: 1,
            sampleOutputs,
        });

        expect(result).toEqual({cappedItems: ['b0', 'b1'], uncappedLength: 2});
    });

    it('resolves the outer array of a nested reference', () => {
        const result = resolveDataPillSampleArray({
            mentionId: 'airtable_1.rows[1].cells[0]',
            occurrence: 0,
            sampleOutputs,
        });

        expect(result?.uncappedLength).toBe(2);
    });

    it('resolves a bracketed map key that a dot split would mangle', () => {
        const result = resolveDataPillSampleArray({mentionId: "quoted_1['some key'][0]", occurrence: 0, sampleOutputs});

        expect(result).toEqual({cappedItems: ['x', 'y'], uncappedLength: 2});
    });

    it('returns undefined when the node has no sample output', () => {
        expect(
            resolveDataPillSampleArray({mentionId: 'missing_1.rows[0]', occurrence: 0, sampleOutputs})
        ).toBeUndefined();
    });

    it('returns undefined when the path does not resolve', () => {
        expect(
            resolveDataPillSampleArray({mentionId: 'propertyTesting_1.nope[0]', occurrence: 0, sampleOutputs})
        ).toBeUndefined();
    });

    it('returns undefined when the resolved value is not an array', () => {
        expect(
            resolveDataPillSampleArray({
                mentionId: 'propertyTesting_1.objectDefaultValues[0]',
                occurrence: 0,
                sampleOutputs,
            })
        ).toBeUndefined();
    });

    it('returns undefined when the reference carries no array index', () => {
        expect(
            resolveDataPillSampleArray({mentionId: 'propertyTesting_1.stringRegEx', occurrence: 0, sampleOutputs})
        ).toBeUndefined();
    });

    it('returns undefined for the occurrence that does not exist', () => {
        expect(
            resolveDataPillSampleArray({mentionId: 'propertyTesting_1.stringRegEx[0]', occurrence: 1, sampleOutputs})
        ).toBeUndefined();
    });

    it('returns undefined for an empty reference', () => {
        expect(resolveDataPillSampleArray({mentionId: undefined, occurrence: 0, sampleOutputs})).toBeUndefined();
    });

    it('caps the listed items but reports the full length', () => {
        const longArray = Array.from({length: 120}, (_, index) => index);
        const result = resolveDataPillSampleArray({
            mentionId: 'long_1.values[0]',
            occurrence: 0,
            sampleOutputs: {long_1: {values: longArray}},
        });

        expect(result?.cappedItems).toHaveLength(MAX_SAMPLE_ARRAY_ROWS);
        expect(result?.uncappedLength).toBe(120);
    });
});

describe('formatSampleValue', () => {
    it('quotes strings', () => {
        expect(formatSampleValue('hello')).toBe('"hello"');
    });

    it('renders numbers and booleans plainly', () => {
        expect(formatSampleValue(42)).toBe('42');
        expect(formatSampleValue(false)).toBe('false');
    });

    it('renders null and undefined', () => {
        expect(formatSampleValue(null)).toBe('null');
        expect(formatSampleValue(undefined)).toBe('undefined');
    });

    it('renders objects as compact json', () => {
        expect(formatSampleValue({id: 'rec1'})).toBe('{"id":"rec1"}');
    });

    it('collapses whitespace so a row stays on one line', () => {
        expect(formatSampleValue('a\n  b')).toBe('"a b"');
    });

    it('truncates long values', () => {
        const formatted = formatSampleValue('x'.repeat(200));

        expect(formatted).toHaveLength(64);
        expect(formatted.endsWith('…')).toBe(true);
    });
});
