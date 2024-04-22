import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
    UpdateWorkflowRequest,
    WorkflowConnectionModel,
    WorkflowModel,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import DestinationTab from '@/pages/platform/workflow-editor/components/node-details-tabs/DestinationTab';
import SourceTab from '@/pages/platform/workflow-editor/components/node-details-tabs/SourceTab';
import {useGetTriggerDefinitionQuery} from '@/queries/platform/triggerDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/queries/platform/workflowNodeOptions.queries';
import {useGetWorkflowNodeOutputQuery} from '@/queries/platform/workflowNodeOutputs.queries';
import {ComponentType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {UseMutationResult, useQueryClient} from '@tanstack/react-query';
import {useGetComponentActionDefinitionQuery} from 'queries/platform/actionDefinitions.queries';
import {useGetComponentDefinitionQuery} from 'queries/platform/componentDefinitions.queries';
import {useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import getSubProperties from '../utils/getSubProperties';
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
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
    workflowNodeOutputs: WorkflowNodeOutputModel[];
}) => {
    const [activeTab, setActiveTab] = useState('description');
    const [currentOperationName, setCurrentOperationName] = useState('');
    const [currentComponent, setCurrentComponent] = useState<ComponentType>();

    const {currentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: currentNode.componentName || currentNode.id,
    });

    const {componentActions, components, dataPills, setComponentActions, setComponents, setDataPills, workflow} =
        useWorkflowDataStore();

    let currentComponentDefinition: ComponentDefinitionModel | undefined;

    if (componentDefinition) {
        currentComponentDefinition = {...componentDefinition};
    }

    const currentWorkflowTrigger = workflow.triggers?.find((trigger) => trigger.name === currentNode.name);
    const currentWorkflowTask = workflow.tasks?.find((task) => task.name === currentNode.name);

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
        !!currentComponentDefinition?.actions && !!getActionName()
    );

    const getTriggerName = (): string => {
        const currentComponentTriggerNames = currentComponentDefinition?.triggers?.map((trigger) => trigger.name);

        return currentComponentTriggerNames?.includes(currentOperationName)
            ? currentOperationName
            : (currentComponentDefinition?.triggers?.[0]?.name as string);
    };

    const {data: currentTriggerDefinition} = useGetTriggerDefinitionQuery(
        {
            componentName: componentDefinition?.name as string,
            componentVersion: componentDefinition?.version as number,
            triggerName: getTriggerName(),
        },
        !!currentNode.componentName && currentNode.trigger && !!componentDefinition
    );

    const {nodeNames} = workflow;

    const currentOperationProperties = currentNode.trigger
        ? currentTriggerDefinition?.properties
        : currentActionDefinition?.properties;

    const currentNodeIndex = nodeNames?.indexOf(currentNode.name);

    const previousNodeNames = nodeNames.length > 1 ? nodeNames?.slice(0, currentNodeIndex) : [];

    const actionDefinitions = workflowNodeOutputs
        .filter((workflowNodeOutput) => workflowNodeOutput?.actionDefinition)
        .map((workflowNodeOutput) => workflowNodeOutput.actionDefinition!);

    const previousComponentProperties = previousComponentDefinitions?.map((componentDefinition, index) => {
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
    });

    const hasOutputData = currentActionDefinition?.outputDefined || currentActionDefinition?.outputFunctionDefined;

    const {data: workflowNodeOutput, refetch: refetchWorkflowNodeOutput} = useGetWorkflowNodeOutputQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentNode.name,
        },
        hasOutputData && activeTab === 'output'
    );

    const workflowConnections: WorkflowConnectionModel[] =
        currentWorkflowTask?.connections || currentWorkflowTrigger?.connections || [];

    const getExistingProperties = (properties: Array<PropertyType>): Array<PropertyType> =>
        properties.filter((property) => {
            if (property.properties) {
                return getExistingProperties(property.properties);
            } else if (property.items) {
                return getExistingProperties(property.items);
            }

            return !!property.name;
        });

    const availableDataPills: Array<DataPillType> = [];

    previousComponentProperties?.forEach((componentProperty, index) => {
        if (!componentProperty || !componentProperty.properties?.length) {
            return;
        }

        const {componentDefinition} = componentProperty;

        const existingProperties = getExistingProperties(componentProperty.properties);

        const nodeName = previousNodeNames[index];

        const formattedProperties: DataPillType[] = existingProperties.map((property) => {
            if (property.properties) {
                return getSubProperties(componentDefinition.icon!, nodeName, property.properties, property.name);
            } else if (property.items) {
                return getSubProperties(componentDefinition.icon!, nodeName, property.items, property.name);
            }

            return {
                componentIcon: componentDefinition.icon,
                id: property.name,
                nodeName,
                value: `${nodeName}/${property.name}`,
            };
        });

        if (existingProperties.length && formattedProperties.length) {
            availableDataPills.push(...formattedProperties);
        }
    });

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

    const otherComponents = components.filter((component) => {
        if (component.workflowNodeName === currentNode?.name) {
            return false;
        } else {
            return true;
        }
    });

    const queryClient = useQueryClient();

    const handleOperationSelectChange = (operationName: string) => {
        if (!currentComponentDefinition || !currentComponent) {
            return;
        }

        setCurrentOperationName(operationName);

        setComponentActions(
            componentActions.map((componentAction) => {
                if (componentAction.workflowNodeName === currentNode.name) {
                    return {
                        ...componentAction,
                        operationName,
                    };
                } else {
                    return componentAction;
                }
            })
        );

        queryClient.invalidateQueries({
            queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
        });

        queryClient.invalidateQueries({
            queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
        });

        setComponents([
            ...components.filter((component) => component.workflowNodeName !== currentNode.name),
            {
                componentName: currentNode.componentName || currentNode.id,
                icon: currentComponentDefinition.icon,
                operationName,
                title: currentComponentDefinition?.title,
                workflowNodeName: currentNode.name,
            },
        ]);

        const {componentName, icon, title, workflowNodeName} = currentComponent;

        delete currentWorkflowTask?.parameters;

        saveWorkflowDefinition(
            {
                componentName,
                icon,
                label: title,
                name: workflowNodeName || currentNode.name,
                operationName,
                type: `${componentName}/v1/${operationName}`,
            },
            workflow,
            updateWorkflowMutation
        );
    };

    useEffect(() => {
        if (componentDefinition) {
            const {icon, name, title} = componentDefinition;

            setCurrentComponent({
                componentName: name,
                icon,
                operationName: currentOperationName,
                title,
                workflowNodeName: currentNode.name,
            });
        }
    }, [componentDefinition, currentNode.name, currentOperationName]);

    // Set currentOperationName depending on the currentComponentAction.operationName
    useEffect(() => {
        if (componentActions?.length) {
            const currentComponentAction = componentActions.find(
                (action) => action.workflowNodeName === currentNode.name
            );

            if (currentComponentAction) {
                setCurrentOperationName(currentComponentAction.operationName);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentActions, currentNode.name]);

    // Set dataPills depending on availableDataPills
    useEffect(() => {
        if (availableDataPills) {
            setDataPills(availableDataPills.flat(Infinity));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [availableDataPills.length]);

    // Tab switching logic
    useEffect(() => {
        if (currentActionFetched && activeTab !== 'output') {
            if (!currentOperationProperties?.length) {
                setActiveTab('description');
            }
        }

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

        if (activeTab === 'properties' && currentActionFetched && !currentOperationProperties) {
            setActiveTab('description');
        }

        if (activeTab === 'output' && !hasOutputData) {
            setActiveTab('description');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        currentActionDefinition?.outputDefined,
        currentActionDefinition?.outputFunctionDefined,
        currentActionFetched,
        currentOperationProperties?.length,
        currentComponentDefinition?.name,
    ]);

    // Close the panel if the current node is deleted
    useEffect(() => {
        if (currentNode.name && !nodeNames.includes(currentNode.name)) {
            setWorkflowNodeDetailsPanelOpen(false);
        }
    }, [currentNode.name, nodeNames, setWorkflowNodeDetailsPanelOpen]);

    // If the current component requires a connection, set the active tab to 'connection'
    useEffect(() => {
        if (currentComponentDefinition?.connectionRequired) {
            setActiveTab('connection');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponentDefinition?.name]);

    // Update outputSchema to match the current action definition
    useEffect(() => {
        refetchWorkflowNodeOutput();
    }, [currentOperationName, refetchWorkflowNodeOutput]);

    if (!workflowNodeDetailsPanelOpen) {
        return <></>;
    }

    return (
        <div
            className="absolute inset-y-4 right-4 z-10 w-screen max-w-[460px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
            key={currentNode.name}
        >
            {currentComponentDefinition ? (
                <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                    <header className="flex items-center p-4 text-lg font-medium">
                        {currentComponentDefinition.icon && (
                            <InlineSVG className="mr-2 size-6" src={currentComponentDefinition.icon} />
                        )}

                        {currentNode.label}

                        <span className="mx-2 text-sm text-gray-500">({currentNode.name})</span>

                        {currentComponentDefinition.description && (
                            <Tooltip delayDuration={500}>
                                <TooltipTrigger>
                                    <InfoCircledIcon className="size-4" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-md" side="bottom">
                                    {currentComponentDefinition.description}
                                </TooltipContent>
                            </Tooltip>
                        )}

                        <button
                            aria-label="Close the node details dialog"
                            className="ml-auto pr-0"
                            onClick={() => setWorkflowNodeDetailsPanelOpen(false)}
                        >
                            <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                        </button>
                    </header>

                    <main className="flex h-full flex-col">
                        {(!!currentComponentDefinition.actions?.length ||
                            !!currentComponentDefinition.triggers?.length) && (
                            <CurrentOperationSelect
                                description={currentActionDefinition?.description}
                                handleValueChange={handleOperationSelectChange}
                                operations={
                                    (currentNode.trigger
                                        ? currentComponentDefinition.triggers
                                        : currentComponentDefinition.actions)!
                                }
                                triggerSelect={currentNode.trigger}
                                value={currentOperationName}
                            />
                        )}

                        {currentActionFetched && nodeTabs.length > 1 && (
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
                            <div className="absolute left-0 top-0 size-full">
                                {activeTab === 'description' && (
                                    <DescriptionTab
                                        componentDefinition={currentComponentDefinition}
                                        currentComponent={currentComponent}
                                        key={`${currentNode.name}_description`}
                                        otherComponents={otherComponents}
                                        updateWorkflowMutation={updateWorkflowMutation}
                                    />
                                )}

                                {activeTab === 'source' && <SourceTab />}

                                {activeTab === 'destination' && <DestinationTab />}

                                {activeTab === 'connection' && workflowConnections.length > 0 && (
                                    <ConnectionTab
                                        componentDefinition={currentComponentDefinition}
                                        key={`${currentNode.name}_connection`}
                                        workflowConnections={workflowConnections}
                                        workflowId={workflow.id!}
                                        workflowNodeName={currentNode.name}
                                    />
                                )}

                                {activeTab === 'properties' &&
                                    (currentOperationProperties?.length ? (
                                        <Properties
                                            currentComponent={currentComponent}
                                            currentComponentDefinition={currentComponentDefinition}
                                            customClassName="p-4"
                                            dataPills={dataPills}
                                            key={`${currentNode.name}_${currentOperationName}_properties`}
                                            operationName={currentOperationName}
                                            properties={currentOperationProperties}
                                        />
                                    ) : (
                                        <div className="flex h-full items-center justify-center text-xl">
                                            Loading...
                                        </div>
                                    ))}

                                {activeTab === 'output' && workflowNodeOutput && (
                                    <OutputTab
                                        currentNode={currentNode}
                                        key={`${currentNode.name}_output`}
                                        outputDefined={currentActionDefinition?.outputDefined ?? false}
                                        outputSchema={workflowNodeOutput.outputSchema}
                                        sampleOutput={workflowNodeOutput.sampleOutput!}
                                        workflowId={workflow.id!}
                                    />
                                )}
                            </div>
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
