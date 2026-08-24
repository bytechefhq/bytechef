import {isWorkflowTask} from './flattenDefinitionTasks';

const EXPRESSION_PATTERN = /\$\{([^}]*)\}/g;

export function getDisabledNodeReferences(parameters: unknown, disabledTaskNames: Set<string>): Array<string> {
    if (disabledTaskNames.size === 0) {
        return [];
    }

    const referencedTaskNames = new Set<string>();

    collectDisabledNodeReferences(parameters, disabledTaskNames, referencedTaskNames);

    return [...referencedTaskNames];
}

function collectDisabledNodeReferences(
    value: unknown,
    disabledTaskNames: Set<string>,
    referencedTaskNames: Set<string>
): void {
    if (typeof value === 'string') {
        for (const match of value.matchAll(EXPRESSION_PATTERN)) {
            addReferencedTaskNames(match[1], disabledTaskNames, referencedTaskNames);
        }

        return;
    }

    if (isWorkflowTask(value) || isNestedTaskList(value)) {
        return;
    }

    if (Array.isArray(value)) {
        for (const item of value) {
            collectDisabledNodeReferences(item, disabledTaskNames, referencedTaskNames);
        }

        return;
    }

    if (value && typeof value === 'object') {
        for (const nestedValue of Object.values(value)) {
            collectDisabledNodeReferences(nestedValue, disabledTaskNames, referencedTaskNames);
        }
    }
}

function isNestedTaskList(value: unknown): boolean {
    if (!Array.isArray(value) || value.length === 0) {
        return false;
    }

    const firstItem = value[0];

    return (
        isWorkflowTask(firstItem) || (Array.isArray(firstItem) && firstItem.length > 0 && isWorkflowTask(firstItem[0]))
    );
}

function addReferencedTaskNames(
    expressionBody: string,
    disabledTaskNames: Set<string>,
    referencedTaskNames: Set<string>
): void {
    for (const disabledTaskName of disabledTaskNames) {
        if (referencedTaskNames.has(disabledTaskName)) {
            continue;
        }

        const escapedTaskName = disabledTaskName.replace(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
        const referencePattern = new RegExp(String.raw`(^|[^\w])${escapedTaskName}([^\w]|$)`);

        if (referencePattern.test(expressionBody)) {
            referencedTaskNames.add(disabledTaskName);
        }
    }
}
