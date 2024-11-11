import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import DataStreamComponentsTab from '@/pages/platform/workflow-editor/components/node-details-tabs/DataStreamComponentsTab';
import {
    ActionDefinitionApi,
    ComponentDefinition,
    ComponentDefinitionBasic,
    GetComponentActionDefinitionRequest,
    GetComponentTriggerDefinitionRequest,
    TriggerDefinitionApi,
    WorkflowConnection,
    WorkflowNodeOutput,
} from '@/shared/middleware/platform/configuration';
import {useDeleteWorkflowNodeTestOutputMutation} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
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
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {
    ComponentPropertiesType,
    DataPillType,
    PropertyAllType,
    UpdateWorkflowMutationType,
    WorkflowDefinitionType,
} from '@/shared/types';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getAllTaskNames from '../utils/getAllTaskNames';
import getDataPillsFromProperties from '../utils/getDataPillsFromProperties';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import CurrentOperationSelect from './CurrentOperationSelect';
import ConnectionTab from './node-details-tabs/ConnectionTab';
import DescriptionTab from './node-details-tabs/DescriptionTab';
import OutputTab from './node-details-tabs/OutputTab';

const TABS = [
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
    const [activeTab, setActiveTab] = useState('description');
    const [availableDataPills, setAvailableDataPills] = useState<Array<DataPillType>>();
    const [currentOperationName, setCurrentOperationName] = useState('');
    const [currentOperationProperties, setCurrentOperationProperties] = useState<Array<PropertyAllType>>([]);
    const [workflowDefinition, setWorkflowDefinition] = useState<WorkflowDefinitionType>({});

    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {componentActions, setComponentActions, setDataPills, workflow} = useWorkflowDataStore();

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: currentNode?.componentName || currentNode?.id || '',
        },
        !!currentNode && !currentNode.taskDispatcher
    );

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            workflowId: workflow.id as string,
            workflowNodeName: currentNode?.name as string,
        },
        !!workflow.id && !!currentNode
    );

    const matchingOperation = [
        ...(currentComponentDefinition?.actions || []),
        ...(currentComponentDefinition?.triggers || []),
    ].find((action) => action.name === currentOperationName);

    const {data: currentActionDefinition, isFetched: currentActionFetched} = useGetComponentActionDefinitionQuery(
        {
            actionName: currentNode?.operationName ?? currentOperationName,
            componentName: currentComponentDefinition?.name as string,
            componentVersion: currentComponentDefinition?.version as number,
        },
        !!currentComponentDefinition?.actions && !currentNode?.trigger && !!matchingOperation
    );

    const getTriggerName = (): string => {
        const currentComponentTriggerNames = currentComponentDefinition?.triggers?.map((trigger) => trigger.name);

        return currentComponentTriggerNames?.includes(currentOperationName)
            ? currentOperationName
            : (currentComponentDefinition?.triggers?.[0]?.name as string);
    };

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
            taskDispatcherName: currentNode?.componentName || currentNode?.id || '',
            taskDispatcherVersion: currentNode?.version || 1,
        },
        !!currentNode && !!currentNode.taskDispatcher
    );

    const currentNodeDefinition = currentNode?.trigger
        ? currentTriggerDefinition
        : currentNode?.taskDispatcher
          ? currentTaskDispatcherDefinition
          : currentActionDefinition;

    const {nodeNames} = workflow;

    const currentNodeIndex = currentNode && nodeNames?.indexOf(currentNode?.name);

    const previousNodeNames = nodeNames.length > 1 ? nodeNames?.slice(0, currentNodeIndex) : [];

    const actionDefinitions = workflowNodeOutputs
        .filter((workflowNodeOutput) => workflowNodeOutput?.actionDefinition)
        .map((workflowNodeOutput) => workflowNodeOutput.actionDefinition!);

    const previousComponentProperties: Array<ComponentPropertiesType> = previousComponentDefinitions?.map(
        (componentDefinition, index) => {
            if (!actionDefinitions?.length) {
                return;
            }

            const outputSchemaDefinition: PropertyAllType | undefined = workflowNodeOutputs[index]?.outputSchema;

            const properties = outputSchemaDefinition?.properties?.length
                ? outputSchemaDefinition.properties
                : outputSchemaDefinition?.items;

            return {
                componentDefinition,
                properties,
            };
        }
    );

    const hasOutputData = currentNodeDefinition?.outputDefined;
    const currentWorkflowTrigger = workflow.triggers?.find((trigger) => trigger.name === currentNode?.name);
    const currentWorkflowTask = workflow.tasks?.find((task) => task.name === currentNode?.name);

    const componentConnections: WorkflowConnection[] =
        currentWorkflowTask?.connections || currentWorkflowTrigger?.connections || [];

    const nodeTabs = TABS.filter(({name}) => {
        if (name === 'connection') {
            return componentConnections.length > 0;
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

    const deleteWorkflowNodeTestOutputMutation = useDeleteWorkflowNodeTestOutputMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflow.id],
            });
        },
    });

    const queryClient = useQueryClient();

    const handleOperationSelectChange = async (newOperationName: string) => {
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
                queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(componentActionDefinitionRequest),
                queryKey: ActionDefinitionKeys.actionDefinition(componentActionDefinitionRequest),
            });
        }

        const {componentName, notes, title, workflowNodeName} = currentComponent;

        saveWorkflowDefinition({
            nodeData: {
                componentName,
                description: notes,
                label: title,
                name: workflowNodeName || currentNode?.name || '',
                operationName: newOperationName,
                parameters: getParametersWithDefaultValues({
                    properties: operationData.properties as Array<PropertyAllType>,
                }),
                trigger: currentNode?.trigger,
                type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
            },
            onSuccess: () => {
                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: {},
                    metadata: {},
                    operationName: newOperationName,
                    parameters: getParametersWithDefaultValues({
                        properties: currentOperationProperties as Array<PropertyAllType>,
                    }),
                });

                const formattedComponentActions = componentActions.map((componentAction) => {
                    if (componentAction.workflowNodeName === currentNode?.name) {
                        return {
                            ...componentAction,
                            operationName: newOperationName,
                        };
                    } else {
                        return componentAction;
                    }
                });

                setComponentActions(formattedComponentActions);
            },
            queryClient,
            updateWorkflowMutation,
            workflow,
        });
    };

    const handlePanelClose = () => {
        useWorkflowNodeDetailsPanelStore.getState().reset();
    };

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

        const dataPills = getDataPillsFromProperties(previousComponentProperties!, workflow, previousNodeNames);

        setAvailableDataPills(dataPills.flat(Infinity));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [previousComponentProperties.length]);

    // Set dataPills depending on availableDataPills
    useEffect(() => {
        if (availableDataPills?.length) {
            setDataPills(availableDataPills.flat(Infinity));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [availableDataPills?.length]);

    // Tab switching logic
    useEffect(() => {
        if (activeTab === 'connection' && componentConnections.length === 0) {
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
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        currentActionDefinition?.outputDefined,
        currentActionFetched,
        currentOperationProperties?.length,
        currentComponentDefinition?.name,
    ]);

    // Close the panel if the current node is deleted
    useEffect(() => {
        if (!currentNode?.name || !nodeNames.includes(currentNode?.name)) {
            useWorkflowNodeDetailsPanelStore.getState().reset();
        }
    }, [currentNode?.name, nodeNames]);

    // If the current component requires a connection, set the active tab to 'connection'
    useEffect(() => {
        if (currentComponentDefinition?.connectionRequired && !workflowTestConfigurationConnections?.length) {
            setActiveTab('connection');
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponentDefinition]);

    useEffect(() => {
        if (currentNode && workflowTestConfigurationConnections?.[0]?.connectionId) {
            setCurrentNode({
                ...currentNode,
                connectionId: workflowTestConfigurationConnections[0]?.connectionId,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowTestConfigurationConnections]);

    // Parse the workflow definition to an object
    useEffect(() => {
        if (workflow.definition) {
            setWorkflowDefinition(JSON.parse(workflow.definition));
        }
    }, [workflow.definition]);

    // Close the panel if the current node is deleted from the workflow definition
    useEffect(() => {
        if (currentNode?.trigger) {
            deleteWorkflowNodeTestOutputMutation.mutate({
                id: workflow.id!,
                workflowNodeName: currentNode.name,
            });

            return;
        }

        if (!workflowDefinition.tasks) {
            return;
        }

        const taskNames = getAllTaskNames(workflowDefinition.tasks);

        if (currentNode && taskNames && !taskNames?.includes(currentNode?.name)) {
            useWorkflowNodeDetailsPanelStore.getState().reset();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNode, workflowDefinition.tasks?.length]);

    // Store new operationName into currentNode
    useEffect(() => {
        if (currentNode?.operationName && currentOperationName) {
            setCurrentNode({
                ...currentNode,
                operationName: currentOperationName,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentOperationName]);

    // Set currentOperationName depending on the currentComponentAction.operationName
    useEffect(() => {
        if (componentActions?.length) {
            const currentComponentAction = componentActions.find(
                (action) => action.workflowNodeName === currentNode?.name
            );

            if (currentComponentAction) {
                setCurrentOperationName(currentComponentAction.operationName);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentActions, currentNode?.name]);

    const currentTaskData = currentComponentDefinition || currentTaskDispatcherDefinition;
    const currentOperationFetcher = currentActionFetched || currentTriggerFetched;

    const actionDataMissing = currentComponent?.operationName && (!matchingOperation?.name || !currentOperationFetcher);

    if (!workflowNodeDetailsPanelOpen || !currentNode?.name || !currentTaskData) {
        return <></>;
    }

    return (
        <div
            className="absolute inset-y-4 right-4 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-xl border border-muted bg-white shadow-lg"
            key={currentNode?.name}
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

                    <span className="mx-2 text-sm text-gray-500">({currentNode?.name})</span>

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
                    {actionDataMissing && (
                        <div className="flex flex-col border-b border-muted p-4">
                            <span className="text-sm leading-6">Actions</span>

                            <Skeleton className="h-9 w-full" />
                        </div>
                    )}

                    {(!!(currentTaskData as ComponentDefinition).actions?.length ||
                        !!(currentTaskData as ComponentDefinition).triggers?.length) && (
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

                    {((!currentNode?.trigger && !currentNode?.taskDispatcher && currentActionFetched) ||
                        currentNode?.taskDispatcher ||
                        (currentNode?.trigger && currentTriggerFetched)) &&
                        nodeTabs.length > 1 && (
                            <div className="flex justify-center">
                                {nodeTabs.map((tab) => (
                                    <Button
                                        className={twMerge(
                                            'grow justify-center whitespace-nowrap rounded-none border-0 border-b border-gray-200 bg-white text-sm font-medium py-5 text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                            activeTab === tab?.name &&
                                                'border-blue-500 text-blue-500 hover:text-blue-500'
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

                    <div className="relative h-full overflow-y-scroll">
                        {currentTaskData && (
                            <div className="absolute left-0 top-0 size-full">
                                {activeTab === 'description' && (
                                    <DescriptionTab
                                        key={`${currentNode?.name}_description`}
                                        updateWorkflowMutation={updateWorkflowMutation}
                                    />
                                )}

                                {activeTab === 'dataStreamComponents' && <DataStreamComponentsTab />}

                                {activeTab === 'connection' &&
                                    componentConnections.length > 0 &&
                                    currentNode &&
                                    currentComponentDefinition && (
                                        <ConnectionTab
                                            componentDefinition={currentComponentDefinition}
                                            key={`${currentNode?.name}_connection`}
                                            workflowConnections={componentConnections}
                                            workflowId={workflow.id!}
                                            workflowNodeName={currentNode?.name}
                                            workflowTestConfigurationConnections={workflowTestConfigurationConnections}
                                        />
                                    )}

                                {activeTab === 'properties' &&
                                    currentTaskData &&
                                    (currentOperationProperties?.length ? (
                                        <Properties
                                            customClassName="p-4"
                                            key={`${currentNode?.name}_${currentOperationName}_properties`}
                                            operationName={currentOperationName}
                                            properties={currentOperationProperties}
                                        />
                                    ) : (
                                        <div className="flex h-full items-center justify-center text-xl">
                                            Loading...
                                        </div>
                                    ))}

                                {activeTab === 'output' && currentNode && currentComponentDefinition && (
                                    <OutputTab
                                        connectionMissing={
                                            currentComponentDefinition.connectionRequired &&
                                            !workflowTestConfigurationConnections?.length
                                        }
                                        currentNode={currentNode}
                                        key={`${currentNode?.name}_output`}
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
