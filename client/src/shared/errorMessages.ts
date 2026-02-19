/**
 * Centralized user-facing error messages for the platform.
 * Use these keys instead of inline strings so messages can be referenced,
 * updated, and later localized from one place.
 */
export const ERROR_MESSAGES = {
    PROPERTY: {
        DECIMAL_POINTS_NOT_ALLOWED: 'Decimal points are not allowed',
        FIELD_REQUIRED: 'This field is required.',
        INCORRECT_VALUE: 'Incorrect value',
        MAX_DECIMAL_PLACES: (max: number): string => `Maximum ${max} decimal places allowed`,
        VALUE_DOES_NOT_MATCH_PATTERN: 'Value does not match the required pattern',
        VALUE_MUST_BE_VALID_INTEGER: 'Value must be a valid integer',
        VALUE_MUST_BE_VALID_NUMBER: 'Value must be a valid number',
    },
} as const;
