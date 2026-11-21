type MonacoType = typeof import('monaco-editor');

export const JSON_SCHEMA_MODEL_DIRECTORY = 'bytechef/json-schema';

const JSON_SCHEMA_META_SCHEMA_URI = 'https://bytechef.io/schemas/json-schema-draft-07.json';

const SIMPLE_TYPES = ['array', 'boolean', 'integer', 'null', 'number', 'object', 'string'];

const STRING_FORMATS = [
    'date',
    'date-time',
    'duration',
    'email',
    'hostname',
    'idn-email',
    'idn-hostname',
    'ipv4',
    'ipv6',
    'iri',
    'iri-reference',
    'json-pointer',
    'regex',
    'relative-json-pointer',
    'time',
    'uri',
    'uri-reference',
    'uri-template',
    'uuid',
];

const NON_NEGATIVE_INTEGER = {minimum: 0, type: 'integer'};

const SCHEMA_ARRAY = {items: {$ref: '#'}, minItems: 1, type: 'array'};

export const JSON_SCHEMA_META_SCHEMA = {
    $id: JSON_SCHEMA_META_SCHEMA_URI,
    default: true,
    properties: {
        $comment: {description: 'A note for schema authors. It has no effect on validation.', type: 'string'},
        $id: {description: 'The URI that identifies this schema.', format: 'uri-reference', type: 'string'},
        $ref: {description: 'A reference to another schema, for example #/definitions/address.', type: 'string'},
        $schema: {description: 'The JSON Schema dialect of this document.', format: 'uri', type: 'string'},
        additionalItems: {$ref: '#', description: 'The schema of array items past those listed in items.'},
        additionalProperties: {
            $ref: '#',
            description: 'The schema of properties not listed in properties, or false to forbid them.',
        },
        allOf: {...SCHEMA_ARRAY, description: 'The value must be valid against every schema in the list.'},
        anyOf: {...SCHEMA_ARRAY, description: 'The value must be valid against at least one schema in the list.'},
        const: {description: 'The value must equal this value.'},
        contains: {$ref: '#', description: 'At least one array item must be valid against this schema.'},
        contentEncoding: {description: 'The encoding of string content, for example base64.', type: 'string'},
        contentMediaType: {description: 'The media type of string content, for example image/png.', type: 'string'},
        default: {description: 'The value to use when none is given.'},
        definitions: {
            additionalProperties: {$ref: '#'},
            default: {},
            description: 'Reusable schemas, referenced with #/definitions/<name>.',
            type: 'object',
        },
        dependencies: {
            additionalProperties: {anyOf: [{$ref: '#'}, {items: {type: 'string'}, type: 'array'}]},
            description: 'Schemas or required properties that apply when a property is present.',
            type: 'object',
        },
        description: {description: 'An explanation of what the value means.', type: 'string'},
        else: {$ref: '#', description: 'The schema to apply when the if schema does not match.'},
        enum: {description: 'The value must be one of these values.', items: true, type: 'array'},
        examples: {description: 'Example values.', items: true, type: 'array'},
        exclusiveMaximum: {description: 'The number must be less than this value.', type: 'number'},
        exclusiveMinimum: {description: 'The number must be greater than this value.', type: 'number'},
        format: {
            anyOf: [{enum: STRING_FORMATS}, {type: 'string'}],
            description: 'The semantic format of a string value.',
        },
        if: {$ref: '#', description: 'When the value matches this schema, then applies, else else.'},
        items: {
            anyOf: [{$ref: '#'}, SCHEMA_ARRAY],
            default: true,
            description: 'The schema of every array item, or a list of schemas by position.',
        },
        maxItems: {...NON_NEGATIVE_INTEGER, description: 'The largest number of array items.'},
        maxLength: {...NON_NEGATIVE_INTEGER, description: 'The longest string length.'},
        maxProperties: {...NON_NEGATIVE_INTEGER, description: 'The largest number of object properties.'},
        maximum: {description: 'The largest allowed number.', type: 'number'},
        minItems: {...NON_NEGATIVE_INTEGER, description: 'The smallest number of array items.'},
        minLength: {...NON_NEGATIVE_INTEGER, description: 'The shortest string length.'},
        minProperties: {...NON_NEGATIVE_INTEGER, description: 'The smallest number of object properties.'},
        minimum: {description: 'The smallest allowed number.', type: 'number'},
        multipleOf: {description: 'The number must be a multiple of this value.', exclusiveMinimum: 0, type: 'number'},
        not: {$ref: '#', description: 'The value must not be valid against this schema.'},
        oneOf: {...SCHEMA_ARRAY, description: 'The value must be valid against exactly one schema in the list.'},
        pattern: {description: 'A regular expression a string value must match.', format: 'regex', type: 'string'},
        patternProperties: {
            additionalProperties: {$ref: '#'},
            default: {},
            description: 'Schemas of the properties whose names match a regular expression.',
            propertyNames: {format: 'regex'},
            type: 'object',
        },
        properties: {
            additionalProperties: {$ref: '#'},
            default: {},
            description: 'The schema of each named object property.',
            type: 'object',
        },
        propertyNames: {$ref: '#', description: 'The schema every property name must match.'},
        readOnly: {
            default: false,
            description: 'The value is managed by the owner and should not be sent.',
            type: 'boolean',
        },
        required: {
            description: 'The names of the properties an object must have.',
            items: {type: 'string'},
            type: 'array',
            uniqueItems: true,
        },
        then: {$ref: '#', description: 'The schema to apply when the if schema matches.'},
        title: {description: 'A short name for the value.', type: 'string'},
        type: {
            anyOf: [{enum: SIMPLE_TYPES}, {items: {enum: SIMPLE_TYPES}, minItems: 1, type: 'array', uniqueItems: true}],
            description: 'The JSON type, or list of types, of the value.',
        },
        uniqueItems: {default: false, description: 'Array items must all be different.', type: 'boolean'},
        writeOnly: {default: false, description: 'The value is sent but never returned.', type: 'boolean'},
    },
    type: ['object', 'boolean'],
};

export function jsonSchemaModelPath(editorId: string): string {
    return `file:///${JSON_SCHEMA_MODEL_DIRECTORY}/${editorId.replace(/[^A-Za-z0-9_-]/g, '')}.json`;
}

export function configureJsonSchemaCompletion(monaco: MonacoType): void {
    const {jsonDefaults} = monaco.json;
    const diagnosticsOptions = jsonDefaults.diagnosticsOptions;
    const otherSchemas = (diagnosticsOptions.schemas ?? []).filter(
        (schema) => schema.uri !== JSON_SCHEMA_META_SCHEMA_URI
    );

    jsonDefaults.setDiagnosticsOptions({
        ...diagnosticsOptions,
        schemas: [
            ...otherSchemas,
            {
                fileMatch: [`${JSON_SCHEMA_MODEL_DIRECTORY}/*.json`],
                schema: JSON_SCHEMA_META_SCHEMA,
                uri: JSON_SCHEMA_META_SCHEMA_URI,
            },
        ],
    });
}
