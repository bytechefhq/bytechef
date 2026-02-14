import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import BranchCaseLabel from './BranchCaseLabel';

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

    const correctedSourceX = !isHorizontal && isMiddleCaseEdge ? targetX : sourceX;
    const correctedSourceY = isHorizontal && isMiddleCaseEdge ? targetY : sourceY;

    const [edgePath] = getSmoothStepPath({
        sourcePosition,
        sourceX: correctedSourceX,
        sourceY: correctedSourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const targetNodeId = id.split('=>')[1];

    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const caseKey = targetNode?.data?.caseKey as string | number | undefined;

    return (
        <>
            <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />

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
