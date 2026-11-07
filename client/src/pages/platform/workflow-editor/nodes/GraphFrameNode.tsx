import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {GraphTransitionType, NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {PlusIcon, Wand2Icon} from 'lucide-react';
import {memo, useCallback, useEffect, useState} from 'react';

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

const GraphFrameNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [addNodePopoverOpen, setAddNodePopoverOpen] = useState(false);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const graphPendingConnection = useWorkflowEditorStore((state) => state.graphPendingConnection);
    const setGraphPendingConnection = useWorkflowEditorStore((state) => state.setGraphPendingConnection);

    const {updateWorkflowMutation} = useWorkflowEditor();

    const graphId = data.graphFrame?.graphId ?? data.graphId ?? data.taskDispatcherId;
    const height = data.graphFrame?.height ?? GRAPH_FRAME_MIN_HEIGHT;
    const width = data.graphFrame?.width ?? GRAPH_FRAME_MIN_WIDTH;

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

        const dispatcherParameters = (dispatcherNode?.data as NodeDataType)?.parameters;

        const transitions: GraphTransitionType[] = dispatcherParameters?.transitions ?? [];

        const graphStartPinnedBox = getGraphStartPinnedBox();

        const startNodeName = dispatcherParameters?.startNode || memberSizes[0]?.name;

        const nodePositions = await autoPlaceGraphMembers({
            direction: layoutDirection,
            memberSizes,
            pinnedBoxes: [graphStartPinnedBox],
            startNodeName,
            transitions,
        });

        saveWorkflowNodesPosition({draggedNodeId: graphId, nodePositions, updateWorkflowMutation});
    }, [graphId, layoutDirection, updateWorkflowMutation]);

    const handleAddNodePopoverOpenChange = useCallback(
        (open: boolean) => {
            setAddNodePopoverOpen(open);

            const {graphPendingConnection: currentGraphPendingConnection} = useWorkflowEditorStore.getState();

            if (!open && currentGraphPendingConnection?.graphId === graphId) {
                setGraphPendingConnection(undefined);
            }
        },
        [graphId, setGraphPendingConnection]
    );

    useEffect(() => {
        if (graphPendingConnection?.graphId === graphId && graphPendingConnection?.from) {
            setAddNodePopoverOpen(true);
        }
    }, [graphId, graphPendingConnection]);

    return (
        <div
            {...{[GRAPH_FRAME_ID_ATTRIBUTE]: graphId}}
            className="rounded-lg border-2 border-dashed border-stroke-neutral-secondary bg-transparent"
            data-nodetype="graphFrame"
            style={{height, width}}
        >
            <div className="flex items-center justify-end px-3" style={{height: GRAPH_FRAME_HEADER_HEIGHT}}>
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

                        <WorkflowNodesPopoverMenu
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
