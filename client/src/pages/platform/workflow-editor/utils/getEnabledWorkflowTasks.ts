import {getEffectivelyDisabledTaskNames} from './getEffectivelyDisabledTaskNames';

interface WorkflowWithTasksI<T extends {name: string}> {
    definition?: string;
    tasks?: Array<T>;
}

function getDefinitionDisabledTaskNames(definition: string | undefined): Set<string> {
    if (!definition) {
        return new Set();
    }

    try {
        const parsedDefinition = JSON.parse(definition);

        return getEffectivelyDisabledTaskNames(parsedDefinition.tasks ?? []);
    } catch {
        return new Set();
    }
}

export function getEnabledWorkflowTasks<T extends {name: string}>(workflow: WorkflowWithTasksI<T>): Array<T> {
    const disabledTaskNames = getDefinitionDisabledTaskNames(workflow.definition);

    return (workflow.tasks ?? []).filter((task) => !disabledTaskNames.has(task.name));
}
