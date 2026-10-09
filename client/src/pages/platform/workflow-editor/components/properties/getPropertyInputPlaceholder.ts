interface GetPropertyInputPlaceholderPropsI {
    defaultValue?: unknown;
    isNumericalInput?: boolean;
    maxValue?: number;
    minValue?: number;
    placeholder?: string;
    required?: boolean;
}

export default function getPropertyInputPlaceholder({
    defaultValue,
    isNumericalInput,
    maxValue,
    minValue,
    placeholder,
    required,
}: GetPropertyInputPlaceholderPropsI): string {
    const range =
        isNumericalInput && typeof minValue === 'number' && typeof maxValue === 'number'
            ? `From ${minValue} to ${maxValue}`
            : undefined;

    const isPrimitiveDefaultValue =
        typeof defaultValue === 'string' || typeof defaultValue === 'number' || typeof defaultValue === 'boolean';

    const formattedDefaultValue = isPrimitiveDefaultValue ? String(defaultValue) : '';

    if (!required && formattedDefaultValue) {
        return range ? `Default: ${formattedDefaultValue} · ${range}` : `Default: ${formattedDefaultValue}`;
    }

    return range || placeholder || `Type ${isNumericalInput ? 'a number' : 'something'}...`;
}
