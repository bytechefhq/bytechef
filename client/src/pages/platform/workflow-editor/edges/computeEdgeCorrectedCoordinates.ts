import {ALIGNED_SIDE_CASE_THRESHOLD} from '@/shared/constants';
import {Position} from '@xyflow/react';

interface EdgeCorrectionProps {
    isHorizontal: boolean;
    isMiddleCaseEdge: boolean;
    sourceNodeType?: string;
    sourcePosition: Position;
    sourceX: number;
    sourceY: number;
    targetNodeType?: string;
    targetPosition: Position;
    targetX: number;
    targetY: number;
}

interface EdgeCorrectionResultI {
    correctedSourcePosition: Position;
    correctedSourceX: number;
    correctedSourceY: number;
    correctedTargetPosition: Position;
    correctedTargetX: number;
    correctedTargetY: number;
}

/**
 * Corrects edge coordinates and handle positions for branch case edges.
 *
 * Middle-case edges (the central branch path) need their cross-axis coordinate
 * snapped to the target/source so the path is straight through the ghost node.
 *
 * In LR mode, aligned side-case edges have a small Y mismatch (~7px) due to
 * handle offsets within ghost nodes (side handles at Position.Top/Bottom vs
 * target handles at Position.Left/Right center). When the Y difference is within
 * the threshold set by alignBranchCaseChildren, this function corrects the
 * coordinates and overrides handle positions to produce a straight horizontal edge.
 */
export default function computeEdgeCorrectedCoordinates({
    isHorizontal,
    isMiddleCaseEdge,
    sourceNodeType,
    sourcePosition,
    sourceX,
    sourceY,
    targetNodeType,
    targetPosition,
    targetX,
    targetY,
}: EdgeCorrectionProps): EdgeCorrectionResultI {
    const isSourceTopGhost = sourceNodeType === 'taskDispatcherTopGhostNode';
    const isTargetBottomGhost = targetNodeType === 'taskDispatcherBottomGhostNode';

    const isAlignedSideCaseFromGhost =
        isHorizontal &&
        !isMiddleCaseEdge &&
        isSourceTopGhost &&
        Math.abs(sourceY - targetY) <= ALIGNED_SIDE_CASE_THRESHOLD;

    const isAlignedSideCaseToGhost =
        isHorizontal &&
        !isMiddleCaseEdge &&
        isTargetBottomGhost &&
        Math.abs(sourceY - targetY) <= ALIGNED_SIDE_CASE_THRESHOLD;

    const correctedSourceX = !isHorizontal && isMiddleCaseEdge && isSourceTopGhost ? targetX : sourceX;
    const correctedTargetX = !isHorizontal && isMiddleCaseEdge && isTargetBottomGhost ? sourceX : targetX;

    const correctedSourceY =
        (isHorizontal && isMiddleCaseEdge && isSourceTopGhost) || isAlignedSideCaseFromGhost ? targetY : sourceY;
    const correctedTargetY =
        (isHorizontal && isMiddleCaseEdge && isTargetBottomGhost) || isAlignedSideCaseToGhost ? sourceY : targetY;

    const correctedSourcePosition = isAlignedSideCaseFromGhost ? Position.Right : sourcePosition;
    const correctedTargetPosition = isAlignedSideCaseToGhost ? Position.Left : targetPosition;

    return {
        correctedSourcePosition,
        correctedSourceX,
        correctedSourceY,
        correctedTargetPosition,
        correctedTargetX,
        correctedTargetY,
    };
}
