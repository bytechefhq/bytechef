import GraphTransitionPopover from '@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionPopover';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {ViewportPortal} from '@xyflow/react';

const GraphTransitionEditorLayer = () => {
    const edges = useWorkflowDataStore((state) => state.edges);
    const graphTransitionLabelPositions = useWorkflowEditorStore((state) => state.graphTransitionLabelPositions);

    const selectedTransitionEdges = edges.filter((edge) => edge.selected && edge.type === GRAPH_TRANSITION_EDGE_TYPE);

    const selectedTransitionEdge = selectedTransitionEdges.length === 1 ? selectedTransitionEdges[0] : undefined;

    const {graphId, index, readOnly} = (selectedTransitionEdge?.data ?? {}) as {
        graphId?: string;
        index?: number;
        readOnly?: boolean;
    };

    const labelPosition = selectedTransitionEdge ? graphTransitionLabelPositions[selectedTransitionEdge.id] : undefined;

    if (readOnly || graphId === undefined || index === undefined || !labelPosition) {
        return null;
    }

    return (
        <ViewportPortal>
            <div
                className="nodrag nopan pointer-events-auto absolute z-20"
                style={{
                    transform: `translate(-50%, 16px) translate(${labelPosition.labelX}px, ${labelPosition.labelY}px)`,
                }}
            >
                <GraphTransitionPopover graphId={graphId} index={index} />
            </div>
        </ViewportPortal>
    );
};

export default GraphTransitionEditorLayer;
