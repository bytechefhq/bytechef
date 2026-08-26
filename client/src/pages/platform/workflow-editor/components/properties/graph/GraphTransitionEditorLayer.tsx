import GraphTransitionPopover from '@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionPopover';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {ViewportPortal} from '@xyflow/react';

/**
 * The open graph transition editor, rendered ONCE for the whole canvas rather than by the edge it
 * belongs to.
 *
 * The editor used to be a child of `GraphTransitionEdge`, which read naturally — the popover belongs
 * to that transition — but could not survive: React Flow recreates its edge components whenever the
 * edges array is replaced, which a relayout does after every debounced save. Each of those took the
 * caret out of the condition field, destroyed the data pill suggestion popup, and ran the editor's
 * unmount flush against a store that no longer named a node, silently dropping the keystrokes it was
 * flushing. Nothing handed to React Flow explained it and nothing outside the library could prevent
 * it, so the editor was moved out of reach instead. A text field with a caret and a pending save
 * cannot live inside a component the layout engine is free to recreate.
 *
 * Mounted as a child of `<ReactFlow>`, so edge churn cannot reach it. It reads WHICH transition is
 * open from the edges themselves, the single source of truth for selection, and only takes the label
 * position from the store — so a position left behind by an edge that has since been deleted or
 * deselected is never looked up, and needs no cleanup.
 *
 * `ViewportPortal` puts it in the same flow coordinates the edge label layer uses, so it pans and
 * zooms with the canvas exactly as it did before.
 */
const GraphTransitionEditorLayer = () => {
    const edges = useWorkflowDataStore((state) => state.edges);
    const graphTransitionLabelPositions = useWorkflowEditorStore((state) => state.graphTransitionLabelPositions);

    const selectedTransitionEdges = edges.filter((edge) => edge.selected && edge.type === GRAPH_TRANSITION_EDGE_TYPE);

    // The editor points the details panel's current node at its graph, and that is a single global
    // slot — so under a box multi-select it stands down rather than letting two editors fight over
    // it. `GraphTransitionPopover` says so in its own body for the same reason.
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
            {/* Hung below the label anchor rather than centred on it, so it does not cover the
                condition badge that selection also reveals. */}

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
