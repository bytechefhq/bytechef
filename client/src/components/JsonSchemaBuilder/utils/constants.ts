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

export const schemaTypes: SchemaTypeOptionType[] = [
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

export const formatOptions = [
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

export const stringValidSchemaProperties: StringValidSchemaFieldType[] = [
    ...commonValidProperties,
    'enum',
    'format',
    'maxLength',
    'minLength',
    'pattern',
];

export const numberValidSchemaProperties: NumberValidSchemaFieldType[] = [
    ...commonValidProperties,
    'maximum',
    'minimum',
    'multipleOf',
];

export const integerValidSchemaProperties: IntegerValidSchemaFieldType[] = [
    ...commonValidProperties,
    'maximum',
    'minimum',
    'multipleOf',
];

export const boolValidSchemaProperties: BoolValidSchemaFieldType[] = [...commonValidProperties];

export const arrayValidSchemaProperties: ArrayValidSchemaFieldType[] = [
    ...commonValidProperties,
    'maxItems',
    'minItems',
    'uniqueItems',
    'items',
];

export const objectValidSchemaProperties: ObjectValidSchemaFieldType[] = [
    ...commonValidProperties,
    'required',
    'properties',
];

const commonSchemaOptions: CommonSchemaFieldOptionType[] = [{label: 'description', type: 'text', value: 'description'}];

export const stringSchemaOptions: StringSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minLength', type: 'number', value: 'minLength'},
    {label: 'requirement.maxLength', type: 'number', value: 'maxLength'},
    {label: 'options', type: 'multi_creatable', value: 'enum'},
    {label: 'pattern', type: 'text', value: 'pattern'},
    {
        label: 'format',
        optionList: formatOptions,
        type: 'select',
        value: 'format',
    },
];

export const numberSchemaOptions: NumberSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minimum', type: 'number', value: 'minimum'},
    {label: 'requirement.maximum', type: 'number', value: 'maximum'},
    {label: 'requirement.multipleOf', type: 'number', value: 'multipleOf'},
];

export const integerSchemaOptions: IntegerSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minimum', type: 'number', value: 'minimum'},
    {label: 'requirement.maximum', type: 'number', value: 'maximum'},
    {label: 'requirement.multipleOf', type: 'number', value: 'multipleOf'},
];

export const boolSchemaOptions: BoolSchemaFieldOptionType[] = [...commonSchemaOptions];

export const objectSchemaOptions: ObjectSchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.required', type: 'required', value: 'required'},
];

export const arraySchemaOptions: ArraySchemaFieldOptionType[] = [
    ...commonSchemaOptions,
    {label: 'requirement.minItems', type: 'number', value: 'minItems'},
    {label: 'requirement.maxItems', type: 'number', value: 'maxItems'},
    {label: 'requirement.uniqueItems', type: 'boolean', value: 'uniqueItems'},
];

export const typeToOptions: Record<SchemaType, SchemaMenuOptionType[]> = {
    array: arraySchemaOptions,
    boolean: boolSchemaOptions,
    integer: integerSchemaOptions,
    number: numberSchemaOptions,
    object: objectSchemaOptions,
    string: stringSchemaOptions,
};

export const typeToValidFields: Record<SchemaType, string[]> = {
    array: arrayValidSchemaProperties,
    boolean: boolValidSchemaProperties,
    integer: integerValidSchemaProperties,
    number: numberValidSchemaProperties,
    object: objectValidSchemaProperties,
    string: stringValidSchemaProperties,
};
