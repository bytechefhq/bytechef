export type IsFieldDirtyType = (path: string) => boolean;

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isEmptyFieldValue(value: unknown): boolean {
    return value === '' || value === null || (Array.isArray(value) && value.length === 0);
}

function isPathOrAncestorDirty(path: string, isFieldDirty: IsFieldDirtyType): boolean {
    const segments = path.split('.');

    return segments.some((_segment, index) => isFieldDirty(segments.slice(0, index + 1).join('.')));
}

function isEqualValue(value: unknown, otherValue: unknown): boolean {
    if (value === otherValue) {
        return true;
    }

    if (Array.isArray(value) && Array.isArray(otherValue)) {
        return (
            value.length === otherValue.length && value.every((item, index) => isEqualValue(item, otherValue[index]))
        );
    }

    if (isPlainObject(value) && isPlainObject(otherValue)) {
        const keys = Object.keys(value);

        return (
            keys.length === Object.keys(otherValue).length &&
            keys.every((key) => key in otherValue && isEqualValue(value[key], otherValue[key]))
        );
    }

    return false;
}

function findRemovedKeys(currentValues: Record<string, unknown>, initialValues: Record<string, unknown>): string[] {
    return Object.keys(initialValues).filter(
        (initialKey) => initialValues[initialKey] !== undefined && currentValues[initialKey] === undefined
    );
}

function pickChangedValuesAtPath(
    currentValues: Record<string, unknown>,
    initialValues: Record<string, unknown>,
    isFieldDirty: IsFieldDirtyType,
    parentPath: string
): Record<string, unknown> {
    const result: Record<string, unknown> = {};

    for (const [key, currentValue] of Object.entries(currentValues)) {
        const initialValue = initialValues[key];
        const path = parentPath ? `${parentPath}.${key}` : key;

        if (currentValue === undefined) {
            continue;
        }

        if (isPlainObject(currentValue)) {
            const initialObject = isPlainObject(initialValue) ? initialValue : {};

            const removedKeys = findRemovedKeys(currentValue, initialObject);

            if (removedKeys.length > 0) {
                result[key] = {
                    ...currentValue,
                    ...Object.fromEntries(removedKeys.map((removedKey) => [removedKey, undefined])),
                };

                continue;
            }

            const changedNestedValues = pickChangedValuesAtPath(currentValue, initialObject, isFieldDirty, path);

            if (Object.keys(changedNestedValues).length > 0) {
                result[key] = changedNestedValues;
            }

            continue;
        }

        if (
            initialValue === undefined &&
            isEmptyFieldValue(currentValue) &&
            !isPathOrAncestorDirty(path, isFieldDirty)
        ) {
            continue;
        }

        if (!isEqualValue(currentValue, initialValue)) {
            result[key] = currentValue;
        }
    }

    return result;
}

export default function pickChangedValues(
    currentValues: Record<string, unknown>,
    initialValues: Record<string, unknown>,
    isFieldDirty: IsFieldDirtyType = () => true
): Record<string, unknown> {
    const result = pickChangedValuesAtPath(currentValues, initialValues, isFieldDirty, '');

    for (const removedKey of findRemovedKeys(currentValues, initialValues)) {
        result[removedKey] = undefined;
    }

    return result;
}
