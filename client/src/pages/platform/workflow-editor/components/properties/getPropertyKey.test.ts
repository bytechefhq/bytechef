import {describe, expect, it} from 'vitest';

import getPropertyKey from './getPropertyKey';

describe('getPropertyKey', () => {
    it('uses the name on its own', () => {
        expect(getPropertyKey('uri')).toBe('uri');
    });

    // HTTP Client declares seven sibling properties all named bodyContent, one per body content type, told
    // apart only by their display condition. Keying them by name alone collided, and React drops or duplicates
    // children whose keys collide.
    it('tells same-named siblings apart by their display condition', () => {
        const jsonKey = getPropertyKey('bodyContent', "body.bodyContentType == 'JSON'");
        const xmlKey = getPropertyKey('bodyContent', "body.bodyContentType == 'XML'");

        expect(jsonKey).not.toBe(xmlKey);
    });

    it('returns an empty key for an unnamed property', () => {
        expect(getPropertyKey(undefined)).toBe('');
        expect(getPropertyKey(undefined, "body.bodyContentType == 'JSON'")).toBe('');
    });
});
