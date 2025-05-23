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
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import {convertNameToCamelCase} from '../../ai-agent-editor/utils/clusterElementsUtils';
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
    setPopoverOpen: (open: boolean) => void;
    sourceNodeId: string;
    trigger?: boolean;
}

const WorkflowNodesPopoverMenuOperationList = ({
    clusterElementType,
    componentDefinition,
    edgeId,
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

    const {projectId} = useParams();

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
                projectId: +projectId!,
                queryClient,
                updateWorkflowMutation,
            });
        },
        [projectId, queryClient, updateWorkflowMutation, workflow]
    );

    const saveClusterElementToWorkflow = useCallback(
        (clusterElementData: ClusterElementItemType, objectKey: string, isMultipleElements: boolean) => {
            if (!workflow.definition) {
                return;
            }

            const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

            const currentClusterRootTask = workflowDefinitionTasks?.find(
                (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
            );
            const existingClusterElements = currentClusterRootTask?.clusterElements;

            const clusterElements = {...(existingClusterElements || {})};

            if (isMultipleElements) {
                clusterElements[objectKey] = [
                    ...(Array.isArray(clusterElements[objectKey]) ? clusterElements[objectKey] : []),
                    clusterElementData,
                ];
            } else {
                clusterElements[objectKey] = clusterElementData;
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
                projectId: +projectId!,
                queryClient,
                updateWorkflowMutation,
            });

            setPopoverOpen(false);
        },
        [
            rootClusterElementNodeData,
            setRootClusterElementNodeData,
            currentNode,
            projectId,
            queryClient,
            updateWorkflowMutation,
            setPopoverOpen,
            setCurrentNode,
            workflow,
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

                const objectKey = clusterElementType;

                const currentClusterElementDefinition = rootClusterElementDefinition?.clusterElementTypes?.find(
                    (clusterElementType) => convertNameToCamelCase(clusterElementType.name as string) === objectKey
                );

                if (!currentClusterElementDefinition) {
                    console.error(`Unknown cluster element type: ${objectKey}`);
                    return;
                }

                const isMultipleElements = !!currentClusterElementDefinition.multipleElements;

                const getClusterElementDefinitionRequest = {
                    componentName: componentName,
                    componentVersion: version,
                    // eslint-disable-next-line sort-keys
                    clusterElementName: isMultipleElements ? operationName : objectKey,
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

                saveClusterElementToWorkflow(clusterElementData, objectKey, isMultipleElements);

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
                        projectId: +projectId!,
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
                        placeholderId: sourceNodeId,
                        projectId: +projectId!,
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
            edgeId,
            queryClient,
            getNodeData,
            captureComponentUsed,
            saveNodeToWorkflow,
            setPopoverOpen,
            clusterElementType,
            rootClusterElementDefinition?.clusterElementTypes,
            saveClusterElementToWorkflow,
            workflow,
            edges,
            nodes,
            projectId,
            updateWorkflowMutation,
            sourceNodeId,
        ]
    );

    return (
        <div
            className="flex w-workflow-nodes-popover-actions-menu-width flex-col rounded-r-lg border-l border-l-border/50"
            key={`${sourceNodeId}-operationList`}
        >
            <header className="flex items-center space-x-2 rounded-tr-lg bg-white px-3 py-1.5">
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

            <ul className="h-96 space-y-2 overflow-auto rounded-br-lg bg-muted p-3">
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
