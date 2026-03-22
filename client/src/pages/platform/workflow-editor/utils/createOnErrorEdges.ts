import {
    CHILDLESS_TASK_DISPATCHER_NAMES,
    EDGE_STYLES,
    ON_ERROR_WIRE_KEY_ERROR_BRANCH,
    ON_ERROR_WIRE_KEY_MAIN_BRANCH,
    TASK_DISPATCHER_NAMES,
} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

function nestedBottomGhostIdForDispatcherTask(taskNodeId: string): string {
    const componentName = taskNodeId.split('_')[0];

    if (componentName === 'fork-join') {
        return `${taskNodeId}-forkJoin-bottom-ghost`;
    }

    if (componentName === 'on-error') {
        return `${taskNodeId}-onError-bottom-ghost`;
    }

    return `${taskNodeId}-${componentName}-bottom-ghost`;
}

/**
 * Creates placeholder edges for an empty on-error branch (top ghost -> placeholder -> bottom ghost).
 */
function createPlaceholderEdges(onErrorId: string, branchSide: 'left' | 'right'): Edge[] {
    const topGhostNodeId = `${onErrorId}-onError-top-ghost`;
    const bottomGhostNodeId = `${onErrorId}-onError-bottom-ghost`;
    const placeholderNodeId = `${onErrorId}-onError-${branchSide}-placeholder-0`;

    const baseEdge = {
        style: EDGE_STYLES,
        type: 'smoothstep',
    };

    return [
        {
            id: `${topGhostNodeId}=>${placeholderNodeId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-${branchSide}`,
            target: placeholderNodeId,
            ...baseEdge,
        },
        {
            id: `${placeholderNodeId}=>${bottomGhostNodeId}`,
            source: placeholderNodeId,
            target: bottomGhostNodeId,
            targetHandle: `${bottomGhostNodeId}-${branchSide}`,
            ...baseEdge,
        },
    ];
}

function createBranchEdges(
    onErrorId: string,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    const edgesFromOnErrorToFirstTask = createBranchStartEdge(onErrorId, branchTasks, branchSide, allNodes);

    edges.push(...edgesFromOnErrorToFirstTask);

    if (branchTasks.length > 1) {
        const edgeBetweenTaskNodes = connectSequentialTasks(branchTasks);

        edges.push(...edgeBetweenTaskNodes);
    }

    const edgeFromLastTaskNodeToBottomGhost = createBranchExitEdge(onErrorId, branchTasks, branchSide, allNodes)[0];

    if (!edgeFromLastTaskNodeToBottomGhost) {
        return edges;
    }

    if (
        edgeFromLastTaskNodeToBottomGhost.source === onErrorId ||
        edgeFromLastTaskNodeToBottomGhost.target.includes(onErrorId)
    ) {
        edges.push(edgeFromLastTaskNodeToBottomGhost);
    }

    return edges;
}

function createBranchStartEdge(
    onErrorId: string,
    branchSubtasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const topGhostNodeId = `${onErrorId}-onError-top-ghost`;
    const firstSubtaskId = branchSubtasks[0].name;
    const firstTaskNode = allNodes.find((node) => node.id === firstSubtaskId);

    if (!firstTaskNode) {
        return [];
    }

    return [
        {
            id: `${topGhostNodeId}=>${firstSubtaskId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-${branchSide}`,
            style: EDGE_STYLES,
            target: firstSubtaskId,
            type: 'workflow',
        },
    ];
}

function connectSequentialTasks(branchSubtasks: WorkflowTask[]): Edge[] {
    const edges: Edge[] = [];

    branchSubtasks.forEach((task, index) => {
        if (index < branchSubtasks.length - 1) {
            const sourceTaskNodeId = task.name;
            const targetTaskNodeId = branchSubtasks[index + 1].name;

            const sourceTaskComponentName = sourceTaskNodeId.split('_')[0];

            if (
                TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName) &&
                !CHILDLESS_TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName)
            ) {
                const nestedBottomGhostId = nestedBottomGhostIdForDispatcherTask(sourceTaskNodeId);

                const edgeFromNestedBottomGhostToNextSubtask = {
                    id: `${nestedBottomGhostId}=>${targetTaskNodeId}`,
                    source: nestedBottomGhostId,
                    sourceHandle: `${nestedBottomGhostId}-bottom`,
                    style: EDGE_STYLES,
                    target: targetTaskNodeId,
                    type: 'workflow',
                };

                edges.push(edgeFromNestedBottomGhostToNextSubtask);

                return;
            }

            const edgeBetweenTasks = {
                id: `${sourceTaskNodeId}=>${targetTaskNodeId}`,
                source: sourceTaskNodeId,
                style: EDGE_STYLES,
                target: targetTaskNodeId,
                type: 'workflow',
            };

            edges.push(edgeBetweenTasks);
        }
    });

    return edges;
}

