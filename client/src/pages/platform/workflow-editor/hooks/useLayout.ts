import {stratify, tree} from 'd3-hierarchy';
import {useEffect} from 'react';
import {Edge, Node, ReactFlowState, useReactFlow, useStore} from 'reactflow';

// initialize the tree layout (see https://observablehq.com/@d3/tree for examples)
const layout = tree<Node>()
    // the node size configures the spacing between the nodes ([width, height])
    .nodeSize([200, 150])
    // this is needed for creating equal space between all nodes
    .separation(() => 1);

// the layouting function
// accepts current nodes and edges and returns the layouted nodes with their updated positions
function layoutNodes(nodes: Node[], edges: Edge[]): Node[] {
    // convert nodes and edges into a hierarchical object for using it with the layout function
    const hierarchy = stratify<Node>()
        .id((data) => data.id)
        // get the id of each node by searching through the edges
        // this only works if every node has one connection
        .parentId((node: Node) => edges.find((edge: Edge) => edge.target === node.id)?.source)(nodes);

    // run the layout algorithm with the hierarchy data structure
    const root = layout(hierarchy);

    const descendants = root.descendants();

    // convert the hierarchy back to react flow nodes (the original node is stored as d.data)
    // we only extract the position from the d3 function
    return descendants.map((descendant) => ({
        ...descendant.data,
        position: {
            x: descendant.parent ? descendant.parent.x : descendant.x,
            y: descendant.y,
        },
    }));
}

// this is the store selector that is used for triggering the layout, this returns the number of nodes once they change
const nodeCountSelector = (state: ReactFlowState) => state.nodeInternals.size;

export default function useLayout() {
    const nodeCount = useStore(nodeCountSelector);

    const {getEdges, getNodes, setNodes} = useReactFlow();

    useEffect(() => {
        const nodes = getNodes();
        const edges = getEdges();

        const targetNodes = layoutNodes(nodes, edges);

        return setNodes(targetNodes);
    }, [nodeCount, getEdges, getNodes, setNodes]);
}
