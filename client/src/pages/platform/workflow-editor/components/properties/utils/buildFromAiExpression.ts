export interface FromAiExpressionParamsI {
    arrayName?: string;
    defaultValue?: unknown;
    description?: string;
    name: string;
    options?: Array<{value?: unknown}> | null;
    required?: boolean;
    type: string;
}

export default function buildFromAiExpression({
    arrayName,
    defaultValue,
    description,
    name,
    options,
    required = false,
    type,
}: FromAiExpressionParamsI): string {
    const mapEntries: string[] = [];

    if (description) {
        const escapedDescription = description.replace(/'/g, "''");

        mapEntries.push(`'description': '${escapedDescription}'`);
    }

    if (defaultValue !== '' && defaultValue !== null && defaultValue !== undefined) {
        const defaultValueString = String(defaultValue);

        if (!defaultValueString.startsWith('=')) {
            const escapedDefault = defaultValueString.replace(/'/g, "''");

            mapEntries.push(`'defaultValue': '${escapedDefault}'`);
        }
    }

    if (options != null && options.length > 0) {
        const optionValues = options.map((option) => `'${String(option?.value ?? '').replace(/'/g, "''")}'`).join(', ');

        mapEntries.push(`'options': {${optionValues}}`);
    }

    mapEntries.push(`'required': ${required}`);

    const qualifiedName = arrayName ? `${arrayName}_${name}` : name;

    return `=fromAi('${qualifiedName}', '${type}', {${mapEntries.join(', ')}})`;
}
