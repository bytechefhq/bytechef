const formatScalarValue = (value: unknown): string => {
    if (typeof value === 'string') {
        return value;
    }

    if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint') {
        return value.toString();
    }

    return JSON.stringify(value) ?? '';
};

const formatWorkflowInputTestValue = (value: unknown): string => {
    if (value == null) {
        return '';
    }

    if (typeof value === 'object') {
        return Object.values(value as Record<string, unknown>)
            .filter((memberValue) => memberValue != null && memberValue !== '')
            .map(formatScalarValue)
            .join(', ');
    }

    return formatScalarValue(value);
};

export default formatWorkflowInputTestValue;
