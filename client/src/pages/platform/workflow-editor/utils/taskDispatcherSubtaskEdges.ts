import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {Edge, Node} from '@xyflow/react';

import {getNestedBottomGhostId, getWorkflowTaskComponentName, isNestingTaskDispatcher} from './workflowNodeNameUtils';

export function getSubtaskExitNodeId(task: WorkflowTask): string {
    const componentName = getWorkflowTaskComponentName(task);

    if (isNestingTaskDispatcher(componentName)) {
        return getNestedBottomGhostId(task.name, componentName);
    }

    return task.name;
}

export function createSubtaskChainEdges(subtasks: WorkflowTask[], nestedExitHandlePosition?: string): Edge[] {
    const edges: Edge[] = [];

    subtasks.forEach((subtask, index) => {
        const targetTaskNodeId = subtasks[index + 1]?.name;

        if (!targetTaskNodeId) {
            return;
        }

        const sourceNodeId = getSubtaskExitNodeId(subtask);
        const leavesNestedDispatcher = sourceNodeId !== subtask.name;

        edges.push({
            id: `${sourceNodeId}=>${targetTaskNodeId}`,
            source: sourceNodeId,
            ...(leavesNestedDispatcher && nestedExitHandlePosition
                ? {sourceHandle: `${sourceNodeId}-${nestedExitHandlePosition}`}
                : {}),
            style: EDGE_STYLES,
            target: targetTaskNodeId,
            type: 'workflow',
        });
    });

    return edges;
}

export function createBranchExitEdges(
    bottomGhostNodeId: string,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    if (branchTasks.length === 0) {
        return [];
    }

    const lastTask = branchTasks[branchTasks.length - 1];
    const lastTaskNode = allNodes.find((node) => node.id === lastTask.name);

    const lastTaskComponentName = getWorkflowTaskComponentName(lastTask);

    const sourceNodeId =
        lastTaskNode?.data.taskDispatcher && !CHILDLESS_TASK_DISPATCHER_NAMES.includes(lastTaskComponentName)
            ? getNestedBottomGhostId(lastTask.name, lastTaskComponentName)
            : lastTask.name;

    return [
        {
            id: `${sourceNodeId}=>${bottomGhostNodeId}`,
            source: sourceNodeId,
            style: EDGE_STYLES,
            target: bottomGhostNodeId,
            targetHandle: `${bottomGhostNodeId}-${branchSide}`,
            type: 'workflow',
        },
    ];
}
