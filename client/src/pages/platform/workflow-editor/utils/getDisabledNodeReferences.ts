import {isWorkflowTask} from './flattenDefinitionTasks';

const EXPRESSION_PATTERN = /\$\{([^}]*)\}/g;

/**
 * Advisory-only scan: returns the distinct disabled task names referenced inside any
 * `${...}` expression found in string values of `parameters` (recursing into nested
 * objects/arrays). A mention of a disabled name in plain prose outside `${...}` is not a
 * reference. Never blocks saving or running -- it only surfaces that the expression will
 * resolve to null at runtime because the referenced task is skipped.
 *
 * Deliberately does NOT descend into nested tasks (e.g. condition.caseTrue, loop.iteratee,
 * branch.cases[].tasks, fork-join.branches): a task-dispatcher's `parameters` contains its
 * entire descendant subtree, and a reference made by a descendant's own parameters belongs to
 * that descendant's own warning, not its ancestor's -- otherwise one real reference would fire a
 * duplicate warning on every dispatcher between it and the workflow root.
 */
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

/**
 * A list of tasks (condition.caseTrue, loop.iteratee, parallel.tasks) or a list of lists of
 * tasks (fork-join.branches). Both shapes hold nested descendant tasks, not this node's own
 * configuration. `branch.cases` is a list of `{key, tasks}` objects rather than a list of tasks
 * itself, so it is not caught here -- its non-`tasks` fields (e.g. `key`) still get scanned
 * normally, and its nested `tasks` array is skipped when the recursion reaches it directly.
 */
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

        const escapedTaskName = disabledTaskName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const referencePattern = new RegExp(`(^|[^\\w])${escapedTaskName}([^\\w]|$)`);

        if (referencePattern.test(expressionBody)) {
            referencedTaskNames.add(disabledTaskName);
        }
    }
}
