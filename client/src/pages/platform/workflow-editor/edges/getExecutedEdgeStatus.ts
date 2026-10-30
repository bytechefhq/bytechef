import {WorkflowTestNodeStateI} from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {type Edge, type Node} from '@xyflow/react';

import {toConditionCaseKey} from './collectTakenEmptyConditionCases';

export function resolveTestStateNodeName(node?: Node): string | undefined {
    if (!node) {
        return undefined;
    }

    const data = node.data as {taskDispatcherId?: string; workflowNodeName?: string} | undefined;

    return data?.workflowNodeName ?? data?.taskDispatcherId ?? node.id;
}

export interface DisabledNodeBypassI {
    disabledTaskNames: Set<string>;
    edges: Edge[];
    nodes: Node[];
}

function isDisabledTaskNode(node: Node, disabledTaskNames: Set<string>): boolean {
    const data = node.data as {disabled?: boolean; workflowNodeName?: string} | undefined;

    if (!data?.workflowNodeName) {
        return false;
    }

    return Boolean(data.disabled) || disabledTaskNames.has(data.workflowNodeName);
}

function skipDisabledNodes(
    node: Node | undefined,
    direction: 'downstream' | 'upstream',
    {disabledTaskNames, edges, nodes}: DisabledNodeBypassI
): Node | undefined {
    const visitedNodeIds = new Set<string>();

    let currentNode = node;

    while (currentNode && isDisabledTaskNode(currentNode, disabledTaskNames) && !visitedNodeIds.has(currentNode.id)) {
        const currentNodeId = currentNode.id;

        visitedNodeIds.add(currentNodeId);

        const adjacentEdges = edges.filter((edge) =>
            direction === 'downstream' ? edge.source === currentNodeId : edge.target === currentNodeId
        );

        if (adjacentEdges.length !== 1) {
            return currentNode;
        }

        const adjacentNodeId = direction === 'downstream' ? adjacentEdges[0].target : adjacentEdges[0].source;

        const adjacentNode = nodes.find((candidateNode) => candidateNode.id === adjacentNodeId);

        if (!adjacentNode) {
            return currentNode;
        }

        currentNode = adjacentNode;
    }

    return currentNode;
}

export function bypassDisabledEdgeEndpoints(
    sourceNode: Node | undefined,
    targetNode: Node | undefined,
    disabledNodeBypass: DisabledNodeBypassI
): {sourceNode: Node | undefined; targetNode: Node | undefined} {
    return {
        sourceNode: skipDisabledNodes(sourceNode, 'upstream', disabledNodeBypass),
        targetNode: skipDisabledNodes(targetNode, 'downstream', disabledNodeBypass),
    };
}

function resolveNodeStatus(
    node: Node | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>
): WorkflowTestNodeStateI['status'] | undefined {
    const name = resolveTestStateNodeName(node);

    if (!name) {
        return undefined;
    }

    return workflowTestNodeStates[name]?.status;
}

const isPlaceholderNode = (node: Node | undefined): boolean =>
    node?.type === 'placeholder' || node?.type === 'readonlyPlaceholder';

export function isConditionPlaceholderNode(node: Node | undefined): boolean {
    return isPlaceholderNode(node) && Boolean((node?.data as {conditionId?: string} | undefined)?.conditionId);
}

function isTakenEmptyConditionCase(placeholderNode: Node | undefined, takenEmptyConditionCases?: Set<string>): boolean {
    const data = placeholderNode?.data as {conditionCase?: string; conditionId?: string} | undefined;

    if (!data?.conditionId || !data.conditionCase || !takenEmptyConditionCases) {
        return false;
    }

    return takenEmptyConditionCases.has(toConditionCaseKey(data.conditionId, data.conditionCase));
}

function hasOnlyEmptyConditionCases(node: Node | undefined): boolean {
    const parameters = (node?.data as {parameters?: {[key: string]: unknown}} | undefined)?.parameters;

    if (!parameters || !(CONDITION_CASE_TRUE in parameters || CONDITION_CASE_FALSE in parameters)) {
        return false;
    }

    return [parameters[CONDITION_CASE_TRUE], parameters[CONDITION_CASE_FALSE]].every(
        (caseTasks) => !Array.isArray(caseTasks) || caseTasks.length === 0
    );
}

export default function getExecutedEdgeStatus(
    sourceNode: Node | undefined,
    targetNode: Node | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>,
    takenEmptyConditionCases?: Set<string>
): 'COMPLETED' | 'FAILED' | undefined {
    if (isPlaceholderNode(sourceNode) || isPlaceholderNode(targetNode)) {
        const placeholderNode = isPlaceholderNode(sourceNode) ? sourceNode : targetNode;

        return isTakenEmptyConditionCase(placeholderNode, takenEmptyConditionCases) ? 'COMPLETED' : undefined;
    }

    if (targetNode?.type === 'taskDispatcherTopGhostNode' && hasOnlyEmptyConditionCases(sourceNode)) {
        return undefined;
    }

    const sourceStatus = resolveNodeStatus(sourceNode, workflowTestNodeStates);

    if (targetNode?.type === 'taskDispatcherBottomGhostNode') {
        return sourceStatus === 'COMPLETED' ? 'COMPLETED' : undefined;
    }

    if (sourceStatus !== 'COMPLETED' && sourceStatus !== 'FAILED') {
        return undefined;
    }

    const targetStatus = resolveNodeStatus(targetNode, workflowTestNodeStates);

    if (targetStatus === 'COMPLETED' || targetStatus === 'FAILED') {
        return 'COMPLETED';
    }

    return undefined;
}
