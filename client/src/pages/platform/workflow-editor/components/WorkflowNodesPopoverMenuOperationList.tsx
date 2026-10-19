import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ActionDefinition,
    ActionDefinitionApi,
    ComponentDefinition,
    TriggerDefinition,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {ClickedOperationType, NodeDataType, PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, Loader2Icon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import {convertNameToSnakeCase, getClusterElementsLabel} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import addClusterElementToClusterRoot from '../utils/addClusterElementToClusterRoot';
import calculateNodeInsertIndex from '../utils/calculateNodeInsertIndex';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleComponentAddedSuccess, {
    handleComponentAddedError,
    openNodeDetailsPanelForNewNode,
} from '../utils/handleComponentAddedSuccess';
import handleTaskDispatcherSubtaskOperationClick from '../utils/handleTaskDispatcherSubtaskOperationClick';
import {resolveClusterRootId} from '../utils/resolveClusterRootId';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

interface WorkflowNodesPopoverMenuOperationListProps {
    clusterElementType?: string;
    componentDefinition: ComponentDefinition;
    edgeId?: string;
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
    multipleClusterElementsNode,
    setPopoverOpen,
    sourceNodeId,
    sourceNodeName,
    trigger,
}: WorkflowNodesPopoverMenuOperationListProps) => {
    const [loadingOperationName, setLoadingOperationName] = useState<string | null>(null);

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

    const {clusterRootComponentDefinitions, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                clusterRootComponentDefinitions: state.clusterRootComponentDefinitions,
                rootClusterElementNodeData: state.rootClusterElementNodeData,
                setRootClusterElementNodeData: state.setRootClusterElementNodeData,
            }))
        );

    const mainClusterRootNodeData = useMemo(() => {
        const sourceNode = nodes.find((node) => node.id === sourceNodeId);

        if (!sourceNode) {
            return rootClusterElementNodeData?.componentName && rootClusterElementNodeData?.workflowNodeName
                ? rootClusterElementNodeData
                : undefined;
        }

        const sourceNodeData = sourceNode.data as NodeDataType;
        const mainClusterRootId = resolveClusterRootId(sourceNodeData);

        if (!mainClusterRootId) {
            return rootClusterElementNodeData?.componentName && rootClusterElementNodeData?.workflowNodeName
                ? rootClusterElementNodeData
                : undefined;
        }

        const mainClusterRootNode = nodes.find((node) => node.id === mainClusterRootId);

        if (mainClusterRootNode) {
            return mainClusterRootNode.data as NodeDataType;
        }

        return rootClusterElementNodeData?.componentName && rootClusterElementNodeData?.workflowNodeName
            ? rootClusterElementNodeData
            : undefined;
    }, [nodes, rootClusterElementNodeData, sourceNodeId]);

    const mainClusterRootComponentDefinition =
        clusterRootComponentDefinitions[mainClusterRootNodeData?.workflowNodeName ?? ''];
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

    const {actions, clusterElements, clusterRoot, icon, name, title, triggers, version} = componentDefinition;

    const clusterElementOperations = useMemo(() => {
        if (!clusterElementType) {
            return [];
        }

        const matchingOperations = clusterElements?.filter((clusterElement) => {
            return clusterElement.type === convertNameToSnakeCase(clusterElementType as string);
        });

        return matchingOperations;
    }, [clusterElementType, clusterElements]);

    const operations = useMemo(() => {
        if (trigger) {
            return triggers;
        }

        if (clusterElementType) {
            return clusterElementOperations;
        }

        return actions;
    }, [trigger, triggers, clusterElementType, clusterElementOperations, actions]);

    const operationsHeading = useMemo(() => {
        if (trigger) {
            return 'Triggers';
        }

        if (clusterElementType) {
            return getClusterElementsLabel(clusterElementType);
        }

        return 'Actions';
    }, [trigger, clusterElementType]);

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
                name: trigger ? (sourceNodeName ?? getFormattedName('trigger')) : getFormattedName(componentName),
                operationName,
                parameters: getParametersWithDefaultValues({
                    properties: definition?.properties as Array<PropertyAllType>,
                }),
                trigger: trigger,
                type: `${componentName}/v${version}/${operationName}`,
                version,
                workflowNodeName: trigger
                    ? (sourceNodeName ?? getFormattedName('trigger'))
                    : getFormattedName(componentName),
            };
        },
        [clusterRoot, sourceNodeName, trigger]
    );

    const saveNodeToWorkflow = useCallback(
        (nodeData: NodeDataType, nodeIndex?: number) => {
            saveWorkflowDefinition({
                nodeData,
                nodeIndex,
                onError: () => handleComponentAddedError({nodeData}),
                onSuccess: () =>
                    handleComponentAddedSuccess({
                        nodeData,
                        queryClient,
                        workflow,
                    }),
                updateWorkflowMutation: updateWorkflowMutation!,
            });

            openNodeDetailsPanelForNewNode(nodeData);
        },
        [queryClient, updateWorkflowMutation, workflow]
    );

    const saveClusterElementToWorkflow = useCallback(
        async ({
            clusterElementName,
            clusterElementType,
            isMultipleElements = false,
            label,
            sourceNodeId,
        }: {
            clusterElementName: string;
            clusterElementType: string;
            isMultipleElements?: boolean;
            label?: string;
            sourceNodeId: string;
        }) => {
            if (!workflow.definition || !mainClusterRootComponentDefinition) {
                return;
            }

            if (!mainClusterRootNodeData?.workflowNodeName || !mainClusterRootNodeData?.componentName) {
                console.error('Root cluster element node data is missing required properties');

                return;
            }

            const nestedClusterElements = await addClusterElementToClusterRoot({
                clusterElementName,
                clusterElementType,
                clusterRootComponentDefinition: mainClusterRootComponentDefinition,
                clusterRootId: mainClusterRootNodeData.workflowNodeName,
                componentDefinition,
                isMultipleElements,
                label,
                queryClient,
                sourceNodeId,
                updateWorkflowMutation: updateWorkflowMutation!,
                workflow,
            });

            if (!nestedClusterElements) {
                return;
            }

            setRootClusterElementNodeData({
                ...mainClusterRootNodeData,
                clusterElements: nestedClusterElements,
            } as typeof mainClusterRootNodeData);

            if (currentNode?.clusterRoot && !currentNode.isNestedClusterRoot) {
                setCurrentNode({
                    ...currentNode,
                    clusterElements: nestedClusterElements,
                });
            }

            if (workflowNodeDetailsPanelOpen && currentNode?.workflowNodeName === sourceNodeName) {
                setCurrentNode({
                    ...mainClusterRootNodeData,
                    clusterElements: nestedClusterElements,
                });

                setWorkflowNodeDetailsPanelOpen(false);
            }
        },
        [
            workflow,
            mainClusterRootComponentDefinition,
            mainClusterRootNodeData,
            componentDefinition,
            setRootClusterElementNodeData,
            currentNode,
            workflowNodeDetailsPanelOpen,
            sourceNodeName,
            updateWorkflowMutation,
            setCurrentNode,
            setWorkflowNodeDetailsPanelOpen,
            queryClient,
        ]
    );

    const handleOperationClick = useCallback(
        async (clickedOperation: ClickedOperationType) => {
            if (!componentDefinition || loadingOperationName) {
                return;
            }

            const {componentName, operationName, version} = clickedOperation;

            setLoadingOperationName(operationName);
            setLatestComponentDefinition(componentDefinition);

            try {
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
                        staleTime: DEFINITION_STALE_TIME,
                    });

                    const newTriggerNodeData = getNodeData(clickedOperation, clickedComponentTriggerDefinition);

                    saveNodeToWorkflow(newTriggerNodeData, 0);

                    setPopoverOpen(false);

                    return;
                }

                if (clusterElementType) {
                    captureComponentUsed(componentName, undefined, operationName);

                    await saveClusterElementToWorkflow({
                        clusterElementName: operationName,
                        clusterElementType,
                        isMultipleElements: multipleClusterElementsNode,
                        label: clickedOperation.componentLabel,
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
                    staleTime: DEFINITION_STALE_TIME,
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
                            clusterRoot,
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
                            clusterRoot,
                            operation: clickedOperation,
                            operationDefinition: clickedComponentActionDefinition,
                            placeholderId: sourceNodeId,
                            queryClient,
                            taskDispatcherContext,
                            updateWorkflowMutation: updateWorkflowMutation!,
                            workflow,
                        });
                    } else {
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
                };

                if (edgeId) {
                    handleEdgeCase();
                } else {
                    handleNonEdgeCase();
                }

                setPopoverOpen(false);
            } catch (error) {
                console.error('Failed to load operation definition:', error);

                setLoadingOperationName(null);
            }
        },
        [
            clusterRoot,
            componentDefinition,
            loadingOperationName,
            setLatestComponentDefinition,
            trigger,
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

                    <h3 className="text-sm text-muted-foreground">{operationsHeading}</h3>
                </div>
            </header>

            <ul className="h-96 space-y-2 overflow-auto rounded-br-lg p-3">
                {operations?.map((operation) => {
                    const isLoading = loadingOperationName === operation.name;
                    const isDisabled = loadingOperationName !== null;

                    return (
                        <li
                            className={`space-y-1 rounded border-2 border-transparent bg-white px-2 py-1 ${isDisabled ? 'cursor-wait opacity-70' : 'cursor-pointer hover:border-blue-200'}`}
                            key={operation.name}
                            onClick={() => {
                                if (isDisabled) {
                                    return;
                                }

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
                            <div className="flex items-center gap-2">
                                <div className="min-w-0 flex-1 space-y-1">
                                    <h3 className="text-sm">{operation.title}</h3>

                                    <p className="text-xs wrap-break-word text-muted-foreground">
                                        {operation.description}
                                    </p>
                                </div>

                                {isLoading && (
                                    <Loader2Icon className="size-4 shrink-0 animate-spin text-muted-foreground" />
                                )}
                            </div>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default WorkflowNodesPopoverMenuOperationList;
