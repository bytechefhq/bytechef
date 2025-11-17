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
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import {
    convertNameToSnakeCase,
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
import processClusterElementsHierarchy from '../utils/processClusterElementsHierarchy';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {getTaskDispatcherTask} from '../utils/taskDispatcherConfig';

interface WorkflowNodesPopoverMenuOperationListProps {
    clusterElementType?: string;
    componentDefinition: ComponentDefinition;
    edgeId?: string;
    invalidateWorkflowQueries: () => void;
    multipleClusterElementsNode?: boolean;
    setPopoverOpen: (open: boolean) => void;
    sourceNodeId: string;
    sourceNodeName?: string;
    trigger?: boolean;
}

const WorkflowNodesPopoverMenuOperationList = ({
    clusterElementType,
    componentDefinition,
    edgeId,
    invalidateWorkflowQueries,
    multipleClusterElementsNode,
    setPopoverOpen,
    sourceNodeId,
    sourceNodeName,
    trigger,
}: WorkflowNodesPopoverMenuOperationListProps) => {
    const {setLatestComponentDefinition, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setLatestComponentDefinition: state.setLatestComponentDefinition,
            workflow: state.workflow,
        }))
    );
    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const {
        clusterElementsCanvasOpen,
        mainClusterRootComponentDefinition,
        rootClusterElementNodeData,
        setRootClusterElementNodeData,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            mainClusterRootComponentDefinition: state.mainClusterRootComponentDefinition,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setRootClusterElementNodeData: state.setRootClusterElementNodeData,
        }))
    );
    const {currentNode, setCurrentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                currentNode: state.currentNode,
                setCurrentNode: state.setCurrentNode,
                setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
                workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
            }))
        );

    const {captureComponentUsed} = useAnalytics();

    const {updateWorkflowMutation} = useWorkflowEditor();

    const queryClient = useQueryClient();

    const {actions, clusterElement, clusterElements, clusterRoot, icon, name, title, triggers, version} =
        componentDefinition;

    const clusterElementOperations = useMemo(() => {
        if (!clusterElementType) {
            return [];
        }

        const matchingOperations = clusterElements?.filter((clusterElement) => {
            return clusterElement.type === convertNameToSnakeCase(clusterElementType as string);
        });

        return matchingOperations;
    }, [clusterElementType, clusterElements]);

    const operations = useMemo(
        () => (trigger ? triggers : clusterElementsCanvasOpen && clusterElement ? clusterElementOperations : actions),
        [trigger, triggers, clusterElementsCanvasOpen, clusterElement, clusterElementOperations, actions]
    );

    const getNodeData = useCallback(
        (operation: ClickedOperationType, definition: ActionDefinition | TriggerDefinition) => {
            const {componentLabel, componentName, icon, operationName, version} = operation;

            return {
                ...(clusterRoot
                    ? {
                          clusterElements: {},
                      }
                    : {}),
                componentName,
                componentVersion: version,
                icon: icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={icon} />
                ) : (
                    <ComponentIcon className="size-9 text-gray-700" />
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
        [clusterRoot, trigger]
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
        ({
            clusterElementData,
            clusterElementType,
            isMultipleElements = false,
            sourceNodeId,
        }: {
            clusterElementData: ClusterElementItemType;
            clusterElementType: string;
            isMultipleElements?: boolean;
            sourceNodeId: string;
        }) => {
            if (!workflow.definition || !mainClusterRootComponentDefinition) {
                return;
            }

            const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

            const mainClusterRootTask = rootClusterElementNodeData?.workflowNodeName
                ? getTaskDispatcherTask({
                      taskDispatcherId: rootClusterElementNodeData.workflowNodeName,
                      tasks: workflowDefinitionTasks,
                  })
                : undefined;

            if (!mainClusterRootTask) {
                return;
            }

            const clusterElements = initializeClusterElementsObject({
                clusterElementsData: mainClusterRootTask?.clusterElements || {},
                mainClusterRootComponentDefinition,
                mainClusterRootTask,
            });

            const updatedClusterElements = processClusterElementsHierarchy({
                clusterElementData,
                clusterElements,
                elementType: clusterElementType,
                isMultipleElements,
                mainRootId: rootClusterElementNodeData?.workflowNodeName,
                sourceNodeId,
            });

            const updatedNodeData = {
                ...mainClusterRootTask,
                clusterElements: updatedClusterElements.nestedClusterElements,
            };

            setRootClusterElementNodeData({
                ...rootClusterElementNodeData,
                clusterElements: updatedClusterElements.nestedClusterElements,
            } as typeof rootClusterElementNodeData);

            if (currentNode?.clusterRoot && !currentNode.isNestedClusterRoot) {
                setCurrentNode({
                    ...currentNode,
                    clusterElements: updatedClusterElements.nestedClusterElements,
                });
            }

            if (workflowNodeDetailsPanelOpen && currentNode?.workflowNodeName === sourceNodeName) {
                if (rootClusterElementNodeData) {
                    setCurrentNode({
                        ...rootClusterElementNodeData,
                        clusterElements: updatedClusterElements.nestedClusterElements,
                    });
                }

                setWorkflowNodeDetailsPanelOpen(false);
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
            mainClusterRootComponentDefinition,
            rootClusterElementNodeData,
            setRootClusterElementNodeData,
            currentNode,
            workflowNodeDetailsPanelOpen,
            sourceNodeName,
            invalidateWorkflowQueries,
            updateWorkflowMutation,
            setCurrentNode,
            setWorkflowNodeDetailsPanelOpen,
            queryClient,
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

                const getClusterElementDefinitionRequest = {
                    clusterElementName: operationName,
                    componentName,
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
                    clusterElements: componentDefinition.clusterRoot ? {} : undefined,
                    label: clickedOperation.componentLabel,
                    metadata: {},
                    name: getFormattedName(componentName),
                    parameters:
                        getParametersWithDefaultValues({
                            properties: clickedClusterElementDefinition?.properties as Array<PropertyAllType>,
                        }) || {},
                    type: `${componentName}/v${version}/${operationName}`,
                };

                saveClusterElementToWorkflow({
                    clusterElementData,
                    clusterElementType,
                    isMultipleElements: multipleClusterElementsNode,
                    sourceNodeId,
                });

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
            saveClusterElementToWorkflow,
            multipleClusterElementsNode,
            sourceNodeId,
            edges,
            nodes,
            invalidateWorkflowQueries,
            updateWorkflowMutation,
            workflow,
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
