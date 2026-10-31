import computeExitEdgeJogCenter from '@/pages/platform/workflow-editor/edges/computeExitEdgeJogCenter';
import {getTriggerFanInBusCenter} from '@/pages/platform/workflow-editor/edges/computeTriggerFanIn';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

export default function ChangeCanvasEdge({
    data,
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    target,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const isTriggerFanIn = !!(data as Record<string, unknown>)?.triggerFanIn;

    const busCenter = getTriggerFanInBusCenter({isTriggerFanIn, sourcePosition, sourceX, sourceY});

    const exitJogCenter = computeExitEdgeJogCenter({
        correctedSourceX: sourceX,
        correctedSourceY: sourceY,
        correctedTargetX: targetX,
        correctedTargetY: targetY,
        isHorizontal: layoutDirection === 'LR',
        isTriggerFanIn,
        targetNodeType: target.endsWith('-bottom-ghost') ? 'taskDispatcherBottomGhostNode' : undefined,
    });

    const [edgePath] = getSmoothStepPath({
        borderRadius: 10,
        ...busCenter,
        ...exitJogCenter,
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return (
        <BaseEdge className="fill-none stroke-stroke-neutral-tertiary stroke-2" id={id} path={edgePath} style={style} />
    );
}
