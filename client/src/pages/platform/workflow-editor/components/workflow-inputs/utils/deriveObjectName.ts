/**
 * Derives the field-mapping object name from a static mapObjectFields-shaped test value. The object name is the single
 * top-level key (optionally inside a `mapObjectFields` envelope). Returns `undefined` when the JSON is invalid or empty.
 */
export default function deriveObjectName(testValue: string | undefined): string | undefined {
    if (!testValue) {
        return undefined;
    }

    try {
        const parsed = JSON.parse(testValue) as Record<string, unknown>;
        const root = (parsed.mapObjectFields as Record<string, unknown>) ?? parsed;
        const keys = Object.keys(root);

        return keys.length > 0 ? keys[0] : undefined;
    } catch {
        return undefined;
    }
}
