import {Edge, Node} from '@xyflow/react';

import {NodeDataType, WorkflowTaskType} from '../types';
import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from './constants';

function createPlaceholderEdges(conditionId: string, branchSide: 'left' | 'right'): Edge[] {
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;
    const bottomGhostNodeId = `${conditionId}-condition-bottom-ghost`;
    const placeholderNodeId = `${conditionId}-condition-${branchSide}-placeholder-0`;

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
    conditionId: string,
    branchTasks: WorkflowTaskType[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    const edgesFromConditionToFirstTask = createBranchStartEdge(conditionId, branchTasks, branchSide, allNodes);

    edges.push(...edgesFromConditionToFirstTask);

    if (branchTasks.length > 1) {
        const edgeBetweenTaskNodes = connectSequentialTasks(branchTasks);

        edges.push(...edgeBetweenTaskNodes);
    }

    const edgeFromLastTaskNodeToBottomGhost = createBranchExitEdge(conditionId, branchTasks, branchSide, allNodes)[0];

    if (!edgeFromLastTaskNodeToBottomGhost) {
        return edges;
    }

    if (
        edgeFromLastTaskNodeToBottomGhost.source === conditionId ||
        edgeFromLastTaskNodeToBottomGhost.target.includes(conditionId)
    ) {
        edges.push(edgeFromLastTaskNodeToBottomGhost);
    }

    return edges;
}

function createBranchStartEdge(
    conditionId: string,
    branchSubtasks: WorkflowTaskType[],
    conditionCase: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;
    const firstSubtaskId = branchSubtasks[0].name;
    const firstTaskNode = allNodes.find((node) => node.id === firstSubtaskId);

    if (!firstTaskNode) {
        return [];
    }

    return [
        {
            id: `${topGhostNodeId}=>${firstSubtaskId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-${conditionCase}`,
            style: EDGE_STYLES,
            target: firstSubtaskId,
            type: 'workflow',
        },
    ];
}

function connectSequentialTasks(branchSubtasks: WorkflowTaskType[]): Edge[] {
    const edges: Edge[] = [];

    branchSubtasks.forEach((task, index) => {
        if (index < branchSubtasks.length - 1) {
            const sourceTaskNodeId = task.name;
            const targetTaskNodeId = branchSubtasks[index + 1].name;

            const sourceTaskComponentName = task.name.split('_')[0];

            if (TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName)) {
                const nestedBottomGhostId = `${sourceTaskNodeId}-${sourceTaskComponentName}-bottom-ghost`;

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
    conditionId: string,
    branchTasks: WorkflowTaskType[],
    branchSide: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];

    if (branchTasks.length > 0) {
        const lastTaskNodeId = branchTasks[branchTasks.length - 1].name;
        const bottomGhostNodeId = `${conditionId}-condition-bottom-ghost`;
        const lastTaskNode = allNodes.find((node) => node.id === lastTaskNodeId);

        const lastTaskComponentName = lastTaskNodeId.split('_')[0];

        if (lastTaskNode?.data.taskDispatcher && !CHILDLESS_TASK_DISPATCHER_NAMES.includes(lastTaskComponentName)) {
            const nestedBottomGhostId = `${lastTaskNodeId}-${lastTaskComponentName}-bottom-ghost`;

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

export function getConditionBranchSide(
    conditionId: string,
    tasks: WorkflowTaskType[],
    parentConditionId: string
): 'left' | 'right' {
    const parentConditionTask = tasks?.find((task) => task.name === parentConditionId);

    if (!parentConditionTask) {
        return 'right';
    }

    const inTrueBranch = Array.isArray(parentConditionTask.parameters?.caseTrue)
        ? parentConditionTask.parameters.caseTrue.some((task: WorkflowTaskType) => task.name === conditionId)
        : false;

    return inTrueBranch ? 'left' : 'right';
}

export function hasTaskInConditionBranches(conditionId: string, taskId: string, tasks: WorkflowTaskType[]): boolean {
    const condition = tasks?.find((task) => task.name === conditionId);

    if (!condition || !condition.parameters) {
        return false;
    }

    const caseTrueTasks = Array.isArray(condition.parameters.caseTrue) ? condition.parameters.caseTrue : [];
    const caseFalseTasks = Array.isArray(condition.parameters.caseFalse) ? condition.parameters.caseFalse : [];
    const allBranchTasks = [...caseTrueTasks, ...caseFalseTasks];

    return allBranchTasks.some((task) => task.name === taskId);
}

export default function createConditionEdges(conditionNode: Node, allNodes: Node[]): Edge[] {
    const edges: Edge[] = [];
    const conditionNodeData: NodeDataType = conditionNode.data as NodeDataType;
    const conditionId = conditionNode.id;
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;

    const {parameters} = conditionNodeData;

    edges.push({
        id: `${conditionId}=>${topGhostNodeId}`,
        source: conditionId,
        style: EDGE_STYLES,
        target: topGhostNodeId,
        type: 'smoothstep',
    });

    const caseTrueSubtasks: WorkflowTaskType[] = Array.isArray(parameters?.caseTrue) ? parameters.caseTrue : [];
    const caseFalseSubtasks: WorkflowTaskType[] = Array.isArray(parameters?.caseFalse) ? parameters.caseFalse : [];

    if (caseTrueSubtasks.length > 0) {
        edges.push(...createBranchEdges(conditionId, caseTrueSubtasks, 'left', allNodes));
    } else {
        edges.push(...createPlaceholderEdges(conditionId, 'left'));
    }

    if (caseFalseSubtasks.length > 0) {
        edges.push(...createBranchEdges(conditionId, caseFalseSubtasks, 'right', allNodes));
    } else {
        edges.push(...createPlaceholderEdges(conditionId, 'right'));
    }

    return edges;
}
