import {Workflow} from '@/shared/middleware/platform/configuration';

interface NamedTypeI {
    name: string;
    type: string;
}

function isNamedType(value: unknown): value is NamedTypeI {
    return (
        typeof value === 'object' &&
        value !== null &&
        typeof (value as NamedTypeI).name === 'string' &&
        typeof (value as NamedTypeI).type === 'string'
    );
}

function collectTypes(value: unknown, types: string[]) {
    if (Array.isArray(value)) {
        value.forEach((item) => collectTypes(item, types));

        return;
    }

    if (typeof value !== 'object' || value === null) {
        return;
    }

    if (isNamedType(value)) {
        types.push(value.type);
    }

    Object.values(value).forEach((nestedValue) => collectTypes(nestedValue, types));
}

export function parseWorkflowDefinition(definition: string): Workflow {
    const parsed = JSON.parse(definition);

    return {
        definition,
        description: parsed.description,
        label: parsed.label,
        tasks: parsed.tasks ?? [],
        triggers: parsed.triggers ?? [],
    } as Workflow;
}

export function collectComponentNames(workflow: Workflow): string[] {
    const types: string[] = [];

    collectTypes(workflow.tasks, types);
    collectTypes(workflow.triggers, types);

    return Array.from(new Set(types.map((type) => type.split('/')[0])));
}
