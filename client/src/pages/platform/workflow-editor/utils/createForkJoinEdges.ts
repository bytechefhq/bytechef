import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates edges for the left ghost node in a fork-join task
 */
function createEdgesForLeftGhost(forkJoinId: string): Edge[] {
    const topGhostId = `${forkJoinId}-forkJoin-top-ghost`;
    const leftGhostId = `${forkJoinId}-taskDispatcher-left-ghost`;
    const bottomGhostId = `${forkJoinId}-forkJoin-bottom-ghost`;

    const edgeFromTopGhostToLeftGhost = {
        id: `${topGhostId}=>${leftGhostId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-left`,
        style: EDGE_STYLES,
        target: leftGhostId,
        type: 'smoothstep',
    };

    const edgeFromLeftGhostToBottomGhost = {
        id: `${leftGhostId}=>${bottomGhostId}`,
        source: leftGhostId,
        style: EDGE_STYLES,
        target: bottomGhostId,
        targetHandle: `${bottomGhostId}-left`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToLeftGhost, edgeFromLeftGhostToBottomGhost];
}

/**
 * Creates edges for the placeholder node in a fork-join task
 */
function createEdgesForPlaceholder(forkJoinId: string, branchCount: number): Edge[] {
    const topGhostId = `${forkJoinId}-forkJoin-top-ghost`;
    const bottomGhostId = `${forkJoinId}-forkJoin-bottom-ghost`;
    const placeholderId = `${forkJoinId}-forkJoin-placeholder-${branchCount}`;

    const edgeFromTopGhostToPlaceholder = {
        id: `${topGhostId}=>${placeholderId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-right`,
        style: EDGE_STYLES,
        target: placeholderId,
        type: 'smoothstep',
    };

    const edgeFromPlaceholderToBottomGhost = {
        id: `${placeholderId}=>${bottomGhostId}`,
        source: placeholderId,
        style: EDGE_STYLES,
        target: bottomGhostId,
        targetHandle: `${bottomGhostId}-right`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToPlaceholder, edgeFromPlaceholderToBottomGhost];
}

