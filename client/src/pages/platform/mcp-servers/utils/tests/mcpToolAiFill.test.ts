import {PropertyAllType} from '@/shared/types';
import {describe, expect, it, vi} from 'vitest';

import {
    collectHiddenToolPaths,
    collectPropertyDefaults,
    computeAiFillValues,
    hasAiFillCandidates,
    isEmptyToolValue,
    isFromAiEligibleProperty,
} from '../mcpToolAiFill';

const loadOptionsNone = vi.fn().mockResolvedValue(null);

function stringProperty(overrides: Record<string, unknown> = {}): PropertyAllType {
    return {controlType: 'TEXT', name: 'subject', type: 'STRING', ...overrides} as unknown as PropertyAllType;
}

function compute(
    properties: PropertyAllType[],
    values: Record<string, unknown> = {},
    overrides: Record<string, unknown> = {}
) {
    return computeAiFillValues({
        includeAdvanced: true,
        includeConditional: true,
        loadOptions: loadOptionsNone,
        properties,
        values,
        ...overrides,
    });
}

describe('isEmptyToolValue', () => {
    it('treats undefined, null and empty string as empty', () => {
        expect(isEmptyToolValue(undefined)).toBe(true);
        expect(isEmptyToolValue(null)).toBe(true);
        expect(isEmptyToolValue('')).toBe(true);
        expect(isEmptyToolValue(0)).toBe(false);
        expect(isEmptyToolValue(false)).toBe(false);
    });
});

describe('isFromAiEligibleProperty', () => {
    it('rejects containers and expression-disabled properties', () => {
        expect(isFromAiEligibleProperty({name: 'a', type: 'OBJECT'} as unknown as PropertyAllType)).toBe(false);
        expect(isFromAiEligibleProperty({name: 'a', type: 'ARRAY'} as unknown as PropertyAllType)).toBe(false);
        expect(isFromAiEligibleProperty(stringProperty({expressionEnabled: false}))).toBe(false);
    });

    it('accepts select and multi select properties', () => {
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'SELECT'}))).toBe(true);
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'MULTI_SELECT'}))).toBe(true);
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'SELECT', optionsDataSource: {}}))).toBe(true);
    });

    it('accepts text area only for string properties', () => {
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'TEXT_AREA'}))).toBe(true);
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'TEXT_AREA', type: 'INTEGER'}))).toBe(false);
    });

    it('accepts integer and number input types', () => {
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'INTEGER', type: 'INTEGER'}))).toBe(true);
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'NUMBER', type: 'NUMBER'}))).toBe(true);
    });

    it('rejects unsupported and missing control types, and accepts options data source on a text input', () => {
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'CODE_EDITOR', type: 'INTEGER'}))).toBe(false);
        expect(isFromAiEligibleProperty(stringProperty({controlType: 'ARRAY_BUILDER', type: 'ARRAY'}))).toBe(false);
        expect(isFromAiEligibleProperty(stringProperty({controlType: undefined}))).toBe(false);
        expect(isFromAiEligibleProperty(stringProperty({controlType: undefined, optionsDataSource: {}}))).toBe(false);
        expect(isFromAiEligibleProperty(stringProperty({optionsDataSource: {}}))).toBe(true);
    });
});

