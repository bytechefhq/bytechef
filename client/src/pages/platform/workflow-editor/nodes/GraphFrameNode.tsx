import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {LayoutDirectionType} from '@/shared/constants';
import {GraphTransitionType, NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {PlusIcon, Wand2Icon} from 'lucide-react';
import {memo, useCallback, useEffect, useRef, useState} from 'react';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {
    GRAPH_FRAME_HEADER_HEIGHT,
    GRAPH_FRAME_ID_ATTRIBUTE,
    GRAPH_FRAME_MIN_HEIGHT,
    GRAPH_FRAME_MIN_WIDTH,
    autoPlaceGraphMembers,
    getGraphStartPinnedBox,
} from '../utils/graph/graphFrameGeometry';
import {collectGraphMemberSizes} from '../utils/graph/graphMemberPlacement';
import saveWorkflowNodesPosition from '../utils/saveWorkflowNodesPosition';
import styles from './NodeTypes.module.css';

const HEADER_BUTTON_CLASSNAME =
    'nodrag flex items-center gap-1 rounded px-2 py-1 text-xs text-content-neutral-secondary hover:bg-surface-neutral-secondary';

/**
 * The auto-sizing container a `graph/v1` dispatcher's member tasks live inside. Its size is not its
 * own to choose: the layout pre-pass measures the members and writes `data.graphFrame`, and this
 * component only paints the box at that size — which is what makes the surrounding flow reflow
 * around a growing graph.
 *
 * The graph dispatcher's own `WorkflowNode` sits above the box as the chain node it is (settings,
 * delete, copy), so the frame itself is chrome: neither draggable nor selectable, and the only
 * things it owns are the chain anchors on its top and bottom edges plus the two header controls —
 * which both edit the workflow, so they are left out entirely on a read-only canvas that has no
 * `updateWorkflowMutation` to edit it with.
 *
 * Its `data-graph-frame-id` is what the canvas hit-tests: releasing a transition over empty space
 * inside the box, and dropping a component into it, both resolve the graph through this element.
 */
const GraphFrameNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [addNodePopoverOpen, setAddNodePopoverOpen] = useState(false);

    // `undefined` until the first pass has run, which is how mounting is told apart from a flip.
    const previousLayoutDirectionRef = useRef<LayoutDirectionType | undefined>(undefined);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const graphPendingConnection = useWorkflowEditorStore((state) => state.graphPendingConnection);
    const setGraphPendingConnection = useWorkflowEditorStore((state) => state.setGraphPendingConnection);

    const {updateWorkflowMutation} = useWorkflowEditor();

    const graphId = data.graphFrame?.graphId ?? data.graphId ?? data.taskDispatcherId;
    const height = data.graphFrame?.height ?? GRAPH_FRAME_MIN_HEIGHT;
    const width = data.graphFrame?.width ?? GRAPH_FRAME_MIN_WIDTH;

    /**
     * Lays every member out left-to-right over the transition graph and persists the result. The
     * Start pill is pinned so the layout lands clear of it rather than under it.
     *
     * Nothing here has to clear the pre-pass's pending auto-placed positions: a position is written
     * for every member, so the next pass has none left to invent and replaces the pending map
     * wholesale.
     */
    const handleAutoArrange = useCallback(async () => {
        if (!graphId || !updateWorkflowMutation) {
            return;
        }

        const {nodes} = useWorkflowDataStore.getState();

        const memberSizes = collectGraphMemberSizes(graphId, nodes);

        if (memberSizes.length === 0) {
            return;
        }

        const dispatcherNode = nodes.find((node) => node.id === graphId);

        const transitions: GraphTransitionType[] =
            (dispatcherNode?.data as NodeDataType)?.parameters?.transitions ?? [];

        const nodePositions = await autoPlaceGraphMembers(
            memberSizes,
            transitions,
            [getGraphStartPinnedBox()],
            layoutDirection,
            (dispatcherNode?.data as NodeDataType)?.parameters?.startNode || memberSizes[0]?.name
        );

        // `draggedNodeId` names the node a DRAG persisted, which this is not: the save helper reads
        // it only to update a trigger's own position, and a graph dispatcher is never a trigger.
        // Naming the graph is the closest honest answer — nothing here consumes it.
        saveWorkflowNodesPosition({draggedNodeId: graphId, nodePositions, updateWorkflowMutation});
    }, [graphId, layoutDirection, updateWorkflowMutation]);

    const handleAddNodePopoverOpenChange = useCallback(
        (open: boolean) => {
            setAddNodePopoverOpen(open);

            // Dismissing the popover abandons the pending connection. A pick does not reach here
            // first — the task is appended, and the pending connection consumed, before the
            // operation list closes the popover.
            if (!open && useWorkflowEditorStore.getState().graphPendingConnection?.graphId === graphId) {
                setGraphPendingConnection(undefined);
            }
        },
        [graphId, setGraphPendingConnection]
    );

    // Member positions are stored, and say nothing about which way the canvas flows — so a graph
    // laid out for TB keeps that shape when the canvas is flipped to LR, with its transitions then
    // running across the arrangement instead of along it. Re-arranging on the flip is destructive by
    // construction: a position placed by hand is overwritten along with the rest, because nothing in
    // the model distinguishes the two.
    //
    // Deliberately skips the first pass. The direction is whatever it already was on mount, and
    // rewriting every member's position on load would make opening a workflow an edit.
    useEffect(() => {
        if (previousLayoutDirectionRef.current === undefined) {
            previousLayoutDirectionRef.current = layoutDirection;

            return;
        }

        if (previousLayoutDirectionRef.current === layoutDirection) {
            return;
        }

        previousLayoutDirectionRef.current = layoutDirection;

        handleAutoArrange();
    }, [handleAutoArrange, layoutDirection]);

    useEffect(() => {
        // Only a released TRANSITION needs the picker: it knows where the new task goes but not
        // what it is, and `deriveGraphPendingConnection` only ever raises one with a source member.
        // A component dropped into the box named itself at drop time and raises a pending
        // connection purely to carry the position, so opening the picker for it would leave the
        // component list sitting on top of the node it just created.
        if (graphPendingConnection?.graphId === graphId && graphPendingConnection?.from) {
            setAddNodePopoverOpen(true);
        }
    }, [graphId, graphPendingConnection]);

    return (
        <div
            {...{[GRAPH_FRAME_ID_ATTRIBUTE]: graphId}}
            className="rounded-lg border-2 border-dashed border-stroke-neutral-secondary bg-surface-neutral-secondary/40"
            data-nodetype="graphFrame"
            style={{height, width}}
        >
            <div className="flex items-center justify-between px-3" style={{height: GRAPH_FRAME_HEADER_HEIGHT}}>
                <span className="text-sm font-semibold text-content-neutral-secondary">Graph</span>

                {/* Both controls edit the workflow, and an execution view has no mutation to edit
                    it with. Rendering them disabled there would put two dead buttons on a surface
                    where nothing else is editable — the task nodes are swapped for read-only ones
                    and the transitions are stamped read-only — so they are left out entirely. */}

                {updateWorkflowMutation && (
                    <div className="flex items-center gap-1">
                        <button
                            aria-label="Auto-arrange"
                            className={HEADER_BUTTON_CLASSNAME}
                            onClick={handleAutoArrange}
                            type="button"
                        >
                            <Wand2Icon className="size-3.5" />
                            Auto-arrange
                        </button>

                        {/* The graph's single add-node placeholder is the insertion anchor the
                            popover resolves the graph from; it is never painted, so this button is
                            what opens the popover for it. A transition released over empty frame
                            space opens the same popover, with the drop point and the source member
                            riding along in `graphPendingConnection`. */}

                        <WorkflowNodesPopoverMenu
                            // A graph member is a plain task. A cluster element only exists inside a
                            // cluster root, so offering the tab here lists things that cannot be added.
                            hideClusterElementComponents
                            hideTriggerComponents
                            onOpenChange={handleAddNodePopoverOpenChange}
                            open={addNodePopoverOpen}
                            sourceNodeId={`${graphId}-graph-placeholder`}
                        >
                            <button aria-label="Add node" className={HEADER_BUTTON_CLASSNAME} type="button">
                                <PlusIcon className="size-3.5" />
                                Add node
                            </button>
                        </WorkflowNodesPopoverMenu>
                    </div>
                )}
            </div>

            <Handle
                className={styles.handle}
                id={`${id}-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={styles.handle}
                id={`${id}-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export default memo(GraphFrameNode);
