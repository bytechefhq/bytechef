const isPlainObject = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

/**
 * Serializes a dependency-values record into a deterministic string: keys are sorted so that two records with the
 * same entries always produce the same output regardless of insertion order, and values that `JSON.stringify` cannot
 * handle (circular structures, BigInt) fall back to `String(value)` instead of throwing so option loading can never
 * crash the UI over an exotic dependency value.
 */
export const stableSerialize = (record: Record<string, unknown>): string => {
    const serializedEntries = Object.keys(record)
        .sort()
        .map((key) => {
            let serializedValue: string;

            try {
                serializedValue = JSON.stringify(record[key]) ?? 'undefined';
            } catch {
                serializedValue = String(record[key]);
            }

            return `${JSON.stringify(key)}:${serializedValue}`;
        });

    return `{${serializedEntries.join(',')}}`;
};

/**
 * Extracts the `sub` (external user id) claim from a JWT without verifying its signature. The SDK needs the external
 * user id for action-execution URLs; the server independently verifies the token, so client-side decoding is safe to
 * read claims from. Returns `undefined` when the token is malformed.
 */
export const decodeJwtSubject = (jwtToken: string): string | undefined => {
    try {
        const payloadSegment = jwtToken.split('.')[1];

        if (!payloadSegment) {
            return undefined;
        }

        const normalized = payloadSegment.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(atob(normalized)) as {sub?: string};

        return payload.sub;
    } catch {
        return undefined;
    }
};

/**
 * Builds the cache key under which a component-defined input's resolved options are stored in (and read from) the
 * `workflowInputOptions` map. The key mirrors the component-reference identity tuple
 * (`componentName`, `componentVersion`, `groupName`, the property/member name, and the resolved dependency values)
 * so that two distinct component inputs — or two group members in different components — that happen to share a
 * property/member name do not collide on the same slot. The write side that populates `workflowInputOptions` MUST
 * key with this same function.
 */
export const optionsCacheKey = (
    componentName: string,
    componentVersion: number,
    groupName: string,
    propertyName: string,
    dependencyValues: Record<string, unknown>
): string =>
    `${componentName}:${componentVersion}:${groupName}:${propertyName}:${stableSerialize(dependencyValues ?? {})}`;

/**
 * Merges locally overridden workflow inputs over the server-side inputs one level deep: when both sides hold a plain
 * object for the same input (a group input's member map), the member entries are merged instead of the override
 * replacing the whole group — otherwise server-provided members the user never touched would be dropped on save.
 */
export const mergeWorkflowInputs = (
    serverInputs: Record<string, unknown>,
    overrides: Record<string, unknown> | undefined
): Record<string, unknown> => {
    const mergedInputs: Record<string, unknown> = {...serverInputs};

    Object.entries(overrides ?? {}).forEach(([inputName, overrideValue]) => {
        const serverValue = mergedInputs[inputName];

        if (isPlainObject(serverValue) && isPlainObject(overrideValue)) {
            mergedInputs[inputName] = {...serverValue, ...overrideValue};
        } else {
            mergedInputs[inputName] = overrideValue;
        }
    });

    return mergedInputs;
};
