import {ROOT_CLUSTER_ELEMENT_NAMES} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ActionDefinition,
    ActionDefinitionApi,
    ClusterElementDefinitionApi,
    ComponentDefinition,
    TriggerDefinition,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {ClickedOperationType, ClusterElementItemType, NodeDataType, PropertyAllType} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import {
    convertNameToCamelCase,
    getClusterElementsLabel,
    initializeClusterElementsObject,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import calculateNodeInsertIndex from '../utils/calculateNodeInsertIndex';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleComponentAddedSuccess from '../utils/handleComponentAddedSuccess';
import handleTaskDispatcherSubtaskOperationClick from '../utils/handleTaskDispatcherSubtaskOperationClick';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

interface WorkflowNodesPopoverMenuOperationListProps {
    clusterElementType?: string;
    componentDefinition: ComponentDefinition;
    edgeId?: string;
    invalidateWorkflowQueries: () => void;
    rootClusterElementDefinition?: ComponentDefinition;
    setPopoverOpen: (open: boolean) => void;
    sourceNodeId: string;
    trigger?: boolean;
}

const WorkflowNodesPopoverMenuOperationList = ({
    clusterElementType,
    componentDefinition,
    edgeId,
    invalidateWorkflowQueries,
    rootClusterElementDefinition,
    setPopoverOpen,
    sourceNodeId,
    trigger,
}: WorkflowNodesPopoverMenuOperationListProps) => {
    const {setLatestComponentDefinition, workflow} = useWorkflowDataStore();

    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const {nodes: clusterElementNodes} = useClusterElementsDataStore();

    const {clusterElementsCanvasOpen, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore();

    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const {captureComponentUsed} = useAnalytics();

    const {updateWorkflowMutation} = useWorkflowEditor();

    const queryClient = useQueryClient();

    const {actions, clusterElement, clusterElements, icon, name, title, triggers, version} = componentDefinition;

    const operations = useMemo(
        () => (trigger ? triggers : clusterElementsCanvasOpen && clusterElement ? clusterElements : actions),
        [trigger, triggers, clusterElementsCanvasOpen, clusterElement, clusterElements, actions]
    );

    const getNodeData = useCallback(
        (operation: ClickedOperationType, definition: ActionDefinition | TriggerDefinition) => {
            const {componentLabel, componentName, icon, operationName, version} = operation;

            const isRootClusterElement = ROOT_CLUSTER_ELEMENT_NAMES.includes(componentName);

            return {
                ...(isRootClusterElement
                    ? {
                          clusterElements: {},
                      }
                    : {}),
                componentName,
                componentVersion: version,
                icon: icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={icon} />
                ) : (
                    <Component1Icon className="size-9 text-gray-700" />
                ),
                label: componentLabel,
                metadata: undefined,
                name: trigger ? 'trigger_1' : getFormattedName(componentName),
                operationName,
                parameters: getParametersWithDefaultValues({
                    properties: definition?.properties as Array<PropertyAllType>,
                }),
                trigger: trigger,
                type: `${componentName}/v${version}/${operationName}`,
                version,
                workflowNodeName: trigger ? 'trigger_1' : getFormattedName(componentName),
            };
        },
        [trigger]
    );

    const saveNodeToWorkflow = useCallback(
        (nodeData: NodeDataType, nodeIndex?: number) => {
            saveWorkflowDefinition({
                invalidateWorkflowQueries,
                nodeData,
                nodeIndex,
                onSuccess: () =>
                    handleComponentAddedSuccess({
                        nodeData,
                        queryClient,
                        workflow,
                    }),
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        },
        [invalidateWorkflowQueries, queryClient, updateWorkflowMutation, workflow]
    );

    const saveClusterElementToWorkflow = useCallback(
        (clusterElementData: ClusterElementItemType, clusterElementType: string, isMultipleElements: boolean) => {
            if (!workflow.definition || !rootClusterElementDefinition) {
                return;
            }

            const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

            const currentClusterRootTask = workflowDefinitionTasks?.find(
                (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
            );

            if (!currentClusterRootTask) return;

            const nodePositions = clusterElementNodes.reduce<Record<string, {x: number; y: number}>>(
                (accumulator, node) => {
                    accumulator[node.id] = {
                        x: node.position.x,
                        y: node.position.y,
                    };

                    return accumulator;
                },
                {}
            );

            const clusterElements = initializeClusterElementsObject(
                currentClusterRootTask?.clusterElements || {},
                rootClusterElementDefinition
            );

            if (isMultipleElements) {
                clusterElements[clusterElementType] = [
                    ...(Array.isArray(clusterElements[clusterElementType]) ? clusterElements[clusterElementType] : []),
                    clusterElementData,
                ];
            } else {
                clusterElements[clusterElementType] = clusterElementData;
            }

            Object.entries(clusterElements).forEach(([elementKey, elementValue]) => {
                if (elementKey !== clusterElementType || isMultipleElements) {
                    if (Array.isArray(elementValue)) {
                        clusterElements[elementKey] = elementValue.map((element) => {
                            const elementNodeId = element.name;

                            const elementPosition = nodePositions[elementNodeId];

                            if (elementPosition) {
                                return {
                                    ...element,
                                    metadata: {
                                        ...element?.metadata,
                                        ui: {
                                            ...element?.metadata?.ui,
                                            nodePosition: elementPosition,
                                        },
                                    },
                                };
                            }

                            return element;
                        });
                    } else if (elementValue != null && 'name' in elementValue) {
                        const elementNodeId = elementValue.name;
                        const elementPosition = nodePositions[elementNodeId];

                        if (elementPosition) {
                            clusterElements[elementKey] = {
                                ...elementValue,
                                metadata: {
                                    ...elementValue?.metadata,
                                    ui: {
                                        ...elementValue?.metadata?.ui,
                                        nodePosition: elementPosition,
                                    },
                                },
                            } as ClusterElementItemType;
                        }
                    }
                }
            });

            const placeholderPositions = Object.entries(nodePositions).reduce<Record<string, {x: number; y: number}>>(
                (accumulator, [nodeId, position]) => {
                    if (nodeId.includes('placeholder')) {
                        accumulator[nodeId] = position;
                    }
                    return accumulator;
                },
                {}
            );

            const rootNodePosition = rootClusterElementNodeData?.workflowNodeName
                ? nodePositions[rootClusterElementNodeData.workflowNodeName]
                : undefined;

            const metadata = {
                ...currentClusterRootTask.metadata,
                ui: {
                    ...currentClusterRootTask.metadata?.ui,
                    nodePosition: rootNodePosition,
                    placeholderPositions: placeholderPositions || {},
                },
            };

            const updatedNodeData = {
                ...currentClusterRootTask,
                clusterElements,
                metadata,
            };

            setRootClusterElementNodeData({
                ...rootClusterElementNodeData,
                clusterElements,
            } as typeof rootClusterElementNodeData);

            if (currentNode?.rootClusterElement) {
                setCurrentNode({
                    ...currentNode,
                    clusterElements,
                });
            }

            saveWorkflowDefinition({
                invalidateWorkflowQueries,
                nodeData: updatedNodeData,
                onSuccess: () => {
                    handleComponentAddedSuccess({
                        nodeData: updatedNodeData,
                        queryClient,
                        workflow,
                    });
                },
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        },
        [
            workflow,
            rootClusterElementDefinition,
            clusterElementNodes,
            rootClusterElementNodeData,
            setRootClusterElementNodeData,
            currentNode,
            invalidateWorkflowQueries,
            queryClient,
            updateWorkflowMutation,
            setCurrentNode,
        ]
    );

    const handleOperationClick = useCallback(
        async (clickedOperation: ClickedOperationType) => {
            if (!componentDefinition) {
                return;
            }

            const {componentName, operationName, version} = clickedOperation;

            setLatestComponentDefinition(componentDefinition);

            if (trigger) {
                captureComponentUsed(componentName, undefined, operationName);

                const getTriggerDefinitionRequest = {
                    componentName,
                    componentVersion: version,
                    triggerName: operationName,
                };

                const clickedComponentTriggerDefinition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new TriggerDefinitionApi().getComponentTriggerDefinition(getTriggerDefinitionRequest),
                    queryKey: TriggerDefinitionKeys.triggerDefinition(getTriggerDefinitionRequest),
                });

                const newTriggerNodeData = getNodeData(clickedOperation, clickedComponentTriggerDefinition);

                saveNodeToWorkflow(newTriggerNodeData, 0);

                setPopoverOpen(false);

                return;
            }

            if (clusterElementsCanvasOpen && clusterElementType) {
                captureComponentUsed(componentName, undefined, operationName);

                const currentClusterElementDefinition = rootClusterElementDefinition?.clusterElementTypes?.find(
                    (currentClusterElementType) =>
                        convertNameToCamelCase(currentClusterElementType.name as string) === clusterElementType
                );

                if (!currentClusterElementDefinition) {
                    console.error(`Unknown cluster element type: ${clusterElementType}`);

                    return;
                }

                const isMultipleElements = !!currentClusterElementDefinition.multipleElements;

                const getClusterElementDefinitionRequest = {
                    clusterElementName: operationName,
                    componentName: componentName,
                    componentVersion: version,
                };

                const clickedClusterElementDefinition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition(
                            getClusterElementDefinitionRequest
                        ),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(getClusterElementDefinitionRequest),
                });

                const clusterElementData = {
                    label: clickedOperation.componentLabel,
                    metadata: {},
                    name: getFormattedName(componentName),
                    parameters:
                        getParametersWithDefaultValues({
                            properties: clickedClusterElementDefinition?.properties as Array<PropertyAllType>,
                        }) || {},
                    type: `${componentName}/v${version}/${operationName}`,
                };

                saveClusterElementToWorkflow(clusterElementData, clusterElementType, isMultipleElements);

                setPopoverOpen(false);

                return;
            }

            const getActionDefinitionRequest = {
                actionName: operationName,
                componentName,
                componentVersion: componentDefinition.version,
            };

            const clickedComponentActionDefinition = await queryClient.fetchQuery({
                queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
                queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
            });

            const newWorkflowNodeData = getNodeData(clickedOperation, clickedComponentActionDefinition);

            const handleEdgeCase = () => {
                const clickedEdge = edges.find((edge) => edge.id === edgeId);

                if (!clickedEdge) {
                    return;
                }

                const taskDispatcherContext = getTaskDispatcherContext({edge: clickedEdge, nodes});

                if (taskDispatcherContext?.taskDispatcherId) {
                    handleTaskDispatcherSubtaskOperationClick({
                        invalidateWorkflowQueries,
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        queryClient,
                        taskDispatcherContext,
                        updateWorkflowMutation: updateWorkflowMutation!,
                        workflow,
                    });

                    return;
                }

                captureComponentUsed(componentName, operationName, undefined);

                const insertIndex = calculateNodeInsertIndex(clickedEdge.target);

                saveNodeToWorkflow(newWorkflowNodeData, insertIndex);
            };

            const handleNonEdgeCase = () => {
                const sourceNode = nodes.find((node) => node.id === sourceNodeId);

                const taskDispatcherContext = getTaskDispatcherContext({node: sourceNode});

                if (taskDispatcherContext?.taskDispatcherId) {
                    handleTaskDispatcherSubtaskOperationClick({
                        invalidateWorkflowQueries,
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        placeholderId: sourceNodeId,
                        queryClient,
                        taskDispatcherContext,
                        updateWorkflowMutation: updateWorkflowMutation!,
                        workflow,
                    });
                } else if (!clusterElementsCanvasOpen) {
                    const placeholderNode = nodes.find((node) => node.id === sourceNodeId);

                    if (!placeholderNode) {
                        return;
                    }

                    captureComponentUsed(componentName, operationName, undefined);

                    let insertIndex: number | undefined = undefined;

                    if (sourceNodeId?.includes('bottom-placeholder')) {
                        const sourceNodeIndex = nodes.findIndex((node) => node.id === sourceNodeId);

                        const nextNode = nodes[sourceNodeIndex + 1];

                        insertIndex = calculateNodeInsertIndex(nextNode?.id);
                    }

                    saveNodeToWorkflow(newWorkflowNodeData, insertIndex);
                }

                setPopoverOpen(false);
            };

            if (edgeId) {
                handleEdgeCase();
            } else {
                handleNonEdgeCase();
            }
        },
        [
            componentDefinition,
            setLatestComponentDefinition,
            trigger,
            clusterElementsCanvasOpen,
            clusterElementType,
            queryClient,
            getNodeData,
            edgeId,
            captureComponentUsed,
            saveNodeToWorkflow,
            setPopoverOpen,
            rootClusterElementDefinition?.clusterElementTypes,
            saveClusterElementToWorkflow,
            edges,
            nodes,
            invalidateWorkflowQueries,
            updateWorkflowMutation,
            workflow,
            sourceNodeId,
        ]
    );

    return (
        <div
            className="flex w-workflow-nodes-popover-actions-menu-width flex-col rounded-r-lg border-l border-l-border/50"
            key={`${sourceNodeId}-operationList`}
        >
            <header className="flex items-center space-x-2 rounded-tr-lg px-3 py-1.5">
                {icon ? (
                    <InlineSVG className="size-8" loader={<ComponentIcon className="size-8" />} src={icon} />
                ) : (
                    <ComponentIcon className="size-8" />
                )}

                <div className="flex w-full flex-col">
                    <h2 className="text-lg font-semibold">{title}</h2>

                    <h3 className="text-sm text-muted-foreground">
                        {trigger
                            ? 'Triggers'
                            : clusterElementsCanvasOpen
                              ? getClusterElementsLabel(clusterElementType as string)
                              : 'Actions'}
                    </h3>
                </div>
            </header>

            <ul className="h-96 space-y-2 overflow-auto rounded-br-lg p-3">
                {operations?.map((operation) => (
                    <li
                        className="cursor-pointer space-y-1 rounded border-2 border-transparent bg-white px-2 py-1 hover:border-blue-200"
                        key={operation.name}
                        onClick={() => {
                            handleOperationClick({
                                componentLabel: title,
                                componentName: name,
                                icon: icon,
                                operationName: operation.name,
                                type: `${name}/v${version}/${operation.name}`,
                                version: version,
                            });
                        }}
                    >
                        <h3 className="text-sm">{operation.title}</h3>

                        <p className="break-words text-xs text-muted-foreground">{operation.description}</p>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default WorkflowNodesPopoverMenuOperationList;
