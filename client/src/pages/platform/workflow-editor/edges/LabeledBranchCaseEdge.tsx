import {BaseEdge, EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import BranchCaseLabel from './BranchCaseLabel';

export default function LabeledBranchCaseEdge({
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

    const [edgePath] = getSmoothStepPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const targetNodeId = id.split('=>')[1];

    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const caseKey = targetNode?.data?.caseKey as string | undefined;

    return (
        <>
            <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />

            {caseKey && <BranchCaseLabel caseKey={caseKey} edgeId={id} sourceY={sourceY} targetX={targetX} />}
        </>
    );
}
