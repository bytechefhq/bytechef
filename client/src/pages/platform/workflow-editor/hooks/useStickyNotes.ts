import {Node, useReactFlow} from '@xyflow/react';
import {useCallback, useEffect} from 'react';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {
    STICKY_NOTE_DEFAULT_HEIGHT,
    STICKY_NOTE_DEFAULT_WIDTH,
    STICKY_NOTE_NODE_TYPE,
    StickyNoteNodeDataType,
    addStickyNote,
    buildStickyNoteNodes,
    compensateStickyNotePosition,
    extractStickyNotes,
} from '../utils/stickyNoteUtils';

const POSITION_EPSILON = 0.5;

const STACKING_OFFSET_STEP = 24;

function stickyNoteNodeDiffers(currentNode: Node, targetNode: Node): boolean {
    const currentData = currentNode.data as StickyNoteNodeDataType;
    const targetData = targetNode.data as StickyNoteNodeDataType;

    return (
        currentData.content !== targetData.content ||
        currentData.color !== targetData.color ||
        currentNode.width !== targetNode.width ||
        currentNode.height !== targetNode.height ||
        Math.abs(currentNode.position.x - targetNode.position.x) > POSITION_EPSILON ||
        Math.abs(currentNode.position.y - targetNode.position.y) > POSITION_EPSILON
    );
}

/**
 * Keeps sticky note canvas nodes in sync with the `stickyNotes` array of the
 * workflow definition, and exposes the "add note" handler. The layout effect in
 * `useLayout` only re-runs on structural task/trigger changes, so note
 * add/edit/delete/undo/redo would otherwise never reach the canvas.
 */
export default function useStickyNotes({readOnly}: {readOnly: boolean}) {
    const definition = useWorkflowDataStore((state) => state.workflow.definition);
    const isNodeDragging = useWorkflowDataStore((state) => state.isNodeDragging);
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const {updateWorkflowMutation} = useWorkflowEditor();

    const {screenToFlowPosition} = useReactFlow();

    const handleAddStickyNote = useCallback(() => {
        if (!updateWorkflowMutation) {
            return;
        }

        const canvasElement = document.querySelector('.react-flow');
        const canvasBounds = canvasElement?.getBoundingClientRect();

        const screenCenter = canvasBounds
            ? {x: canvasBounds.left + canvasBounds.width / 2, y: canvasBounds.top + canvasBounds.height / 2}
            : {x: window.innerWidth / 2, y: window.innerHeight / 2};

        const flowCenter = screenToFlowPosition(screenCenter);

        // Offset consecutive notes a bit so they don't stack exactly on top of each other
        const existingStickyNotesCount = extractStickyNotes(useWorkflowDataStore.getState().workflow.definition).length;
        const stackingOffset = (existingStickyNotesCount % 8) * STACKING_OFFSET_STEP;

        const position = compensateStickyNotePosition({
            x: flowCenter.x - STICKY_NOTE_DEFAULT_WIDTH / 2 + stackingOffset,
            y: flowCenter.y - STICKY_NOTE_DEFAULT_HEIGHT / 2 + stackingOffset,
        });

        addStickyNote({position, updateWorkflowMutation});
    }, [screenToFlowPosition, updateWorkflowMutation]);

    useEffect(() => {
        if (readOnly || isNodeDragging) {
            return;
        }

        const {nodes, savedPositionCrossAxisShift, setNodes} = useWorkflowDataStore.getState();

        const crossAxis = layoutDirection === 'TB' ? 'x' : 'y';

        const stickyNoteNodes = buildStickyNoteNodes({
            crossAxis,
            crossAxisShift: savedPositionCrossAxisShift,
            definition,
            readOnly: false,
        });

        const stickyNoteNodesById = new Map(
            stickyNoteNodes.map((stickyNoteNode) => [stickyNoteNode.id, stickyNoteNode])
        );

        const seenStickyNoteIds = new Set<string>();
        const reconciledNodes: Array<Node> = [];

        let changed = false;

        for (const node of nodes) {
            if (node.type !== STICKY_NOTE_NODE_TYPE) {
                reconciledNodes.push(node);

                continue;
            }

            const targetNode = stickyNoteNodesById.get(node.id);

            if (!targetNode) {
                changed = true;

                continue;
            }

            seenStickyNoteIds.add(node.id);

            if (stickyNoteNodeDiffers(node, targetNode)) {
                // Spread preserves transient canvas state (selection) the target lacks
                reconciledNodes.push({...node, ...targetNode});

                changed = true;
            } else {
                reconciledNodes.push(node);
            }
        }

        for (const stickyNoteNode of stickyNoteNodes) {
            if (!seenStickyNoteIds.has(stickyNoteNode.id)) {
                reconciledNodes.push(stickyNoteNode);

                changed = true;
            }
        }

        if (changed) {
            setNodes(reconciledNodes);
        }
    }, [definition, isNodeDragging, layoutDirection, readOnly]);

    return {handleAddStickyNote};
}
