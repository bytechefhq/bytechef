import {TASK_DISPATCHER_NAMES} from '@/shared/constants';

interface ComputeEdgeButtonPositionProps {
    correctedSourceX: number;
    correctedSourceY: number;
    correctedTargetX: number;
    correctedTargetY: number;
    edgeCenterX: number;
    edgeCenterY: number;
    isHorizontal: boolean;
    sourceNodeComponentName?: string;
    sourceNodeTaskDispatcherId?: string;
    sourceNodeType?: string;
    targetNodeType?: string;
}

export default function computeEdgeButtonPosition({
    correctedSourceX,
    correctedSourceY,
    correctedTargetX,
    correctedTargetY,
    edgeCenterX,
    edgeCenterY,
    isHorizontal,
    sourceNodeComponentName,
    sourceNodeTaskDispatcherId,
    sourceNodeType,
    targetNodeType,
}: ComputeEdgeButtonPositionProps): {x: number; y: number} {
    const isMainAxisEdge = isHorizontal
        ? Math.abs(correctedSourceX - correctedTargetX) > Math.abs(correctedSourceY - correctedTargetY)
        : Math.abs(correctedSourceY - correctedTargetY) > Math.abs(correctedSourceX - correctedTargetX);

    const isEdgeFromBranchTopGhostNode =
        sourceNodeType === 'taskDispatcherTopGhostNode' && sourceNodeTaskDispatcherId?.startsWith('branch');

    // Merge edges between two bottom ghosts (nested dispatcher → enclosing
    // dispatcher) bend around the frame corner, so the path center can land on
    // the horizontal run — those fall through to the pin-to-source-column logic
    // below. Only continuation edges leaving a bottom ghost keep the path center.
    const isBottomGhostContinuationEdge =
        sourceNodeType === 'taskDispatcherBottomGhostNode' && targetNodeType !== 'taskDispatcherBottomGhostNode';

    // A trailing edge (chain end → its frame's bottom bar) bends right beside
    // the bar, so its path center sits on the bottom jog next to foreign
    // columns — the "+" belongs midway down the column's own vertical run
    const isExitEdgeToBottomGhost =
        targetNodeType === 'taskDispatcherBottomGhostNode' &&
        sourceNodeType !== 'taskDispatcherBottomGhostNode' &&
        sourceNodeType !== 'taskDispatcherTopGhostNode';

    if (isExitEdgeToBottomGhost) {
        if (isHorizontal) {
            return {
                x: Math.min(correctedSourceX, correctedTargetX) + Math.abs(correctedTargetX - correctedSourceX) * 0.5,
                y: correctedSourceY,
            };
        }

        return {
            x: correctedSourceX,
            y: Math.min(correctedSourceY, correctedTargetY) + Math.abs(correctedTargetY - correctedSourceY) * 0.5,
        };
    }

    if ((isMainAxisEdge && !isEdgeFromBranchTopGhostNode) || isBottomGhostContinuationEdge) {
        return {
            x: edgeCenterX,
            y: edgeCenterY,
        };
    }

    let posX;
    let posY;

    if (isHorizontal) {
        posX = Math.min(correctedSourceX, correctedTargetX) + Math.abs(correctedTargetX - correctedSourceX) * 0.5;

        if (sourceNodeType === 'taskDispatcherTopGhostNode') {
            posY = correctedTargetY;

            if (targetNodeType === 'workflow' && isEdgeFromBranchTopGhostNode) {
                posX += 15;
            }
        } else if (targetNodeType === 'taskDispatcherBottomGhostNode') {
            posY = correctedSourceY;
        } else if (sourceNodeComponentName && TASK_DISPATCHER_NAMES.includes(sourceNodeComponentName)) {
            posY = correctedTargetY;
        }
    } else {
        posY = Math.min(correctedSourceY, correctedTargetY) + Math.abs(correctedTargetY - correctedSourceY) * 0.5;

        if (sourceNodeType === 'taskDispatcherTopGhostNode') {
            posX = correctedTargetX;

            if (targetNodeType === 'workflow' && isEdgeFromBranchTopGhostNode) {
                posY += 15;
            }
        } else if (targetNodeType === 'taskDispatcherBottomGhostNode') {
            posX = correctedSourceX;
        } else if (sourceNodeComponentName && TASK_DISPATCHER_NAMES.includes(sourceNodeComponentName)) {
            posX = correctedTargetX;
        }
    }

    return {x: posX ?? edgeCenterX, y: posY ?? edgeCenterY};
}
