export default function getFormattedDependencyKey(values: unknown[] = []): string {
    if (!values || values.length === 0) {
        return '';
    }

    if (values.some((value) => value === undefined)) {
        return '';
    }

    return values
        .map((value) => {
            if (value && typeof value === 'object') {
                return JSON.stringify(value);
            }

            return String(value);
        })
        .join(',');
}
