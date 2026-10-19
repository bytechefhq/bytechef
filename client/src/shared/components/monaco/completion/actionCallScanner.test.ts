import {describe, expect, it} from 'vitest';

import {findActionCallKeySlot, findActionCallValueSlot} from './actionCallScanner';

const at = (source: string, language: 'javascript' | 'python' | 'ruby' = 'javascript') =>
    findActionCallKeySlot(source, source.length, language);

describe('findActionCallKeySlot', () => {
    it('finds a bare key being typed in the first argument', () => {
        expect(at('function perform(input, connection, context) {\n  context.component.httpClient.get({ ur')).toEqual({
            calleeText: 'context.component.httpClient.get',
            existingKeys: [],
            prefix: 'ur',
            propertyPath: [],
            rootKeys: [],
            rootValues: {},
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
            rootKeys: ['latitude'],
            rootValues: {latitude: 1},
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

    it('collects literal values of the first argument, including from a nested key slot', () => {
        expect(
            at('context.component.a.b({method: "POST", count: -2, flag: true, empty: null, body: {ke')
        ).toMatchObject({
            propertyPath: ['body'],
            rootKeys: ['method', 'count', 'flag', 'empty', 'body'],
            rootValues: {count: -2, empty: null, flag: true, method: 'POST'},
        });
    });

    it('leaves out values that are not plain literals', () => {
        expect(at('context.component.a.b({uri: input.uri, path: `/${id}`, size: 1 + 2, name: "x" + y, ')).toMatchObject(
            {
                rootKeys: ['uri', 'path', 'size', 'name'],
                rootValues: {},
            }
        );
    });

    it('reads Python and Ruby literal keywords', () => {
        expect(at("context.component.a.b({'flag': True, 'none': None, ", 'python')).toMatchObject({
            rootValues: {flag: true, none: null},
        });
        expect(at('context.component.a.b({flag: false, label: "a#{b}", ', 'ruby')).toMatchObject({
            rootKeys: ['flag', 'label'],
            rootValues: {flag: false},
        });
    });
});

describe('findActionCallValueSlot', () => {
    const valueAt = (source: string, language: 'javascript' | 'python' | 'ruby' = 'javascript') =>
        findActionCallValueSlot(source, source.length, language);

    it('finds an unquoted value being typed after a key', () => {
        expect(valueAt('context.component.a.b({uri: "x", method: PO')).toEqual({
            calleeText: 'context.component.a.b',
            prefix: 'PO',
            propertyPath: ['method'],
            rootKeys: ['uri', 'method'],
            rootValues: {uri: 'x'},
        });
    });

    it('finds a quoted value with its quote and raw prefix', () => {
        expect(valueAt("context.component.a.b({method: 'app/js")).toMatchObject({
            prefix: 'app/js',
            propertyPath: ['method'],
            quote: "'",
        });
    });

    it('walks into nested objects and reads Python and Ruby keys', () => {
        expect(valueAt('context.component.a.b({headers: {mode: ')).toMatchObject({
            prefix: '',
            propertyPath: ['headers', 'mode'],
        });
        expect(valueAt('context.component.a.b({"method": "G', 'python')).toMatchObject({
            prefix: 'G',
            propertyPath: ['method'],
            quote: '"',
        });
        expect(valueAt('context.component.a.b({"method" => :', 'ruby')).toBeUndefined();
        expect(valueAt('context.component.a.b({"method" => "G', 'ruby')).toMatchObject({propertyPath: ['method']});
    });

    it('returns undefined in a key position or after a value expression', () => {
        expect(valueAt('context.component.a.b({meth')).toBeUndefined();
        expect(valueAt('context.component.a.b({method: input.verb + ')).toBeUndefined();
        expect(valueAt('context.component.a.b({method: f(')).toBeUndefined();
    });
});

describe('findActionCallKeySlot nested values', () => {
    it('keeps closed nested objects and the open objects around the cursor', () => {
        const slot = findActionCallKeySlot(
            'context.component.a.b({body: {mode: "raw", size: 2}, items: [{type: "a"}, {type: "list", ',
            'context.component.a.b({body: {mode: "raw", size: 2}, items: [{type: "a"}, {type: "list", '.length,
            'javascript'
        );

        expect(slot).toMatchObject({
            propertyPath: ['items', '1'],
            rootValues: {body: {mode: 'raw', size: 2}, items: [{type: 'a'}, {type: 'list'}]},
        });
    });
});
