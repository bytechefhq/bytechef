import {describe, expect, it} from 'vitest';

import {toFlatKey} from './names';

describe('toFlatKey', () => {
    it.each([
        ['approve_refund', 'APPROVE_REFUND'],
        ['approve-refund', 'APPROVE_REFUND'],
        ['a--b', 'A_B'],
        ['crm', 'CRM'],
        ['-x-', '_X_'],
    ])('%s → %s', (key, expected) => {
        expect(toFlatKey(key)).toBe(expected);
    });
});
