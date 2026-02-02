import {toast} from '@/hooks/use-toast';

interface SafeJsonParseResultI {
    /** The parsed JSON data, or an empty object if parsing failed */
    data: Record<string, unknown>;
    /** Whether the parse succeeded (true) or failed (false) */
    success: boolean;
}

/**
 * Safely parses a JSON string with error handling and optional toast notifications.
 * Returns an empty object on parse failure instead of throwing.
 *
 * @param jsonString - The JSON string to parse. If undefined/empty, returns {data: {}, success: true}
 * @param context - Optional context for error message (e.g., "request body schema"). Used in toast and console log.
 * @param showToast - Whether to show a toast notification on parse failure (default: true).
 *                    Set to false when you want to handle the error silently (e.g., validation checks).
 *                    The `success` field in the return value indicates whether parsing succeeded regardless of this setting.
 * @returns Object with `data` (parsed result or empty object) and `success` (boolean indicating parse success)
 *
 * @example
 * // With toast on failure (default)
 * const {data, success} = safeJsonParse(jsonString, 'request body');
 *
 * @example
 * // Silent validation (no toast)
 * const {success} = safeJsonParse(jsonString, undefined, false);
 * if (!success) {
 *   // Handle validation failure manually
 * }
 */
export const safeJsonParse = (
    jsonString: string | undefined,
    context?: string,
    showToast = true
): SafeJsonParseResultI => {
    if (!jsonString) {
        return {data: {}, success: true};
    }

    try {
        return {data: JSON.parse(jsonString) as Record<string, unknown>, success: true};
    } catch (error) {
        console.error(`Failed to parse JSON${context ? ` for ${context}` : ''}:`, error);

        if (showToast) {
            toast({
                description: context
                    ? `Invalid JSON in ${context}. Using empty object.`
                    : 'Invalid JSON format. Using empty object.',
                title: 'Invalid JSON',
                variant: 'destructive',
            });
        }

        return {data: {}, success: false};
    }
};
