import {describe, expect, it, vi} from 'vitest';

import {JSON_SCHEMA_META_SCHEMA, configureJsonSchemaCompletion, jsonSchemaModelPath} from './jsonSchemaCompletion';

describe('jsonSchemaModelPath', () => {
    it('places each editor model in the JSON Schema directory under a file name built from the React id', () => {
        expect(jsonSchemaModelPath(':r3:')).toBe('file:///bytechef/json-schema/r3.json');
        expect(jsonSchemaModelPath('«r1f»')).toBe('file:///bytechef/json-schema/r1f.json');
    });
});

describe('configureJsonSchemaCompletion', () => {
    const jsonDefaultsWith = (schemas?: {fileMatch?: string[]; uri: string}[]) => ({
        diagnosticsOptions: {allowComments: false, schemas, validate: true},
        setDiagnosticsOptions: vi.fn(),
    });

    it('binds the JSON Schema meta-schema to the JSON Schema editor models and keeps the other options', () => {
        const jsonDefaults = jsonDefaultsWith([{fileMatch: ['other.json'], uri: 'https://example.com/other.json'}]);

        configureJsonSchemaCompletion({json: {jsonDefaults}} as never);

        expect(jsonDefaults.setDiagnosticsOptions).toHaveBeenCalledWith({
            allowComments: false,
            schemas: [
                {fileMatch: ['other.json'], uri: 'https://example.com/other.json'},
                {
                    fileMatch: ['bytechef/json-schema/*.json'],
                    schema: JSON_SCHEMA_META_SCHEMA,
                    uri: 'https://bytechef.io/schemas/json-schema-draft-07.json',
                },
            ],
            validate: true,
        });
    });

    it('replaces its own association instead of adding a second one', () => {
        const jsonDefaults = jsonDefaultsWith([{uri: 'https://bytechef.io/schemas/json-schema-draft-07.json'}]);

        configureJsonSchemaCompletion({json: {jsonDefaults}} as never);

        expect(jsonDefaults.setDiagnosticsOptions.mock.calls[0][0].schemas).toHaveLength(1);
    });
});

describe('JSON_SCHEMA_META_SCHEMA', () => {
    it('offers the seven JSON types as the values of type', () => {
        expect(JSON_SCHEMA_META_SCHEMA.properties.type.anyOf[0]).toEqual({
            enum: ['array', 'boolean', 'integer', 'null', 'number', 'object', 'string'],
        });
    });
});
