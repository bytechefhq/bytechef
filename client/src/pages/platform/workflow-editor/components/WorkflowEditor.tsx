import defaultEdges from '@/shared/defaultEdges';
import defaultNodes from '@/shared/defaultNodes';
import {
    ActionDefinitionBasic,
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
    WorkflowTrigger,
} from '@/shared/middleware/platform/configuration';
import {ComponentOperationType} from '@/shared/types';
import {usePrevious} from '@uidotdev/usehooks';
import {DragEventHandler, useCallback, useEffect, useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';
import ReactFlow, {Controls, MiniMap, useReactFlow, useStore} from 'reactflow';

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

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasic[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const WorkflowEditor = ({componentDefinitions, taskDispatcherDefinitions}: WorkflowEditorProps) => {
    const [latestComponentName, setLatestComponentName] = useState('');
    const [nodeOperations] = useState<Array<ComponentOperationType>>([]);
    const [viewportWidth, setViewportWidth] = useState(0);
    const [workflowComponentWithAlias, setWorkflowComponentWithAlias] = useState<
        (ComponentDefinitionBasic & {actions?: Array<ActionDefinitionBasic>; workflowNodeName: string}) | undefined
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

    // Reset workflow data store when projectWorkflowId changes
    useEffect(() => {
        useWorkflowDataStore.getState().reset();
        useWorkflowNodeDetailsPanelStore.getState().reset();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, projectWorkflowId]);

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
                if (node.data.type !== 'placeholder' && node?.data.workflowNodeName) {
                    return node?.data.workflowNodeName;
                }
            });

            setWorkflow({
                ...workflow,
                nodeNames: workflowNodeNames.filter((nodeName) => !!nodeName),
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [setWorkflow, componentNames]);

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

    // Update nodeNames and componentActions when workflow definition changes
    useEffect(() => {
        setWorkflow({
            ...workflow,
            nodeNames: [
                ...(workflow.tasks?.map((task) => task.name) || []),
                ...(workflow.triggers?.map((trigger) => trigger.name) || []),
            ],
        });

        const workflowComponents: Array<WorkflowTrigger & WorkflowTask> = [
            workflow.triggers?.[0] || defaultNodes[0].data,
            ...(workflow?.tasks || []),
        ];

        setComponentActions(
            workflowComponents.map((component) => {
                const componentName = component.type!.split('/')[0];
                const operationName = component.type!.split('/')[2];

                return {
                    componentName,
                    operationName,
                    workflowNodeName: component.name,
                };
            })
        );

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.tasks, workflow.triggers]);

    useLayout({componentDefinitions, taskDispatcherDefinitions});

    return (
        <div className="flex h-full flex-1 flex-col">
            <ReactFlow
                defaultEdges={defaultEdges}
                defaultNodes={defaultNodes}
                defaultViewport={{
                    x: viewportWidth / 2,
                    y: 50,
                    zoom: 1,
                }}
                deleteKeyCode={null}
                edgeTypes={edgeTypes}
                maxZoom={1.5}
                minZoom={0.6}
                nodeTypes={nodeTypes}
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
