import {EdgeProps, useReactFlow} from 'reactflow';
import {useGetComponentDefinitionsQuery} from '../../../queries/componentDefinitions';
import {useGetTaskDispatcherDefinitionsQuery} from '../../../queries/taskDispatcherDefinitions';

const uuid = (): string =>
    new Date().getTime().toString(36) + Math.random().toString(36).slice(2);

// this hook implements the logic for clicking the button on a workflow edge
// on edge click: create a node in between the two nodes that are connected by the edge
function useEdgeClick(id: EdgeProps['id']) {
    const {getEdge, setEdges, setNodes} = useReactFlow();

    const {data: components} = useGetComponentDefinitionsQuery();

    const {data: flowControls} = useGetTaskDispatcherDefinitionsQuery();

    const handleEdgeClick = () => {
        // first we retrieve the edge object to get the source and target id
        const edge = getEdge(id);

        if (!edge) {
            return;
        }

        // this is the node object that will be added in between source and target node
        const contextualMenuNode = {
            id: uuid(),
            position: {
                x: 450,
                y: 0,
            },
            data: {
                label: 'contextualMenu',
                setNodes: setNodes,
                components,
                flowControls,
            },
            type: 'contextualMenu',
        };

        // new connection from source to new node
        const sourceEdge = {
            id: `${edge.source}->${contextualMenuNode.id}`,
            source: edge.source,
            target: contextualMenuNode.id,
            type: 'workflow',
        };

        // new connection from new node to target
        const targetEdge = {
            id: `${contextualMenuNode.id}->${edge.target}`,
            source: contextualMenuNode.id,
            target: edge.target,
            type: 'workflow',
        };

        // remove the edge that was clicked as we have a new connection with a node inbetween
        setEdges((edges) =>
            edges
                .filter((edge) => edge.id !== id)
                .concat([sourceEdge, targetEdge])
        );

        setNodes((nodes) => {
            if (!nodes.find((node) => node.type === 'contextualMenu')) {
                return nodes.concat(contextualMenuNode);
            } else {
                return nodes;
            }
        });
    };

    return handleEdgeClick;
}

export default useEdgeClick;
