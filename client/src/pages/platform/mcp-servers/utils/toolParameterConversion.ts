export const toFormParameters = (record: Record<string, unknown>): Record<string, unknown> =>
    Object.fromEntries(
        Object.entries(record).map(([key, value]) => {
            if (value === null) {
                return [key, ''];
            }

            if (value && typeof value === 'object' && !Array.isArray(value)) {
                return [key, toFormParameters(value as Record<string, unknown>)];
            }

            return [key, value];
        })
    );

export const toStoredParameters = (record: Record<string, unknown>): Record<string, unknown> =>
    Object.fromEntries(
        Object.entries(record).map(([key, value]) => {
            if (value === '') {
                return [key, null];
            }

            if (value && typeof value === 'object' && !Array.isArray(value)) {
                return [key, toStoredParameters(value as Record<string, unknown>)];
            }

            return [key, value];
        })
    );

export const toStoredWorkflowParameters = (record: Record<string, unknown>): Record<string, unknown> => {
    const sanitizeValue = (value: unknown): unknown => {
        if (value === '') {
            return null;
        }

        if (Array.isArray(value)) {
            return value.map((item) => sanitizeValue(item));
        }

        if (value && typeof value === 'object') {
            return toStoredWorkflowParameters(value as Record<string, unknown>);
        }

        return value;
    };

    return Object.fromEntries(Object.entries(record).map(([key, value]) => [key, sanitizeValue(value)]));
};
