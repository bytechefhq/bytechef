import {AI_AGENT_EDGE_LABELS} from '@/shared/constants';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useAiAgentDataStore from '../stores/useAiAgentDataStore';

export default function LabeledAiAgentEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const {nodes} = useAiAgentDataStore(
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

    const nodeType = node?.data.clusterElementType;

    function getEdgeLabel(type: string) {
        return AI_AGENT_EDGE_LABELS[type as keyof typeof AI_AGENT_EDGE_LABELS] || type;
    }

    function getEdgeLabelPosition(nodeType: string | undefined, targetX: number, targetY: number) {
        const defaultPosition = `translate(-50%, -120%) translate(${targetX}px,${targetY}px)`;

        if (nodeType === 'tools') {
            return `translate(-50%, -50%) translate(${targetX}px,${targetY}px)`;
        }

        return defaultPosition;
    }

    return (
        <>
            <BaseEdge className="fill-none stroke-gray-300 stroke-2" id={id} path={edgePath} style={style} />

            <EdgeLabelRenderer key={id}>
                <div
                    className="flex items-center rounded-md border-2 border-stroke-neutral-tertiary bg-white px-2 py-1 text-xs font-medium shadow-sm"
                    style={{
                        position: 'absolute',
                        transform: getEdgeLabelPosition(nodeType as string, targetX, targetY),
                    }}
                >
                    {nodeType ? <span>{getEdgeLabel(nodeType as string)}</span> : ''}
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
