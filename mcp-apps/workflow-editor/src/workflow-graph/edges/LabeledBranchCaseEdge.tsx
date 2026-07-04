// Ported from client/src/pages/platform/workflow-editor/edges/LabeledBranchCaseEdge.tsx.
// Adaptations: store-free (pinned TB direction; source/target nodes read via useReactFlow
// instead of the Zustand workflow-data store), and the interactive BranchCaseLabel (edit /
// add / delete case controls) is replaced by a static read-only label since the marketing
// site never authors workflows.

import {BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath, useReactFlow} from '@xyflow/react';

import {LAYOUT_DIRECTION} from '../layout/constants';
import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';

export default function LabeledBranchCaseEdge({
    data,
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const {getNode} = useReactFlow();

    const isHorizontal = LAYOUT_DIRECTION === 'LR';
    const isMiddleCaseEdge = !!(data as Record<string, unknown>)?.isMiddleCase;

    const sourceNodeId = id.split('=>')[0];
    const targetNodeId = id.split('=>')[1];

    const sourceNode = getNode(sourceNodeId);
    const targetNode = getNode(targetNodeId);

    const {correctedSourcePosition, correctedSourceX, correctedSourceY} = computeEdgeCorrectedCoordinates({
        isHorizontal,
        isMiddleCaseEdge,
        sourceNodeType: sourceNode?.type,
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const [edgePath] = getSmoothStepPath({
        borderRadius: 10,
        sourcePosition: correctedSourcePosition,
        sourceX: correctedSourceX,
        sourceY: correctedSourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const caseKey = targetNode?.data?.caseKey as string | number | undefined;
    const labelX = (sourceX + targetX) / 2;
    const labelY = (sourceY + targetY) / 2;

    return (
        <>
            <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />

            {caseKey != null && (
                <EdgeLabelRenderer key={`${id}-case-label`}>
                    <div
                        className="z-10 flex items-center rounded-md border-2 border-stroke-neutral-tertiary bg-white p-1 px-2 text-xs font-medium shadow-sm"
                        style={{
                            pointerEvents: 'none',
                            position: 'absolute',
                            transform: `translate(${labelX}px, ${labelY}px) translate(-50%, -50%)`,
                        }}
                    >
                        {caseKey === 'default' ? 'default' : String(caseKey)}
                    </div>
                </EdgeLabelRenderer>
            )}
        </>
    );
}
