import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {
    ComponentDefinitionBasicModel,
    WorkflowConnectionModel,
    WorkflowModel,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import DestinationTab from '@/pages/automation/project/components/node-details-tabs/DestinationTab';
import SourceTab from '@/pages/automation/project/components/node-details-tabs/SourceTab';
import {useGetWorkflowNodeOutputQuery} from '@/queries/platform/workflowNodeOutputs.queries';
import {ComponentType, CurrentComponentDefinitionType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import Properties from 'components/Properties/Properties';
import {useGetComponentActionDefinitionQuery} from 'queries/platform/actionDefinitions.queries';
import {useGetComponentDefinitionQuery} from 'queries/platform/componentDefinitions.queries';
import {useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import getSubProperties from '../utils/getSubProperties';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import CurrentActionSelect from './CurrentActionSelect';
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
    onPropertyChange,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: {
    onPropertyChange?: () => void;
    previousComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
    workflowNodeOutputs: WorkflowNodeOutputModel[];
}) => {
    const [activeTab, setActiveTab] = useState('description');
    const [currentActionName, setCurrentActionName] = useState('');
    const [currentComponent, setCurrentComponent] = useState<ComponentType>();

    const {currentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: currentNode.componentName || currentNode.id,
    });

    const {componentActions, components, dataPills, setComponentActions, setComponents, setDataPills, workflow} =
        useWorkflowDataStore();

    let currentComponentDefinition: CurrentComponentDefinitionType | undefined;

    if (componentDefinition) {
        currentComponentDefinition = componentDefinition;

        if (currentComponentDefinition) {
            currentComponentDefinition.workflowNodeName = currentNode.name;
        }
    }

    const currentWorkflowTask = workflow.tasks?.find((task) => task.name === currentNode.name);

    const getActionName = (): string => {
        const currentComponentActionNames = currentComponentDefinition?.actions?.map((action) => action.name);

        return currentComponentActionNames?.includes(currentActionName)
            ? currentActionName
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

    const {nodeNames} = workflow;

    const currentActionProperties = currentActionDefinition?.properties;

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

    const taskConnections: WorkflowConnectionModel[] = currentWorkflowTask?.connections || [];

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
            return taskConnections.length > 0;
        }

        if (name === 'source' || name === 'destination') {
            return currentComponentDefinition?.name === 'dataStream';
        }

        if (name === 'output') {
            return hasOutputData;
        }

        if (name === 'properties') {
            return currentActionProperties?.length;
        }

        return true;
    });

    const otherComponents = components.filter((component) => {
        if (component.workflowNodeName === currentComponentDefinition?.workflowNodeName) {
            return false;
        } else {
            return true;
        }
    });

    const handleActionSelectChange = (actionName: string) => {
        setCurrentActionName(actionName);

        setComponentActions(
            componentActions.map((componentAction) => {
                if (componentAction.workflowNodeName === currentNode.name) {
                    return {
                        ...componentAction,
                        actionName,
                    };
                } else {
                    return componentAction;
                }
            })
        );

        if (currentComponentDefinition) {
            setComponents([
                ...components.filter((component) => component.workflowNodeName !== currentNode.name),
                {
                    actionName,
                    componentName: currentNode.componentName || currentNode.id,
                    icon: currentComponentDefinition.icon,
                    title: currentComponentDefinition?.title,
                    workflowNodeName: currentNode.name,
                },
            ]);

            if (!currentComponent) {
                return;
            }

            const {componentName, icon, title, workflowNodeName} = currentComponent;

            delete currentWorkflowTask?.parameters;

            saveWorkflowDefinition(
                {
                    actionName,
                    componentName,
                    icon,
                    label: title,
                    name: workflowNodeName!,
                    type: `${componentName}/v1/${actionName}`,
                },
                workflow,
                updateWorkflowMutation
            );
        }
    };

    useEffect(() => {
        if (componentDefinition) {
            // @ts-expect-error Backend needs to be updated to return the correct type
            const {icon, name, title, workflowNodeName} = componentDefinition;

            setCurrentComponent({
                actionName: currentActionName,
                componentName: name,
                icon,
                title,
                workflowNodeName,
            });
        }
    }, [currentActionName, currentComponentDefinition, componentDefinition, currentNode.name, workflow.tasks]);

    // Set currentActionName depending on the currentComponentAction.actionName
    useEffect(() => {
        if (componentActions?.length) {
            const currentComponentAction = componentActions.find(
                (action) => action.workflowNodeName === currentNode.name
            );

            if (currentComponentAction) {
                setCurrentActionName(currentComponentAction.actionName);
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
            if (!currentActionProperties?.length) {
                setActiveTab('description');
            }
        }

        if (activeTab === 'connection' && taskConnections.length === 0) {
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

        if (activeTab === 'properties' && currentActionFetched && !currentActionProperties) {
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
        currentActionProperties?.length,
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
    }, [currentActionName, refetchWorkflowNodeOutput]);

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
                        {!!currentComponentDefinition.actions?.length && (
                            <CurrentActionSelect
                                actions={currentComponentDefinition.actions}
                                description={currentActionDefinition?.description}
                                handleValueChange={handleActionSelectChange}
                                value={currentActionName}
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

                                {activeTab === 'connection' && taskConnections.length > 0 && (
                                    <ConnectionTab
                                        componentDefinition={currentComponentDefinition}
                                        key={`${currentNode.name}_connection`}
                                        taskConnections={taskConnections}
                                        workflowId={workflow.id!}
                                        workflowNodeName={currentNode.name}
                                    />
                                )}

                                {activeTab === 'properties' &&
                                    (currentActionProperties?.length ? (
                                        <Properties
                                            actionName={currentActionName}
                                            currentComponent={currentComponent}
                                            currentComponentDefinition={currentComponentDefinition}
                                            customClassName="p-4"
                                            dataPills={dataPills}
                                            key={`${currentNode.name}_${currentActionName}_properties`}
                                            onChange={onPropertyChange}
                                            properties={currentActionProperties}
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
