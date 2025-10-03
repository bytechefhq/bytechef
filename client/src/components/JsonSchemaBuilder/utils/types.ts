export type SchemaRecordType = Record<string, unknown>;

export type SchemaType = 'string' | 'number' | 'integer' | 'object' | 'array' | 'boolean';

export type SchemaTypeOptionType = {value: SchemaType; label: string};

export type CommonSchemaFieldType = 'description';

export type StringSchemaFieldType = CommonSchemaFieldType | 'enum' | 'minLength' | 'maxLength' | 'pattern' | 'format';
export type NumberSchemaFieldType = CommonSchemaFieldType | 'minimum' | 'maximum' | 'multipleOf';
export type IntegerSchemaFieldType = CommonSchemaFieldType | 'minimum' | 'maximum' | 'multipleOf';
export type BoolSchemaFieldType = CommonSchemaFieldType;
export type ObjectSchemaFieldType = CommonSchemaFieldType | 'required';
export type ArraySchemaFieldType = CommonSchemaFieldType | 'uniqueItems' | 'minItems' | 'maxItems';

export type SchemaFieldOptionValueType = 'text' | 'number' | 'boolean' | 'multi_creatable' | 'select' | 'required';

export type CommonValidSchemaFieldType = CommonSchemaFieldType | 'title' | 'type';
export type StringValidSchemaFieldType = StringSchemaFieldType | CommonValidSchemaFieldType;
export type NumberValidSchemaFieldType = NumberSchemaFieldType | CommonValidSchemaFieldType;
export type IntegerValidSchemaFieldType = IntegerSchemaFieldType | CommonValidSchemaFieldType;
export type BoolValidSchemaFieldType = BoolSchemaFieldType | CommonValidSchemaFieldType;
export type ArrayValidSchemaFieldType = ArraySchemaFieldType | CommonValidSchemaFieldType | 'items';
export type ObjectValidSchemaFieldType = ObjectSchemaFieldType | CommonValidSchemaFieldType | 'properties';

export type SchemaFieldOptionType = {
    label: string;
    type: SchemaFieldOptionValueType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    optionList?: any;
};

export type CommonSchemaFieldOptionType = SchemaFieldOptionType & {
    value: CommonSchemaFieldType;
};

export type StringSchemaFieldOptionType = SchemaFieldOptionType & {
    value: StringSchemaFieldType;
};
export type NumberSchemaFieldOptionType = SchemaFieldOptionType & {
    value: NumberSchemaFieldType;
};
export type IntegerSchemaFieldOptionType = SchemaFieldOptionType & {
    value: IntegerSchemaFieldType;
};
export type BoolSchemaFieldOptionType = SchemaFieldOptionType & {
    value: BoolSchemaFieldType;
};
export type ObjectSchemaFieldOptionType = SchemaFieldOptionType & {
    value: ObjectSchemaFieldType;
};
export type ArraySchemaFieldOptionType = SchemaFieldOptionType & {
    value: ArraySchemaFieldType;
};

export type SchemaMenuOptionType =
    | StringSchemaFieldOptionType
    | NumberSchemaFieldOptionType
    | IntegerSchemaFieldOptionType
    | BoolSchemaFieldOptionType
    | ObjectSchemaFieldOptionType
    | ArraySchemaFieldOptionType;
