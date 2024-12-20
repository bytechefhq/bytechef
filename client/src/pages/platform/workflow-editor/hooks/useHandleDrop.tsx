import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ActionDefinitionApi,
    ComponentDefinitionApi,
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionApi,
    TaskDispatcherDefinitionBasic,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {ClickedDefinitionType, PropertyAllType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {useQueryClient} from '@tanstack/react-query';
import {PlayIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {Edge, Node, useReactFlow} from 'reactflow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

export default function useHandleDrop(): [
    (targetNode: Node, droppedNode: ClickedDefinitionType) => void,
    (targetEdge: Edge, droppedNode: ClickedDefinitionType) => void,
    (droppedNode: ClickedDefinitionType) => void,
] {
    const {workflow} = useWorkflowDataStore();

    const {captureComponentUsed} = useAnalytics();

    const {setEdges} = useWorkflowDataStore();

    const {getEdges, getNodes} = useReactFlow();

    const newNodeId = getRandomId();
    const nodes = getNodes();
    const edges = getEdges();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    async function handleDropOnPlaceholderNode(targetNode: Node, droppedNode: ClickedDefinitionType) {
        const sourceEdge = edges.find((edge) => edge.target === targetNode.id);

        if (!sourceEdge) {
            return;
        }

        const newWorkflowEdge = {
            id: `${sourceEdge.source}=>${targetNode.id}`,
            source: sourceEdge.source,
            target: targetNode.id,
            type: 'workflow',
        };

        const newPlaceholderEdge = {
            id: `${targetNode.id}=>${newNodeId}`,
            source: targetNode.id,
            target: newNodeId,
            type: 'placeholder',
        };

        const edgeIndex = edges.findIndex((edge) => edge.id === sourceEdge?.id);

        edges[edgeIndex] = newWorkflowEdge;

        setEdges([...edges, newPlaceholderEdge]);

        const newWorkflowNode = {
            ...targetNode,
            data: {
                componentName: droppedNode.name,
                icon: droppedNode?.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={droppedNode?.icon} />
                ) : (
                    <PlayIcon className="size-9 text-gray-700" />
                ),
                label: droppedNode?.title,
                name: getFormattedName(droppedNode.name!, nodes),
                taskDispatcher: droppedNode.taskDispatcher,
                version: droppedNode.version,
                workflowNodeName: getFormattedName(droppedNode.name!, nodes),
            },
            name: droppedNode.name,
            type: 'workflow',
        };

        let parameters;

        if (newWorkflowNode.data.taskDispatcher) {
            const draggedTaskDispatcherDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                        taskDispatcherName: newWorkflowNode.data.componentName,
                        taskDispatcherVersion: newWorkflowNode.data.version ?? 1,
                    }),
                queryKey: TaskDispatcherKeys.taskDispatcherDefinition({
                    taskDispatcherName: newWorkflowNode.data.componentName,
                    taskDispatcherVersion: newWorkflowNode.data.version,
                }),
            });

            captureComponentUsed(newWorkflowNode.data.componentName);

            parameters = {
                ...getParametersWithDefaultValues({
                    properties: draggedTaskDispatcherDefinition?.properties as Array<PropertyAllType>,
                }),
                caseFalse: [],
                caseTrue: [],
            };
        } else {
            const draggedComponentDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: newWorkflowNode.data.componentName,
                        componentVersion: newWorkflowNode.data.version,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: newWorkflowNode.data.componentName,
                    componentVersion: newWorkflowNode.data.version,
                }),
            });

            const getActionDefinitionRequest = {
                actionName: draggedComponentDefinition.actions?.[0].name as string,
                componentName: newWorkflowNode.data.componentName,
                componentVersion: draggedComponentDefinition.version,
            };

            const draggedComponentActionDefinition = await queryClient.fetchQuery({
                queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
                queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
            });

            captureComponentUsed(
                newWorkflowNode.data.componentName,
                draggedComponentDefinition.actions?.[0].name,
                undefined
            );

            parameters = getParametersWithDefaultValues({
                properties: draggedComponentActionDefinition.properties || [],
            });
        }

        let taskNodeIndex: number | undefined = undefined;

        if (targetNode.id.includes('bottom-placeholder')) {
            const targetNodeIndex = getNodes().findIndex((node) => node.id === targetNode.id);

            const nextNode = getNodes()[targetNodeIndex + 1];

            taskNodeIndex = workflow.tasks?.findIndex((task) => task.name === nextNode.id);
        }

        saveWorkflowDefinition({
            nodeData: {
                ...newWorkflowNode.data,
                parameters,
            },
            nodeIndex: taskNodeIndex,
            queryClient,
            updateWorkflowMutation,
            workflow,
        });
    }

    async function handleDropOnWorkflowEdge(targetEdge: Edge, droppedNode: ClickedDefinitionType) {
        const previousNode = nodes.find((node) => node.id === targetEdge.source);
        const nextNode = nodes.find((node) => node.id === targetEdge.target);

        if (!nextNode || !previousNode) {
            return;
        }

        const targetEdgeIndex = edges.findIndex((edge) => edge.id === targetEdge.id);

        const newWorkflowNode = {
            data: {
                componentName: droppedNode.name,
                icon: droppedNode?.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={droppedNode?.icon} />
                ) : (
                    <PlayIcon className="size-9 text-gray-700" />
                ),
                label: droppedNode?.title,
                name: getFormattedName(droppedNode.name!, nodes),
                taskDispatcher: droppedNode.taskDispatcher,
                version: droppedNode.version,
                workflowNodeName: getFormattedName(droppedNode.name!, nodes),
            },
            id: getRandomId(),
            name: droppedNode.name,
            position: {
                x: 0,
                y: 0,
            },
            type: 'workflow',
        };

        const newWorkflowEdge = {
            id: `${newWorkflowNode.id}=>${nextNode.id}`,
            source: newWorkflowNode.id,
            target: nextNode.id,
            type: 'workflow',
        };

        edges[targetEdgeIndex] = {
            ...targetEdge,
            id: `${previousNode.id}=>${newWorkflowNode.id}`,
            target: newWorkflowNode.id,
        };

        edges.splice(targetEdgeIndex, 0, newWorkflowEdge);

        setEdges(edges);

        let parameters;

        if (newWorkflowNode.data.taskDispatcher) {
            const draggedTaskDispatcherDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                        taskDispatcherName: newWorkflowNode.data.componentName,
                        taskDispatcherVersion: newWorkflowNode.data.version ?? 1,
                    }),
                queryKey: TaskDispatcherKeys.taskDispatcherDefinition({
                    taskDispatcherName: newWorkflowNode.data.componentName,
                    taskDispatcherVersion: newWorkflowNode.data.version,
                }),
            });

            captureComponentUsed(newWorkflowNode.data.componentName);

            parameters = {
                ...getParametersWithDefaultValues({
                    properties: draggedTaskDispatcherDefinition?.properties as Array<PropertyAllType>,
                }),
                caseFalse: [],
                caseTrue: [],
            };
        } else {
            const draggedComponentDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: newWorkflowNode.data.componentName,
                        componentVersion: newWorkflowNode.data.version,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: newWorkflowNode.data.componentName,
                    componentVersion: newWorkflowNode.data.version,
                }),
            });

            const getActionDefinitionRequest = {
                actionName: draggedComponentDefinition.actions?.[0].name as string,
                componentName: newWorkflowNode.data.componentName,
                componentVersion: draggedComponentDefinition.version,
            };

            const draggedComponentActionDefinition = await queryClient.fetchQuery({
                queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
                queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
            });

            captureComponentUsed(
                newWorkflowNode.data.componentName,
                draggedComponentDefinition.actions?.[0].name,
                undefined
            );

            parameters = getParametersWithDefaultValues({
                properties: draggedComponentActionDefinition.properties || [],
            });
        }

        saveWorkflowDefinition({
            nodeData: {
                ...newWorkflowNode.data,
                parameters,
            },
            nodeIndex: targetEdgeIndex,
            queryClient,
            updateWorkflowMutation,
            workflow,
        });
    }

    async function handleDropOnTriggerNode(droppedNode: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic) {
        const {icon, name, title, version} = droppedNode;

        const draggedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: name,
                    componentVersion: version,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: name,
                componentVersion: version,
            }),
        });

        const {triggers} = draggedComponentDefinition;

        const newTriggerNodeData = {
            componentName: name,
            icon: icon ? (
                <InlineSVG className="size-9 text-gray-700" src={icon} />
            ) : (
                <PlayIcon className="size-9 text-gray-700" />
            ),
            label: title,
            name: 'trigger_1',
            trigger: true,
            type: `${name}/v${version}/${triggers?.[0].name}`,
            workflowNodeName: 'trigger_1',
        };

        const draggedComponentTriggerDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new TriggerDefinitionApi().getComponentTriggerDefinition({
                    componentName: name,
                    componentVersion: version,
                    triggerName: triggers?.[0].name as string,
                }),
            queryKey: TriggerDefinitionKeys.triggerDefinition({
                componentName: name,
                componentVersion: version,
                triggerName: triggers?.[0].name as string,
            }),
        });

        captureComponentUsed(name, undefined, triggers?.[0].name);

        saveWorkflowDefinition({
            nodeData: {
                ...newTriggerNodeData,
                operationName: draggedComponentTriggerDefinition.name,
                parameters: getParametersWithDefaultValues({
                    properties: draggedComponentTriggerDefinition.properties || [],
                }),
            },
            queryClient,
            updateWorkflowMutation,
            workflow,
        });
    }

    return [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode];
}
