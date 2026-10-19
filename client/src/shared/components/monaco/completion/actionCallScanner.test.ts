import {describe, expect, it} from 'vitest';

import {findActionCallKeySlot} from './actionCallScanner';

const at = (source: string, language: 'javascript' | 'python' | 'ruby' = 'javascript') =>
    findActionCallKeySlot(source, source.length, language);

describe('findActionCallKeySlot', () => {
    it('finds a bare key being typed in the first argument', () => {
        expect(at('function perform(input, connection, context) {\n  context.component.httpClient.get({ ur')).toEqual({
            calleeText: 'context.component.httpClient.get',
            existingKeys: [],
            prefix: 'ur',
            propertyPath: [],
        });
    });

    it('collects the keys already written and ignores values', () => {
        expect(at('context.component.a.b({uri: "x:y", body: f(1, 2), ')).toMatchObject({
            existingKeys: ['uri', 'body'],
            prefix: '',
        });
    });

    it('walks into a nested object and an array item', () => {
        expect(at('context.component.a.b({headers: {acc')).toMatchObject({prefix: 'acc', propertyPath: ['headers']});
        expect(at('context.component.a.b({items: [{ke')).toMatchObject({prefix: 'ke', propertyPath: ['items', '0']});
    });

    it('finds a Python key inside quotes', () => {
        expect(at('context.component.googleMaps.get_address({"latitude": 1, "lon', 'python')).toEqual({
            calleeText: 'context.component.googleMaps.get_address',
            existingKeys: ['latitude'],
            prefix: 'lon',
            propertyPath: [],
        });
    });

    it('reads Ruby hash rockets and symbol keys', () => {
        expect(at('context.component.a.b({"uri" => "x", body: 1, "he', 'ruby')).toMatchObject({
            existingKeys: ['uri', 'body'],
            prefix: 'he',
        });
    });

    it('ignores a ternary colon inside a value', () => {
        expect(at('context.component.a.b({uri: flag ? 1 : 2, ')).toMatchObject({existingKeys: ['uri']});
    });

    it('returns undefined in a value position, outside a call, or in a later argument', () => {
        expect(at('context.component.a.b({uri: ')).toBeUndefined();
        expect(at('const value = {ur')).toBeUndefined();
        expect(at('context.component.a.b({}, {ur')).toBeUndefined();
        expect(at('context.component.a.b({uri: "ht')).toBeUndefined();
    });

    it('skips braces inside strings and comments', () => {
        expect(at('context.component.a.b({\n  // } not a close\n  text: "}", ')).toMatchObject({
            existingKeys: ['text'],
        });
    });
});
