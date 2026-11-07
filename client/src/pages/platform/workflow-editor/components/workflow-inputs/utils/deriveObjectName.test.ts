import {describe, expect, it} from 'vitest';

import deriveObjectName from './deriveObjectName';

describe('deriveObjectName', () => {
    it('returns the single top-level key of the mapObjectFields JSON', () => {
        expect(deriveObjectName('{"Contacts": {"applicationFields": []}}')).toBe('Contacts');
    });

    it('unwraps a mapObjectFields envelope', () => {
        expect(deriveObjectName('{"mapObjectFields": {"Leads": {}}}')).toBe('Leads');
    });

    it('returns undefined for invalid JSON', () => {
        expect(deriveObjectName('not json')).toBeUndefined();
    });
});
