import {EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates the base Branch structure edges (branch -> top ghost -> placeholder -> bottom ghost)
 */
function createBaseBranchStructureEdges(branchId: string): Edge[] {
    const topGhostId = `${branchId}-branch-top-ghost`;

    const edgeFromBranchToTopGhost = {
        id: `${branchId}=>${topGhostId}`,
        source: branchId,
        style: EDGE_STYLES,
        target: topGhostId,
        type: 'smoothstep',
    };

    return [edgeFromBranchToTopGhost];
}

/**
 * Creates edges for a single case within the branch
 * Handles both cases with tasks and empty cases with placeholders
 */
function createEdgesForSingleCase(
    branchId: string,
    caseKey: string | number,
    caseTasks: WorkflowTask[],
    handlePosition: 'left' | 'middle' | 'right'
): Edge[] {
    const edges: Edge[] = [];
    const topGhostId = `${branchId}-branch-top-ghost`;
    const bottomGhostId = `${branchId}-branch-bottom-ghost`;
    const topGhostHandlePosition = handlePosition === 'middle' ? 'bottom' : handlePosition;
    const bottomGhostHandlePosition = handlePosition === 'middle' ? 'top' : handlePosition;

    if (!caseTasks || caseTasks.length === 0) {
        const placeholderId = `${branchId}-branch-${caseKey}-placeholder-0`;
        const isMiddle = handlePosition === 'middle';

        const edgeFromTopGhostToPlaceholder = {
            ...(isMiddle ? {data: {isMiddleCase: true}} : {}),
            id: `${topGhostId}=>${placeholderId}`,
            source: topGhostId,
            sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
            style: EDGE_STYLES,
            target: placeholderId,
            type: 'labeledBranchCase',
        };

        const edgeFromPlaceholderToBottomGhost = {
            ...(isMiddle ? {data: {isMiddleCase: true}} : {}),
            id: `${placeholderId}=>${bottomGhostId}`,
            source: placeholderId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: isMiddle ? 'workflow' : 'smoothstep',
        };

        edges.push(edgeFromTopGhostToPlaceholder, edgeFromPlaceholderToBottomGhost);

        return edges;
    }

    const firstTaskId = caseTasks[0].name;
    const isMiddle = handlePosition === 'middle';

    const edgeFromTopGhostToFirstTask = {
        ...(isMiddle ? {data: {isMiddleCase: true}} : {}),
        id: `${topGhostId}=>${firstTaskId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
        style: EDGE_STYLES,
        target: firstTaskId,
        type: 'workflow',
    };

    edges.push(edgeFromTopGhostToFirstTask);

    caseTasks.forEach((caseTask, index) => {
        const sourceTaskId = caseTask.name;
        const targetTaskId = caseTasks[index + 1]?.name;

        if (!targetTaskId) {
            return;
        }

        const sourceTaskComponentName = sourceTaskId.split('_')[0];

        if (TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName) && sourceTaskComponentName !== 'loopBreak') {
            const nestedBottomGhostId = `${sourceTaskId}-${sourceTaskComponentName}-bottom-ghost`;

            const edgeFromNestedGhostToNextTask = {
                id: `${nestedBottomGhostId}=>${targetTaskId}`,
                source: nestedBottomGhostId,
                style: EDGE_STYLES,
                target: targetTaskId,
                type: 'workflow',
            };

            edges.push(edgeFromNestedGhostToNextTask);
        } else {
            const edgeBetweenTasks = {
                id: `${sourceTaskId}=>${targetTaskId}`,
                source: sourceTaskId,
                style: EDGE_STYLES,
                target: targetTaskId,
                type: 'workflow',
            };

            edges.push(edgeBetweenTasks);
        }
    });

    const lastTaskId = caseTasks[caseTasks.length - 1].name;
    const lastTaskComponentName = lastTaskId.split('_')[0];

    if (TASK_DISPATCHER_NAMES.includes(lastTaskComponentName) && lastTaskComponentName !== 'loopBreak') {
        const nestedBottomGhostId = `${lastTaskId}-${lastTaskComponentName}-bottom-ghost`;

        const edgeFromNestedGhostToBottomGhost = {
            ...(isMiddle ? {data: {isMiddleCase: true}} : {}),
            id: `${nestedBottomGhostId}=>${bottomGhostId}`,
            source: nestedBottomGhostId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${handlePosition}`,
            type: 'workflow',
        };

        edges.push(edgeFromNestedGhostToBottomGhost);
    } else {
        const edgeFromLastTaskToBottomGhost = {
            ...(isMiddle ? {data: {isMiddleCase: true}} : {}),
            id: `${lastTaskId}=>${bottomGhostId}`,
            source: lastTaskId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: 'workflow',
        };

        edges.push(edgeFromLastTaskToBottomGhost);
    }

    return edges;
}

/**
 * Distributes cases into left, middle, and right groups
 */
function distributeCases(allCases: Array<{key: string; tasks: WorkflowTask[]}>): {
    leftCases: typeof allCases;
    middleCase: (typeof allCases)[0] | null;
    rightCases: typeof allCases;
} {
    const isEvenCount = allCases.length % 2 === 0;

    if (isEvenCount) {
        const halfPoint = allCases.length / 2;

        return {
            leftCases: allCases.slice(0, halfPoint),
            middleCase: null,
            rightCases: allCases.slice(halfPoint),
        };
    } else {
        const middleIndex = Math.floor(allCases.length / 2);

        return {
            leftCases: allCases.slice(0, middleIndex),
            middleCase: allCases[middleIndex],
            rightCases: allCases.slice(middleIndex + 1),
        };
    }
}

/**
 * Creates all edges for branch and its subtasks
 */
export default function createBranchEdges(branchNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData = branchNode.data as NodeDataType;
    const branchId = branchNode.id;

    const baseStructureEdges = createBaseBranchStructureEdges(branchId);

    edges.push(...baseStructureEdges);

    const defaultCase = {
        key: 'default',
        tasks: nodeData.parameters?.default || [],
    };

    const customCases = (nodeData.parameters?.cases || []).map((caseItem: BranchCaseType) => ({
        key: caseItem.key,
        tasks: caseItem.tasks || [],
    }));

    const allCases = [defaultCase, ...customCases];

    // For completely empty branch, ensure we have at least a template case
    if (allCases.length === 1 && allCases[0].tasks.length === 0) {
        allCases.push({key: 'case_0', tasks: []});
    }

    // Distribute cases
    const {leftCases, middleCase, rightCases} = distributeCases(allCases);

    leftCases.forEach((caseItem) => {
        const caseEdges = createEdgesForSingleCase(branchId, caseItem.key, caseItem.tasks, 'left');

        edges.push(...caseEdges);
    });

    if (middleCase) {
        const caseEdges = createEdgesForSingleCase(branchId, middleCase.key, middleCase.tasks, 'middle');

        edges.push(...caseEdges);
    }

    rightCases.forEach((caseItem) => {
        const caseEdges = createEdgesForSingleCase(branchId, caseItem.key, caseItem.tasks, 'right');

        edges.push(...caseEdges);
    });

    return edges;
}
