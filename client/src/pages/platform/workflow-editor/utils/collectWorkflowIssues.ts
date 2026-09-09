import {WorkflowInput, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';

import {WorkflowIssueI} from '../stores/useWorkflowIssuesStore';
import getDuplicateNodeNames from './getDuplicateNodeNames';
import {forEachNestedTaskGroup} from './taskTraversalUtils';

const DATA_PILL_PATTERN = /\$\{([^}]+)}/g;
const REFERENCE_ROOT_PATTERN = /^([a-zA-Z_][a-zA-Z0-9_]*)/;
const NESTED_TASK_KEYS = new Set([
    'branches',
    'caseFalse',
    'caseTrue',
    'cases',
    'default',
    'iteratee',
    'main-branch',
    'on-error-branch',
    'tasks',
]);

interface CollectWorkflowIssuesProps {
    inputs?: Array<Pick<WorkflowInput, 'name'>>;
    tasks?: Array<WorkflowTask>;
    triggers?: Array<WorkflowTrigger>;
}

function collectNestedTasks(
    tasks: Array<WorkflowTask>,
    collectedTasks: Array<WorkflowTask>,
    collectedNames: Set<string>
): void {
    for (const currentTask of tasks) {
        if (!collectedNames.has(currentTask.name)) {
            collectedNames.add(currentTask.name);

            collectedTasks.push(currentTask);
        }

        if (currentTask.parameters) {
            forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, (nestedTasks) =>
                collectNestedTasks(nestedTasks, collectedTasks, collectedNames)
            );
        }
    }
}

function collectTasks(tasks: Array<WorkflowTask>, collectedTasks: Array<WorkflowTask>): void {
    const collectedNames = new Set<string>();

    for (const currentTask of tasks) {
        collectedTasks.push(currentTask);
        collectedNames.add(currentTask.name);
    }

    for (const currentTask of tasks) {
        if (currentTask.parameters) {
            forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, (nestedTasks) =>
                collectNestedTasks(nestedTasks, collectedTasks, collectedNames)
            );
        }
    }
}

function collectExpressions(value: unknown, expressions: Array<string>): void {
    if (typeof value === 'string') {
        for (const match of value.matchAll(DATA_PILL_PATTERN)) {
            expressions.push(match[1]);
        }

        return;
    }

    if (Array.isArray(value)) {
        for (const item of value) {
            collectExpressions(item, expressions);
        }

        return;
    }

    if (value && typeof value === 'object') {
        for (const [key, nestedValue] of Object.entries(value)) {
            if (!NESTED_TASK_KEYS.has(key)) {
                collectExpressions(nestedValue, expressions);
            }
        }
    }
}

export default function collectWorkflowIssues({
    inputs = [],
    tasks = [],
    triggers = [],
}: CollectWorkflowIssuesProps): Array<WorkflowIssueI> {
    const allTasks: Array<WorkflowTask> = [];

    collectTasks(tasks, allTasks);

    const knownNames = new Set<string>([
        ...triggers.map((currentTrigger) => currentTrigger.name),
        ...allTasks.map((currentTask) => currentTask.name),
        ...inputs.map((input) => input.name),
    ]);

    const issues: Array<WorkflowIssueI> = getDuplicateNodeNames(allTasks, triggers).map((duplicateNodeName) => ({
        kind: 'DUPLICATE_NODE_NAME',
        message: `Node names must be unique. Duplicate node name: ${duplicateNodeName}`,
        nodeName: duplicateNodeName,
        severity: 'ERROR',
        source: 'SWEEP',
    }));

    for (const currentTask of allTasks) {
        const expressions: Array<string> = [];
        const reportedExpressions = new Set<string>();

        collectExpressions(currentTask.parameters, expressions);

        for (const expression of expressions) {
            const rootMatch = REFERENCE_ROOT_PATTERN.exec(expression);

            if (!rootMatch || reportedExpressions.has(expression)) {
                continue;
            }

            const isFunctionCall = expression[rootMatch[0].length] === '(';

            if (isFunctionCall || knownNames.has(rootMatch[1])) {
                continue;
            }

            reportedExpressions.add(expression);

            issues.push({
                kind: 'BROKEN_REFERENCE',
                message: `"${rootMatch[1]}" is missing from the workflow (referenced as ${expression})`,
                nodeName: currentTask.name,
                propertyPath: expression,
                severity: 'ERROR',
                source: 'SWEEP',
            });
        }
    }

    return issues;
}
