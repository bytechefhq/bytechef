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
