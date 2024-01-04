import {ComponentDefinitionKeys, useGetComponentDefinitionQuery} from '@/queries/componentDefinitions.queries';
import {ComponentActionType} from '@/types/types';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import {
    ComponentDefinitionApi,
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
} from 'middleware/hermes/configuration';
import {DragEventHandler, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import ReactFlow, {Controls, Edge, MiniMap, Node, NodeDimensionChange, useReactFlow, useStore} from 'reactflow';

import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import defaultEdges from '../edges/defaultEdges';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import usePrevious from '../hooks/usePrevious';
import PlaceholderNode from '../nodes/PlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';
import defaultNodes from '../nodes/defaultNodes';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';

export type WorkflowEditorProps = {
    componentDefinitions: ComponentDefinitionBasicModel[];
    currentWorkflowId: string;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
};

const WorkflowEditor = ({componentDefinitions, currentWorkflowId, taskDispatcherDefinitions}: WorkflowEditorProps) => {
    const [edges, setEdges] = useState(defaultEdges);
    const [latestComponentName, setLatestComponentName] = useState('');
    const [nodeActions, setNodeActions] = useState<Array<ComponentActionType>>([]);
    const [nodeNames, setNodeNames] = useState<Array<string>>([]);
    const [nodes, setNodes] = useState(defaultNodes);
    const [viewportWidth, setViewportWidth] = useState(0);

    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {componentActions, componentNames, setComponentActions, setComponentNames} = useWorkflowDataStore();
    const {setWorkflowDefinitions, workflowDefinitions} = useWorkflowDefinitionStore();

    const {getEdge, getNode, getNodes, setViewport} = useReactFlow();

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge] = useHandleDrop();

    const queryClient = new QueryClient();

    const previousComponentNames: Array<string> | undefined = usePrevious(componentNames);

    const currentWorkflowDefinition = workflowDefinitions[currentWorkflowId!];

    const nodeTypes = useMemo(
        () => ({
            placeholder: PlaceholderNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            placeholder: PlaceholderEdge,
            workflow: WorkflowEdge,
        }),
        []
    );

    const width = useStore((store) => store.width);

    const {data: workflowComponent} = useGetComponentDefinitionQuery(
        {
            componentName: latestComponentName || componentNames[componentNames.length - 1],
        },
        !!componentNames.length
    );

    const onDrop: DragEventHandler = (event) => {
        const droppedNodeName = event.dataTransfer.getData('application/reactflow');

        const droppedNode = [...componentDefinitions, ...taskDispatcherDefinitions].find(
            (node) => node.name === droppedNodeName
        );

        if (!droppedNode) {
            return;
        }

        if (event.target instanceof HTMLElement) {
            const targetNodeElement = event.target.closest('.react-flow__node') as HTMLElement;

            if (targetNodeElement) {
                const targetNodeId = targetNodeElement.dataset.id!;

                const targetNode = getNode(targetNodeId);

                if (targetNode) {
                    handleDropOnPlaceholderNode(targetNode, droppedNode);
                }
            }
        } else if (event.target instanceof SVGElement) {
            const targetEdgeElement = event.target.closest('.react-flow__edge');

            if (targetEdgeElement) {
                const targetEdge = getEdge(targetEdgeElement.id);

                if (targetEdge) {
                    handleDropOnWorkflowEdge(targetEdge, droppedNode);
                }
            }
        }
    };

    const workflowComponentWithAlias = useMemo(() => {
        if (!workflowComponent) {
            return undefined;
        }

        const workflowNodes = nodeNames.filter((nodeName) => nodeName === workflowComponent.name);

        return {
            ...workflowComponent,
            workflowNodeName: `${workflowComponent.name}_${workflowNodes.length + 1}`,
        };
    }, [nodeNames, workflowComponent]);

    const defaultNodesWithWorkflowNodes = useMemo(() => {
        const workflowTasks = currentWorkflowDefinition?.tasks?.filter((task) => task.name);

        const workflowNodes = workflowTasks?.map((workflowNode, index) => {
            const componentName = workflowNode.type?.split('/')[0];
            const actionName = workflowNode.type?.split('/')[2];

            const componentDefinition = componentDefinitions.find(
                (componentDefinition) => componentDefinition.name === componentName
            );

            if (componentDefinition) {
                return {
                    data: {
                        ...componentDefinition,
                        ...workflowNode,
                        actionName,
                        componentName: componentDefinition.name,
                        icon: (
                            <InlineSVG
                                className="h-9 w-9"
                                loader={<Component1Icon className="h-9 w-9 flex-none text-gray-900" />}
                                src={componentDefinition.icon!}
                            />
                        ),
                        id: componentDefinition.name,
                        label: componentDefinition.title,
                        name: workflowNode.name,
                        type: 'workflow',
                    },
                    id: workflowNode.name,
                    position: {x: 0, y: 150 * (index + 1)},
                    type: 'workflow',
                };
            }
        });

        if (workflowNodes) {
            setNodeNames(workflowNodes.map((node) => node!.data.name));

            setNodeActions(
                workflowNodes.map((node) => ({
                    actionName: node!.data.actionName!,
                    componentName: node!.data.componentName!,
                    workflowNodeName: node?.data.name,
                }))
            );

            const nodes = [...defaultNodes, ...workflowNodes];

            nodes.splice(1, 1);

            return nodes;
        }
    }, [componentDefinitions, currentWorkflowDefinition.tasks]);

    const defaultEdgesWithWorkflowEdges = useMemo(() => {
        const workflowEdges: Array<Edge> = [];

        if (defaultNodesWithWorkflowNodes) {
            defaultNodesWithWorkflowNodes.forEach((node, index) => {
                const nextNode = defaultNodesWithWorkflowNodes[index + 1];

                if (nextNode) {
                    workflowEdges.push({
                        id: `${node!.id}=>${nextNode?.id}`,
                        source: node!.id,
                        target: nextNode?.id,
                        type: 'workflow',
                    });
                } else {
                    const lastNodeId = getRandomId();

                    defaultNodesWithWorkflowNodes.push({
                        data: {label: '+'},
                        id: lastNodeId,
                        position: {x: 0, y: 150 * (index + 1)},
                        type: 'placeholder',
                    });

                    workflowEdges.push({
                        id: `${node!.id}=>${lastNodeId}`,
                        source: node!.id,
                        target: lastNodeId,
                        type: 'placeholder',
                    });
                }
            });

            return workflowEdges;
        }
    }, [defaultNodesWithWorkflowNodes]);

    useEffect(() => {
        if (componentNames && previousComponentNames?.length) {
            const latestName = componentNames.find((componentName) => !previousComponentNames?.includes(componentName));

            if (latestName) {
                setLatestComponentName(latestName);
            }
        }
    }, [componentNames, previousComponentNames]);

    useEffect(() => {
        setComponentActions(nodeActions);
    }, [nodeActions, setComponentActions]);

    useEffect(() => {
        if (defaultNodesWithWorkflowNodes) {
            const workflowNodes = defaultNodesWithWorkflowNodes.filter((node) => node?.data.componentName);

            setNodeNames(workflowNodes.map((node) => node?.data.name));

            setComponentNames(workflowNodes.map((node) => node?.data.componentName));

            setNodes(defaultNodesWithWorkflowNodes as Array<Node>);
        }
    }, [defaultNodesWithWorkflowNodes, currentWorkflowId, setComponentNames]);

    useEffect(() => {
        if (defaultEdgesWithWorkflowEdges) {
            setEdges(defaultEdgesWithWorkflowEdges);
        }
    }, [defaultEdgesWithWorkflowEdges, currentWorkflowId]);

    useEffect(() => {
        if (workflowComponentWithAlias?.actions) {
            const {actions, name} = workflowComponentWithAlias;

            let workflowNodeName = `${name}_1`;
            let index = 2;

            while (componentActions.some((action) => action.workflowNodeName === workflowNodeName)) {
                workflowNodeName = `${name}_${index}`;

                index++;
            }

            setComponentActions([
                ...componentActions,
                {
                    actionName: actions[0].name,
                    componentName: name,
                    workflowNodeName,
                },
            ]);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowComponentWithAlias?.workflowNodeName]);

    useEffect(() => {
        setViewportWidth(width);

        const adaptedViewportWidth = workflowNodeDetailsPanelOpen ? width / 2 - window.innerWidth / 6 : width / 2;

        setViewport({
            x: adaptedViewportWidth,
            y: 50,
            zoom: 1,
        });
    }, [workflowNodeDetailsPanelOpen, setViewport, width]);

    const handleNodeChange = async (changes: NodeDimensionChange[]) => {
        const changesIds = changes.map((change) => change.id);

        const changesIncludeExistingNodes = defaultNodesWithWorkflowNodes?.some((node) =>
            changesIds.includes(node?.data.id)
        );

        if (changesIncludeExistingNodes) {
            return;
        }

        const workflowNodes = getNodes();

        const newNode = workflowNodes.find((node) => node.id === changes[0].id);

        if (!newNode?.data.componentName) {
            return;
        }

        const {componentName, label, name, parameters} = newNode.data;

        const newNodeComponentDefinition = await queryClient.fetchQuery({
            queryFn: () => new ComponentDefinitionApi().getComponentDefinition({componentName}),
            queryKey: ComponentDefinitionKeys.componentDefinition({componentName}),
        });

        if (!newNodeComponentDefinition) {
            return;
        }

        const newWorkflowNode = {
            componentName,
            label,
            name,
            parameters,
            type: `${componentName}/v1/${newNodeComponentDefinition.actions?.[0].name}`,
        };

        const workflowNodeAlreadyExists = currentWorkflowDefinition.tasks?.some(
            (task) => task.name === newWorkflowNode.name
        );

        if (workflowNodeAlreadyExists) {
            return;
        }

        setWorkflowDefinitions({
            ...workflowDefinitions,
            [currentWorkflowId]: {
                ...currentWorkflowDefinition,
                tasks: [...(currentWorkflowDefinition.tasks || []), newWorkflowNode],
            },
        });
    };

    useLayout();

    return (
        <div className="flex h-full flex-1 flex-col">
            <ReactFlow
                defaultEdges={defaultEdgesWithWorkflowEdges}
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                defaultNodes={defaultNodesWithWorkflowNodes as Node<any, string | undefined>[]}
                defaultViewport={{
                    x: viewportWidth / 2,
                    y: 50,
                    zoom: 1,
                }}
                deleteKeyCode={null}
                edgeTypes={edgeTypes}
                edges={edges}
                maxZoom={1.5}
                minZoom={0.6}
                nodeTypes={nodeTypes}
                nodes={nodes}
                nodesConnectable={false}
                nodesDraggable={false}
                onDrop={onDrop}
                onNodesChange={(changes) => {
                    if (changes.length > 1) {
                        handleNodeChange(changes as NodeDimensionChange[]);
                    }
                }}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <MiniMap />

                <Controls />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
