import {describe, expect, it} from 'vitest';

import {decodePath, encodeParameters, encodePath, safeResolvePath} from './encodingUtils';

describe('safeResolvePath', () => {
    it('resolves a value at a valid path', () => {
        expect(safeResolvePath({input: {name: 'value'}}, 'input.name')).toBe('value');
    });

    it('returns undefined for a valid path that does not exist', () => {
        expect(safeResolvePath({input: {}}, 'input.missing')).toBeUndefined();
    });

    it('returns undefined instead of throwing for an invalid object path', () => {
        // A user typing n8n-style "${httpClient_1}" as an OBJECT property key produces a path
        // segment containing "{"/"}", which object-resolve-path rejects as invalid and throws on.
        // The lookup must degrade to undefined so a bad expression never bricks the node.
        expect(() => safeResolvePath({input: {}}, 'input.${httpClient_1}')).not.toThrow();
        expect(safeResolvePath({input: {}}, 'input.${httpClient_1}')).toBeUndefined();
    });

    it('returns undefined instead of throwing when the object is undefined', () => {
        expect(() => safeResolvePath(undefined, 'input.name')).not.toThrow();
        expect(safeResolvePath(undefined, 'input.name')).toBeUndefined();
    });
});

describe('encodeParameters', () => {
    it('encodes each non-ASCII character of a key with its own character code', () => {
        const encodedParameters = encodeParameters({Broj_etaža: 'a', Hlađenje: 'b', Sažetak: 'c'});

        expect(Object.keys(encodedParameters)).toEqual([
            encodePath('Broj_etaža'),
            encodePath('Hlađenje'),
            encodePath('Sažetak'),
        ]);
    });

    it('round-trips non-ASCII keys through decodePath', () => {
        const encodedParameters = encodeParameters({Broj_etaža: 'a', Željeni_rok: 'b'});

        expect(Object.keys(encodedParameters).map(decodePath)).toEqual(['Broj_etaža', 'Željeni_rok']);
    });

    it('resolves a nested non-ASCII key with the path the editor builds for it', () => {
        const encodedParameters = encodeParameters({row: {values: {Sažetak: '${aiText_1.sazetak}'}}});

        expect(safeResolvePath(encodedParameters, encodePath('row.values.Sažetak'))).toBe('${aiText_1.sazetak}');
    });
});
