import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';

/**
 * The graph task dispatcher stamps every dispatched child task's {@code parameters.__node} with the name of the
 * graph node it belongs to (see server `GraphTaskUtils.stampNode`/`dispatchNodeTask`). This is the client-side
 * mirror of that constant.
 */
export const GRAPH_NODE_PARAMETER_KEY = '__node';

/** The graph task dispatcher's own task type prefix, e.g. `graph/v1`. */
const GRAPH_TYPE_PREFIX = 'graph/';

/** One dispatch of one graph node -- one visit of it, in the order the run made them. */
export interface GraphNodeVisitI {
    nodeName: string;
    taskExecution: TaskExecution;
    visitNumber: number;
}

export function isGraphTaskExecution(taskExecution: TaskExecution): boolean {
    return !!taskExecution.type?.startsWith(GRAPH_TYPE_PREFIX);
}

/**
 * The only part of a task execution the `__node` stamp lives on. Declared structurally rather than as
 * `TaskExecution` so the editor's `collectGraphNodeExecutions` can call this with the SEPARATE
 * generated model it works in (`platform/workflow/test`) instead of re-implementing the read.
 */
export interface GraphNodeStampedTaskExecutionI {
    workflowTask?: {parameters?: {[key: string]: unknown}};
}

export function getGraphNodeName(taskExecution: GraphNodeStampedTaskExecutionI): string | undefined {
    const parameters = taskExecution.workflowTask?.parameters;

    if (!parameters || typeof parameters !== 'object') {
        return undefined;
    }

    const nodeName = (parameters as Record<string, unknown>)[GRAPH_NODE_PARAMETER_KEY];

    return typeof nodeName === 'string' ? nodeName : undefined;
}

/**
 * Turns a graph task execution's flat, chronologically-ordered children into the visit timeline the execution view
 * lists.
 *
 * The mapping is one-to-one: a graph node IS a single task, dispatched once per visit
 * (`GraphTaskUtils#dispatchNodeTask` builds exactly one sub task execution per routing decision, stamped
 * `taskNumber` 1), so there is no such thing as a multi-task visit to collapse. What the pass still carries is the
 * per-node visit COUNT, which a cycle makes meaningful: revisiting the same node -- whether through a self-loop or
 * a longer cycle back to it -- produces `node (visit 2)`, `node (visit 3)`, and so on.
 *
 * Children missing a `__node` stamp (defensive: should not happen for a real graph child) fall back to the task's
 * own name, and then to the literal `'unknown'`, so nothing is silently dropped from the timeline. Note that
 * `collectGraphNodeExecutions` -- the editor-side reader that shares `getGraphNodeName` with this one -- falls back
 * to `''` for the same case instead: a visit row needs SOMETHING to label itself with, whereas a nameless
 * execution must match no transition endpoint at all, which `getExecutedGraphTransitionStatus` enforces by
 * rejecting an empty `from`/`to`.
 */
export function toGraphNodeVisits(children: TaskExecution[]): GraphNodeVisitI[] {
    const visitCountByNodeName = new Map<string, number>();

    return children.map((child) => {
        const nodeName = getGraphNodeName(child) ?? child.workflowTask?.name ?? 'unknown';
        const visitNumber = (visitCountByNodeName.get(nodeName) ?? 0) + 1;

        visitCountByNodeName.set(nodeName, visitNumber);

        return {nodeName, taskExecution: child, visitNumber};
    });
}
