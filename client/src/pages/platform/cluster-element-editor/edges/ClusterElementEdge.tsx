import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {BaseEdge, EdgeProps, getBezierPath} from '@xyflow/react';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import useCalledClusterElementEdgeStyle from './useCalledClusterElementEdgeStyle';

export default function ClusterElementEdge({
    id,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    target,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const clusterElementsCanvasNode = useClusterElementsDataStore((state) =>
        state.nodes.find((node) => node.id === target)
    );
    const workflowCanvasNode = useWorkflowDataStore((state) => state.nodes.find((node) => node.id === target));

    const [edgePath] = getBezierPath({sourcePosition, sourceX, sourceY, targetPosition, targetX, targetY});

    const calledClusterElementEdgeStyle = useCalledClusterElementEdgeStyle(
        clusterElementsCanvasNode || workflowCanvasNode,
        style
    );

    return (
        <BaseEdge
            className={calledClusterElementEdgeStyle.className}
            id={id}
            path={edgePath}
            style={calledClusterElementEdgeStyle.style}
        />
    );
}
