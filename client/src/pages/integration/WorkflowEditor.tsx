import React, {useState, useRef, useCallback, useMemo} from 'react';
import ReactFlow, {
    ReactFlowProvider,
    addEdge,
    useNodesState,
    useEdgesState,
    Controls,
    MiniMap,
    Edge,
    Node,
    Connection,
    ReactFlowInstance,
    ReactFlowRefType,
    XYPosition,
} from 'reactflow';
import 'reactflow/dist/base.css';
import CustomNode from './CustomNode';
import ButtonEdge from './ButtonEdge';
import ContextualMenu from './ContextualMenu';
import {ComponentDefinitionModel} from 'data-access/component-definition';
import {TaskDispatcherDefinitionModel} from 'data-access/task-dispatcher-definition';

import './workflowEditor.css';

let id = 0;

const getId = () => `dndnode_${id++}`;

interface WorkflowEditorProps {
    data: {
        components: Array<ComponentDefinitionModel>;
        flowControls: Array<TaskDispatcherDefinitionModel>;
    };
}

const WorkflowEditor = ({data}: WorkflowEditorProps): JSX.Element => {
    const [nodes, setNodes, onNodesChange] = useNodesState<Node[]>([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState<Edge[]>([]);

    const [reactFlowInstance, setReactFlowInstance] =
        useState<ReactFlowInstance>();

    const reactFlowWrapper = useRef<ReactFlowRefType>(null);

    const nodeTypes = useMemo(
        () => ({
            default: CustomNode,
            contextualMenu: ContextualMenu,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            buttonEdge: ButtonEdge,
        }),
        []
    );

    const handleButtonEdgeClick = useCallback(() => {
        const {components, flowControls} = data;

        const contextualMenuNode: Node = {
            id: getId(),
            position: {
                x: 25,
                y: 0,
            },
            type: 'contextualMenu',
            data: {
                label: 'contextualMenu',
                setNodes: setNodes,
                components,
                flowControls,
            },
        };

        setNodes((nodes) => {
            if (!nodes.find((node) => node.type === 'contextualMenu')) {
                return nodes.concat(contextualMenuNode);
            } else {
                return nodes;
            }
        });
    }, [data, setNodes]);

    const onConnect = useCallback(
        (params: Edge | Connection) => {
            setEdges((edges) =>
                addEdge(
                    {
                        ...params,
                        type: 'buttonEdge',
                        data: {
                            onClick: handleButtonEdgeClick,
                        },
                    },
                    edges
                )
            );
        },
        [handleButtonEdgeClick, setEdges]
    );

    interface DragEvent<T = Element>
        extends React.MouseEvent<T, DragEventInit> {
        dataTransfer: DataTransfer;
    }

    const onDragOver = useCallback((event: DragEvent) => {
        event.preventDefault();

        event.dataTransfer.dropEffect = 'move';
    }, []);

    const onDrop = useCallback(
        (event: DragEvent) => {
            event.preventDefault();

            const nodeLabel = event.dataTransfer.getData(
                'application/reactflow'
            );

            if (!nodeLabel || !reactFlowWrapper.current || !reactFlowInstance) {
                return;
            }

            const reactFlowBounds =
                reactFlowWrapper.current.getBoundingClientRect();

            const position: XYPosition = reactFlowInstance.project({
                x: event.clientX - reactFlowBounds.left,
                y: event.clientY - reactFlowBounds.top,
            });

            const newNode: Node = {
                id: getId(),
                position,
                data: {label: nodeLabel},
            };

            setNodes((nodes) => nodes.concat(newNode));
        },
        [reactFlowInstance, setNodes]
    );

    return (
        <div className="flex h-full flex-1 flex-col">
            <ReactFlowProvider>
                <div className="h-full flex-1" ref={reactFlowWrapper}>
                    <ReactFlow
                        nodes={nodes}
                        edges={edges}
                        edgeTypes={edgeTypes}
                        onNodesChange={onNodesChange}
                        onEdgesChange={onEdgesChange}
                        onConnect={onConnect}
                        onInit={setReactFlowInstance}
                        onDrop={onDrop}
                        onDragOver={onDragOver}
                        nodeTypes={nodeTypes}
                        fitView
                    >
                        <MiniMap />

                        <Controls />
                    </ReactFlow>
                </div>
            </ReactFlowProvider>
        </div>
    );
};

export default WorkflowEditor;
