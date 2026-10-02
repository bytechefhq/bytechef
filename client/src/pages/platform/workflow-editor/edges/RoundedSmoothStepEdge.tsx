import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import AddBranchChip from './AddBranchChip';
import BinaryCaseLabel from './BinaryCaseLabel';
import computeBinaryCaseLabel from './computeBinaryCaseLabel';
import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';
import {getTriggerFanInBusCenter} from './computeTriggerFanIn';

export default function RoundedSmoothStepEdge({
    data,
    id,
    source,
    sourceHandleId,
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

    const addBranchPlaceholderId = (data as Record<string, unknown>)?.addBranchPlaceholderId as string | undefined;

    const busCenter = getTriggerFanInBusCenter({
        isTriggerFanIn,
        sourcePosition,
        sourceX,
        sourceY,
    });

    // Smoothstep edges into a bottom bar (empty-case placeholder trails in the
    // editor, EVERY trailing edge in the read-only conversion where all edges
    // become 'smoothstep') bend beside the bar instead of at the path midpoint
    // — the same no-crossing rule WorkflowEdge applies to its exit edges. This
    // component has no node lookup, so the bar is detected by its id suffix.
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

    const binaryCaseLabel = computeBinaryCaseLabel({
        layoutDirection,
        source,
        sourceHandleId,
        sourceX,
        targetY,
    });

    return (
        <>
            <BaseEdge
                className="fill-none stroke-stroke-neutral-tertiary stroke-2"
                id={id}
                path={edgePath}
                style={style}
            />

            {binaryCaseLabel && <BinaryCaseLabel edgeId={id} label={binaryCaseLabel} />}

            {addBranchPlaceholderId && (
                <AddBranchChip
                    edgeId={id}
                    layoutDirection={layoutDirection}
                    placeholderId={addBranchPlaceholderId}
                    sourceX={sourceX}
                    sourceY={sourceY}
                    targetX={targetX}
                    targetY={targetY}
                />
            )}
        </>
    );
}
