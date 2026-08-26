import {getGraphNodeName} from '@/shared/components/workflow-executions/util/toGraphNodeVisits';
import {TaskExecution, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';

import {GraphNodeExecutionI} from './getExecutedEdgeStatus';

/**
 * Finds THE FIRST task execution of one workflow node in the run's execution tree. A graph can be
 * nested inside another dispatcher, so its own execution is not necessarily a top-level one — a
 * condition carries it under `children`, a loop under one of its `iterations`.
 *
 * Known limitation: a dispatcher that runs its body more than once produces one execution of the
 * graph PER iteration, and this returns only the first. A graph inside a loop therefore paints
 * iteration 1's routing for the whole run. Picking a single iteration to highlight is the real
 * problem there — the canvas draws ONE frame per graph, so it has nowhere to show the others — and
 * an iteration picker is out of scope here.
 */
function findTaskExecution(taskExecutions: TaskExecution[], workflowNodeName: string): TaskExecution | undefined {
    for (const taskExecution of taskExecutions) {
        if (taskExecution.workflowTask?.name === workflowNodeName) {
            return taskExecution;
        }

        const nested = findTaskExecution(
            [...(taskExecution.children ?? []), ...(taskExecution.iterations ?? []).flat()],
            workflowNodeName
        );

        if (nested) {
            return nested;
        }
    }

    return undefined;
}

/**
 * Reads every node dispatch a `graph/v1` frame made during a test run, in the order the server
 * returned them. Each child of the graph's own task execution is exactly one visit of one member —
 * `GraphTaskUtils#dispatchNodeTask` creates one sub task execution per routing decision — so this is
 * the run's routing history, which is what tells `getExecutedGraphTransitionStatus` which
 * transitions were actually taken.
 *
 * Empty whenever nothing ran, the graph never ran, or the run's payload carries no children: an
 * un-highlighted transition is the correct rendering for "we do not know", and the canvas paints one
 * either way.
 */
export default function collectGraphNodeExecutions(
    workflowTestExecution: WorkflowTestExecution | undefined,
    graphId: string
): GraphNodeExecutionI[] {
    const graphTaskExecution = findTaskExecution(workflowTestExecution?.job?.taskExecutions ?? [], graphId);

    return (graphTaskExecution?.children ?? []).map((child) => ({
        // `__node` is the dispatcher's own stamp and the authority; the task's name matches it for
        // every graph the editor writes, and is the fallback for anything hand-edited. A dispatch
        // that answers to neither is named '' DELIBERATELY, unlike the visit rows in
        // `toGraphNodeVisits`, which label the same case 'unknown': an unidentifiable execution must
        // match no transition endpoint, and `getExecutedGraphTransitionStatus` rejects an empty
        // `from`/`to` so it cannot.
        nodeName: getGraphNodeName(child) ?? child.workflowTask?.name ?? '',
        startDate: child.startDate,
        status: child.status,
    }));
}
