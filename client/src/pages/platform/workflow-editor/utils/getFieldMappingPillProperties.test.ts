import {describe, expect, it} from 'vitest';

import getFieldMappingPillProperties from './getFieldMappingPillProperties';

describe('getFieldMappingPillProperties', () => {
    it('builds one child property per applicationFields entry', () => {
        const sample = JSON.stringify({
            Contacts: {
                applicationFields: {
                    fields: [
                        {label: 'Title', value: 'title'},
                        {label: 'Email', value: 'email'},
                    ],
                },
            },
        });

        expect(getFieldMappingPillProperties(sample)).toEqual([
            {label: 'Title', name: 'title', type: 'STRING'},
            {label: 'Email', name: 'email', type: 'STRING'},
        ]);
    });

    it('returns an empty array for invalid or empty samples', () => {
        expect(getFieldMappingPillProperties(undefined)).toEqual([]);
        expect(getFieldMappingPillProperties('nope')).toEqual([]);
    });
});
