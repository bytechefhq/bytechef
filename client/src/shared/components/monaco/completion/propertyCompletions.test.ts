import {describe, expect, it} from 'vitest';

import {buildPropertyItems, findNestedProperties} from './propertyCompletions';

import type {Property} from '@/shared/middleware/platform/configuration';

const properties = [
    {description: 'Target address', name: 'uri', required: true, type: 'STRING'},
    {
        name: 'headers',
        properties: [{name: 'accept', type: 'STRING'}],
        type: 'OBJECT',
    },
    {
        items: [{properties: [{name: 'key', type: 'STRING'}], type: 'OBJECT'}],
        name: 'queryParameters',
        type: 'ARRAY',
    },
    {name: 'timeout', type: 'INTEGER'},
    {hidden: true, name: 'internal', type: 'STRING'},
    {name: 'dynamic', type: 'DYNAMIC_PROPERTIES'},
] as unknown as Property[];

describe('findNestedProperties', () => {
    it('returns the top-level list for an empty path', () => {
        expect(findNestedProperties(properties, [])).toBe(properties);
    });

    it('walks into an object property', () => {
        expect(findNestedProperties(properties, ['headers'])?.map((property) => property.name)).toEqual(['accept']);
    });

    it('walks into an array item object', () => {
        expect(findNestedProperties(properties, ['queryParameters', '0'])?.map((property) => property.name)).toEqual([
            'key',
        ]);
    });

    it('returns undefined for an unknown or scalar property', () => {
        expect(findNestedProperties(properties, ['nope'])).toBeUndefined();
        expect(findNestedProperties(properties, ['uri'])).toBeUndefined();
    });
});

describe('buildPropertyItems', () => {
    it('offers visible properties not yet present, required first, in definition order', () => {
        const items = buildPropertyItems(properties, ['timeout']);

        expect(items.map((item) => item.label)).toEqual(['uri', 'headers', 'queryParameters']);
        expect(items[0]).toMatchObject({
            detail: 'STRING · required',
            documentation: 'Target address',
            insertAsSnippet: false,
            insertText: 'uri',
            kind: 'property',
        });
        expect(items.map((item) => item.sortText)).toEqual(['0_000', '1_001', '1_002']);
    });
});
