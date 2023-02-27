import {Component1Icon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import {memo, useEffect, useState} from 'react';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '../../../middleware/definition-registry';
import WorkflowNodesList from '../components/WorkflowNodesList';

const uuid = (): string =>
    new Date().getTime().toString(36) + Math.random().toString(36).slice(2);

interface ContextualMenuProps {
    components: ComponentDefinitionBasicModel[] | undefined;
    flowControls: TaskDispatcherDefinitionModel[] | undefined;
    id: string;
}

const ContextualMenu = ({
    components,
    flowControls,
    id,
}: ContextualMenuProps): JSX.Element => {
    const [filter, setFilter] = useState('');
    const [filteredComponents, setFilteredComponents] = useState<
        Array<ComponentDefinitionBasicModel>
    >([]);
    const [filteredFlowControls, setFilteredFlowControls] = useState<
        Array<TaskDispatcherDefinitionModel>
    >([]);

    const {getNode, setEdges, setNodes} = useReactFlow();

    const handleItemClick = (
        clickedItem:
            | ComponentDefinitionBasicModel
            | TaskDispatcherDefinitionModel
    ) => {
        const placeholderNode = getNode(id);

        if (!placeholderNode) {
            return;
        }

        const placeholderId = placeholderNode.id;
        const childPlaceholderId = uuid();

        // create a placeholder node that will be added as a child of the clicked node
        const childPlaceholderNode = {
            id: childPlaceholderId,
            position: {
                x: placeholderNode.position.x,
                y: placeholderNode.position.y,
            },
            type: 'placeholder',
            data: {label: '+'},
            style: {
                zIndex: 9999,
            },
        };

        // we need a connection from the clicked node to the new placeholder
        const childPlaceholderEdge = {
            id: `${placeholderId}=>${childPlaceholderId}`,
            source: placeholderId,
            target: childPlaceholderId,
            type: 'placeholder',
        };

        setNodes((nodes: Node[]) =>
            nodes
                .map((node) => {
                    // here we are changing the type of the clicked node from placeholder to workflow
                    if (node.id === placeholderId) {
                        return {
                            ...node,
                            type: 'workflow',
                            data: {
                                label: clickedItem.display?.label,
                                name: clickedItem?.name,
                                icon: (
                                    <Component1Icon className="h-8 w-8 text-gray-700" />
                                ),
                            },
                        };
                    }

                    return node;
                })
                // add the new placeholder node
                .concat([childPlaceholderNode])
        );

        setEdges((edges: Edge[]) =>
            edges
                .map((edge) => {
                    // here we are changing the type of the connecting edge from placeholder to workflow
                    if (edge.target === id) {
                        return {
                            ...edge,
                            type: 'workflow',
                            markerEnd: {
                                type: MarkerType.ArrowClosed,
                            },
                        };
                    }

                    return edge;
                })
                // add the new placeholder edge
                .concat([childPlaceholderEdge])
        );
    };

    useEffect(() => {
        if (components && flowControls) {
            setFilteredComponents(
                components.filter(
                    (component) =>
                        component.name
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()) ||
                        component.display?.label
                            ?.toLowerCase()
                            .includes(filter.toLowerCase())
                )
            );

            setFilteredFlowControls(
                flowControls.filter(
                    (flowControl) =>
                        flowControl.name
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()) ||
                        flowControl.display?.label
                            ?.toLowerCase()
                            .includes(filter.toLowerCase())
                )
            );
        }
    }, [components, filter, flowControls]);

    return (
        // eslint-disable-next-line tailwindcss/no-custom-classname
        <div className="nowheel rounded-md bg-white shadow-md">
            {typeof components === 'undefined' ||
                (typeof flowControls === 'undefined' && (
                    <div className="py-2 px-3 text-xs text-gray-500">
                        Something went wrong.
                    </div>
                ))}

            <header className="border-b border-gray-200 px-3 pt-2 text-center text-gray-600">
                <Input
                    name="contextualMenuFilter"
                    placeholder="Filter workflow nodes"
                    value={filter}
                    onChange={(event) => setFilter(event.target.value)}
                />
            </header>

            <main className="max-h-64 overflow-auto bg-gray-100">
                <WorkflowNodesList
                    components={filteredComponents}
                    flowControls={filteredFlowControls}
                    onItemClick={handleItemClick}
                />
            </main>
        </div>
    );
};

export default memo(ContextualMenu);
