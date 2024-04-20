import {UpdateWorkflowRequest} from '@/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import {ComponentOperationType} from '@/types/types';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
    WorkflowModel,
} from 'middleware/platform/configuration';
import {DragEventHandler, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import ReactFlow, {
    Controls,
    Edge,
    MiniMap,
    Node,
    NodeChange,
    NodeDimensionChange,
    useReactFlow,
    useStore,
} from 'reactflow';

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
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasicModel[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const WorkflowEditor = ({
    componentDefinitions,
    taskDispatcherDefinitions,
    updateWorkflowMutation,
}: WorkflowEditorProps) => {
    const [edges, setEdges] = useState(defaultEdges);
    const [latestComponentName, setLatestComponentName] = useState('');
    const [nodeOperations, setNodeOperations] = useState<Array<ComponentOperationType>>([]);
    const [nodes, setNodes] = useState(defaultNodes);
    const [viewportWidth, setViewportWidth] = useState(0);

    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {componentActions, setComponentActions, setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames, nodeNames} = workflow;

    const {getEdge, getNode, getNodes, setViewport} = useReactFlow();

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge] = useHandleDrop();

    const previousComponentNames: Array<string> | undefined = usePrevious(componentNames || []);

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

    const lastComponentName = componentNames?.[componentNames?.length - 1];

    const {data: workflowComponent} = useGetComponentDefinitionQuery(
        {
            componentName: latestComponentName || lastComponentName!,
        },
        !!componentNames?.length
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
        if (!workflowComponent || !nodeNames?.length) {
            return undefined;
        }

        const sameNodeNames = nodeNames.filter((nodeName) => nodeName === workflowComponent.name);

        return {
            ...workflowComponent,
            workflowNodeName: `${workflowComponent.name}_${sameNodeNames.length + 1}`,
        };
    }, [nodeNames, workflowComponent]);

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

        const workflowNodes = workflowComponents?.map((workflowComponent, index) => {
            const componentName = workflowComponent.type?.split('/')[0];
            const operationName = workflowComponent.type?.split('/')[2];

            const componentDefinition = componentDefinitions.find(
                (componentDefinition) => componentDefinition.name === componentName
            )!;

            return {
                data: {
                    ...workflowComponent,
                    componentName: componentDefinition.name,
                    icon: (
                        <InlineSVG
                            className="size-9"
                            loader={<Component1Icon className="size-9 flex-none text-gray-900" />}
                            src={componentDefinition.icon!}
                        />
                    ),
                    id: componentDefinition.name,
                    label: componentDefinition.title,
                    name: workflowComponent.name,
                    operationName,
                    trigger: index === 0,
                    type: 'workflow',
                },
                id: workflowComponent.name,
                position: {x: 0, y: 150 * index},
                type: 'workflow',
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
    }, [componentDefinitions, workflow?.tasks, workflow?.triggers]);

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

    const handleNodeChange = async (changes: NodeDimensionChange[]) => {
        const changesIds = changes.map((change) => change.id);

        const changesIncludeExistingNodes = defaultNodesWithWorkflowNodes?.some((node) =>
            changesIds.includes(node?.data.name)
        );

        if (changesIncludeExistingNodes) {
            return;
        }

        const workflowNodes = getNodes();

        const newNode = workflowNodes.find((node) => node.id === changes[0].id);

        if (!newNode?.data.componentName) {
            return;
        }

        if (workflow.triggers?.length && newNode?.data.trigger) {
            return;
        }

        saveWorkflowDefinition(newNode.data, workflow!, updateWorkflowMutation);
    };

    const workflowNodes = getNodes();

    // Update workflow node names when nodes change
    useEffect(() => {
        if (workflowNodes?.length) {
            const workflowNodeNames = workflowNodes.map((node) => {
                if (node.data.type === 'workflow' && node?.data.name) {
                    return node?.data.name;
                }
            });

            setWorkflow({
                ...workflow,
                nodeNames: workflowNodeNames.filter((nodeName) => !!nodeName),
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [setWorkflow, workflowNodes?.length]);

    // Set latest component name when component names change
    useEffect(() => {
        if (componentNames && previousComponentNames?.length) {
            const latestName = componentNames.find((componentName) => !previousComponentNames?.includes(componentName));

            if (latestName) {
                setLatestComponentName(latestName);
            }
        }
    }, [componentNames, previousComponentNames]);

    // Set component actions when node actions change
    useEffect(() => {
        setComponentActions(nodeOperations);
    }, [nodeOperations, setComponentActions]);

    // Reconstruct editor nodes on re-render
    useEffect(() => {
        if (defaultNodesWithWorkflowNodes) {
            const workflowNodes = defaultNodesWithWorkflowNodes.filter((node) => node?.data.componentName);

            setWorkflow({
                ...workflow,
                componentNames: workflowNodes.map((node) => node?.data.componentName),
                nodeNames: workflowNodes.map((node) => node?.data.name),
            });

            setNodes(defaultNodesWithWorkflowNodes as Array<Node>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes, setWorkflow]);

    // Reconstruct editor edges on re-render
    useEffect(() => {
        if (defaultEdgesWithWorkflowEdges) {
            setEdges(defaultEdgesWithWorkflowEdges);
        }
    }, [defaultEdgesWithWorkflowEdges]);

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

        const adaptedViewportWidth = workflowNodeDetailsPanelOpen ? width / 2 - window.innerWidth / 6 : width / 2;

        setViewport({
            x: adaptedViewportWidth,
            y: 50,
            zoom: 1,
        });
    }, [workflowNodeDetailsPanelOpen, setViewport, width]);

    // If no custom trigger is set on first render, set Manual Trigger as default trigger
    useEffect(() => {
        if (!workflow.triggers?.length && defaultNodes[0].data) {
            saveWorkflowDefinition(defaultNodes[0].data, workflow, updateWorkflowMutation);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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
