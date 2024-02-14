import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import {ComponentActionType} from '@/types/types';
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
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

export type WorkflowEditorProps = {
    componentDefinitions: ComponentDefinitionBasicModel[];
    projectId: number;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
};

const WorkflowEditor = ({
    componentDefinitions,
    taskDispatcherDefinitions,
    updateWorkflowMutation,
}: WorkflowEditorProps) => {
    const [edges, setEdges] = useState(defaultEdges);
    const [latestComponentName, setLatestComponentName] = useState('');
    const [nodeActions, setNodeActions] = useState<Array<ComponentActionType>>([]);
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

        const workflowNodes = nodeNames.filter((nodeName) => nodeName === workflowComponent.name);

        return {
            ...workflowComponent,
            workflowNodeName: `${workflowComponent.name}_${workflowNodes.length + 1}`,
        };
    }, [nodeNames, workflowComponent]);

    const defaultNodesWithWorkflowNodes = useMemo(() => {
        const workflowTasks = workflow?.tasks?.filter((task) => task.name);

        const workflowNodes = workflowTasks?.map((workflowTask, index) => {
            const componentName = workflowTask.type?.split('/')[0];
            const actionName = workflowTask.type?.split('/')[2];

            const componentDefinition = componentDefinitions.find(
                (componentDefinition) => componentDefinition.name === componentName
            );

            if (componentDefinition) {
                return {
                    data: {
                        ...workflowTask,
                        actionName,
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
                        name: workflowTask.name,
                        type: 'workflow',
                    },
                    id: workflowTask.name,
                    position: {x: 0, y: 150 * (index + 1)},
                    type: 'workflow',
                };
            }
        });

        if (workflowNodes) {
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
    }, [componentDefinitions, workflow?.tasks]);

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

    // Only gets invoked when a new node is added to the workflow using the last node
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

        saveWorkflowDefinition(newNode.data, workflow!, updateWorkflowMutation);
    };

    const workflowNodes = getNodes();

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
            setWorkflow({
                ...workflow,
                componentNames: workflowNodes.map((node) => node?.data.componentName),
                nodeNames: workflowNodes.map((node) => node?.data.name),
            });

            setNodes(defaultNodesWithWorkflowNodes as Array<Node>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes, setWorkflow]);

    useEffect(() => {
        if (defaultEdgesWithWorkflowEdges) {
            setEdges(defaultEdgesWithWorkflowEdges);
        }
    }, [defaultEdgesWithWorkflowEdges]);

    useEffect(() => {
        if (workflowComponentWithAlias?.actions) {
            const {actions, name} = workflowComponentWithAlias;

            let workflowNodeName = `${name}_1`;
            let index = 2;

            while (componentActions.some((action) => action.workflowNodeName === workflowNodeName)) {
                workflowNodeName = `${name}_${index}`;

                index++;
            }

            const actionNames = componentActions.map((action) => action.actionName);

            if (!actionNames.includes(actions[0].name)) {
                setComponentActions([
                    ...componentActions,
                    {
                        actionName: actions[0].name,
                        componentName: name,
                        workflowNodeName,
                    },
                ]);
            }
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
