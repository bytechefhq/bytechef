import {describe, expect, it, vi} from 'vitest';

import {
    buildPropertyItems,
    findNestedProperties,
    getPropertyCompletionItems,
    getPropertyValueItems,
} from './propertyCompletions';

import type {Option, Property} from '@/shared/middleware/platform/configuration';

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

        expect(items.map((item) => item.label)).toEqual(['uri', 'headers', 'queryParameters', 'dynamic']);
        expect(items[0]).toMatchObject({
            detail: 'STRING · required',
            documentation: 'Target address',
            insertAsSnippet: false,
            insertText: 'uri',
            kind: 'property',
        });
        expect(items.map((item) => item.sortText)).toEqual(['0_000', '1_001', '1_002', '1_005']);
        expect(items[3].detail).toBe('OBJECT');
    });
});

describe('getPropertyCompletionItems', () => {
    const conditionedProperties = [
        {defaultValue: 'GET', name: 'method', type: 'STRING'},
        {name: 'format', type: 'STRING'},
        {displayCondition: "contains({'POST','PUT'}, method)", name: 'body', type: 'OBJECT'},
        {displayCondition: "format == 'xml'", name: 'xmlRoot', type: 'STRING'},
        {name: 'uri', type: 'STRING'},
    ] as unknown as Property[];

    const request = {
        existingKeys: [],
        loadDisplayConditions: vi.fn(async () => ({})),
        parameters: {},
        presentKeys: [],
        properties: conditionedProperties,
        propertyPath: [],
    };

    it('hides a property whose condition is false once every value it reads is known', async () => {
        const items = await getPropertyCompletionItems({
            ...request,
            parameters: {format: 'json'},
            presentKeys: ['format'],
        });

        expect(items.map((item) => item.label)).toEqual(['method', 'format', 'uri']);
        expect(request.loadDisplayConditions).toHaveBeenCalledWith({format: 'json', method: 'GET'});
    });

    it('keeps a property whose condition reads a value that is not known', async () => {
        const items = await getPropertyCompletionItems({...request, presentKeys: ['format']});

        expect(items.map((item) => item.label)).toEqual(['method', 'format', 'xmlRoot', 'uri']);
    });

    it('keeps a property whose condition holds', async () => {
        const items = await getPropertyCompletionItems({
            ...request,
            loadDisplayConditions: vi.fn(async () => ({"contains({'POST','PUT'}, method)": true})),
            parameters: {method: 'POST'},
            presentKeys: ['method'],
        });

        expect(items.map((item) => item.label)).toContain('body');
    });

    it('offers every property when the display conditions cannot be evaluated', async () => {
        const items = await getPropertyCompletionItems({
            ...request,
            loadDisplayConditions: vi.fn(async () => {
                throw new Error('failed');
            }),
            parameters: {format: 'json'},
        });

        expect(items).toHaveLength(5);
    });

    it('loads dynamic sub-properties inside the dynamic property object', async () => {
        const loadDynamicProperties = vi.fn(async () => [
            {name: 'Name', type: 'STRING'},
            {name: 'Email', required: true, type: 'STRING'},
        ]) as unknown as (
            propertyName: string,
            lookupDependsOnPaths: string[],
            parameters: Record<string, unknown>
        ) => Promise<Property[]>;

        const items = await getPropertyCompletionItems({
            existingKeys: ['Name'],
            loadDynamicProperties,
            parameters: {baseId: 'app1'},
            presentKeys: ['baseId', 'fields'],
            properties: [
                {name: 'baseId', type: 'STRING'},
                {
                    name: 'fields',
                    propertiesDataSource: {propertiesLookupDependsOn: ['baseId']},
                    type: 'DYNAMIC_PROPERTIES',
                },
            ] as unknown as Property[],
            propertyPath: ['fields'],
        });

        expect(items.map((item) => item.label)).toEqual(['Email']);
        expect(loadDynamicProperties).toHaveBeenCalledWith('fields', ['baseId'], {baseId: 'app1'});
    });

    it('offers nothing inside a dynamic property when its properties cannot be loaded', async () => {
        const items = await getPropertyCompletionItems({
            existingKeys: [],
            loadDynamicProperties: vi.fn(async () => {
                throw new Error('failed');
            }),
            parameters: {},
            presentKeys: [],
            properties: [{name: 'fields', type: 'DYNAMIC_PROPERTIES'}] as unknown as Property[],
            propertyPath: ['fields'],
        });

        expect(items).toEqual([]);
    });
});

