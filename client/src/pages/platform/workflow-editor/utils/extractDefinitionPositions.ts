import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import {forEachNestedTaskGroup} from './taskTraversalUtils';

/**
 * Recursively collects saved node positions from a parsed task array.
 */
function collectTaskPositions(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tasks: any[],
    positionMap: Map<string, {x: number; y: number}>
): void {
    for (const task of tasks) {
        if (task.metadata?.ui?.nodePosition) {
            positionMap.set(task.name, task.metadata.ui.nodePosition);
        }

        if (task.parameters) {
            forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (subtasks) => {
                collectTaskPositions(subtasks, positionMap);
            });
        }
    }
}

/**
 * Extracts saved node positions from the workflow definition JSON string.
 * Used to sync positions into layout nodes because storeTasks uses
 * fingerprint equality that ignores position metadata changes.
 */
export default function extractDefinitionPositions(definition: string): Map<string, {x: number; y: number}> {
    const positionMap = new Map<string, {x: number; y: number}>();

    try {
        const parsed = JSON.parse(definition);

        if (parsed.triggers?.[0]?.metadata?.ui?.nodePosition) {
            positionMap.set(parsed.triggers[0].name, parsed.triggers[0].metadata.ui.nodePosition);
        }

        if (parsed.tasks) {
            collectTaskPositions(parsed.tasks as WorkflowTask[], positionMap);
        }
    } catch {
        // ignore parse errors
    }

    return positionMap;
}
