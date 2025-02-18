import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {ActionDefinitionApi, ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ClickedOperationType, NodeDataType, PropertyAllType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from '../utils/getFormattedName';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import handleComponentAddedSuccess from '../utils/handleComponentAddedSuccess';
import handleConditionChildOperationClick from '../utils/handleConditionChildOperationClick';
import handleLoopChildOperationClick from '../utils/handleLoopChildOperationClick';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

interface WorkflowNodesPopoverMenuOperationListProps {
    componentDefinition: ComponentDefinition;
    conditionId?: string;
    edge?: boolean;
    loopId?: string;
    setPopoverOpen: (open: boolean) => void;
    sourceNodeId: string;
    trigger?: boolean;
}

const WorkflowNodesPopoverMenuOperationList = ({
    componentDefinition,
    conditionId,
    edge,
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

    const handleOperationClick = useCallback(
        async (clickedOperation: ClickedOperationType) => {
            if (!componentDefinition) {
                return;
            }

            const {componentLabel, componentName, icon, operationName, version} = clickedOperation;

            setLatestComponentDefinition(componentDefinition);

            if (trigger) {
                captureComponentUsed(componentName, undefined, operationName);

                const newTriggerNodeData: NodeDataType = {
                    componentName: componentName,
                    icon: (
                        <>
                            {icon ? (
                                <InlineSVG className="size-9 text-gray-700" src={icon} />
                            ) : (
                                <Component1Icon className="size-9 text-gray-700" />
                            )}
                        </>
                    ),
                    label: componentLabel,
                    metadata: undefined,
                    name: 'trigger_1',
                    operationName,
                    parameters: undefined,
                    trigger: true,
                    type: `${componentName}/v${version}/${operationName}`,
                    version,
                    workflowNodeName: 'trigger_1',
                };

                saveWorkflowDefinition({
                    nodeData: newTriggerNodeData,
                    onSuccess: () =>
                        handleComponentAddedSuccess({
                            nodeData: newTriggerNodeData,
                            queryClient,
                            workflow,
                        }),
                    queryClient,
                    updateWorkflowMutation,
                });

                setPopoverOpen(false);

                return;
            }

            const newWorkflowNodeData = {
                componentName,
                icon: icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={icon} />
                ) : (
                    <Component1Icon className="size-9 text-gray-700" />
                ),
                label: componentLabel,
                metadata: undefined,
                name: getFormattedName(componentName),
                operationName,
                parameters: {},
                type: `${componentName}/v${version}/${operationName}`,
                version,
                workflowNodeName: getFormattedName(componentName),
            };

            const getActionDefinitionRequest = {
                actionName: operationName,
                componentName,
                componentVersion: componentDefinition.version,
            };

            const clickedComponentActionDefinition = await queryClient.fetchQuery({
                queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
                queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
            });

            const saveNodeToDefinition = (nodeData: NodeDataType, nodeIndex?: number) => {
                saveWorkflowDefinition({
                    nodeData: {
                        ...nodeData,
                        parameters: getParametersWithDefaultValues({
                            properties: clickedComponentActionDefinition?.properties as Array<PropertyAllType>,
                        }),
                    },
                    nodeIndex,
                    onSuccess: () =>
                        handleComponentAddedSuccess({
                            nodeData,
                            queryClient,
                            workflow,
                        }),
                    queryClient,
                    updateWorkflowMutation,
                });
            };

            const handleEdgeCase = () => {
                const clickedEdge = edges.find((edge) => edge.id === sourceNodeId);

                if (!clickedEdge) {
                    return;
                }

                captureComponentUsed(componentName, operationName, undefined);

                const newWorkflowNode = {
                    data: newWorkflowNodeData,
                    id: getRandomId(),
                    position: {x: 0, y: 0},
                    type: 'workflow',
                };

                const previousWorkflowNodeIndex = nodes.findIndex((node) => node.id === clickedEdge.source);
                const tempNodes = [...nodes];

                tempNodes.splice(previousWorkflowNodeIndex + 1, 0, newWorkflowNode);

                const previousWorkflowTaskIndex = workflow.tasks?.findIndex((task) => task.name === clickedEdge.source);

                saveNodeToDefinition(newWorkflowNodeData, (previousWorkflowTaskIndex ?? 0) + 1);
            };

            const handleNonEdgeCase = () => {
                if (loopId) {
                    handleLoopChildOperationClick({
                        loopId,
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        placeholderId: sourceNodeId,
                        queryClient,
                        updateWorkflowMutation,
                        workflow,
                    });
                } else if (conditionId) {
                    handleConditionChildOperationClick({
                        conditionId,
                        operation: clickedOperation,
                        operationDefinition: clickedComponentActionDefinition,
                        placeholderId: sourceNodeId,
                        queryClient,
                        updateWorkflowMutation,
                        workflow,
                    });
                } else {
                    const placeholderNode = nodes.find((node) => node.id === sourceNodeId);

                    if (!placeholderNode) {
                        return;
                    }

                    captureComponentUsed(componentName, operationName, undefined);

                    let taskNodeIndex: number | undefined = undefined;

                    if (sourceNodeId.includes('bottom-placeholder')) {
                        const sourceNodeIndex = nodes.findIndex((node) => node.id === sourceNodeId);

                        const nextNode = nodes[sourceNodeIndex + 1];

                        taskNodeIndex = workflow.tasks?.findIndex((task) => task.name === nextNode.id);
                    }

                    saveNodeToDefinition(newWorkflowNodeData, taskNodeIndex);
                }
                setPopoverOpen(false);
            };

            if (edge) {
                handleEdgeCase();
            } else {
                handleNonEdgeCase();
            }
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [componentDefinition, trigger]
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

                        <p className="text-xs text-muted-foreground">{operation.description}</p>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default WorkflowNodesPopoverMenuOperationList;