describe('getPropertyValueItems', () => {
    const valueProperties = [
        {
            name: 'method',
            options: [
                {description: 'Read', label: 'GET', value: 'GET'},
                {label: 'Create', value: 'POST'},
            ],
            type: 'STRING',
        },
        {name: 'followRedirect', type: 'BOOLEAN'},
        {name: 'baseId', type: 'STRING'},
        {name: 'tableId', optionsDataSource: {optionsLookupDependsOn: ['baseId']}, type: 'STRING'},
        {name: 'headers', properties: [{name: 'mode', options: [{value: 'a'}], type: 'STRING'}], type: 'OBJECT'},
        {name: 'mixed', options: [{value: {nested: true}}, {value: 3}], type: 'STRING'},
    ] as unknown as Property[];

    const request = {
        parameters: {},
        presentKeys: [],
        properties: valueProperties,
        propertyPath: ['method'],
        replaceLength: 2,
        toInsertText: (value: unknown) => `<${String(value)}>`,
    };

    it('offers static options with labels, values and descriptions', async () => {
        const items = await getPropertyValueItems(request);

        expect(items).toEqual([
            {
                documentation: 'Read',
                filterText: 'GET',
                insertAsSnippet: false,
                insertText: '<GET>',
                kind: 'value',
                label: 'GET',
                replaceLength: 2,
                sortText: '000',
            },
            {
                detail: 'POST',
                documentation: undefined,
                filterText: 'POST Create',
                insertAsSnippet: false,
                insertText: '<POST>',
                kind: 'value',
                label: 'Create',
                replaceLength: 2,
                sortText: '001',
            },
        ]);
    });

    it('offers true and false for a boolean property', async () => {
        const items = await getPropertyValueItems({...request, propertyPath: ['followRedirect']});

        expect(items.map((item) => item.insertText)).toEqual(['<true>', '<false>']);
    });

    it('offers options of a nested property', async () => {
        const items = await getPropertyValueItems({...request, propertyPath: ['headers', 'mode']});

        expect(items.map((item) => item.label)).toEqual(['a']);
    });

    it('skips options that are not plain values or that cannot be written at the cursor', async () => {
        const items = await getPropertyValueItems({
            ...request,
            propertyPath: ['mixed'],
            toInsertText: (value: unknown) => (typeof value === 'number' ? undefined : String(value)),
        });

        expect(items).toEqual([]);
    });

    it('loads options from the connection with the other written values', async () => {
        const loadOptions = vi.fn(async () => [{label: 'Customers', value: 'tbl1'}] as Option[]);

        const items = await getPropertyValueItems({
            ...request,
            loadOptions,
            parameters: {baseId: 'app1', tableId: 'tb'},
            presentKeys: ['baseId', 'tableId'],
            propertyPath: ['tableId'],
        });

        expect(items.map((item) => [item.label, item.insertText])).toEqual([['Customers', '<tbl1>']]);
        expect(loadOptions).toHaveBeenCalledWith('tableId', ['baseId'], {baseId: 'app1'});
    });

    it('offers nothing when the options cannot be loaded', async () => {
        const items = await getPropertyValueItems({
            ...request,
            loadOptions: vi.fn(async () => {
                throw new Error('failed');
            }),
            propertyPath: ['tableId'],
        });

        expect(items).toEqual([]);
    });

    it('offers static options of a dynamic property', async () => {
        const items = await getPropertyValueItems({
            ...request,
            loadDynamicProperties: vi.fn(async () => [
                {name: 'Status', options: [{value: 'Done'}], type: 'STRING'},
            ]) as unknown as (
                propertyName: string,
                lookupDependsOnPaths: string[],
                parameters: Record<string, unknown>
            ) => Promise<Property[]>,
            properties: [{name: 'fields', type: 'DYNAMIC_PROPERTIES'}] as unknown as Property[],
            propertyPath: ['fields', 'Status'],
        });

        expect(items.map((item) => item.label)).toEqual(['Done']);
    });
});
