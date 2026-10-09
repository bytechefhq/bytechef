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
    const range = isNumericalInput && minValue && maxValue ? `From ${minValue} to ${maxValue}` : undefined;

    const hasDefaultValue = !required && defaultValue !== undefined && defaultValue !== null && defaultValue !== '';

    if (hasDefaultValue) {
        return range ? `Default: ${defaultValue} · ${range}` : `Default: ${defaultValue}`;
    }

    return range || placeholder || `Type ${isNumericalInput ? 'a number' : 'something'}...`;
}
