import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

interface AnimationOptionsI {
    duration?: number;
}

function easeOutCubic(t: number): number {
    return 1 - Math.pow(1 - t, 3);
}

function positionsAreEqual(previousNodes: Node[], targetNodes: Node[]): boolean {
    if (previousNodes.length !== targetNodes.length) {
        return false;
    }

    const previousPositionMap = new Map(previousNodes.map((node) => [node.id, node.position]));

    return targetNodes.every((targetNode) => {
        const previousPosition = previousPositionMap.get(targetNode.id);

        if (!previousPosition) {
            return false;
        }

        return (
            Math.abs(previousPosition.x - targetNode.position.x) < 1 &&
            Math.abs(previousPosition.y - targetNode.position.y) < 1
        );
    });
}

/**
 * Largest per-node travel (px, either axis) that still animates. Beyond this the tween snaps to the
 * target instead: a long tween sweeps nodes across a big stretch of canvas while every frame
 * re-renders the whole flow, and under heavy main-thread load (e.g. an AI Hub chat streaming next to
 * the embedded editor) Chrome's compositor has been observed leaving paint trails — one stale
 * afterimage of the moving node per painted frame. A large centering shift also just reads better as
 * a snap (the same reasoning useLayout applies when the node count changes).
 */
const MAX_ANIMATED_DISPLACEMENT = 800;

function maxDisplacement(previousNodes: Node[], targetNodes: Node[]): number {
    const previousPositionMap = new Map(previousNodes.map((node) => [node.id, node.position]));

    let largestDisplacement = 0;

    for (const targetNode of targetNodes) {
        const previousPosition = previousPositionMap.get(targetNode.id);

        if (!previousPosition) {
            continue;
        }

        largestDisplacement = Math.max(
            largestDisplacement,
            Math.abs(previousPosition.x - targetNode.position.x),
            Math.abs(previousPosition.y - targetNode.position.y)
        );
    }

    return largestDisplacement;
}

const GHOST_AND_PLACEHOLDER_TYPES = new Set([
    'placeholder',
    'taskDispatcherBottomGhostNode',
    'taskDispatcherLeftGhostNode',
    'taskDispatcherTopGhostNode',
]);

/**
 * Returns the direct parent dispatcher ID for a node, or null if the node is a root-level node.
 * Ghost/placeholder nodes use taskDispatcherId; workflow nodes use their nesting data.
 *
 * A node React Flow already parents (a graph frame's members and chrome) is excluded: its position
 * is relative to that parent, so it follows for free — offsetting it against the dispatcher's
 * ABSOLUTE interpolated position on top of that would slide it around inside its frame for the
 * length of the tween.
 */
function getDirectParentDispatcherId(node: Node): string | null {
    if (node.parentId) {
        return null;
    }

    const data = node.data as NodeDataType;

    if (GHOST_AND_PLACEHOLDER_TYPES.has(node.type || '')) {
        return data.taskDispatcherId || null;
    }

    if (data.conditionData?.conditionId) {
        return data.conditionData.conditionId;
    }

    if (data.loopData?.loopId) {
        return data.loopData.loopId;
    }

    if (data.branchData?.branchId) {
        return data.branchData.branchId;
    }

    if (data.parallelData?.parallelId) {
        return data.parallelData.parallelId;
    }

    if (data.eachData?.eachId) {
        return data.eachData.eachId;
    }

    if (data.forkJoinData?.forkJoinId) {
        return data.forkJoinData.forkJoinId;
    }

    if (data.graphData?.graphId) {
        return data.graphData.graphId;
    }

    return null;
}

/**
 * Pre-computes target offsets for all nodes that belong to a task dispatcher.
 * During animation these nodes follow their parent's interpolated position, preventing edge jitter.
 */
function buildParentOffsets(targetNodes: Node[]): Map<string, {offsetX: number; offsetY: number; parentId: string}> {
    const targetPositionMap = new Map(targetNodes.map((node) => [node.id, node.position]));
    const parentOffsets = new Map<string, {offsetX: number; offsetY: number; parentId: string}>();

    for (const targetNode of targetNodes) {
        const parentId = getDirectParentDispatcherId(targetNode);

        if (!parentId) {
            continue;
        }

        const parentTargetPosition = targetPositionMap.get(parentId);

        if (!parentTargetPosition) {
            continue;
        }

        parentOffsets.set(targetNode.id, {
            offsetX: targetNode.position.x - parentTargetPosition.x,
            offsetY: targetNode.position.y - parentTargetPosition.y,
            parentId,
        });
    }

    return parentOffsets;
}

