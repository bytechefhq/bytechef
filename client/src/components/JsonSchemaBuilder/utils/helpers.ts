import {typeToOptions, typeToValidFields} from './constants';
import {SchemaRecordType, SchemaType} from './types';

export const getAllSchemaKeys = (schema: object) => Object.keys(schema);

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaField = (obj: any, path: string | string[]) => {
    const keys = Array.isArray(path) ? path : path.split('.');

    return keys.reduce((acc, key) => (acc && acc[key] !== undefined ? acc[key] : undefined), obj);
};

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaFields = (obj: any, keys: string[]) => {
    return keys
        ? keys.reduce(
              (acc, key) => {
                  if (obj[key] !== undefined) {
                      acc[key] = obj[key];
                  }
                  return acc;
              },
              {} as Record<string, any>
          )
        : {};
};

export const getSchemaType = (schema: any) => getSchemaField(schema, 'type');

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaTitle = (schema: any) => getSchemaField(schema, 'title');

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaProperty = (schema: any, key: string) => getSchemaField(schema, ['properties', key]);

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaProperties = (schema: any) => getSchemaField(schema, 'properties');

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaItems = (schema: any) => getSchemaField(schema, 'items');

/* eslint-disable @typescript-eslint/no-explicit-any */
export const getSchemaRequired = (schema: any) => getSchemaField(schema, 'required');

export const getSchemaRequiredProperties = (schema: SchemaRecordType) => {
    const required = getSchemaRequired(schema);
    const properties = getSchemaProperties(schema);

    return getSchemaFields(properties, required);
};

/* eslint-disable @typescript-eslint/no-explicit-any */
export const setSchemaField = (path: string | string[], value: any, obj: any) => {
    const keys = Array.isArray(path) ? path : path.split('.');

    const lastKey = keys.pop();
    const lastObj = keys.reduce((obj, key) => (obj[key] = obj[key] || {}), obj);

    if (lastKey !== undefined) {
        lastObj[lastKey] = value;
    }

    return obj;
};

export const setSchemaType = (value: any, obj: any) => setSchemaField('type', value, obj);

export const setSchemaTitle = (value: any, obj: any) => setSchemaField('title', value, obj);

export const setSchemaProperties = (value: any, obj: any) => setSchemaField('properties', value, obj);

export const setSchemaProperty = (key: string, value: any, obj: any) => setSchemaField(['properties', key], value, obj);

export const setSchemaItems = (value: any, obj: any) => setSchemaField('items', value, obj);

export const deleteSchemaField = (path: string | string[], obj: any) => {
    const keys = Array.isArray(path) ? path : path.split('.');

    const lastKey = keys.pop();
    const lastObj = keys.reduce((obj, key) => (obj[key] = obj[key] || {}), obj);

    if (lastKey !== undefined) {
        delete lastObj[lastKey];
    }

    return obj;
};

export const deleteSchemaProperty = (key: string, obj: any) => deleteSchemaField(['properties', key], obj);

export const addSchemaProperty = (schema: SchemaRecordType) => setSchemaProperty(`__${Date.now()}__`, {}, schema);

export const renameSchemaField = (oldKey: string, newKey: string) => (obj: Record<string, any>) => {
    return Object.keys(obj).reduce(
        (acc, key) => {
            acc[key === oldKey ? newKey : key] = obj[key];

            return acc;
        },
        {} as Record<string, any>
    );
};

export const renameSchemaProperty = (oldKey: string, newKey: string, schema: SchemaRecordType) => {
    const properties = getSchemaProperties(schema);
    const renamedProperties = renameSchemaField(oldKey, newKey)(properties);
    return setSchemaProperties(renamedProperties, schema);
};

export const isEmpty = (value: any) => {
    return (
        value == null || // From standard.js: Always use === - but obj == null is allowed to check null || undefined
        (typeof value === 'object' && Object.keys(value).length === 0) ||
        (typeof value === 'string' && value.trim().length === 0)
    );
};

export const isSchemaObject = (schema: SchemaRecordType) => getSchemaType(schema) === 'object';

export const isSchemaArray = (schema: SchemaRecordType) => getSchemaType(schema) === 'array';

export const isFieldRequired = (key: string, schema: SchemaRecordType) => {
    const required = getSchemaRequired(schema);
    return required ? required.includes(key) : false;
};

export const hasSchemaProperties = (schema: SchemaRecordType) => !isEmpty(getSchemaProperties(schema));

export const hasSchemaItems = (schema: SchemaRecordType) => !isEmpty(getSchemaItems(schema));

export const getSchemaMenuOptions = (type: SchemaType) => typeToOptions[type];

export const findOption = (value: string) => (options: {value: string}[]) =>
    options.find((option) => option.value === value);

export const optionsToStrings = (options: {value: string}[]) => options.map((option) => option.value);

export const stringsToOptions = (strings: string[]) => (strings ? strings.map((s) => ({label: s, value: s})) : []);

export const schemaFieldAsOption = (key: string, schema: SchemaRecordType) => {
    const title = getSchemaTitle(schema);

    if (!isEmpty(title)) {
        return {label: title, value: key};
    }
    return {label: key, value: key};
};

export const fieldsToOptions = (fields: Record<string, any>) => {
    return Object.entries(fields).map(([key, val]) => schemaFieldAsOption(key, val));
};

export const schemaPropertiesAsOptions = (schema: SchemaRecordType) => fieldsToOptions(getSchemaProperties(schema));

export const schemaRequiredPropertiesAsOptions = (schema: SchemaRecordType) =>
    fieldsToOptions(getSchemaRequiredProperties(schema));

export const getValidFields = (type: SchemaType) => typeToValidFields[type];

export const removeWrongFields = (schema: SchemaRecordType) => {
    const type = getSchemaType(schema);
    const fields = getValidFields(type);

    return getSchemaFields(schema, fields);
};

export const setSchemaTypeAndRemoveWrongFields = (value: any, schema: SchemaRecordType) => {
    if (!schema) {
        schema = {};
    }

    const updatedSchema = setSchemaType(value, schema);

    return removeWrongFields(updatedSchema);
};

export const translateLabels = (t: (text: string) => string, list: any[]) =>
    list ? list.map((item: any) => ({...item, label: t(item.label)})) : [];
