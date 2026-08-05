import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import BranchCaseLabel from './BranchCaseLabel';
import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';
import useExecutedEdgeStatus from './useExecutedEdgeStatus';

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
    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const isHorizontal = layoutDirection === 'LR';
    const isMiddleCaseEdge = !!(data as Record<string, unknown>)?.isMiddleCase;

    const executedEdgeStatus = useExecutedEdgeStatus(id);

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

    const caseKey = targetNode?.data?.caseKey as string | number | undefined;

    return (
        <>
            <BaseEdge
                className={twMerge(
                    'fill-none stroke-stroke-neutral-tertiary stroke-2',
                    executedEdgeStatus === 'COMPLETED' && 'stroke-green-500',
                    executedEdgeStatus === 'FAILED' && 'stroke-red-500'
                )}
                id={id}
                path={edgePath}
                style={style}
            />

            {caseKey && (
                <BranchCaseLabel
                    caseKey={caseKey}
                    edgeId={id}
                    layoutDirection={layoutDirection}
                    sourceX={sourceX}
                    sourceY={sourceY}
                    targetX={targetX}
                    targetY={targetY}
                />
            )}
        </>
    );
}
