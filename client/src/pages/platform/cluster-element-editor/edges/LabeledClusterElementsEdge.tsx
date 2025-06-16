import {BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

export default function LabeledClusterElementsEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const {nodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const [edgePath] = getBezierPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    const targetNodeId = id.split('=>')[1];

    const targetPlaceholder = nodes.find((node) => node.id === targetNodeId);
    const targetClusterElement = nodes.find((node) => node.id === targetNodeId.split('-')[1]);

    const node = nodes.find((currentNode) => {
        if (targetPlaceholder) {
            return currentNode === targetPlaceholder;
        } else {
            return currentNode === targetClusterElement;
        }
    });

    const nodeLabel = node?.data.clusterElementLabel;

    function getEdgeLabelPosition() {
        const defaultPosition = `translate(-50%, 0%) translate(${sourceX}px,${sourceY}px)`;

        return defaultPosition;
    }

    return (
        <>
            <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />

            <EdgeLabelRenderer key={id}>
                <div
                    className="w-14 items-center rounded-md border-2 border-stroke-neutral-tertiary bg-white p-1 text-center text-xs font-medium"
                    style={{
                        position: 'absolute',
                        transform: getEdgeLabelPosition(),
                    }}
                >
                    {nodeLabel ? <span>{node?.data?.clusterElementLabel as string}</span> : ''}
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
