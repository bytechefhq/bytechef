import defaultEdges from '@/shared/defaultEdges';
import defaultNodes from '@/shared/defaultNodes';
import {
    ActionDefinitionBasicModel,
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
} from '@/shared/middleware/platform/configuration';
import {ComponentOperationType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {Component1Icon} from '@radix-ui/react-icons';
import {usePrevious} from '@uidotdev/usehooks';
import {DragEventHandler, useCallback, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useParams} from 'react-router-dom';
import ReactFlow, {Controls, Edge, MiniMap, Node, useReactFlow, useStore} from 'reactflow';

import ConditionChildEdge from '../edges/ConditionChildEdge';
import ConditionEdge from '../edges/ConditionEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import ConditionNode from '../nodes/ConditionNode';
import PlaceholderNode from '../nodes/PlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

const FALSE_CONDITION_NODE_POSITION_X = 310;
const TRUE_CONDITION_NODE_POSITION_X = -140;

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasicModel[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
}

const WorkflowEditor = ({componentDefinitions, taskDispatcherDefinitions}: WorkflowEditorProps) => {
    const [edges, setEdges] = useState<Array<Edge>>();
    const [latestComponentName, setLatestComponentName] = useState('');
    const [nodeOperations, setNodeOperations] = useState<Array<ComponentOperationType>>([]);
    const [nodes, setNodes] = useState<Array<Node>>();
    const [viewportWidth, setViewportWidth] = useState(0);
    const [workflowComponentWithAlias, setWorkflowComponentWithAlias] = useState<
        | (ComponentDefinitionBasicModel & {actions?: Array<ActionDefinitionBasicModel>; workflowNodeName: string})
        | undefined
    >();

    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {componentActions, latestComponentDefinition, setComponentActions, setWorkflow, workflow} =
        useWorkflowDataStore();

    const {componentNames} = workflow;

    const {getEdge, getNode, getNodes, setViewport} = useReactFlow();

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode] = useHandleDrop();

    const {projectId, projectWorkflowId} = useParams();

    const previousComponentNames: Array<string> | undefined = usePrevious(componentNames || []);

    const nodeTypes = useMemo(
        () => ({
            condition: ConditionNode,
            placeholder: PlaceholderNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            condition: ConditionEdge,
            conditionChild: ConditionChildEdge,
            placeholder: PlaceholderEdge,
            workflow: WorkflowEdge,
        }),
        []
    );

    const width = useStore((store) => store.width);

    const onDragOver: DragEventHandler = useCallback((event) => {
        if (event.target instanceof HTMLButtonElement && event.target.dataset.nodeType === 'workflow') {
            return;
        }

        event.preventDefault();

        event.dataTransfer.dropEffect = 'move';
    }, []);

    const onDrop: DragEventHandler = (event) => {
        const droppedNodeData = event.dataTransfer.getData('application/reactflow');

        let droppedNodeType = '';
        let droppedNodeName;

        if (droppedNodeData.includes('--')) {
            droppedNodeName = droppedNodeData.split('--')[0];

            droppedNodeType = droppedNodeData.split('--')[1];
        } else {
            droppedNodeName = droppedNodeData;
        }

        const droppedNode = [...componentDefinitions, ...taskDispatcherDefinitions].find(
            (node) => node.name === droppedNodeName
        );

        if (!droppedNode) {
            return;
        }

        if (droppedNodeType === 'trigger') {
            const targetChildNode = (event.target as HTMLElement).closest('.react-flow__node > div') as HTMLElement;

            const targetNodeType = targetChildNode?.dataset.nodetype;

            const targetNodeElement =
                event.target instanceof HTMLElement
                    ? targetChildNode?.parentNode
                    : (event.target as SVGElement).closest('.react-flow__node');

            if (targetNodeType === 'trigger' && targetNodeElement instanceof HTMLElement) {
                const targetNodeId = targetNodeElement.dataset.id;

                if (!targetNodeId) {
                    return;
                }

                const targetNode = getNode(targetNodeId);

                if (targetNode) {
                    handleDropOnTriggerNode(droppedNode);
                }

                return;
            }
        } else {
            if (event.target instanceof HTMLElement) {
                const targetNodeElement = event.target.closest('.react-flow__node') as HTMLElement;

                if (!targetNodeElement) {
                    return;
                }

                if (targetNodeElement.dataset.nodetype === 'trigger') {
                    return;
                }

                const targetNodeId = targetNodeElement.dataset.id!;

                const targetNode = getNode(targetNodeId);

                if (targetNode && targetNode.type === 'placeholder') {
                    if (targetNode?.position.x === 0 && targetNode?.position.y === 0) {
                        return;
                    }

                    handleDropOnPlaceholderNode(targetNode, droppedNode);
                }
            } else if (event.target instanceof SVGElement) {
                const targetEdgeElement = event.target.closest('.react-flow__edge') as HTMLElement;

                if (
                    !targetEdgeElement ||
                    (targetEdgeElement.parentNode as HTMLElement).dataset?.nodetype === 'trigger'
                ) {
                    return;
                }

                const targetEdge = getEdge(targetEdgeElement.id);

                if (targetEdge) {
                    handleDropOnWorkflowEdge(targetEdge, droppedNode);

                    return;
                }
            }
        }
    };

    const defaultNodesWithWorkflowNodes: Array<Node> | undefined = useMemo(() => {
        if (!workflow || !componentDefinitions.length) {
            return;
        }

        const workflowTasks = workflow.tasks?.filter((task) => task.name);
        const workflowTrigger = workflow.triggers?.[0] || defaultNodes[0].data;

        let workflowComponents = workflowTasks;

        if (workflowTrigger) {
            workflowComponents = [workflowTrigger, ...(workflowTasks || [])];
        }

        const workflowNodes = workflowComponents?.map((component, index) => {
            const componentName = component.type?.split('/')[0];
            const operationName = component.type?.split('/')[2];

            const combinedDefinitions = [...componentDefinitions, ...taskDispatcherDefinitions];

            let workflowNodeDefinition = combinedDefinitions.find((definition) => definition.name === componentName);

            if (!workflowNodeDefinition) {
                workflowNodeDefinition = combinedDefinitions.find(
                    (componentDefinition) => componentDefinition.name === 'missing'
                )!;
            }

            const nodeType =
                taskDispatcherDefinitions.find((definition) => definition.name === componentName)?.name ?? 'workflow';

            const previousWorkflowNode = workflowComponents?.[index - 1];

            let positionY = 150 * index;
            let positionX = 0;

            const previousConditionNodeCount = workflowComponents
                ?.slice(0, index)
                .filter((node) => node.type.startsWith('condition')).length;

            const previousNodeCount = workflowComponents?.slice(0, index).filter((node) => node.name).length;

            if (previousWorkflowNode && previousNodeCount > 1) {
                if (nodeType !== 'condition' && !component.metadata?.conditionChild) {
                    positionY =
                        (previousNodeCount - previousConditionNodeCount) * 150 +
                        (previousConditionNodeCount || 1) * 150;

                    if (previousWorkflowNode.metadata?.conditionChild) {
                        positionY += 100;
                    }
                } else if (nodeType === 'condition') {
                    positionY =
                        (previousNodeCount - previousConditionNodeCount + 1) * 150 + previousConditionNodeCount * 100;
                }
            }

            if (component.metadata?.conditionTrue) {
                positionX = TRUE_CONDITION_NODE_POSITION_X;
            }

            if (component.metadata?.conditionFalse) {
                positionX = FALSE_CONDITION_NODE_POSITION_X;
            }

            const previousTrueConditionNodeCount = workflowComponents
                .slice(0, index)
                .filter((component) => component.metadata?.conditionTrue).length;

            const previousFalseConditionNodeCount = workflowComponents
                .slice(0, index)
                .filter((component) => component.metadata?.conditionFalse).length;

            const previousNonConditionChildNodeCount = workflowComponents
                .slice(0, index)
                .filter((component) => !component.metadata?.conditionChild).length;

            if (component.metadata?.conditionChild) {
                if (component.metadata.conditionTrue) {
                    positionY = previousNonConditionChildNodeCount * 150 + 150 * previousTrueConditionNodeCount;
                } else if (component.metadata.conditionFalse) {
                    positionY = previousNonConditionChildNodeCount * 150 + 150 * previousFalseConditionNodeCount;
                }
            } else {
                const previousNodeCount = workflowComponents
                    .slice(0, index)
                    .filter((component) => component.name).length;

                positionY =
                    (previousNodeCount - Math.min(previousTrueConditionNodeCount, previousFalseConditionNodeCount)) *
                    150;
            }

            return {
                data: {
                    ...component,
                    componentName: workflowNodeDefinition.name,
                    icon: (
                        <InlineSVG
                            className="size-9"
                            loader={<Component1Icon className="size-9 flex-none text-gray-900" />}
                            src={workflowNodeDefinition.icon!}
                        />
                    ),
                    id: workflowNodeDefinition.name,
                    label: workflowNodeDefinition.title,
                    name: component.name,
                    operationName,
                    trigger: index === 0,
                    type: nodeType,
                },
                id: component.name,
                position: {
                    x: positionX,
                    y: positionY,
                },
                type: nodeType,
            };
        });

        if (workflowNodes?.length) {
            setNodeOperations(
                workflowNodes.map((node) => ({
                    componentName: node.data.componentName,
                    operationName: node.data.operationName,
                    workflowNodeName: node.data.name,
                }))
            );

            return workflowNodes;
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow?.tasks, workflow?.triggers, workflow.id]);

    const defaultEdgesWithWorkflowEdges = useMemo(() => {
        const workflowEdges: Array<Edge> = [];

        if (defaultNodesWithWorkflowNodes) {
            const lastNodeId = getRandomId();

            defaultNodesWithWorkflowNodes.forEach((node, index) => {
                const nextNode = defaultNodesWithWorkflowNodes[index + 1];

                let edgeType = node.type === 'workflow' ? 'workflow' : 'condition';

                if (node.data.metadata?.conditionChild) {
                    edgeType = 'conditionChild';
                }

                if (nextNode) {
                    if (edgeType === 'condition') {
                        const nextTrueConditionNode = defaultNodesWithWorkflowNodes
                            .slice(index + 1)
                            .find((node) => node.data.metadata?.conditionTrue || !node.data.metadata?.conditionChild);

                        workflowEdges.push({
                            id: `${node!.id}left=>${nextTrueConditionNode?.id || lastNodeId}`,
                            source: node!.id,
                            sourceHandle: 'left',
                            target: nextTrueConditionNode?.id || lastNodeId,
                            type: edgeType,
                        });

                        const nextFalseConditionNode = defaultNodesWithWorkflowNodes.slice(index + 1).find((node) => {
                            return node.data.metadata?.conditionFalse || !node.data.metadata?.conditionChild;
                        });

                        workflowEdges.push({
                            id: `${node!.id}right=>${nextFalseConditionNode?.id || lastNodeId}`,
                            source: node!.id,
                            sourceHandle: 'right',
                            target: nextFalseConditionNode?.id || lastNodeId,
                            type: edgeType,
                        });
                    } else if (edgeType === 'conditionChild') {
                        if (node.data.metadata?.conditionTrue) {
                            const nextTrueConditionNode = defaultNodesWithWorkflowNodes
                                .slice(index + 1)
                                .find(
                                    (node) => node.data.metadata?.conditionTrue || !node.data.metadata?.conditionChild
                                );

                            workflowEdges.push({
                                id: `${node!.id}left=>${nextTrueConditionNode?.id || lastNodeId}`,
                                source: node!.id,
                                sourceHandle: 'left',
                                target: nextTrueConditionNode?.id || lastNodeId,
                                type: edgeType,
                            });
                        } else if (node.data.metadata?.conditionFalse) {
                            const nextFalseConditionNode = defaultNodesWithWorkflowNodes
                                .slice(index + 1)
                                .find((node) => {
                                    return node.data.metadata?.conditionFalse || !node.data.metadata?.conditionChild;
                                });

                            workflowEdges.push({
                                id: `${node!.id}right=>${nextFalseConditionNode?.id || lastNodeId}`,
                                source: node!.id,
                                sourceHandle: 'right',
                                target: nextFalseConditionNode?.id || lastNodeId,
                                type: edgeType,
                            });
                        }
                    } else {
                        workflowEdges.push({
                            id: `${node!.id}=>${nextNode?.id}`,
                            source: node!.id,
                            target: nextNode?.id,
                            type: edgeType,
                        });
                    }
                } else {
                    const lastNode = node;

                    let positionY = lastNode?.position.y + 150;

                    if (lastNode.type === 'condition') {
                        positionY += 100;
                    }

                    defaultNodesWithWorkflowNodes.push({
                        data: {label: '+'},
                        id: lastNodeId,
                        position: {x: 0, y: positionY},
                        type: 'placeholder',
                    });

                    let lastEdgeType = lastNode?.type === 'workflow' ? 'placeholder' : lastNode?.type;

                    if (lastNode?.data.metadata?.conditionChild) {
                        lastEdgeType = 'conditionChild';
                    }

                    if (lastEdgeType === 'condition') {
                        const newWorkflowEdge = {
                            source: node!.id,
                            target: lastNodeId,
                            type: lastEdgeType,
                        };

                        workflowEdges.push({
                            ...newWorkflowEdge,
                            id: `${node!.id}left=>${lastNodeId}`,
                            sourceHandle: 'left',
                        });

                        workflowEdges.push({
                            ...newWorkflowEdge,
                            id: `${node!.id}right=>${lastNodeId}`,
                            sourceHandle: 'right',
                        });
                    } else {
                        workflowEdges.push({
                            id: `${node!.id}=>${lastNodeId}`,
                            source: node!.id,
                            target: lastNodeId,
                            type: lastEdgeType,
                        });
                    }
                }
            });

            return workflowEdges;
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes, workflow.id]);

    // Reset workflow data store when projectWorkflowId changes
    useEffect(() => {
        useWorkflowDataStore.getState().reset();
        useWorkflowNodeDetailsPanelStore.getState().reset();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, projectWorkflowId]);

    // Update nodes and edges when workflow changes
    useEffect(() => {
        if (!defaultNodesWithWorkflowNodes) {
            return;
        }

        const nodes: Array<Node> = defaultNodesWithWorkflowNodes.map((node, index) => {
            if (node.data.metadata?.conditionTrue) {
                node.position.x = TRUE_CONDITION_NODE_POSITION_X;
            }

            if (node.data.metadata?.conditionFalse) {
                node.position.x = FALSE_CONDITION_NODE_POSITION_X;
            }

            if (node.data.metadata?.conditionChild) {
                const parentConditionNode = defaultNodesWithWorkflowNodes
                    .slice(0, index)
                    .find((node) => node.data.type.startsWith('condition'));

                const basePositionY = (parentConditionNode?.position.y || 0) + 150;

                if (node.data.metadata.conditionTrue) {
                    const previousTrueConditionNodeCount = defaultNodesWithWorkflowNodes
                        .slice(0, index)
                        .filter((node) => node.data.metadata?.conditionTrue).length;

                    node.position.y = basePositionY + 150 * previousTrueConditionNodeCount;
                } else if (node.data.metadata.conditionFalse) {
                    const previousFalseConditionNodeCount = defaultNodesWithWorkflowNodes
                        .slice(0, index)
                        .filter((node) => node.data.metadata?.conditionFalse).length;

                    node.position.y = basePositionY + 150 * previousFalseConditionNodeCount;
                }
            }

            const previousNode = defaultNodesWithWorkflowNodes[index - 1];

            if (previousNode && !node.data.metadata?.conditionChild) {
                const previousLowestNode = defaultNodesWithWorkflowNodes
                    .slice(0, index)
                    .sort((a, b) => b.position.y - a.position.y)[0];

                node.position.y = previousLowestNode.position.y + 150;

                if (index === defaultNodesWithWorkflowNodes.length - 1 && previousNode.type?.startsWith('condition')) {
                    node.position.y = previousLowestNode.position.y + 250;
                }
            }

            return node;
        });

        setNodes(nodes);

        setEdges(defaultEdgesWithWorkflowEdges);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes?.length, defaultEdgesWithWorkflowEdges?.length, workflow.id]);

    // Set workflowComponentWithAlias when latestComponentDefinition is changed
    useEffect(() => {
        if (!latestComponentDefinition || !componentNames?.length || !latestComponentName) {
            return;
        }

        const sameComponentNames = componentNames.filter((nodeName) => nodeName === latestComponentDefinition.name);

        setWorkflowComponentWithAlias({
            ...latestComponentDefinition,
            workflowNodeName: `${latestComponentDefinition.name}_${sameComponentNames.length}`,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [latestComponentDefinition?.name, componentNames]);

    // Update workflow node names when nodes change
    useEffect(() => {
        const workflowNodes = getNodes();

        if (workflowNodes?.length) {
            const workflowNodeNames = workflowNodes.map((node) => {
                if (node.data.type !== 'placeholder' && node?.data.name) {
                    return node?.data.name;
                }
            });

            setWorkflow({
                ...workflow,
                nodeNames: workflowNodeNames.filter((nodeName) => !!nodeName),
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [setWorkflow, nodes]);

    // Set latest component name when component names change
    useEffect(() => {
        if (componentNames && previousComponentNames?.length) {
            const latestName = componentNames.find((componentName) => {
                const currentNameCount = componentNames.filter((name) => name === componentName).length;

                const previousNameCount = previousComponentNames?.filter((name) => name === componentName).length;

                return currentNameCount > previousNameCount;
            });

            if (latestName) {
                setLatestComponentName(latestName);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [previousComponentNames, componentNames]);

    // Set component actions when node actions change
    useEffect(() => {
        setComponentActions(nodeOperations);
    }, [nodeOperations, setComponentActions]);

    // Reconstruct editor nodes on re-render
    useEffect(() => {
        const outOfDate = componentNames?.some(
            (componentName) => !defaultNodesWithWorkflowNodes?.some((node) => node.data.componentName === componentName)
        );

        if (outOfDate) {
            return;
        }

        if (defaultNodesWithWorkflowNodes?.length) {
            const workflowNodes = defaultNodesWithWorkflowNodes.filter((node) => node?.data.componentName);

            setWorkflow({
                ...workflow,
                componentNames: workflowNodes.map((node) => node?.data.componentName),
                nodeNames: workflowNodes.map((node) => node?.data.name),
            });

            setNodes(defaultNodesWithWorkflowNodes as Array<Node>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes]);

    // Reconstruct editor edges on re-render
    useEffect(() => {
        const isEdgesUnchanged = defaultEdgesWithWorkflowEdges?.every((edge, index) => edge.id === edges?.[index]?.id);

        if (!isEdgesUnchanged) {
            setEdges(defaultEdgesWithWorkflowEdges);
        }
    }, [defaultEdgesWithWorkflowEdges, edges]);

    // Append counter to workflowNodeName when a new node with the same name is added
    useEffect(() => {
        if (workflowComponentWithAlias?.actions) {
            const {actions, name} = workflowComponentWithAlias;

            let workflowNodeName = `${name}_1`;
            let index = 2;

            while (componentActions.some((action) => action.workflowNodeName === workflowNodeName)) {
                workflowNodeName = `${name}_${index}`;

                index++;
            }

            const operationNames = componentActions.map((action) => action.operationName);

            if (actions.length && !operationNames.includes(actions[0].name)) {
                setComponentActions([
                    ...componentActions,
                    {
                        componentName: name,
                        operationName: actions[0].name,
                        workflowNodeName,
                    },
                ]);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowComponentWithAlias?.workflowNodeName]);

    // Set viewport width and position
    useEffect(() => {
        setViewportWidth(width);

        const adaptedViewportWidth = workflowNodeDetailsPanelOpen
            ? width / 2 - window.innerWidth / 6.5
            : width / 2 - 38;

        setViewport({
            x: adaptedViewportWidth,
            y: 50,
            zoom: 1,
        });
    }, [workflowNodeDetailsPanelOpen, setViewport, width]);

    useLayout();

    return (
        <div className="flex h-full flex-1 flex-col">
            <ReactFlow
                defaultEdges={defaultEdgesWithWorkflowEdges || defaultEdges}
                defaultNodes={defaultNodesWithWorkflowNodes || defaultNodes}
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
                onDragOver={onDragOver}
                onDrop={onDrop}
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
