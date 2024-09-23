import {ActionDefinitionApi, ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {ClickedOperationType, PropertyAllType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

const WorkflowNodesPopoverMenuOperationList = ({
    componentDefinition,
    condition,
    edge,
    id,
    trigger,
}: {
    componentDefinition: ComponentDefinition;
    condition?: boolean;
    edge?: boolean;
    id: string;
    trigger?: boolean;
}) => {
    const {setLatestComponentDefinition, setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames} = workflow;

    const {getEdge, getNode, getNodes, setEdges, setNodes} = useReactFlow();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const {currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const queryClient = useQueryClient();

    const {actions, icon, name, title, triggers, version} = componentDefinition;

    const operations = trigger ? triggers : actions;

    const handleOperationClick = async (clickedOperation: ClickedOperationType) => {
        if (!componentDefinition) {
            return;
        }

        const {componentLabel, componentName, icon, operationName, version} = clickedOperation;

        setLatestComponentDefinition(componentDefinition);

        if (trigger) {
            const placeholderNode = getNode(id);

            setNodes((nodes: Node[]) =>
                nodes.map((node) => {
                    if (node.id === placeholderNode?.id) {
                        const newTriggerNode = {
                            ...node,
                            data: {
                                ...node.data,
                                componentName: componentName,
                                connections: undefined,
                                icon: (
                                    <>
                                        {icon ? (
                                            <InlineSVG className="size-9 text-gray-700" src={icon} />
                                        ) : (
                                            <Component1Icon className="size-9 text-gray-700" />
                                        )}
                                    </>
                                ),
                                id: getFormattedName(componentName!, nodes),
                                label: componentLabel,
                                metadata: undefined,
                                name: 'trigger_1',
                                operationName: operationName,
                                parameters: undefined,
                                trigger: true,
                                type: `${componentName}/v${version}/${operationName}`,
                                workflowNodeName: 'trigger_1',
                            },
                            id: getFormattedName(componentName!, nodes),
                            type: 'workflow',
                        };

                        saveWorkflowDefinition(newTriggerNode.data, workflow, updateWorkflowMutation, undefined, () => {
                            queryClient.invalidateQueries({
                                queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                                    id: workflow.id!,
                                    lastWorkflowNodeName: currentNode?.name,
                                }),
                            });

                            setWorkflow({
                                ...workflow,
                                componentNames: [componentName, ...componentNames.slice(1)],
                            });

                            if (currentNode?.trigger) {
                                setCurrentNode(newTriggerNode.data);
                            }

                            if (trigger) {
                                setCurrentComponent({
                                    componentName: newTriggerNode.data.componentName,
                                    notes: newTriggerNode.data.description,
                                    operationName: newTriggerNode.data.operationName,
                                    title: newTriggerNode.data.label,
                                    type: newTriggerNode.data.type,
                                    workflowNodeName: newTriggerNode.data.workflowNodeName,
                                });
                            }
                        });

                        return newTriggerNode;
                    }

                    return node;
                })
            );

            return;
        }

        const getActionDefinitionRequest = {
            actionName: clickedOperation.operationName,
            componentName: clickedOperation.componentName,
            componentVersion: componentDefinition.version,
        };

        const clickedComponentActionDefinition = await queryClient.fetchQuery({
            queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
            queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
        });

        if (edge) {
            const clickedEdge = getEdge(id);

            if (!clickedEdge) {
                return;
            }

            const nodes = getNodes();

            const workflowNodeName = getFormattedName(clickedOperation.componentName!, nodes);

            const newWorkflowNode = {
                data: {
                    componentName: clickedOperation.componentName,
                    icon: clickedOperation.icon && (
                        <InlineSVG
                            className="size-9"
                            loader={<ComponentIcon className="size-9" />}
                            src={clickedOperation.icon}
                        />
                    ),
                    label: clickedOperation.componentLabel,
                    metadata: {},
                    name: workflowNodeName,
                    type: clickedOperation.type,
                    workflowNodeName,
                },
                id: getRandomId(),
                position: {
                    x: 0,
                    y: 0,
                },
                type: 'workflow',
            };

            let sourceEdgeType = condition ? 'condition' : 'workflow';

            if (clickedEdge.type === 'conditionChild') {
                sourceEdgeType = 'conditionChild';
            }

            let sourceEdge = {
                id: `${clickedEdge.source}->${newWorkflowNode.id}`,
                source: clickedEdge.source,
                sourceHandle: clickedEdge.sourceHandle,
                target: newWorkflowNode.id,
                type: sourceEdgeType,
            };

            const targetEdge = {
                id: `${newWorkflowNode.id}->${clickedEdge.target}`,
                source: newWorkflowNode.id,
                sourceHandle: clickedEdge.sourceHandle,
                target: clickedEdge.target,
                type: condition ? 'condition' : 'workflow',
            };

            if (sourceEdgeType === 'condition') {
                sourceEdge = {
                    ...sourceEdge,
                    id: `${clickedEdge.source}${clickedEdge.sourceHandle}->${newWorkflowNode.id}`,
                    sourceHandle: clickedEdge.sourceHandle,
                };

                targetEdge.sourceHandle = clickedEdge.sourceHandle;
            }

            if (condition) {
                newWorkflowNode.data.metadata = {
                    ...newWorkflowNode.data.metadata,
                    conditionChild: true,
                    conditionFalse: clickedEdge.sourceHandle === 'right',
                    conditionTrue: clickedEdge.sourceHandle === 'left',
                };
            }

            setNodes((nodes) => {
                const previousWorkflowNode = nodes.find((node) => node.id === clickedEdge.source);

                const previousComponentNameIndex = componentNames.findIndex(
                    (name) => name === previousWorkflowNode?.data.componentName
                );

                const tempComponentNames = [...componentNames];

                tempComponentNames.splice(previousComponentNameIndex + 1, 0, newWorkflowNode.data.componentName);

                setWorkflow({
                    ...workflow,
                    componentNames: tempComponentNames,
                    nodeNames: [...workflow.nodeNames, newWorkflowNode.data.name],
                });

                const previousWorkflowNodeIndex = nodes.findIndex((node) => node.id === clickedEdge.source);

                const tempNodes = [...nodes];

                tempNodes.splice(previousWorkflowNodeIndex + 1, 0, newWorkflowNode);

                saveWorkflowDefinition(
                    {
                        ...newWorkflowNode.data,
                        parameters: getParametersWithDefaultValues({
                            properties: clickedComponentActionDefinition?.properties as Array<PropertyAllType>,
                        }),
                    },
                    workflow!,
                    updateWorkflowMutation,
                    previousWorkflowNodeIndex,
                    () => {
                        queryClient.invalidateQueries({
                            queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                                id: workflow.id!,
                                lastWorkflowNodeName: currentNode?.name,
                            }),
                        });
                    }
                );

                return tempNodes;
            });

            setEdges((edges) => edges.filter((edge) => edge.id !== id).concat([sourceEdge, targetEdge]));
        } else {
            const placeholderNode = getNode(id);

            if (!placeholderNode) {
                return;
            }

            const placeholderId = placeholderNode.id;
            const childPlaceholderId = getRandomId();

            const childPlaceholderNode = {
                data: {label: '+'},
                id: childPlaceholderId,
                position: {
                    x: placeholderNode.position.x,
                    y: placeholderNode.position.y,
                },
                style: {
                    zIndex: 9999,
                },
                type: 'placeholder',
            };

            const childPlaceholderEdge = {
                id: `${placeholderId}=>${childPlaceholderId}`,
                source: placeholderId,
                target: childPlaceholderId,
                type: 'placeholder',
            };

            setNodes((nodes: Node[]) =>
                nodes
                    .map((node) => {
                        if (node.id === placeholderId) {
                            const workflowNodeName = getFormattedName(clickedOperation.componentName!, nodes);

                            const newWorkflowNodeData = {
                                componentName: clickedOperation.componentName,
                                icon: clickedOperation.icon && (
                                    <InlineSVG
                                        className="size-9"
                                        loader={<ComponentIcon className="size-9" />}
                                        src={clickedOperation.icon}
                                    />
                                ),
                                label: clickedOperation.componentLabel,
                                name: workflowNodeName,
                                type: clickedOperation.type,
                                workflowNodeName,
                            };

                            setWorkflow({
                                ...workflow,
                                componentNames: [...componentNames, clickedOperation.componentName],
                                nodeNames: [...workflow.nodeNames, workflowNodeName],
                            });

                            saveWorkflowDefinition(
                                {
                                    ...newWorkflowNodeData,
                                    parameters: getParametersWithDefaultValues({
                                        properties:
                                            clickedComponentActionDefinition?.properties as Array<PropertyAllType>,
                                    }),
                                },
                                workflow!,
                                updateWorkflowMutation,
                                undefined,
                                () => {
                                    queryClient.invalidateQueries({
                                        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                                            id: workflow.id!,
                                            lastWorkflowNodeName: currentNode?.name,
                                        }),
                                    });
                                }
                            );

                            return {
                                ...node,
                                data: newWorkflowNodeData,
                                type: 'workflow',
                            };
                        }

                        return node;
                    })
                    .concat([childPlaceholderNode])
            );

            setEdges((edges: Edge[]) =>
                edges
                    .map((edge) => {
                        if (edge.target === id) {
                            return {
                                ...edge,
                                markerEnd: {
                                    type: MarkerType.ArrowClosed,
                                },
                                type: 'workflow',
                            };
                        }

                        return edge;
                    })
                    .concat([childPlaceholderEdge])
            );
        }
    };

    return (
        <div className="flex w-workflow-nodes-popover-actions-menu-width flex-col rounded-r-lg border-l">
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

            <ul className="h-96 space-y-2 overflow-scroll rounded-br-lg bg-muted p-3">
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

                        <p className="text-xs text-muted-foreground">{operation.description}</p>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default WorkflowNodesPopoverMenuOperationList;
