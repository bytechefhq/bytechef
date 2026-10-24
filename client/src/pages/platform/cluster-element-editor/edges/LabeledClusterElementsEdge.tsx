import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath} from '@xyflow/react';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import useCalledClusterElementEdgeStyle from './useCalledClusterElementEdgeStyle';

export default function LabeledClusterElementsEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetX,
    targetY,
}: EdgeProps) {
    const {nodes: clusterElementsCanvasNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );
    const {nodes: workflowCanvasNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const nodes = useMemo(
        () => [...clusterElementsCanvasNodes, ...workflowCanvasNodes],
        [clusterElementsCanvasNodes, workflowCanvasNodes]
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

    const calledClusterElementEdgeStyle = useCalledClusterElementEdgeStyle(targetClusterElement, style);

    function getEdgeLabelPosition() {
        const defaultPosition = `translate(-50%, 0%) translate(${sourceX}px,${sourceY}px)`;

        return defaultPosition;
    }

    return (
        <>
            <BaseEdge
                className={calledClusterElementEdgeStyle.className}
                id={id}
                path={edgePath}
                style={calledClusterElementEdgeStyle.style}
            />

            <EdgeLabelRenderer key={id}>
                <div
                    className="z-10 max-w-20 p-0.5 text-center text-xs font-medium text-wrap"
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
