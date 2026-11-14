import {PropertyAllType} from '@/shared/types';

export const getMcpToolPropertyFormDefaults = (
    properties: PropertyAllType[],
    savedParameters: Record<string, unknown> | null | undefined
): Record<string, unknown> => {
    const propertyDefaults: Record<string, unknown> = {};

    for (const property of properties) {
        const propertyRecord = property as unknown as Record<string, unknown>;

        const resolvedDefault =
            propertyRecord.defaultValue ??
            propertyRecord.integerDefaultValue ??
            propertyRecord.numberDefaultValue ??
            propertyRecord.booleanDefaultValue ??
            propertyRecord.arrayDefaultValue ??
            propertyRecord.objectDefaultValue;

        if (property.name && resolvedDefault !== undefined && resolvedDefault !== null) {
            propertyDefaults[property.name] = resolvedDefault;
        }
    }

    return {...propertyDefaults, ...savedParameters};
};

const sanitizeValue = (value: unknown): unknown => {
    if (value === '') {
        return null;
    }

    if (Array.isArray(value)) {
        return value.map((item) => sanitizeValue(item));
    }

    if (value && typeof value === 'object') {
        return sanitizeMcpToolParameters(value as Record<string, unknown>);
    }

    return value;
};

export const sanitizeMcpToolParameters = (record: Record<string, unknown>): Record<string, unknown> =>
    Object.fromEntries(Object.entries(record).map(([key, value]) => [key, sanitizeValue(value)]));