function createBranchExitEdge(
    onErrorId: string,
    branchTasks: WorkflowTask[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    if (branchTasks.length > 0) {
        const lastTaskNodeId = branchTasks[branchTasks.length - 1].name;
        const bottomGhostNodeId = `${onErrorId}-onError-bottom-ghost`;
        const lastTaskNode = allNodes.find((node) => node.id === lastTaskNodeId);

        const lastTaskComponentName = lastTaskNodeId.split('_')[0];

        if (lastTaskNode?.data.taskDispatcher && !CHILDLESS_TASK_DISPATCHER_NAMES.includes(lastTaskComponentName)) {
            const nestedBottomGhostId = nestedBottomGhostIdForDispatcherTask(lastTaskNodeId);

            if (nestedBottomGhostId) {
                const nestedGhostToParentGhostEdge = {
                    id: `${nestedBottomGhostId}=>${bottomGhostNodeId}`,
                    source: nestedBottomGhostId,
                    style: EDGE_STYLES,
                    target: bottomGhostNodeId,
                    targetHandle: `${bottomGhostNodeId}-${branchSide}`,
                    type: 'workflow',
                };

                edges.push(nestedGhostToParentGhostEdge);
            }
        } else {
            const edgeFromLastTaskToBottomGhost = {
                id: `${lastTaskNodeId}=>${bottomGhostNodeId}`,
                source: lastTaskNodeId,
                style: EDGE_STYLES,
                target: bottomGhostNodeId,
                targetHandle: `${bottomGhostNodeId}-${branchSide}`,
                type: 'workflow',
            };

            edges.push(edgeFromLastTaskToBottomGhost);
        }
    }

    return edges;
}

/**
 * Determine which branch (left = main, right = on-error) an on-error child sits in.
 */
export function getOnErrorBranchSide(
    onErrorChildTaskId: string,
    tasks: WorkflowTask[],
    parentOnErrorId: string
): 'left' | 'right' {
    const parentOnErrorTask = tasks?.find((task) => task.name === parentOnErrorId);

    if (!parentOnErrorTask) {
        return 'right';
    }

    const mainBranch = parentOnErrorTask.parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH];
    const inMainBranch = Array.isArray(mainBranch)
        ? mainBranch.some((task: WorkflowTask) => task.name === onErrorChildTaskId)
        : false;

    return inMainBranch ? 'left' : 'right';
}

/**
 * Check if a task is in either branch of an on-error dispatcher.
 */
export function hasTaskInOnErrorBranches(onErrorId: string, taskId: string, tasks: WorkflowTask[]): boolean {
    const onErrorTask = tasks?.find((task) => task.name === onErrorId);

    if (!onErrorTask || !onErrorTask.parameters) {
        return false;
    }

    const mainBranchTasks = Array.isArray(onErrorTask.parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH])
        ? onErrorTask.parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH]
        : [];
    const errorBranchTasks = Array.isArray(onErrorTask.parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH])
        ? onErrorTask.parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH]
        : [];
    const allBranchTasks = [...mainBranchTasks, ...errorBranchTasks];

    return allBranchTasks.some((task) => task.name === taskId);
}

/**
 * Creates all edges for an on-error node and its branches.
 * Left-branch edges are inserted before right-branch edges (dagre ordering).
 */
export default function createOnErrorEdges(onErrorNode: Node, allNodes: Node[]): Edge[] {
    const edges: Edge[] = [];
    const onErrorNodeData: NodeDataType = onErrorNode.data as NodeDataType;
    const onErrorId = onErrorNode.id;
    const topGhostNodeId = `${onErrorId}-onError-top-ghost`;

    const {parameters} = onErrorNodeData;

    edges.push({
        id: `${onErrorId}=>${topGhostNodeId}`,
        source: onErrorId,
        style: EDGE_STYLES,
        target: topGhostNodeId,
        type: 'smoothstep',
    });

    const mainBranchSubtasks: WorkflowTask[] = Array.isArray(parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH])
        ? parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH]
        : [];
    const errorBranchSubtasks: WorkflowTask[] = Array.isArray(parameters?.[ON_ERROR_WIRE_KEY_ERROR_BRANCH])
        ? parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH]
        : [];

    if (mainBranchSubtasks.length > 0) {
        edges.push(...createBranchEdges(onErrorId, mainBranchSubtasks, 'left', allNodes));
    } else {
        edges.push(...createPlaceholderEdges(onErrorId, 'left'));
    }

    if (errorBranchSubtasks.length > 0) {
        edges.push(...createBranchEdges(onErrorId, errorBranchSubtasks, 'right', allNodes));
    } else {
        edges.push(...createPlaceholderEdges(onErrorId, 'right'));
    }

    return edges;
}
