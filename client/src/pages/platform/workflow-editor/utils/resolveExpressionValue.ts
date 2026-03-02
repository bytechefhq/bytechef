/**
 * Resolves the value to pass to fieldOnChange based on whether the field
 * is currently in expression mode (value starts with '=').
 *
 * Five branches:
 * 1. Empty input while in expression mode → keep the '=' prefix
 * 2. Empty input outside expression mode → clear to empty string
 * 3. Input already starts with '=' → pass through as-is
 * 4. Non-'=' input while in expression mode → prepend '='
 * 5. Non-'=' input outside expression mode → pass through as-is
 */
export default function resolveExpressionValue(rawValue: string, currentFieldValue: unknown): string {
    const inExpressionMode = typeof currentFieldValue === 'string' && currentFieldValue.startsWith('=');

    if (rawValue === '' && inExpressionMode) {
        return '=';
    } else if (rawValue === '') {
        return '';
    } else if (rawValue.startsWith('=')) {
        return rawValue;
    } else if (inExpressionMode) {
        return `=${rawValue}`;
    } else {
        return rawValue;
    }
}
