import {
    buildValidDataPillReferenceSet,
    isDataPillReferenceValid,
} from '@/pages/platform/workflow-editor/utils/dataPillReferenceValidation';
import {DataPillType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

const pill = (value: string): DataPillType => ({
    id: value,
    value,
});

describe('datapillReferenceValidation', () => {
    describe('buildValidDatapillReferenceSet', () => {
        it('includes each datapill value', () => {
            const set = buildValidDataPillReferenceSet([pill('gmail'), pill('gmail.subject')]);

            expect(set.has('gmail')).toBe(true);
            expect(set.has('gmail.subject')).toBe(true);
        });

        it('adds [0] variant when value contains [index]', () => {
            const set = buildValidDataPillReferenceSet([pill('airtable.rows[index].id')]);

            expect(set.has('airtable.rows[index].id')).toBe(true);
            expect(set.has('airtable.rows[0].id')).toBe(true);
        });
    });

    describe('isDatapillReferenceValid', () => {
        it('treats empty reference as valid', () => {
            const set = buildValidDataPillReferenceSet([pill('accelo')]);

            expect(isDataPillReferenceValid('', set)).toBe(true);
            expect(isDataPillReferenceValid(undefined, set)).toBe(true);
        });

        it('treats any reference as valid when the catalog is empty', () => {
            const emptySet = new Set<string>();

            expect(isDataPillReferenceValid('missing.node', emptySet)).toBe(true);
        });

        it('returns true when reference matches catalog exactly', () => {
            const set = buildValidDataPillReferenceSet([pill('accelo'), pill('accelo.recordId')]);

            expect(isDataPillReferenceValid('accelo', set)).toBe(true);
            expect(isDataPillReferenceValid('accelo.recordId', set)).toBe(true);
        });

        it('returns false when node was removed from catalog', () => {
            const set = buildValidDataPillReferenceSet([pill('gmail'), pill('airtable.id')]);

            expect(isDataPillReferenceValid('accelo.field', set)).toBe(false);
        });

        it('accepts [0] mention id when catalog uses [index]', () => {
            const set = buildValidDataPillReferenceSet([pill('airtable.rows[index].id')]);

            expect(isDataPillReferenceValid('airtable.rows[0].id', set)).toBe(true);
        });

        it('accepts [index] mention id when catalog includes [0] variant in set', () => {
            const set = buildValidDataPillReferenceSet([pill('airtable.rows[index].id')]);

            expect(isDataPillReferenceValid('airtable.rows[index].id', set)).toBe(true);
        });
    });
});
