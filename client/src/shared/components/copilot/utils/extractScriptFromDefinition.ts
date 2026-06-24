export function extractScriptFromDefinition(definition: string, nodeName: string): string | null {
    let parsed: unknown;

    try {
        parsed = JSON.parse(definition);
    } catch {
        return null;
    }

    const script = findScript(parsed, nodeName);

    return typeof script === 'string' ? script : null;
}

function findScript(node: unknown, nodeName: string): unknown {
    if (Array.isArray(node)) {
        for (const item of node) {
            const found = findScript(item, nodeName);

            if (found !== undefined) {
                return found;
            }
        }

        return undefined;
    }

    if (node && typeof node === 'object') {
        const record = node as Record<string, unknown>;

        if (record.name === nodeName) {
            const parameters = record.parameters as Record<string, unknown> | undefined;

            return parameters?.script;
        }

        for (const value of Object.values(record)) {
            const found = findScript(value, nodeName);

            if (found !== undefined) {
                return found;
            }
        }
    }

    return undefined;
}
