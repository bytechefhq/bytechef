/**
 * Whether a parsed tool result is a value a tool-call card shows as parsed: an object, an array or a string. A bare
 * JSON number, boolean or null is not, so the card keeps the text it came from.
 */
export function isParsedToolResultValue(value: unknown): value is object | string {
    return typeof value === 'string' || (typeof value === 'object' && value !== null);
}