// Helper to create edges for a single fork-join branch (lane)
function createForkJoinTaskEdges(
    forkJoinId: string,
    branch: WorkflowTask[],
    position: 'left' | 'middle' | 'right'
): Edge[] {
    const edges: Edge[] = [];
    const topGhostId = `${forkJoinId}-forkJoin-top-ghost`;
    const bottomGhostId = `${forkJoinId}-forkJoin-bottom-ghost`;

    let topGhostHandlePosition = 'left';
    let bottomGhostHandlePosition = 'left';

    if (position === 'middle') {
        topGhostHandlePosition = 'bottom';
        bottomGhostHandlePosition = 'top';
    } else if (position === 'right') {
        topGhostHandlePosition = 'right';
        bottomGhostHandlePosition = 'right';
    }

    if (!branch || branch.length === 0) {
        return edges;
    }

    const edgeFromTopGhostToFirstTask = {
        id: `${topGhostId}=>${branch[0].name}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
        style: EDGE_STYLES,
        target: branch[0].name,
        type: 'workflow',
    };

    edges.push(edgeFromTopGhostToFirstTask);

    branch.forEach((branchTask, index) => {
        const sourceTaskId = branchTask.name;
        const targetTaskId = branch[index + 1]?.name;

        if (!targetTaskId) {
            return;
        }

        const sourceTaskComponentName = sourceTaskId.split('_')[0];

        if (
            TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName) &&
            !CHILDLESS_TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName)
        ) {
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

    const lastTaskId = branch[branch.length - 1].name;
    let lastTaskComponentName = lastTaskId.split('_')[0];

    if (
        TASK_DISPATCHER_NAMES.includes(lastTaskComponentName) &&
        !CHILDLESS_TASK_DISPATCHER_NAMES.includes(lastTaskComponentName)
    ) {
        if (lastTaskComponentName === 'fork-join') {
            lastTaskComponentName = 'forkJoin';
        }

        const nestedBottomGhostId = `${lastTaskId}-${lastTaskComponentName}-bottom-ghost`;

        const edgeFromNestedGhostToBottomGhost = {
            id: `${nestedBottomGhostId}=>${bottomGhostId}`,
            source: nestedBottomGhostId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: 'workflow',
        };

        edges.push(edgeFromNestedGhostToBottomGhost);
    } else {
        const edgeFromLastTaskToBottomGhost = {
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

// Distribute fork-join branches into left, middle, and right groups (copied from parallel)
function distributeBranches<T>(branches: T[]): {
    leftBranches: T[];
    middleBranch: T | null;
    rightBranches: T[];
} {
    const isEvenCount = (branches.length + 1) % 2 === 0;

    if (isEvenCount) {
        const halfPoint = (branches.length + 1) / 2;

        return {
            leftBranches: branches.slice(0, halfPoint),
            middleBranch: null,
            rightBranches: branches.slice(halfPoint),
        };
    } else {
        const middleIndex = Math.floor((branches.length + 1) / 2);

        return {
            leftBranches: branches.slice(0, middleIndex),
            middleBranch: branches[middleIndex],
            rightBranches: branches.slice(middleIndex + 1),
        };
    }
}

/**
 * Creates all edges for a fork-join node and its branches
 */
export default function createForkJoinEdges(forkJoinNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = forkJoinNode.data as NodeDataType;
    const forkJoinId = forkJoinNode.id;
    const branchCount =
        nodeData.parameters?.branches?.filter((branch: WorkflowTask[]) => branch.length > 0).length ?? 0;

    edges.push({
        id: `${forkJoinId}=>${forkJoinId}-forkJoin-top-ghost`,
        source: forkJoinId,
        style: EDGE_STYLES,
        target: `${forkJoinId}-forkJoin-top-ghost`,
        type: 'smoothstep',
    });

    // Use branches (array of arrays) instead of tasks
    const branches: WorkflowTask[][] = Array.isArray(nodeData.parameters?.branches)
        ? nodeData.parameters.branches.map((branch) => (Array.isArray(branch) ? branch : branch ? [branch] : []))
        : [];

    const hasSubtasks = branches.flat().length > 0;

    if (!hasSubtasks) {
        const leftGhostEdges = createEdgesForLeftGhost(forkJoinId);

        edges.push(...leftGhostEdges);
    } else {
        // Distribute branches into left, middle, right
        const {leftBranches, middleBranch, rightBranches} = distributeBranches(branches);

        leftBranches.forEach((branch: WorkflowTask[]) => {
            const branchEdges = createForkJoinTaskEdges(forkJoinId, branch, 'left');

            edges.push(...branchEdges);
        });

        if (middleBranch) {
            const branchEdges = createForkJoinTaskEdges(forkJoinId, middleBranch, 'middle');

            edges.push(...branchEdges);
        }

        rightBranches.forEach((branch: WorkflowTask[]) => {
            const branchEdges = createForkJoinTaskEdges(forkJoinId, branch, 'right');

            edges.push(...branchEdges);
        });
    }

    const placeholderEdges = createEdgesForPlaceholder(forkJoinId, branchCount);

    edges.push(...placeholderEdges);

    return edges;
}

/**
 * Determines the target handle side for a fork-join branch based on branch position
 */
export function getForkJoinBranchSide(
    taskDispatcherId: string,
    tasks: WorkflowTask[],
    parentForkJoinId: string
): 'left' | 'right' | 'bottom' {
    const parentForkJoinTask = tasks?.find((task) => task.name === parentForkJoinId);

    if (!parentForkJoinTask) {
        return 'right';
    }

    const branches = parentForkJoinTask.parameters?.branches || [];
    const branchIndex = branches.findIndex(
        (branch: WorkflowTask[]) => Array.isArray(branch) && branch.some((subtask) => subtask.name === taskDispatcherId)
    );

    if (branchIndex === -1) {
        return 'right';
    }

    const totalBranches = branches.length;

    if (totalBranches === 1) {
        return 'right';
    } else if (branchIndex === 0) {
        return 'left';
    } else if (branchIndex === totalBranches - 1) {
        return 'right';
    } else {
        return 'bottom';
    }
}
