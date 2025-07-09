import {BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

export default function LabeledClusterElementsEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
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
                    className="max-w-20 items-center text-wrap rounded-md border border-stroke-neutral-tertiary bg-white p-0.5 text-center text-xs"
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
