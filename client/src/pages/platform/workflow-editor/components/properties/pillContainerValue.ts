import isObject from 'isobject';

interface IsEmptyPillContainerValuePropsI {
    controlType?: string;
    definedPropertyNames?: string[];
    value: unknown;
}

function isBlank(value: unknown): boolean {
    return value === undefined || value === null || value === '';
}

function isEmptyNestedValue(value: unknown): boolean {
    if (isBlank(value)) {
        return true;
    }

    if (Array.isArray(value)) {
        return value.length === 0;
    }

    if (isObject(value)) {
        return Object.values(value as Record<string, unknown>).every(isEmptyNestedValue);
    }

    return false;
}

function isEmptySchema(value: unknown): boolean {
    if (isBlank(value)) {
        return true;
    }

    let schema = value;

    if (typeof value === 'string') {
        try {
            schema = JSON.parse(value);
        } catch {
            return false;
        }
    }

    return isObject(schema) && Object.keys(schema as object).length === 0;
}

export function isEmptyPillContainerValue({
    controlType,
    definedPropertyNames = [],
    value,
}: IsEmptyPillContainerValuePropsI): boolean {
    if (controlType === 'JSON_SCHEMA_BUILDER') {
        return isEmptySchema(value);
    }

    if (isBlank(value)) {
        return true;
    }

    if (controlType === 'ARRAY_BUILDER') {
        return Array.isArray(value) && value.length === 0;
    }

    if (!isObject(value) || Array.isArray(value)) {
        return false;
    }

    return Object.entries(value as Record<string, unknown>).every(
        ([key, nestedValue]) => definedPropertyNames.includes(key) && isEmptyNestedValue(nestedValue)
    );
}
