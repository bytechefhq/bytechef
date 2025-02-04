import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import DataStreamComponentsTab from '@/pages/platform/workflow-editor/components/node-details-tabs/DataStreamComponentsTab';
import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {
    ActionDefinitionApi,
    ComponentConnection,
    ComponentDefinition,
    ComponentDefinitionBasic,
    GetComponentActionDefinitionRequest,
    GetComponentTriggerDefinitionRequest,
    TriggerDefinitionApi,
    WorkflowNodeOutput,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {
    ActionDefinitionKeys,
    useGetComponentActionDefinitionQuery,
} from '@/shared/queries/platform/actionDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {
    TriggerDefinitionKeys,
    useGetTriggerDefinitionQuery,
} from '@/shared/queries/platform/triggerDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useGetWorkflowNodeParameterDisplayConditionsQuery} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {
    ComponentPropertiesType,
    NodeDataType,
    PropertyAllType,
    TabNameType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useState} from 'react';
import isEqual from 'react-fast-compare';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getDataPillsFromProperties from '../utils/getDataPillsFromProperties';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import updateRootConditionNode from '../utils/updateRootConditionNode';
import CurrentOperationSelect from './CurrentOperationSelect';
import ConnectionTab from './node-details-tabs/ConnectionTab';
import DescriptionTab from './node-details-tabs/DescriptionTab';
import OutputTab from './node-details-tabs/OutputTab';

const TABS: Array<{label: string; name: TabNameType}> = [
    {
        label: 'Description',
        name: 'description',
    },
    {
        label: 'Components',
        name: 'dataStreamComponents',
    },
    {
        label: 'Connection',
        name: 'connection',
    },
    {
        label: 'Properties',
        name: 'properties',
    },
    {
        label: 'Output',
        name: 'output',
    },
];

