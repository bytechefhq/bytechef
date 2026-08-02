import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {GraphNodeType, NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

type CreateGraphNodePropsType = {
    allNodes: Array<Node>;
    graphId: string;
    isNested?: boolean;
};

/**
 * Creates a placeholder node for one graph node's lane (task-insertion point).
 * The same id shape (`<graphId>-graph-node-<nodeIndex>-placeholder-0`) is reused for the
 * trailing "add a new node" column by passing `nodeIndex = nodes.length` — inserting through
 * that placeholder mints a brand-new `nodes[]` entry (Task 1's `updateTaskParameters`
 * shell-creation semantics), exactly like an existing empty lane's placeholder fills that
 * lane's `tasks`.
 */
function createNodePlaceholderNode(graphId: string, nodeIndex: number): Node {
    return {
        data: {
            graphId,
            label: '+',
            nodeIndex,
            taskDispatcherId: graphId,
        },
        id: `${graphId}-graph-node-${nodeIndex}-placeholder-0`,
        position: DEFAULT_NODE_POSITION,
        type: 'placeholder',
    };
}

/**
 * Creates a top ghost node for a graph task
 */
function createTopGhostNode(graphId: string): Node {
    return {
        data: {
            graphId,
            taskDispatcherId: graphId,
        },
        id: `${graphId}-graph-top-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherTopGhostNode',
    };
}

/**
 * Creates a bottom ghost node for a graph task
 */
function createBottomGhostNode(graphId: string, isNested: boolean = false): Node {
    return {
        data: {
            isNestedBottomGhost: isNested,
            taskDispatcherId: graphId,
        },
        id: `${graphId}-graph-bottom-ghost`,
        position: DEFAULT_NODE_POSITION,
        type: 'taskDispatcherBottomGhostNode',
    };
}

/**
 * Creates all necessary auxiliary nodes for a graph task node: one vertical sub-lane per
 * `parameters.nodes[*]` entry (fork/join-column layout), a placeholder for every lane whose
 * `tasks` list is empty (a graph node legitimately stays empty — it may be a pure router
 * driven only by its `next` expression), and one trailing placeholder column for adding a
 * brand-new node. Unlike fork-join, empty lanes are NOT filtered out: graph nodes are
 * name/index-addressed, not anonymous, so every declared node must render its own lane.
 */
export default function createGraphNode({allNodes, graphId, isNested = false}: CreateGraphNodePropsType): Node[] {
    const nodesWithGraph = [...allNodes];
    const insertIndex = nodesWithGraph.findIndex((node) => node.id === graphId) + 1;
    const nodesToAdd: Array<Node> = [];

    const graphNodeData = nodesWithGraph.find((node) => node.id === graphId)?.data as NodeDataType;

    const nodes: Array<GraphNodeType> = graphNodeData?.parameters?.nodes ?? [];

    nodesToAdd.push(createTopGhostNode(graphId));

    nodes.forEach((graphNode, nodeIndex) => {
        if (!graphNode?.tasks || graphNode.tasks.length === 0) {
            nodesToAdd.push(createNodePlaceholderNode(graphId, nodeIndex));
        }
    });

    nodesToAdd.push(createNodePlaceholderNode(graphId, nodes.length));

    nodesToAdd.push(createBottomGhostNode(graphId, isNested));

    nodesWithGraph.splice(insertIndex, 0, ...nodesToAdd);

    return nodesWithGraph;
}