describe('computeAiFillValues', () => {
    it('fills empty eligible string field', async () => {
        const result = await compute([stringProperty({description: 'The subject', required: true})]);

        expect(result).toEqual({
            subject: "=fromAi('subject', 'STRING', {'description': 'The subject', 'required': true})",
        });
    });

    it('skips field holding its default value', async () => {
        const result = await compute([stringProperty({defaultValue: 'Hello'})], {subject: 'Hello'});

        expect(result).toEqual({});
    });

    it('skips field the user typed into', async () => {
        const result = await compute([stringProperty()], {subject: 'typed'});

        expect(result).toEqual({});
    });

    it('skips object and array containers', async () => {
        const result = await compute([
            {name: 'config', type: 'OBJECT'} as unknown as PropertyAllType,
            {name: 'items', type: 'ARRAY'} as unknown as PropertyAllType,
        ]);

        expect(result).toEqual({});
    });

    it('skips tool name and description', async () => {
        const result = await compute([stringProperty({name: 'toolName'}), stringProperty({name: 'toolDescription'})]);

        expect(result).toEqual({});
    });

    it('skips hidden and advanced fields when not included', async () => {
        const result = await compute(
            [stringProperty({hidden: true, name: 'secret'}), stringProperty({advancedOption: true, name: 'extra'})],
            {},
            {includeAdvanced: false}
        );

        expect(result).toEqual({});
    });

    it('skips conditional fields when includeConditional is false', async () => {
        const properties = [stringProperty({displayCondition: 'a == 1'})];

        expect(await compute(properties, {}, {includeConditional: false})).toEqual({});
        expect(Object.keys(await compute(properties))).toEqual(['subject']);
    });

    it('includes static options', async () => {
        const result = await compute([stringProperty({controlType: 'SELECT', options: [{value: 'a'}, {value: 'b'}]})]);

        expect(result.subject).toContain("'options': {'a', 'b'}");
    });

    it('fills array multi select field with its options', async () => {
        const result = await compute([
            {
                controlType: 'MULTI_SELECT',
                name: 'labels',
                options: [{value: 'a'}, {value: 'b'}],
                type: 'ARRAY',
            } as unknown as PropertyAllType,
        ]);

        expect(result.labels).toContain("fromAi('labels', 'ARRAY'");
        expect(result.labels).toContain("'options': {'a', 'b'}");
    });

    it('includes dynamically loaded options', async () => {
        const loadOptions = vi.fn().mockResolvedValue([{value: 'x'}, {value: 'y'}]);

        const result = await compute(
            [stringProperty({controlType: 'SELECT', optionsDataSource: {}})],
            {},
            {loadOptions}
        );

        expect(loadOptions).toHaveBeenCalledWith(expect.objectContaining({name: 'subject'}), 'subject', {});
        expect(result.subject).toContain("'options': {'x', 'y'}");
    });

    it('leaves dynamic options field empty when options cannot be loaded', async () => {
        const result = await compute([stringProperty({controlType: 'SELECT', optionsDataSource: {}})]);

        expect(result).toEqual({});
    });

    it('fills nested eligible field with dotted path', async () => {
        const result = await compute([
            {
                name: 'config',
                properties: [stringProperty({name: 'title'})],
                type: 'OBJECT',
            } as unknown as PropertyAllType,
        ]);

        expect(Object.keys(result)).toEqual(['config.title']);
        expect(result['config.title']).toContain("fromAi('config_title'");
    });

    it('gives same named nested fields distinct argument names', async () => {
        const result = await compute([
            {
                name: 'shipping',
                properties: [stringProperty({name: 'city'})],
                type: 'OBJECT',
            } as unknown as PropertyAllType,
            {
                name: 'billing',
                properties: [stringProperty({name: 'city'})],
                type: 'OBJECT',
            } as unknown as PropertyAllType,
        ]);

        expect(result['shipping.city']).toContain("fromAi('shipping_city'");
        expect(result['billing.city']).toContain("fromAi('billing_city'");
    });
});

describe('hasAiFillCandidates', () => {
    const candidateParams = {includeAdvanced: false, includeConditional: true};

    it('counts an empty dynamic options field as fillable without loading its options', () => {
        expect(
            hasAiFillCandidates({
                ...candidateParams,
                properties: [stringProperty({controlType: 'SELECT', optionsDataSource: {}})],
                values: {},
            })
        ).toBe(true);
    });

    it('skips a dynamic options field when options cannot be loaded', () => {
        expect(
            hasAiFillCandidates({
                ...candidateParams,
                optionsLoadable: false,
                properties: [stringProperty({controlType: 'SELECT', optionsDataSource: {}})],
                values: {},
            })
        ).toBe(false);
    });

    it('is false when every eligible field is filled', () => {
        expect(
            hasAiFillCandidates({...candidateParams, properties: [stringProperty()], values: {subject: 'typed'}})
        ).toBe(false);
    });

    it('ignores fields under hidden paths', () => {
        expect(
            hasAiFillCandidates({
                ...candidateParams,
                hiddenPaths: new Set(['subject']),
                properties: [stringProperty()],
                values: {},
            })
        ).toBe(false);
    });
});

describe('collectHiddenToolPaths', () => {
    it('collects top level and nested paths whose display condition is false', () => {
        const hiddenPaths = collectHiddenToolPaths(
            [
                stringProperty({displayCondition: 'shown', name: 'visible'}),
                stringProperty({displayCondition: 'hidden', name: 'invisible'}),
                {
                    name: 'body',
                    properties: [stringProperty({displayCondition: 'hidden', name: 'content'})],
                    type: 'OBJECT',
                } as unknown as PropertyAllType,
            ],
            {hidden: false, shown: true}
        );

        expect(Array.from(hiddenPaths).sort()).toEqual(['body.content', 'invisible']);
    });

    it('hides nothing without evaluated display conditions', () => {
        expect(collectHiddenToolPaths([stringProperty({displayCondition: 'hidden'})], undefined).size).toBe(0);
    });
});

describe('collectPropertyDefaults', () => {
    it('collects the built-in defaults of object sub-properties as a nested map', () => {
        const properties = [
            stringProperty({defaultValue: 'Hello', name: 'subject'}),
            stringProperty({name: 'empty'}),
            {
                name: 'body',
                properties: [stringProperty({defaultValue: 'JSON', name: 'format'}), stringProperty({name: 'content'})],
                type: 'OBJECT',
            } as unknown as PropertyAllType,
            {
                name: 'options',
                properties: [stringProperty({name: 'note'})],
                type: 'OBJECT',
            } as unknown as PropertyAllType,
        ];

        expect(collectPropertyDefaults(properties)).toEqual({body: {format: 'JSON'}, subject: 'Hello'});
    });
});
