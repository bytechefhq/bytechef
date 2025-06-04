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
import {
    ClickedOperationType,
    ClusterElementItemType,
    NodeDataType,
    PropertyAllType,
    StructureParentType,
} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import {
    convertNameToCamelCase,
    initializeClusterElementsObject,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowMutation} from '../providers/workflowMutationProvider';
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
    rootClusterElementDefinition?: ComponentDefinition;
    parentId: number;
    parentType: StructureParentType;
    setPopoverOpen: (open: boolean) => void;
    sourceNodeId: string;
    trigger?: boolean;
}

const WorkflowNodesPopoverMenuOperationList = ({
    clusterElementType,
    componentDefinition,
    edgeId,
    parentId,
    parentType,
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

    const {clusterElementsCanvasOpen, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore();

    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const {captureComponentUsed} = useAnalytics();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const {actions, icon, name, title, triggers, version} = componentDefinition;

    const operations = useMemo(() => (trigger ? triggers : actions), [trigger, triggers, actions]);

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
                nodeData,
                nodeIndex,
                onSuccess: () =>
                    handleComponentAddedSuccess({
                        nodeData,
                        queryClient,
                        workflow,
                    }),
                parentId,
                parentType,
                queryClient,
                updateWorkflowMutation,
            });
        },
        [parentId, parentType, queryClient, updateWorkflowMutation, workflow]
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

            const clusterElements = initializeClusterElementsObject(
                rootClusterElementDefinition,
                currentClusterRootTask?.clusterElements || {}
            );

            if (isMultipleElements) {
                clusterElements[clusterElementType] = [
                    ...(Array.isArray(clusterElements[clusterElementType]) ? clusterElements[clusterElementType] : []),
                    clusterElementData,
                ];
            } else {
                clusterElements[clusterElementType] = clusterElementData;
            }

            const updatedNodeData = {
                ...currentClusterRootTask,
                clusterElements,
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
                nodeData: updatedNodeData,
                onSuccess: () => {
                    handleComponentAddedSuccess({
                        nodeData: updatedNodeData,
                        queryClient,
                        workflow,
                    });
                },
                parentId,
                parentType,
                queryClient,
                updateWorkflowMutation,
            });

            setPopoverOpen(false);
        },
        [
            workflow,
            rootClusterElementDefinition,
            setRootClusterElementNodeData,
            rootClusterElementNodeData,
            currentNode,
            parentId,
            parentType,
            queryClient,
            updateWorkflowMutation,
            setPopoverOpen,
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
                    clusterElementName: isMultipleElements ? operationName : clusterElementType,
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
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        parentId,
                        parentType,
                        queryClient,
                        taskDispatcherContext,
                        updateWorkflowMutation,
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
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        parentId,
                        parentType,
                        placeholderId: sourceNodeId,
                        queryClient,
                        taskDispatcherContext,
                        updateWorkflowMutation,
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
            parentId,
            parentType,
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

                    <h3 className="text-sm text-muted-foreground">{trigger ? 'Triggers' : 'Actions'}</h3>
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