/**
 * Animates node positions from their current positions to target positions using requestAnimationFrame.
 * All nodes inside a task dispatcher follow their parent's interpolated position to prevent edge jitter.
 * Returns a cancel function to abort the animation.
 */
export default function animateNodePositions(
    previousNodes: Node[],
    targetNodes: Node[],
    setNodes: (nodes: Node[]) => void,
    options?: AnimationOptionsI
): () => void {
    if (positionsAreEqual(previousNodes, targetNodes)) {
        setNodes(targetNodes);

        return () => {};
    }

    if (maxDisplacement(previousNodes, targetNodes) > MAX_ANIMATED_DISPLACEMENT) {
        setNodes(targetNodes);

        return () => {};
    }

    const duration = options?.duration ?? 300;
    const previousPositionMap = new Map(previousNodes.map((node) => [node.id, node.position]));
    const parentOffsets = buildParentOffsets(targetNodes);

    let animationFrameId: number | null = null;

    // The clock starts at the FIRST frame, not at call time: the caller
    // typically triggers an expensive full-canvas render right after this
    // call, and counting that blocking time against the tween made layout
    // switches finish the animation before a single frame was painted.
    let startTime: number | null = null;

    function tick(currentTime: number) {
        if (startTime === null) {
            startTime = currentTime;
        }

        const elapsed = currentTime - startTime;
        const rawProgress = Math.min(elapsed / duration, 1);
        const progress = easeOutCubic(rawProgress);

        // First pass: interpolate root nodes (no parent dispatcher)
        const interpolatedPositionMap = new Map<string, {x: number; y: number}>();

        for (const targetNode of targetNodes) {
            if (parentOffsets.has(targetNode.id)) {
                continue;
            }

            const previousPosition = previousPositionMap.get(targetNode.id);

            if (!previousPosition) {
                interpolatedPositionMap.set(targetNode.id, targetNode.position);

                continue;
            }

            interpolatedPositionMap.set(targetNode.id, {
                x: previousPosition.x + (targetNode.position.x - previousPosition.x) * progress,
                y: previousPosition.y + (targetNode.position.y - previousPosition.y) * progress,
            });
        }

        // Second pass: position child nodes relative to their parent's interpolated position.
        // Uses iterative resolution so nested dispatchers (children of children) are handled correctly.
        let remainingChildNodes = targetNodes.filter((node) => parentOffsets.has(node.id));
        let previousRemainingCount = remainingChildNodes.length + 1;

        while (remainingChildNodes.length > 0 && remainingChildNodes.length < previousRemainingCount) {
            previousRemainingCount = remainingChildNodes.length;

            const stillRemaining: Node[] = [];

            for (const targetNode of remainingChildNodes) {
                const offset = parentOffsets.get(targetNode.id)!;
                const parentPosition = interpolatedPositionMap.get(offset.parentId);

                if (!parentPosition) {
                    stillRemaining.push(targetNode);

                    continue;
                }

                interpolatedPositionMap.set(targetNode.id, {
                    x: parentPosition.x + offset.offsetX,
                    y: parentPosition.y + offset.offsetY,
                });
            }

            remainingChildNodes = stillRemaining;
        }

        // Fallback: any unresolved nodes get their target position
        for (const node of remainingChildNodes) {
            interpolatedPositionMap.set(node.id, node.position);
        }

        const interpolatedNodes = targetNodes.map((targetNode) => ({
            ...targetNode,
            position: interpolatedPositionMap.get(targetNode.id) || targetNode.position,
        }));

        if (rawProgress < 1) {
            setNodes(interpolatedNodes);

            animationFrameId = requestAnimationFrame(tick);
        } else {
            setNodes(targetNodes);
        }
    }

    animationFrameId = requestAnimationFrame(tick);

    return () => {
        if (animationFrameId !== null) {
            cancelAnimationFrame(animationFrameId);
        }
    };
}
