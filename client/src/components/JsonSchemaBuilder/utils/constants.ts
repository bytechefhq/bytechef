import {
    ArraySchemaFieldOptionType,
    ArrayValidSchemaFieldType,
    BoolSchemaFieldOptionType,
    BoolValidSchemaFieldType,
    CommonSchemaFieldOptionType,
    CommonValidSchemaFieldType,
    IntegerSchemaFieldOptionType,
    IntegerValidSchemaFieldType,
    NumberSchemaFieldOptionType,
    NumberValidSchemaFieldType,
    ObjectSchemaFieldOptionType,
    ObjectValidSchemaFieldType,
    SchemaMenuOptionType,
    SchemaType,
    SchemaTypeOptionType,
    StringSchemaFieldOptionType,
    StringValidSchemaFieldType,
} from './types';

export const SCHEMA_TYPES: SchemaTypeOptionType[] = [
    {
        label: 'schemaTypes.text',
        value: 'string',
    },
    {
        label: 'schemaTypes.integer',
        value: 'integer',
    },
    {
        label: 'schemaTypes.decimal',
        value: 'number',
    },
    {
        label: 'schemaTypes.boolean',
        value: 'boolean',
    },
    {
        label: 'schemaTypes.object',
        value: 'object',
    },
    {
        label: 'schemaTypes.array',
        value: 'array',
    },
];

export const FORMAT_OPTIONS = [
    {
        label: 'formatOptions.date',
        value: 'date-time',
    },
    {
        label: 'formatOptions.email',
        value: 'email',
    },
    {
        label: 'formatOptions.hostname',
        value: 'hostname',
    },
    {
        label: 'formatOptions.ipv4',
        value: 'ipv4',
    },
    {
        label: 'formatOptions.ipv6',
        value: 'ipv6',
    },
    {
        label: 'formatOptions.uri',
        value: 'uri',
    },
];

const commonValidProperties: CommonValidSchemaFieldType[] = ['description', 'type', 'title'];

export const STRING_VALID_SCHEMA_PROPERTIES: StringValidSchemaFieldType[] = [
    ...commonValidProperties,
    'enum',
    'format',
    'maxLength',
    'minLength',
    'pattern',
];

export const NUBER_VALID_SCHEMA_PROPERTIES: NumberValidSchemaFieldType[] = [
    ...commonValidProperties,
    'maximum',
    'minimum',
    'multipleOf',
];

export const INTEGER_VALID_SCHEMA_PROPERTIES: IntegerValidSchemaFieldType[] = [
    ...commonValidProperties,
    'maximum',
    'minimum',
    'multipleOf',
];

export const BOOL_VALID_SCHEMA_PROPERTIES: BoolValidSchemaFieldType[] = [...commonValidProperties];

export const arrayValidSchemaProperties: ArrayValidSchemaFieldType[] = [
    ...commonValidProperties,
    'maxItems',
    'minItems',
    'uniqueItems',
    'items',
];

export const OBJECT_VALID_SCHEMA_PROPERTIES: ObjectValidSchemaFieldType[] = [
    ...commonValidProperties,
    'required',
    'properties',
];

const commonSchemaOptions: CommonSchemaFieldOptionType[] = [{label: 'description', type: 'text', value: 'description'}];

export const STRING_SCHEMA_OPTIONS: StringSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minLength', type: 'number', value: 'minLength'},
    {label: 'requirement.maxLength', type: 'number', value: 'maxLength'},
    {label: 'options', type: 'multi_creatable', value: 'enum'},
    {label: 'pattern', type: 'text', value: 'pattern'},
    {
        label: 'format',
        optionList: FORMAT_OPTIONS,
        type: 'select',
        value: 'format',
    },
];

export const NUMBER_SCHEMA_OPTIONS: NumberSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minimum', type: 'number', value: 'minimum'},
    {label: 'requirement.maximum', type: 'number', value: 'maximum'},
    {label: 'requirement.multipleOf', type: 'number', value: 'multipleOf'},
];

export const INTEGER_SCHEMA_OPTIONS: IntegerSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minimum', type: 'number', value: 'minimum'},
    {label: 'requirement.maximum', type: 'number', value: 'maximum'},
    {label: 'requirement.multipleOf', type: 'number', value: 'multipleOf'},
];

export const BOOL_SCHEMA_OPTIONS: BoolSchemaFieldOptionType[] = [...commonSchemaOptions];

export const OBJECT_SCHEMA_OPTIONS: ObjectSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.required', type: 'required', value: 'required'},
];

export const ARRAY_SCHEMA_OPTIONS: ArraySchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minItems', type: 'number', value: 'minItems'},
    {label: 'requirement.maxItems', type: 'number', value: 'maxItems'},
    {label: 'requirement.uniqueItems', type: 'boolean', value: 'uniqueItems'},
];

export const TYPE_TO_OPTIONS: Record<SchemaType, SchemaMenuOptionType[]> = {
    array: ARRAY_SCHEMA_OPTIONS,
    boolean: BOOL_SCHEMA_OPTIONS,
    integer: INTEGER_SCHEMA_OPTIONS,
    number: NUMBER_SCHEMA_OPTIONS,
    object: OBJECT_SCHEMA_OPTIONS,
    string: STRING_SCHEMA_OPTIONS,
};

export const TYPE_TO_VALIDATE_FIELDS: Record<SchemaType, string[]> = {
    array: arrayValidSchemaProperties,
    boolean: BOOL_VALID_SCHEMA_PROPERTIES,
    integer: INTEGER_VALID_SCHEMA_PROPERTIES,
    number: NUBER_VALID_SCHEMA_PROPERTIES,
    object: OBJECT_VALID_SCHEMA_PROPERTIES,
    string: STRING_VALID_SCHEMA_PROPERTIES,
};
