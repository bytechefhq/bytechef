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
import {ClickedOperationType, NodeDataType, PropertyAllType} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleComponentAddedSuccess from '../utils/handleComponentAddedSuccess';
import handleConditionChildOperationClick from '../utils/handleConditionChildOperationClick';
import handleLoopChildOperationClick from '../utils/handleLoopChildOperationClick';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

interface WorkflowNodesPopoverMenuOperationListProps {
    componentDefinition: ComponentDefinition;
    conditionId?: string;
    edgeId?: string;
    loopId?: string;
    setPopoverOpen: (open: boolean) => void;
    sourceNodeId: string;
    trigger?: boolean;
}

const WorkflowNodesPopoverMenuOperationList = ({
    componentDefinition,
    conditionId,
    edgeId,
    loopId,
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

    const {captureComponentUsed} = useAnalytics();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const {actions, icon, name, title, triggers, version} = componentDefinition;

    const operations = useMemo(() => (trigger ? triggers : actions), [trigger, triggers, actions]);

    const {projectId} = useParams();

    const getNodeData = useCallback(
        (operation: ClickedOperationType, definition: ActionDefinition | TriggerDefinition) => {
            const {componentLabel, componentName, icon, operationName, version} = operation;

            return {
                componentName,
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

            const calculateNodeInsertIndex = (targetId: string) => {
                const nextTaskIndex = workflow.tasks?.findIndex((task) => task.name === targetId) ?? 0;

                const conditionTasks =
                    workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('condition/')) || [];

                const loopTasks =
                    workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('loop/')) || [];

                let tasksInConditions = 0;
                let tasksInLoops = 0;

                if (conditionTasks.length) {
                    tasksInConditions = conditionTasks.reduce((count, conditionTask) => {
                        const caseTrueTasks = conditionTask.parameters?.caseTrue?.length || 0;
                        const caseFalseTasks = conditionTask.parameters?.caseFalse?.length || 0;

                        return count + caseTrueTasks + caseFalseTasks;
                    }, 0);
                }

                if (loopTasks.length) {
                    tasksInLoops = loopTasks.reduce(
                        (count, loopTask) => count + loopTask.parameters?.iteratee?.length || 0,
                        0
                    );
                }

                return nextTaskIndex - tasksInConditions - tasksInLoops;
            };

            const handleEdgeCase = () => {
                const clickedEdge = edges.find((edge) => edge.id === edgeId);

                if (!clickedEdge) {
                    return;
                }

                const taskDispatcherContext = getTaskDispatcherContext({edge: clickedEdge, nodes});

                if (taskDispatcherContext?.conditionId) {
                    handleConditionChildOperationClick({
                        conditionId: taskDispatcherContext.conditionId as string,
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

                if (loopId) {
                    handleLoopChildOperationClick({
                        loopId,
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        placeholderId: sourceNodeId,
                        projectId: +projectId!,
                        queryClient,
                        updateWorkflowMutation,
                        workflow,
                    });
                } else if (taskDispatcherContext?.conditionId || conditionId) {
                    handleConditionChildOperationClick({
                        conditionId: (taskDispatcherContext?.conditionId as string) ?? conditionId,
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        placeholderId: sourceNodeId,
                        projectId: +projectId!,
                        queryClient,
                        taskDispatcherContext,
                        updateWorkflowMutation,
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
            getNodeData,
            saveNodeToWorkflow,
            trigger,
            edgeId,
            sourceNodeId,
            loopId,
            conditionId,
            nodes,
            edges,
            workflow,
            projectId,
            queryClient,
            updateWorkflowMutation,
            captureComponentUsed,
            setLatestComponentDefinition,
            setPopoverOpen,
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

                        <p className="break-words text-xs text-muted-foreground">{operation.description}</p>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default WorkflowNodesPopoverMenuOperationList;
