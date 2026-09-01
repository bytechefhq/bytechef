import {
    getArrayIndexSegments,
    resolveArrayIndexTemplate,
    setArrayIndex,
    splitByArrayIndex,
    toArrayIndexTemplate,
} from '@/pages/platform/workflow-editor/utils/dataPillArrayIndex';
import {describe, expect, it} from 'vitest';

describe('dataPillArrayIndex', () => {
    describe('getArrayIndexSegments', () => {
        it('returns an empty list when there is no array access', () => {
            expect(getArrayIndexSegments('airtable_1.recordId')).toEqual([]);
        });

        it('returns the offsets and value of each index in order of appearance', () => {
            expect(getArrayIndexSegments('airtable_1.rows[3].cells[7].value')).toEqual([
                {arrayIndex: 3, endOffsetExclusive: 18, startOffset: 15},
                {arrayIndex: 7, endOffsetExclusive: 27, startOffset: 24},
            ]);
        });

        it('ignores quoted map keys', () => {
            expect(
                getArrayIndexSegments("airtable_1['some key'].rows[2]").map((segment) => segment.arrayIndex)
            ).toEqual([2]);
        });

        it('ignores the [index] placeholder', () => {
            expect(getArrayIndexSegments('airtable_1.rows[index].id')).toEqual([]);
        });
    });

    describe('setArrayIndex', () => {
        it('replaces the index at the given occurrence', () => {
            expect(setArrayIndex({arrayIndex: 4, occurrence: 0, reference: 'airtable_1.rows[0].id'})).toBe(
                'airtable_1.rows[4].id'
            );
        });

        it('leaves the other occurrences untouched', () => {
            expect(setArrayIndex({arrayIndex: 2, occurrence: 1, reference: 'airtable_1.rows[0].cells[0].value'})).toBe(
                'airtable_1.rows[0].cells[2].value'
            );
        });

        it('returns the reference unchanged when the occurrence does not exist', () => {
            expect(setArrayIndex({arrayIndex: 2, occurrence: 3, reference: 'airtable_1.rows[0].id'})).toBe(
                'airtable_1.rows[0].id'
            );
        });

        it('handles multi digit indexes', () => {
            expect(setArrayIndex({arrayIndex: 101, occurrence: 0, reference: 'airtable_1.rows[12].id'})).toBe(
                'airtable_1.rows[101].id'
            );
        });
    });

    describe('toArrayIndexTemplate', () => {
        it('normalizes every numeric index to the placeholder', () => {
            expect(toArrayIndexTemplate('airtable_1.rows[7].cells[2].value')).toBe(
                'airtable_1.rows[index].cells[index].value'
            );
        });

        it('leaves quoted map keys alone', () => {
            expect(toArrayIndexTemplate("airtable_1['2 items'].rows[1]")).toBe("airtable_1['2 items'].rows[index]");
        });

        it('is a no-op for a reference that is already a template', () => {
            expect(toArrayIndexTemplate('airtable_1.rows[index].id')).toBe('airtable_1.rows[index].id');
        });
    });

    describe('resolveArrayIndexTemplate', () => {
        it('defaults every placeholder to zero', () => {
            expect(resolveArrayIndexTemplate('airtable_1.rows[index].id')).toBe('airtable_1.rows[0].id');
        });

        it('resolves every placeholder, not only the first one', () => {
            expect(resolveArrayIndexTemplate('airtable_1.rows[index].cells[index].value')).toBe(
                'airtable_1.rows[0].cells[0].value'
            );
        });

        it('collapses the dotted placeholder produced by path based references', () => {
            expect(resolveArrayIndexTemplate('ftp_1.[index].name')).toBe('ftp_1[0].name');
        });

        it('resolves to the given index', () => {
            expect(resolveArrayIndexTemplate('airtable_1.rows[index].id', 5)).toBe('airtable_1.rows[5].id');
        });
    });

    describe('splitByArrayIndex', () => {
        it('returns a single literal part when there is no array access', () => {
            expect(splitByArrayIndex('airtable_1.recordId')).toEqual([{text: 'airtable_1.recordId', type: 'literal'}]);
        });

        it('splits literals and array indexes and numbers the occurrences', () => {
            expect(splitByArrayIndex('airtable_1.rows[3].cells[7]')).toEqual([
                {text: 'airtable_1.rows', type: 'literal'},
                {arrayIndex: 3, occurrence: 0, type: 'arrayIndex'},
                {text: '.cells', type: 'literal'},
                {arrayIndex: 7, occurrence: 1, type: 'arrayIndex'},
            ]);
        });

        it('handles a reference that starts with an array index', () => {
            expect(splitByArrayIndex('ftp_1[0].name')).toEqual([
                {text: 'ftp_1', type: 'literal'},
                {arrayIndex: 0, occurrence: 0, type: 'arrayIndex'},
                {text: '.name', type: 'literal'},
            ]);
        });
    });
});
