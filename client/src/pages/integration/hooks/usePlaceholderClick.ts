import {
    useGetComponentsQuery,
    useGetFlowControlsQuery,
} from 'queries/integration.queries';
import {NodeProps, useReactFlow} from 'reactflow';

export default function usePlaceholderClick(id: NodeProps['id']) {
    const {getNode, setNodes, setEdges} = useReactFlow();

    const {data: components} = useGetComponentsQuery();

    const {data: flowControls} = useGetFlowControlsQuery();

    const onClick = () => {
        const placeholderNode = getNode(id);

        if (!placeholderNode) {
            return;
        }
        const contextualMenuNode = {
            id: 'contextualMenu',
            position: {
                x: placeholderNode.position.x + 200,
                y: placeholderNode.position.y,
            },
            data: {
                components,
                flowControls,
                label: 'contextualMenu',
                placeholderId: id,
                setNodes: setNodes,
            },
            type: 'contextualMenu',
        };

        // new connection from source to new node
        const sourceEdge = {
            id: `${placeholderNode.id}->${contextualMenuNode.id}`,
            source: placeholderNode.id,
            target: contextualMenuNode.id,
            type: 'placeholder',
        };

        // new connection from new node to target
        const targetEdge = {
            id: `${contextualMenuNode.id}->${placeholderNode.id}`,
            source: contextualMenuNode.id,
            target: placeholderNode.id,
            type: 'placeholder',
        };

        setNodes((nodes) => nodes.concat(contextualMenuNode));

        setEdges((edges) => edges.concat([sourceEdge, targetEdge]));
    };

    return onClick;
}
