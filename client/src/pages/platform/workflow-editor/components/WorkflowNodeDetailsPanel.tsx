import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import DestinationTab from '@/pages/platform/workflow-editor/components/node-details-tabs/DestinationTab';
import SourceTab from '@/pages/platform/workflow-editor/components/node-details-tabs/SourceTab';
import {
    ComponentDefinitionBasicModel,
    WorkflowConnectionModel,
    WorkflowNodeOutputModel,
} from '@/shared/middleware/platform/configuration';
import {useGetComponentActionDefinitionQuery} from '@/shared/queries/platform/actionDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTriggerDefinitionQuery} from '@/shared/queries/platform/triggerDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useGetWorkflowNodeOutputQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {
    ComponentPropertiesType,
    DataPillType,
    PropertyType,
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
        label: 'Source',
        name: 'source',
    },
    {
        label: 'Destination',
        name: 'destination',
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
    previousComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutputModel[];
}) => {
    const [activeTab, setActiveTab] = useState('description');
    const [availableDataPills, setAvailableDataPills] = useState<Array<DataPillType>>();
    const [currentOperationName, setCurrentOperationName] = useState('');
    const [currentOperationProperties, setCurrentOperationProperties] = useState<Array<PropertyType>>([]);
    const [workflowDefinition, setWorkflowDefinition] = useState<WorkflowDefinitionType>({});

    const {
        currentComponent,
        currentNode,
        setCurrentComponent,
        setCurrentComponentDefinition,
        setCurrentNode,
        setWorkflowNodeDetailsPanelOpen,
        workflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore();

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: currentNode?.componentName || currentNode?.id || '',
        },
        !!currentNode
    );

    const {componentActions, setComponentActions, setDataPills, workflow} = useWorkflowDataStore();

    const currentWorkflowTrigger = workflow.triggers?.find((trigger) => trigger.name === currentNode?.name);
    const currentWorkflowTask = workflow.tasks?.find((task) => task.name === currentNode?.name);

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            workflowId: workflow.id as string,
            workflowNodeName: currentNode?.name as string,
        },
        !!workflow.id && !!currentNode
    );

    const getActionName = (): string => {
        const currentComponentActionNames = currentComponentDefinition?.actions?.map((action) => action.name);

        return currentComponentActionNames?.includes(currentOperationName)
            ? currentOperationName
            : (currentComponentDefinition?.actions?.[0]?.name as string);
    };

    const {data: currentActionDefinition, isFetched: currentActionFetched} = useGetComponentActionDefinitionQuery(
        {
            actionName: getActionName(),
            componentName: currentComponentDefinition?.name as string,
            componentVersion: currentComponentDefinition?.version as number,
        },
        !!currentComponentDefinition?.actions && !currentNode?.trigger && !!getActionName()
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

            const outputSchemaDefinition: PropertyType | undefined = workflowNodeOutputs[index]?.outputSchema;

            const properties = outputSchemaDefinition?.properties?.length
                ? outputSchemaDefinition.properties
                : outputSchemaDefinition?.items;

            return {
                componentDefinition,
                properties,
            };
        }
    );

    const hasOutputData =
        currentActionDefinition?.outputDefined ||
        currentActionDefinition?.dynamicOutput ||
        currentTriggerDefinition?.outputDefined ||
        currentTriggerDefinition?.dynamicOutput;

    const {data: workflowNodeOutput, refetch: refetchWorkflowNodeOutput} = useGetWorkflowNodeOutputQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentNode?.name as string,
        },
        !!currentNode?.name && !!workflow.id && hasOutputData && activeTab === 'output'
    );

    const workflowConnections: WorkflowConnectionModel[] =
        currentWorkflowTask?.connections || currentWorkflowTrigger?.connections || [];

    const nodeTabs = TABS.filter(({name}) => {
        if (name === 'connection') {
            return workflowConnections.length > 0;
        }

        if (name === 'source' || name === 'destination') {
            return currentComponentDefinition?.name === 'dataStream';
        }

        if (name === 'output') {
            return hasOutputData;
        }

        if (name === 'properties') {
            return currentOperationProperties?.length;
        }

        return true;
    });

    const queryClient = useQueryClient();

    const handleOperationSelectChange = (newOperationName: string) => {
        if (!currentComponentDefinition || !currentComponent) {
            return;
        }

        queryClient.invalidateQueries({
            queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
        });

        queryClient.invalidateQueries({
            queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
        });

        const {componentName, notes, title, workflowNodeName} = currentComponent;

        saveWorkflowDefinition(
            {
                componentName,
                description: notes,
                label: title,
                name: workflowNodeName || currentNode?.name || '',
                operationName: newOperationName,
                parameters: getParametersWithDefaultValues({
                    properties: currentOperationProperties as Array<PropertyType>,
                }),
                trigger: currentNode?.trigger,
                type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
            },
            workflow,
            updateWorkflowMutation,
            undefined,
            () => {
                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: {},
                    metadata: {},
                    operationName: newOperationName,
                    parameters: getParametersWithDefaultValues({
                        properties: currentOperationProperties as Array<PropertyType>,
                    }),
                });

                setCurrentOperationName(newOperationName);

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
            }
        );
    };

    const handlePanelClose = () => {
        setCurrentNode(undefined);

        setCurrentComponent(undefined);

        setCurrentComponentDefinition(undefined);

        setWorkflowNodeDetailsPanelOpen(false);
    };

    // Set currentOperationProperties depending if the current node is a trigger or an action
    useEffect(
        () =>
            setCurrentOperationProperties(
                currentNode?.trigger
                    ? (currentTriggerDefinition?.properties ?? [])
                    : (currentActionDefinition?.properties ?? [])
            ),
        [currentActionDefinition?.properties, currentNode?.trigger, currentTriggerDefinition?.properties]
    );

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
    }, [componentActions, currentNode?.name]);

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
        if (activeTab === 'connection' && workflowConnections.length === 0) {
            setActiveTab('description');
        }

        if (
            (activeTab === 'source' || activeTab === 'destination') &&
            currentComponentDefinition?.name !== 'dataStream'
        ) {
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
        currentActionDefinition?.dynamicOutput,
        currentActionFetched,
        currentOperationProperties?.length,
        currentComponentDefinition?.name,
    ]);

    // Close the panel if the current node is deleted
    useEffect(() => {
        if (!currentNode?.name || !nodeNames.includes(currentNode?.name)) {
            setWorkflowNodeDetailsPanelOpen(false);
        }
    }, [currentNode?.name, nodeNames, setWorkflowNodeDetailsPanelOpen]);

    // If the current component requires a connection, set the active tab to 'connection'
    useEffect(() => {
        if (currentComponentDefinition?.connectionRequired && !workflowTestConfigurationConnections?.length) {
            setActiveTab('connection');
        }

        setCurrentComponentDefinition(currentComponentDefinition);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponentDefinition]);

    // Update outputSchema to match the current action definition
    useEffect(() => {
        refetchWorkflowNodeOutput();
    }, [currentOperationName, refetchWorkflowNodeOutput]);

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
            return;
        }

        const taskNames = workflowDefinition.tasks?.map((task) => task.name);

        if (currentNode && taskNames && !taskNames?.includes(currentNode?.name)) {
            setWorkflowNodeDetailsPanelOpen(false);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNode, workflowDefinition.tasks?.length]);

    // Store new operationName into currentNode
    useEffect(() => {
        if (currentNode && currentOperationName) {
            setCurrentNode({
                ...currentNode,
                operationName: currentOperationName,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentOperationName]);

    if (!workflowNodeDetailsPanelOpen || !currentNode?.name || !currentComponentDefinition) {
        return <></>;
    }

    return (
        <div
            className="absolute inset-y-4 right-4 z-10 w-screen max-w-[460px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
            key={currentNode?.name}
        >
            {currentComponentDefinition ? (
                <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                    <header className="flex items-center p-4 text-lg font-medium">
                        {currentComponentDefinition.icon && (
                            <InlineSVG className="mr-2 size-6" src={currentComponentDefinition.icon} />
                        )}

                        {currentNode?.label}

                        <span className="mx-2 text-sm text-gray-500">({currentNode?.name})</span>

                        {currentComponentDefinition.description && (
                            <Tooltip delayDuration={500}>
                                <TooltipTrigger>
                                    <InfoCircledIcon className="size-4" />
                                </TooltipTrigger>

                                <TooltipPortal>
                                    <TooltipContent className="max-w-md" side="bottom">
                                        {currentComponentDefinition.description}
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
                        {(!!currentComponentDefinition.actions?.length ||
                            !!currentComponentDefinition.triggers?.length) && (
                            <CurrentOperationSelect
                                description={
                                    currentNode?.trigger
                                        ? currentTriggerDefinition?.description
                                        : currentActionDefinition?.description
                                }
                                handleValueChange={handleOperationSelectChange}
                                operations={
                                    (currentNode?.trigger
                                        ? currentComponentDefinition.triggers
                                        : currentComponentDefinition.actions)!
                                }
                                triggerSelect={currentNode?.trigger}
                                value={currentOperationName}
                            />
                        )}

                        {((!currentNode?.trigger && currentActionFetched) ||
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
                            {currentComponentDefinition && (
                                <div className="absolute left-0 top-0 size-full">
                                    {activeTab === 'description' && (
                                        <DescriptionTab
                                            key={`${currentNode?.name}_description`}
                                            updateWorkflowMutation={updateWorkflowMutation}
                                        />
                                    )}

                                    {activeTab === 'source' && <SourceTab />}

                                    {activeTab === 'destination' && <DestinationTab />}

                                    {activeTab === 'connection' && workflowConnections.length > 0 && currentNode && (
                                        <ConnectionTab
                                            componentDefinition={currentComponentDefinition}
                                            key={`${currentNode?.name}_connection`}
                                            workflowConnections={workflowConnections}
                                            workflowId={workflow.id!}
                                            workflowNodeName={currentNode?.name}
                                            workflowTestConfigurationConnections={workflowTestConfigurationConnections}
                                        />
                                    )}

                                    {activeTab === 'properties' &&
                                        currentComponentDefinition &&
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

                                    {activeTab === 'output' && workflowNodeOutput && currentNode && (
                                        <OutputTab
                                            currentNode={currentNode}
                                            key={`${currentNode?.name}_output`}
                                            outputDefined={currentActionDefinition?.outputDefined ?? false}
                                            outputSchema={workflowNodeOutput.outputSchema}
                                            sampleOutput={workflowNodeOutput.sampleOutput!}
                                            workflowId={workflow.id!}
                                        />
                                    )}
                                </div>
                            )}
                        </div>
                    </main>

                    <footer className="z-50 mt-auto flex bg-white px-4 py-2">
                        <Select defaultValue={currentComponentDefinition.version.toString()}>
                            <SelectTrigger className="w-auto border-none shadow-none">
                                <SelectValue placeholder="Choose version..." />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="1">v1</SelectItem>
                            </SelectContent>
                        </Select>
                    </footer>
                </div>
            ) : (
                <div className="flex w-full justify-center p-4">
                    <span className="text-gray-500">Something went wrong 👾</span>
                </div>
            )}
        </div>
    );
};

export default WorkflowNodeDetailsPanel;
