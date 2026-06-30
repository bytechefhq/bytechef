import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getDuplicateNodeNames(
    tasks: Array<WorkflowTask> = [],
    triggers: Array<WorkflowTrigger> = []
): Array<string> {
    const names: Array<string> = [];

    for (const trigger of triggers) {
        if (trigger?.name) {
            names.push(trigger.name);
        }
    }

    for (const task of tasks) {
        if (task?.name) {
            names.push(task.name);
        }
    }

    const seenNames = new Set<string>();
    const duplicateNames = new Set<string>();

    for (const name of names) {
        if (seenNames.has(name)) {
            duplicateNames.add(name);
        } else {
            seenNames.add(name);
        }
    }

    return [...duplicateNames];
}
