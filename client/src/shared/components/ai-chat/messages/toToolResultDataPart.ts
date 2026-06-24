/**
 * Parses a JSON string, returning the parsed value or `null` on failure. Logs a structured warning so
 * regressions in tool/LLM serializers are visible in production logs rather than silently swallowed.
 */
export const parseJson = <T>(content: string, contextLabel: string): T | null => {
    try {
        return JSON.parse(content) as T;
    } catch (parseError) {
        console.warn('Failed to parse tool-result JSON', {
            context: contextLabel,
            len: content.length,
            message: parseError instanceof Error ? parseError.message : String(parseError),
            sample: content.slice(0, 80),
        });

        return null;
    }
};
