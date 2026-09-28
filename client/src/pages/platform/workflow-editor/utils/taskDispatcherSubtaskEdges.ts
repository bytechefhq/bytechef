import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {Edge, Node} from '@xyflow/react';

import {getNestedBottomGhostId, getWorkflowNodeComponentName} from './workflowNodeNameUtils';

export function getSubtaskExitNodeId(taskNodeId: string): string {
    const componentName = getWorkflowNodeComponentName(taskNodeId);

    if (TASK_DISPATCHER_NAMES.includes(componentName) && !CHILDLESS_TASK_DISPATCHER_NAMES.includes(componentName)) {
        return getNestedBottomGhostId(taskNodeId);
    }

    return taskNodeId;
}

export function createSubtaskChainEdges(subtasks: WorkflowTask[], nestedExitHandlePosition?: string): Edge[] {
    const edges: Edge[] = [];

    subtasks.forEach((subtask, index) => {
        const targetTaskNodeId = subtasks[index + 1]?.name;

        if (!targetTaskNodeId) {
            return;
        }

        const sourceNodeId = getSubtaskExitNodeId(subtask.name);
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

    const lastTaskNodeId = branchTasks[branchTasks.length - 1].name;
    const lastTaskNode = allNodes.find((node) => node.id === lastTaskNodeId);

    const lastTaskComponentName = getWorkflowNodeComponentName(lastTaskNodeId);

    const sourceNodeId =
        lastTaskNode?.data.taskDispatcher && !CHILDLESS_TASK_DISPATCHER_NAMES.includes(lastTaskComponentName)
            ? getNestedBottomGhostId(lastTaskNodeId)
            : lastTaskNodeId;

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
