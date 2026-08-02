import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import GraphNodeLabel from './GraphNodeLabel';
import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';

/**
 * The `graph` analog of `LabeledBranchCaseEdge`: renders the top-ghost -> placeholder edge for
 * an EMPTY graph node lane, carrying the node name chip. Non-empty lanes get their chip from
 * `WorkflowEdge` instead (the top-ghost -> first-task edge stays a plain `workflow` edge type;
 * see the `graphData.nodeIndex` branch there) — mirroring exactly how branch splits its case
 * label between `LabeledBranchCaseEdge` and `WorkflowEdge`.
 */
export default function LabeledGraphNodeEdge({
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
    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const isHorizontal = layoutDirection === 'LR';
    const isMiddleCaseEdge = !!(data as Record<string, unknown>)?.isMiddleCase;

    const sourceNodeId = id.split('=>')[0];
    const targetNodeId = id.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

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

    const nodeIndex = targetNode?.data?.nodeIndex as number | undefined;

    return (
        <>
            <BaseEdge
                className="fill-none stroke-stroke-neutral-tertiary stroke-2"
                id={id}
                path={edgePath}
                style={style}
            />

            {typeof nodeIndex === 'number' && (
                <GraphNodeLabel
                    edgeId={id}
                    layoutDirection={layoutDirection}
                    nodeIndex={nodeIndex}
                    sourceX={sourceX}
                    sourceY={sourceY}
                    targetX={targetX}
                    targetY={targetY}
                />
            )}
        </>
    );
}
