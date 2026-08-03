import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';

/**
 * The graph task dispatcher stamps every dispatched child task's {@code parameters.__node} with the name of the
 * graph node it belongs to (see server `GraphTaskUtils.stampNode`/`dispatchNodeTask`). This is the client-side
 * mirror of that constant.
 */
const NODE_PARAMETER_KEY = '__node';

/** The graph task dispatcher's own task type prefix, e.g. `graph/v1`. */
const GRAPH_TYPE_PREFIX = 'graph/';

/** A single contiguous run of a graph node's task executions -- one dispatch of that node's task list. */
export interface GraphNodeVisitI {
    nodeName: string;
    taskExecutions: TaskExecution[];
    visitNumber: number;
}

export function isGraphTaskExecution(taskExecution: TaskExecution): boolean {
    return !!taskExecution.type?.startsWith(GRAPH_TYPE_PREFIX);
}

function getNodeName(taskExecution: TaskExecution): string | undefined {
    const parameters = taskExecution.workflowTask?.parameters;

    if (!parameters || typeof parameters !== 'object') {
        return undefined;
    }

    const nodeName = (parameters as Record<string, unknown>)[NODE_PARAMETER_KEY];

    return typeof nodeName === 'string' ? nodeName : undefined;
}

/**
 * Groups a graph task execution's flat, chronologically-ordered children into per-visit blocks, preserving
 * transition order across revisits of the same node.
 *
 * A new visit starts whenever the child's `taskNumber` is `1` -- the graph task dispatcher always stamps the first
 * task it dispatches for a node (whether the graph's start node, a `next` transition target, or a self-loop back to
 * the same node) with `taskNumber` `1`, and increments from there only for additional tasks within that SAME node
 * visit (see server `GraphTaskUtils#dispatchNodeTask`). Node CHANGE alone can't be used as the boundary signal,
 * because a self-loop keeps the node name identical across consecutive visits.
 *
 * Children missing a `__node` stamp (defensive: should not happen for a real graph child) fall back to their own
 * singleton visit labeled with the task's own name, so nothing is silently dropped from the timeline.
 */
export function groupGraphChildTaskExecutions(children: TaskExecution[]): GraphNodeVisitI[] {
    const visits: GraphNodeVisitI[] = [];
    const visitCountByNodeName = new Map<string, number>();

    for (const child of children) {
        const nodeName = getNodeName(child) ?? child.workflowTask?.name ?? 'unknown';
        const startsNewVisit = visits.length === 0 || child.taskNumber === 1;

        if (startsNewVisit) {
            const visitNumber = (visitCountByNodeName.get(nodeName) ?? 0) + 1;

            visitCountByNodeName.set(nodeName, visitNumber);

            visits.push({nodeName, taskExecutions: [child], visitNumber});
        } else {
            visits[visits.length - 1].taskExecutions.push(child);
        }
    }

    return visits;
}
