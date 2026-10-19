export function isParsedToolResultValue(value: unknown): value is object | string {
    return typeof value === 'string' || (typeof value === 'object' && value !== null);
}
