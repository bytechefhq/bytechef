import {ActionDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useGetComponentActionDefinitionQuery} from '@/queries/platform/actionDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import {ComponentOperationType, UpdateWorkflowMutationType} from '@/types/types';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {usePrevious} from '@uidotdev/usehooks';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionBasicModel} from 'middleware/platform/configuration';
import {DragEventHandler, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import ReactFlow, {Controls, Edge, MiniMap, Node, NodeDimensionChange, useReactFlow, useStore} from 'reactflow';

import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import defaultEdges from '../edges/defaultEdges';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import PlaceholderNode from '../nodes/PlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';
import defaultNodes from '../nodes/defaultNodes';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasicModel[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const WorkflowEditor = ({
    componentDefinitions,
    taskDispatcherDefinitions,
    updateWorkflowMutation,
}: WorkflowEditorProps) => {
    const [edges, setEdges] = useState<Array<Edge>>();
    const [isSaving, setIsSaving] = useState(false);
    const [latestComponentName, setLatestComponentName] = useState('');
    const [newNode, setNewNode] = useState<Node | undefined>();
    const [nodeOperations, setNodeOperations] = useState<Array<ComponentOperationType>>([]);
    const [nodes, setNodes] = useState<Array<Node>>();
    const [viewportWidth, setViewportWidth] = useState(0);
    const [workflowComponentWithAlias, setWorkflowComponentWithAlias] = useState<
        | (ComponentDefinitionBasicModel & {actions?: Array<ActionDefinitionBasicModel>; workflowNodeName: string})
        | undefined
    >();

    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {componentActions, setComponentActions, setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames} = workflow;

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
            componentName: latestComponentName || lastComponentName,
        },
        !!latestComponentName || !!lastComponentName
    );

    const {data: latestActionDefinition} = useGetComponentActionDefinitionQuery(
        {
            actionName: workflowComponent?.actions?.[0]?.name as string,
            componentName: workflowComponent?.name as string,
            componentVersion: workflowComponent?.version as number,
        },
        !!workflowComponent
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

                    return;
                }
            }
        }
    };

    const handleNodeChange = async (changes: NodeDimensionChange[]) => {
        const changesIds = changes.map((change) => change.id);

        const changesIncludeExistingNodes = defaultNodesWithWorkflowNodes?.some((node) =>
            changesIds.includes(node?.data.name)
        );

        if (changesIncludeExistingNodes) {
            return;
        }

        const workflowNodes = getNodes();

        setNewNode(workflowNodes.find((node) => node.id === changes[0].id));
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

            let componentDefinition = componentDefinitions.find(
                (componentDefinition) => componentDefinition.name === componentName
            )!;

            if (componentDefinition == undefined) {
                componentDefinition = componentDefinitions.find(
                    (componentDefinition) => componentDefinition.name === 'missing'
                )!;
            }

            return {
                data: {
                    ...component,
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
                    name: component.name,
                    operationName,
                    trigger: index === 0,
                    type: 'workflow',
                },
                id: component.name,
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
    }, [workflow?.tasks, workflow?.triggers, workflow.id]);

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
                    const lastNodeId = nodes?.[nodes.length - 1].id ?? getRandomId();

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
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [defaultNodesWithWorkflowNodes, workflow.id]);

    // Update nodes and edges when workflow changes
    useEffect(() => {
        setNodes(defaultNodesWithWorkflowNodes as Array<Node>);

        setEdges(defaultEdgesWithWorkflowEdges);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.id]);

    // Set workflowComponentWithAlias when workflowComponent is fetched
    useEffect(() => {
        if (!workflowComponent || !componentNames?.length || !latestComponentName) {
            return;
        }

        const sameComponentNames = componentNames.filter((nodeName) => nodeName === workflowComponent.name);

        setWorkflowComponentWithAlias({
            ...workflowComponent,
            workflowNodeName: `${workflowComponent.name}_${sameComponentNames.length}`,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowComponent?.name, componentNames]);

    // Save workflow definition with default parameters when a new node is added
    useEffect(() => {
        if (!latestActionDefinition?.properties || !newNode || isSaving || !workflowComponentWithAlias) {
            return;
        }

        setIsSaving(true);

        saveWorkflowDefinition(
            {
                ...newNode.data,
                parameters: getParametersWithDefaultValues({properties: latestActionDefinition?.properties}),
                type: `${newNode.data.componentName}/${workflowComponentWithAlias?.version}/${workflowComponentWithAlias?.actions?.[0].name}`,
            },
            workflow!,
            updateWorkflowMutation
        )
            .then(() => setIsSaving(false))
            .catch((error) => {
                console.error('Save workflow definition failed:', error);

                setIsSaving(false);
            });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [latestActionDefinition?.name, newNode]);

    // Update workflow node names when nodes change
    useEffect(() => {
        const workflowNodes = getNodes();

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