const WorkflowNodeDetailsPanel = ({
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: {
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
}) => {
    const [currentNodeName, setCurrentNodeName] = useState<string | undefined>();
    const [currentOperationName, setCurrentOperationName] = useState('');
    const [currentOperationProperties, setCurrentOperationProperties] = useState<Array<PropertyAllType>>([]);

    const {
        activeTab,
        currentComponent,
        currentNode,
        setActiveTab,
        setCurrentComponent,
        setCurrentNode,
        workflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore();

    const {componentActions, setDataPills, workflow} = useWorkflowDataStore();

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const queryClient = useQueryClient();

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: currentNode?.componentName || '',
            componentVersion: currentNode?.version || 1,
        },
        !!currentNode && !currentNode.taskDispatcher
    );

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            workflowId: workflow.id as string,
            workflowNodeName: currentNode?.workflowNodeName as string,
        },
        !!workflow.id && !!currentNode
    );

    const {nodeNames} = workflow;

    const matchingOperation = useMemo(
        () =>
            [...(currentComponentDefinition?.actions || []), ...(currentComponentDefinition?.triggers || [])].find(
                (action) => action.name === currentOperationName
            ),
        [currentComponentDefinition, currentOperationName]
    );

    const {data: currentActionDefinition, isFetched: currentActionFetched} = useGetComponentActionDefinitionQuery(
        {
            actionName: currentNode?.operationName ?? currentOperationName,
            componentName: currentComponentDefinition?.name as string,
            componentVersion: currentComponentDefinition?.version as number,
        },
        !!currentComponentDefinition?.actions && !currentNode?.trigger && !!matchingOperation
    );

    const getTriggerName = useCallback((): string => {
        const currentComponentTriggerNames = currentComponentDefinition?.triggers?.map((trigger) => trigger.name);

        return currentComponentTriggerNames?.includes(currentOperationName)
            ? currentOperationName
            : (currentComponentDefinition?.triggers?.[0]?.name as string);
    }, [currentComponentDefinition, currentOperationName]);

    const {data: currentTriggerDefinition, isFetched: currentTriggerFetched} = useGetTriggerDefinitionQuery(
        {
            componentName: currentComponentDefinition?.name as string,
            componentVersion: currentComponentDefinition?.version as number,
            triggerName: getTriggerName(),
        },
        !!currentNode?.componentName && currentNode?.trigger && !!currentComponentDefinition
    );

    const {data: currentTaskDispatcherDefinition} = useGetTaskDispatcherDefinitionQuery(
        {
            taskDispatcherName: currentNode?.componentName || '',
            taskDispatcherVersion: currentNode?.version || 1,
        },
        !!currentNode && !!currentNode.taskDispatcher
    );

    const displayConditionsQuery = useGetWorkflowNodeParameterDisplayConditionsQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentNodeName!,
        },
        !!currentNodeName && currentNodeName !== 'manual'
    );

    const {data: workflowNodeParameterDisplayConditions} = displayConditionsQuery;

    const currentNodeDefinition = useMemo(() => {
        if (currentNode?.trigger) {
            return currentTriggerDefinition;
        }

        if (currentNode?.taskDispatcher) {
            return currentTaskDispatcherDefinition;
        }

        return currentActionDefinition;
    }, [currentNode, currentTriggerDefinition, currentTaskDispatcherDefinition, currentActionDefinition]);

    const currentNodeIndex = useMemo(
        () => currentNode && nodeNames?.indexOf(currentNode?.workflowNodeName),
        [currentNode, nodeNames]
    );

    const previousNodeNames = useMemo(
        () => (nodeNames.length > 1 ? nodeNames?.slice(0, currentNodeIndex) : []),
        [nodeNames, currentNodeIndex]
    );

    const previousComponentProperties: Array<ComponentPropertiesType> = useMemo(
        () =>
            previousComponentDefinitions?.map((componentDefinition, index) => {
                const outputSchemaDefinition: PropertyAllType | undefined = workflowNodeOutputs[index]?.outputSchema;

                const properties = outputSchemaDefinition?.properties?.length
                    ? outputSchemaDefinition.properties
                    : outputSchemaDefinition?.items;

                return {
                    componentDefinition,
                    properties,
                };
            }),
        [previousComponentDefinitions, workflowNodeOutputs]
    );

    const hasOutputData = useMemo(() => currentNodeDefinition?.outputDefined, [currentNodeDefinition]);

    const currentWorkflowTrigger = useMemo(
        () => workflow.triggers?.find((trigger) => trigger.name === currentNode?.workflowNodeName),
        [workflow.triggers, currentNode]
    );

    const currentWorkflowTask = useMemo(
        () => workflow.tasks?.find((task) => task.name === currentNode?.workflowNodeName),
        [workflow.tasks, currentNode]
    );

    const currentWorkflowNodeConnections: ComponentConnection[] = useMemo(
        () => currentWorkflowTask?.connections || currentWorkflowTrigger?.connections || [],
        [currentWorkflowTask, currentWorkflowTrigger]
    );

    const nodeTabs = useMemo(() => {
        return TABS.filter(({name}) => {
            if (name === 'connection') {
                return currentWorkflowNodeConnections.length > 0;
            }

            if (name === 'dataStreamComponents') {
                return currentComponentDefinition?.name === 'dataStream';
            }

            if (name === 'output') {
                return hasOutputData;
            }

            if (name === 'properties') {
                return currentNode?.taskDispatcher
                    ? currentTaskDispatcherDefinition?.properties?.length
                    : currentOperationProperties?.length;
            }

            return true;
        });
    }, [
        currentWorkflowNodeConnections,
        currentComponentDefinition,
        hasOutputData,
        currentNode,
        currentTaskDispatcherDefinition,
        currentOperationProperties,
    ]);

    const currentTaskData = useMemo(
        () => currentComponentDefinition || currentTaskDispatcherDefinition,
        [currentComponentDefinition, currentTaskDispatcherDefinition]
    );

    const currentOperationFetched = useMemo(
        () => currentActionFetched || currentTriggerFetched,
        [currentActionFetched, currentTriggerFetched]
    );

    const operationDataMissing = useMemo(
        () => currentComponent?.operationName && (!matchingOperation?.name || !currentOperationFetched),
        [currentComponent, matchingOperation, currentOperationFetched]
    );

    const tabDataExists = useMemo(
        () =>
            (!currentNode?.trigger && !currentNode?.taskDispatcher && currentActionFetched) ||
            currentNode?.taskDispatcher ||
            (currentNode?.trigger &&
                currentTriggerFetched &&
                nodeTabs.length > 1 &&
                currentNode.componentName !== 'manual'),
        [currentNode, currentActionFetched, currentTriggerFetched, nodeTabs]
    );

    const nodeDefinition = useMemo(
        () => currentComponentDefinition || currentTaskDispatcherDefinition || currentTriggerDefinition,
        [currentComponentDefinition, currentTaskDispatcherDefinition, currentTriggerDefinition]
    );

    const currentTaskDataOperations = useMemo(
        () => (currentTaskData as ComponentDefinition)?.actions ?? (currentTaskData as ComponentDefinition)?.triggers,
        [currentTaskData]
    );

    const handleOperationSelectChange = useCallback(
        async (newOperationName: string) => {
            setCurrentOperationName(newOperationName);

            if (!currentComponentDefinition || !currentComponent) {
                return;
            }

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
            });

            let operationData;

            if (currentNode?.trigger) {
                const triggerDefinitionRequest: GetComponentTriggerDefinitionRequest = {
                    componentName: currentComponentDefinition?.name,
                    componentVersion: currentComponentDefinition?.version,
                    triggerName: newOperationName,
                };

                operationData = await queryClient.fetchQuery({
                    queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(triggerDefinitionRequest),
                    queryKey: TriggerDefinitionKeys.triggerDefinition(triggerDefinitionRequest),
                });
            } else {
                const componentActionDefinitionRequest: GetComponentActionDefinitionRequest = {
                    actionName: newOperationName,
                    componentName: currentComponentDefinition.name,
                    componentVersion: currentComponentDefinition.version,
                };

                operationData = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ActionDefinitionApi().getComponentActionDefinition(componentActionDefinitionRequest),
                    queryKey: ActionDefinitionKeys.actionDefinition(componentActionDefinitionRequest),
                });
            }

            const {componentName, description, label, workflowNodeName} = currentComponent;

            let nodeData: NodeDataType = {
                componentName,
                description,
                label,
                name: workflowNodeName || currentNode?.workflowNodeName || '',
                operationName: newOperationName,
                parameters: getParametersWithDefaultValues({
                    properties: operationData.properties as Array<PropertyAllType>,
                }),
                trigger: currentNode?.trigger,
                type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                workflowNodeName,
            };

            if (currentNode?.conditionData) {
                const parentConditionNode = nodes.find(
                    (node) => node.data.name === currentNode?.conditionData?.conditionId
                );

                if (!parentConditionNode) {
                    return;
                }

                const conditionCase = currentNode.conditionData.conditionCase;
                const conditionParameters: Array<WorkflowTask> = (parentConditionNode.data as NodeDataType)
                    ?.parameters?.[conditionCase];

                if (conditionParameters) {
                    const taskIndex = conditionParameters.findIndex((subtask) => subtask.name === currentNode.name);

                    if (taskIndex !== -1) {
                        conditionParameters[taskIndex] = {
                            ...conditionParameters[taskIndex],
                            type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                        };

                        if (!workflow.definition) {
                            return;
                        }

                        const tasks = JSON.parse(workflow.definition).tasks;

                        const updatedParentConditionTask = workflow.tasks?.find(
                            (task) => task.name === currentNode.conditionData?.conditionId
                        );

                        if (!updatedParentConditionTask) {
                            return;
                        }

                        nodeData = updateRootConditionNode({
                            conditionCase,
                            conditionId: currentNode.conditionData.conditionId,
                            nodeIndex: taskIndex,
                            nodes,
                            tasks,
                            updatedParentConditionNodeData: parentConditionNode.data as NodeDataType,
                            updatedParentConditionTask,
                            workflow,
                        });
                    }
                }
            }

            saveWorkflowDefinition({
                nodeData,
                onSuccess: () => {
                    setCurrentComponent({
                        ...currentComponent,
                        displayConditions: {},
                        metadata: {},
                        operationName: newOperationName,
                        parameters: getParametersWithDefaultValues({
                            properties: currentOperationProperties as Array<PropertyAllType>,
                        }),
                        type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                    });
                },
                queryClient,
                subtask: !!currentNode?.conditionData,
                updateWorkflowMutation,
            });
        },
        [
            currentComponentDefinition,
            currentComponent,
            currentNode,
            currentOperationProperties,
            nodes,
            queryClient,
            setCurrentComponent,
            updateWorkflowMutation,
            workflow,
        ]
    );

    const handlePanelClose = useCallback(() => useWorkflowNodeDetailsPanelStore.getState().reset(), []);

    // Set current node name
    useEffect(() => {
        if (currentNode?.name) {
            setCurrentNodeName(currentNode?.name);
        } else {
            setCurrentNodeName(undefined);
        }
    }, [currentNode?.name]);

    // Set currentOperationProperties depending if the current node is a trigger or an action
    useEffect(() => {
        if (currentNodeDefinition?.properties) {
            setCurrentOperationProperties(currentNodeDefinition?.properties);
        }
    }, [currentNodeDefinition?.properties]);

    // Set availableDataPills depending on previousComponentProperties
    useEffect(() => {
        if (!previousComponentProperties) {
            return;
        }

        let filteredNodeNames = previousNodeNames;

        if (currentNode?.conditionData) {
            const parentConditionTask = workflow.tasks?.find(
                (task) => task.name === currentNode.conditionData?.conditionId
            );

            const {conditionCase} = currentNode.conditionData;

            const oppositeConditionCase =
                conditionCase === CONDITION_CASE_TRUE ? CONDITION_CASE_FALSE : CONDITION_CASE_TRUE;

            const oppositeConditionCaseNodeNames = parentConditionTask?.parameters?.[oppositeConditionCase].map(
                (task: WorkflowTask) => task.name
            );

            filteredNodeNames = previousNodeNames.filter(
                (nodeName) => !oppositeConditionCaseNodeNames?.includes(nodeName)
            );
        }

        const dataPills = getDataPillsFromProperties(previousComponentProperties!, filteredNodeNames);

        setDataPills(dataPills.flat(Infinity));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [previousComponentProperties.length]);

    // Tab switching logic
    useEffect(() => {
        if (activeTab === 'connection' && currentWorkflowNodeConnections.length === 0) {
            setActiveTab('description');
        }

        if (activeTab === 'dataStreamComponents' && currentComponentDefinition?.name !== 'dataStream') {
            setActiveTab('description');
        }

        if (currentComponentDefinition?.name === 'manual') {
            setActiveTab('description');
        }

        if (
            activeTab === 'properties' &&
            ((!currentNode?.trigger && currentActionFetched) || (currentNode?.trigger && currentTriggerFetched)) &&
            !currentOperationProperties
        ) {
            setActiveTab('description');
        }

        if (activeTab === 'output' && !hasOutputData) {
            setActiveTab('description');
        }

        if (activeTab === 'properties' && !operationDataMissing && !currentOperationProperties?.length) {
            setActiveTab('description');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        currentActionDefinition?.outputDefined,
        currentActionFetched,
        currentOperationProperties?.length,
        currentComponentDefinition?.name,
    ]);

    // If the current component requires a connection, set the active tab to 'connection'
    useEffect(() => {
        if (currentComponentDefinition?.connectionRequired && !workflowTestConfigurationConnections?.length) {
            setActiveTab('connection');
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponentDefinition]);

    // Update currentNode with connection data, operationName and type
    useEffect(() => {
        if (!currentNode) return;

        let updatedNode = {...currentNode};

        if (currentNode.operationName && currentOperationName) {
            updatedNode = {
                ...updatedNode,
                operationName: currentOperationName,
                type: `${currentComponent?.componentName}/v${currentComponentDefinition?.version}/${currentOperationName}`,
            };
        }

        if (currentWorkflowNodeConnections.length && workflowTestConfigurationConnections?.length) {
            updatedNode = {
                ...updatedNode,
                connectionId: workflowTestConfigurationConnections[0]?.connectionId,
                connections: currentWorkflowNodeConnections,
            };
        }

        if (!isEqual(updatedNode, currentNode)) {
            setCurrentNode(updatedNode);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        currentNode,
        currentOperationName,
        currentComponent?.componentName,
        currentComponentDefinition?.version,
        currentWorkflowNodeConnections,
        workflowTestConfigurationConnections,
    ]);

    // Set currentOperationName depending on the currentComponentAction.operationName
    useEffect(() => {
        if (!componentActions?.length) {
            return;
        }
        const currentComponentAction = componentActions.find(
            (action) => action.workflowNodeName === currentNode?.workflowNodeName
        );

        if (currentComponentAction?.operationName) {
            setCurrentOperationName(currentComponentAction.operationName);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentActions, currentNode?.workflowNodeName]);

    // Update display conditions when currentNode changes
    useEffect(() => {
        if (currentNode && workflowNodeParameterDisplayConditions?.displayConditions) {
            setCurrentNode({
                ...currentNode,
                displayConditions: workflowNodeParameterDisplayConditions.displayConditions,
            });
        }

        if (currentComponent && workflowNodeParameterDisplayConditions?.displayConditions) {
            if (currentComponent.workflowNodeName === currentNode?.name) {
                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: workflowNodeParameterDisplayConditions.displayConditions,
                });
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeParameterDisplayConditions?.displayConditions, currentNode?.name]);

    if (!workflowNodeDetailsPanelOpen || !currentNode?.workflowNodeName || !currentTaskData) {
        return <></>;
    }

    return (
        <div
            className="absolute inset-y-4 right-[70px] z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-lg border border-border/50 bg-background"
            key={currentNode?.workflowNodeName}
        >
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                <header className="flex items-center p-4 text-lg font-medium">
                    {currentTaskData.icon && (
                        <InlineSVG
                            className="mr-2 size-6"
                            loader={<LoadingIcon className="ml-0 mr-2 size-6 text-muted-foreground" />}
                            src={currentTaskData.icon}
                        />
                    )}

                    {currentNode?.label}

                    <span className="mx-2 text-sm text-gray-500">({currentNode?.workflowNodeName})</span>

                    {currentTaskData.description && (
                        <Tooltip delayDuration={500}>
                            <TooltipTrigger>
                                <InfoCircledIcon className="size-4" />
                            </TooltipTrigger>

                            <TooltipPortal>
                                <TooltipContent className="max-w-md" side="bottom">
                                    {currentComponentDefinition
                                        ? currentComponentDefinition.description
                                        : currentTaskDispatcherDefinition?.description}
                                </TooltipContent>
                            </TooltipPortal>
                        </Tooltip>
                    )}

                    <button
                        aria-label="Close the node details dialog"
                        className="ml-auto pr-0"
                        onClick={handlePanelClose}
                    >
                        <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                    </button>
                </header>

                <main className="flex h-full flex-col">
                    {!!currentTaskDataOperations?.length && operationDataMissing && (
                        <div className="flex flex-col border-b border-muted p-4">
                            <span className="text-sm leading-6">Actions</span>

                            <Skeleton className="h-9 w-full" />
                        </div>
                    )}

                    {currentTaskDataOperations && !operationDataMissing && (
                        <CurrentOperationSelect
                            description={
                                currentNode?.trigger
                                    ? currentTriggerDefinition?.description
                                    : currentActionDefinition?.description
                            }
                            handleValueChange={handleOperationSelectChange}
                            operations={
                                (currentNode?.trigger
                                    ? currentComponentDefinition?.triggers
                                    : currentComponentDefinition?.actions)!
                            }
                            triggerSelect={currentNode?.trigger}
                            value={currentOperationName}
                        />
                    )}

                    {tabDataExists && (
                        <div className="flex justify-center">
                            {nodeTabs.map((tab) => (
                                <Button
                                    className={twMerge(
                                        'grow justify-center whitespace-nowrap rounded-none border-0 border-b border-gray-200 bg-white py-5 text-sm font-medium text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                        activeTab === tab?.name && 'border-blue-500 text-blue-500 hover:text-blue-500'
                                    )}
                                    key={tab.name}
                                    name={tab.name}
                                    onClick={() => setActiveTab(tab.name)}
                                    variant="ghost"
                                >
                                    {tab.label}
                                </Button>
                            ))}
                        </div>
                    )}

                    {currentNode.componentName !== 'manual' && !tabDataExists && (
                        <div className="flex justify-center space-x-2 border-b border-gray-200 p-2">
                            <Skeleton className="h-6 w-1/4" />

                            <Skeleton className="h-6 w-1/4" />

                            <Skeleton className="h-6 w-1/4" />

                            <Skeleton className="h-6 w-1/4" />
                        </div>
                    )}

                    <div className="relative h-full overflow-y-scroll">
                        {currentTaskData && (
                            <div className="absolute left-0 top-0 size-full">
                                {activeTab === 'description' &&
                                    (nodeDefinition ? (
                                        <DescriptionTab
                                            key={`${currentNode?.workflowNodeName}_description`}
                                            nodeDefinition={nodeDefinition}
                                            updateWorkflowMutation={updateWorkflowMutation}
                                        />
                                    ) : (
                                        <div className="flex flex-col gap-y-4 p-4">
                                            <div className="flex flex-col gap-y-2">
                                                <Skeleton className="h-6 w-1/4" />

                                                <Skeleton className="h-8 w-full" />
                                            </div>

                                            <div className="flex flex-col gap-y-2">
                                                <Skeleton className="h-6 w-1/4" />

                                                <Skeleton className="h-24 w-full" />
                                            </div>
                                        </div>
                                    ))}

                                {activeTab === 'dataStreamComponents' && <DataStreamComponentsTab />}

                                {activeTab === 'connection' &&
                                    currentWorkflowNodeConnections.length > 0 &&
                                    currentNode &&
                                    currentComponentDefinition && (
                                        <ConnectionTab
                                            componentConnections={currentWorkflowNodeConnections}
                                            componentDefinition={currentComponentDefinition}
                                            key={`${currentNode?.workflowNodeName}_connection`}
                                            workflowId={workflow.id!}
                                            workflowNodeName={currentNode?.workflowNodeName}
                                            workflowTestConfigurationConnections={workflowTestConfigurationConnections}
                                        />
                                    )}

                                {activeTab === 'properties' &&
                                    (!operationDataMissing && currentOperationProperties?.length ? (
                                        <Properties
                                            customClassName="p-4"
                                            displayConditionsQuery={displayConditionsQuery}
                                            key={`${currentNode?.workflowNodeName}_${currentOperationName}_properties`}
                                            operationName={currentOperationName}
                                            properties={currentOperationProperties}
                                        />
                                    ) : (
                                        <div className="flex size-full items-center justify-center">
                                            <LoadingIcon /> Loading...
                                        </div>
                                    ))}

                                {activeTab === 'output' && currentNode && currentComponentDefinition && (
                                    <OutputTab
                                        connectionMissing={
                                            currentComponentDefinition.connectionRequired &&
                                            !workflowTestConfigurationConnections?.length
                                        }
                                        currentNode={currentNode}
                                        key={`${currentNode?.workflowNodeName}_output`}
                                        outputDefined={currentActionDefinition?.outputDefined ?? false}
                                        workflowId={workflow.id!}
                                    />
                                )}
                            </div>
                        )}
                    </div>
                </main>

                <footer className="z-50 mt-auto flex bg-white px-4 py-2">
                    <Select defaultValue={currentTaskData?.version.toString()}>
                        <SelectTrigger className="w-auto border-none shadow-none">
                            <SelectValue placeholder="Choose version..." />
                        </SelectTrigger>

                        <SelectContent>
                            <SelectItem value="1">v1</SelectItem>
                        </SelectContent>
                    </Select>
                </footer>
            </div>
        </div>
    );
};

export default WorkflowNodeDetailsPanel;
