import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates the base Condition structure edges (Condition -> top ghost -> left placeholder -> right placeholder)
 */
function createBaseStructureEdges(conditionNode: Node): Edge[] {
    const conditionId = conditionNode.id;

    const topGhostNodeId = `${conditionId}-condition-top-ghost`;
    const bottomGhostNodeId = `${conditionId}-condition-bottom-ghost`;
    const caseTruePlaceholderNodeId = `${conditionId}-condition-left-placeholder-0`;
    const caseFalsePlaceholderNodeId = `${conditionId}-condition-right-placeholder-0`;

    const caseTrueSubtasks = (conditionNode.data as NodeDataType).parameters?.caseTrue;
    const caseFalseSubtasks = (conditionNode.data as NodeDataType).parameters?.caseFalse;

    const baseEdge = {
        style: EDGE_STYLES,
        type: 'smoothstep',
    };

    const edgeFromConditionToTopGhost = {
        id: `${conditionId}=>${topGhostNodeId}`,
        source: conditionId,
        target: topGhostNodeId,
        ...baseEdge,
    };

    const edges = [edgeFromConditionToTopGhost];

    if (!caseTrueSubtasks) {
        const edgeFromTopGhostToCaseTruePlaceholder = {
            id: `${topGhostNodeId}=>${caseTruePlaceholderNodeId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-left`,
            target: caseTruePlaceholderNodeId,
            ...baseEdge,
        };

        const edgeFromCaseTruePlaceholderToBottomGhost = {
            id: `${caseTruePlaceholderNodeId}=>${bottomGhostNodeId}`,
            source: caseTruePlaceholderNodeId,
            target: bottomGhostNodeId,
            targetHandle: `${bottomGhostNodeId}-left`,
            ...baseEdge,
        };

        edges.push(edgeFromTopGhostToCaseTruePlaceholder, edgeFromCaseTruePlaceholderToBottomGhost);
    }

    if (!caseFalseSubtasks) {
        const edgeFromTopGhostToCaseFalsePlaceholder = {
            id: `${topGhostNodeId}=>${caseFalsePlaceholderNodeId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-right`,
            target: caseFalsePlaceholderNodeId,
            ...baseEdge,
        };

        const edgeFromCaseFalsePlaceholderToBottomGhost = {
            id: `${caseFalsePlaceholderNodeId}=>${bottomGhostNodeId}`,
            source: caseFalsePlaceholderNodeId,
            target: bottomGhostNodeId,
            targetHandle: `${bottomGhostNodeId}-right`,
            ...baseEdge,
        };

        edges.push(edgeFromTopGhostToCaseFalsePlaceholder, edgeFromCaseFalsePlaceholderToBottomGhost);
    }

    return edges;
}

/**
 * Creates all edges for a specific branch
 */
function createBranchEdges(
    conditionId: string,
    branchTasks: WorkflowTask[],
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

/**
 * Create edge from condition to first node in a branch
 */
function createBranchStartEdge(
    conditionId: string,
    branchSubtasks: WorkflowTask[],
    conditionCase: 'left' | 'right',
    allNodes: Node[]
): Edge[] {
    const edges: Edge[] = [];
    const topGhostNodeId = `${conditionId}-condition-top-ghost`;
    const bottomGhostNodeId = `${conditionId}-condition-bottom-ghost`;

    if (branchSubtasks.length > 0) {
        const firstSubtaskId = branchSubtasks[0].name;

        const firstTaskNode = allNodes.find((node) => node.id === firstSubtaskId);

        if (firstTaskNode) {
            const edgeFromTopGhostToTask = {
                id: `${topGhostNodeId}=>${firstSubtaskId}`,
                source: topGhostNodeId,
                sourceHandle: `${topGhostNodeId}-${conditionCase}`,
                style: EDGE_STYLES,
                target: firstSubtaskId,
                type: 'workflow',
            };

            edges.push(edgeFromTopGhostToTask);
        }
    } else {
        const placeholderId = `${conditionId}-condition-${conditionCase}-placeholder-0`;

        const edgeFromTopGhostToPlaceholder = {
            id: `${topGhostNodeId}=>${placeholderId}`,
            source: topGhostNodeId,
            sourceHandle: `${topGhostNodeId}-${conditionCase}`,
            style: EDGE_STYLES,
            target: placeholderId,
            type: 'smoothstep',
        };

        edges.push(edgeFromTopGhostToPlaceholder);

        const edgeFromPlaceholderToBottomGhost = {
            id: `${placeholderId}=>${bottomGhostNodeId}`,
            source: placeholderId,
            style: EDGE_STYLES,
            target: bottomGhostNodeId,
            targetHandle: `${bottomGhostNodeId}-${conditionCase}`,
            type: 'smoothstep',
        };

        edges.push(edgeFromPlaceholderToBottomGhost);
    }

    return edges;
}

/**
 * Create edges between tasks in a branch
 */
function connectSequentialTasks(branchSubtasks: WorkflowTask[]): Edge[] {
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

/**
 * Create edge from last node in a branch to bottom ghost
 */
function createBranchExitEdge(
    conditionId: string,
    branchTasks: WorkflowTask[],
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

/**
 * Determine which branch (left or right) a condition is in
 */
export function getConditionBranchSide(
    conditionId: string,
    tasks: WorkflowTask[],
    parentConditionId: string
): 'left' | 'right' {
    const parentConditionTask = tasks?.find((task) => task.name === parentConditionId);

    if (!parentConditionTask) {
        return 'right';
    }

    const inTrueBranch = Array.isArray(parentConditionTask.parameters?.caseTrue)
        ? parentConditionTask.parameters.caseTrue.some((task: WorkflowTask) => task.name === conditionId)
        : false;

    return inTrueBranch ? 'left' : 'right';
}

/**
 * Check if a task is in any branch of a condition
 */
export function hasTaskInConditionBranches(conditionId: string, taskId: string, tasks: WorkflowTask[]): boolean {
    const condition = tasks?.find((task) => task.name === conditionId);

    if (!condition || !condition.parameters) {
        return false;
    }

    const caseTrueTasks = Array.isArray(condition.parameters.caseTrue) ? condition.parameters.caseTrue : [];
    const caseFalseTasks = Array.isArray(condition.parameters.caseFalse) ? condition.parameters.caseFalse : [];
    const allBranchTasks = [...caseTrueTasks, ...caseFalseTasks];

    return allBranchTasks.some((task) => task.name === taskId);
}

/**
 * Creates all edges for a condition node and its branches
 */
export default function createConditionEdges(conditionNode: Node, allNodes: Node[]): Edge[] {
    const edges: Edge[] = [];
    const conditionNodeData: NodeDataType = conditionNode.data as NodeDataType;
    const conditionId = conditionNode.id;

    const {parameters} = conditionNodeData;

    const baseStructureEdges = createBaseStructureEdges(conditionNode);

    edges.push(...baseStructureEdges);

    const caseTrueSubtasks: WorkflowTask[] = Array.isArray(parameters?.caseTrue) ? parameters.caseTrue : [];
    const caseFalseSubtasks: WorkflowTask[] = Array.isArray(parameters?.caseFalse) ? parameters.caseFalse : [];

    const trueEdges = createBranchEdges(conditionId, caseTrueSubtasks, 'left', allNodes);
    const falseEdges = createBranchEdges(conditionId, caseFalseSubtasks, 'right', allNodes);

    edges.push(...trueEdges);
    edges.push(...falseEdges);

    return edges;
}
