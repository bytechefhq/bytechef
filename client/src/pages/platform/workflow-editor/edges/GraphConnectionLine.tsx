import {GRAPH_TRANSITION_EDGE_COLOR, GRAPH_TRANSITION_EDGE_DANGLING_COLOR} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {InternalNode, Node, ViewportPortal, getSmoothStepPath, useConnection} from '@xyflow/react';

export function getGraphConnectionDragGraphId(fromNode: InternalNode | Node | null | undefined): string | undefined {
    const nodeData = fromNode?.data as NodeDataType | undefined;

    return nodeData?.graphData?.graphId ?? nodeData?.graphStart?.graphId;
}

const GraphConnectionLine = () => {
    const connection = useConnection();

    if (!connection.inProgress || !getGraphConnectionDragGraphId(connection.fromNode)) {
        return null;
    }

    const [path] = getSmoothStepPath({
        borderRadius: 10,
        sourcePosition: connection.fromPosition,
        sourceX: connection.from.x,
        sourceY: connection.from.y,
        targetPosition: connection.toPosition,
        targetX: connection.to.x,
        targetY: connection.to.y,
    });

    return (
        <ViewportPortal>
            <svg
                aria-hidden="true"
                className="pointer-events-none"
                data-graph-connection-line
                style={{height: '100%', left: 0, overflow: 'visible', position: 'absolute', top: 0, width: '100%'}}
            >
                <path
                    d={path}
                    fill="none"
                    stroke={
                        connection.isValid === false
                            ? GRAPH_TRANSITION_EDGE_DANGLING_COLOR
                            : GRAPH_TRANSITION_EDGE_COLOR
                    }
                    strokeDasharray="6 4"
                    strokeWidth={2}
                />
            </svg>
        </ViewportPortal>
    );
};

export default GraphConnectionLine;
