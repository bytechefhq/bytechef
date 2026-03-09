import {Workflow} from '@/shared/middleware/platform/configuration';

import {getTask} from './getTask';

/**
 * Resolves the current label for a workflow node by reading from the authoritative
 * workflow definition (tasks/triggers). Falls back to the provided fallback label
 * (typically from React Flow node data) when the node is not found in the workflow.
 *
 * This is necessary because useLayout intentionally skips re-rendering React Flow nodes
 * for decorative-only changes (like label edits) as a performance optimization,
 * which can leave node data stale.
 */
export function getNodeLabel({
    fallbackLabel,
    workflow,
    workflowNodeName,
}: {
    fallbackLabel?: string;
    workflow: Workflow;
    workflowNodeName: string;
}): string | undefined {
    const workflowTaskOrTrigger =
        workflow.triggers?.find((trigger) => trigger.name === workflowNodeName) ||
        getTask({tasks: workflow.tasks || [], workflowNodeName});

    return workflowTaskOrTrigger?.label || fallbackLabel;
}
