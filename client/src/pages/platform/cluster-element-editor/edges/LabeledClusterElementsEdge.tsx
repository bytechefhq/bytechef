import RequiredMark from '@/components/RequiredMark';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CLUSTER_ELEMENT_EDGE_LABEL_GAP} from '@/shared/constants';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, getBezierPath, useInternalNode} from '@xyflow/react';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import useCalledClusterElementEdgeStyle from './useCalledClusterElementEdgeStyle';

const EDGE_LABEL_Z_INDEX_OFFSET = 10;

export default function LabeledClusterElementsEdge({
    id,
    source,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    target,
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

    const sourceInternalNode = useInternalNode(source);
    const targetInternalNode = useInternalNode(target);

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

    const labelZIndex =
        Math.max(sourceInternalNode?.internals.z ?? 0, targetInternalNode?.internals.z ?? 0) +
        EDGE_LABEL_Z_INDEX_OFFSET;

    function getEdgeLabelPosition() {
        const defaultPosition = `translate(-50%, 0%) translate(${sourceX}px,${sourceY + CLUSTER_ELEMENT_EDGE_LABEL_GAP}px)`;

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
                    className="max-w-20 p-0.5 text-center text-xs font-medium text-wrap"
                    style={{
                        position: 'absolute',
                        transform: getEdgeLabelPosition(),
                        zIndex: labelZIndex,
                    }}
                >
                    {nodeLabel ? (
                        <span>
                            {node?.data?.clusterElementLabel as string}

                            {!!node?.data?.clusterElementRequired && <RequiredMark />}
                        </span>
                    ) : (
                        ''
                    )}
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
