// In controlled (react-hook-form) function mode the TipTap editor reports its content WITHOUT the
// leading "=". Re-add it so the form value stays a well-formed expression; empty content clears.
export function reconstructControlledExpressionValue(rawValue: string | number): string {
    const stringValue = typeof rawValue === 'string' ? rawValue : String(rawValue ?? '');
    const trimmed = stringValue.trim();

    if (trimmed === '') {
        return '';
    }

    return trimmed.startsWith('=') ? trimmed : `=${trimmed}`;
}
