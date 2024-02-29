import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ComponentDefinitionBasicModel,
    WorkflowConnectionModel,
    WorkflowNodeOutputModel,
} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import DestinationTab from '@/pages/automation/project/components/node-details-tabs/DestinationTab';
import SourceTab from '@/pages/automation/project/components/node-details-tabs/SourceTab';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import {WorkflowNodeDisplayConditionKeys} from '@/queries/platform/workflowNodeDisplayConditions.queries';
import {useGetWorkflowNodeOutputQuery} from '@/queries/platform/workflowNodeOutputs.queries';
import {ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import Properties from 'components/Properties/Properties';
import {useGetComponentActionDefinitionQuery} from 'queries/platform/actionDefinitions.queries';
import {
    useGetComponentDefinitionQuery,
    useGetComponentDefinitionsQuery,
} from 'queries/platform/componentDefinitions.queries';
import {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
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
    previousComponentDefinitions,
    workflowNodeOutputs,
}: {
    previousComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    workflowNodeOutputs: WorkflowNodeOutputModel[];
}) => {
    const [activeTab, setActiveTab] = useState('description');
    const [componentDefinitionNames, setComponentDefinitionNames] = useState<Array<string>>([]);
    const [currentActionName, setCurrentActionName] = useState('');
    const [currentComponentData, setCurrentComponentData] = useState<ComponentDataType>();

    const {currentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery({
        componentName: currentNode.componentName || currentNode.id,
    });

    const {componentActions, componentData, dataPills, setComponentActions, setComponentData, setDataPills, workflow} =
        useWorkflowDataStore();

    let currentComponent: CurrentComponentType | undefined;

    if (currentComponentDefinition) {
        currentComponent = currentComponentDefinition;

        if (currentComponent) {
            currentComponent.workflowNodeName = currentNode.name;
        }
    }

    const {projectId} = useParams();

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            if (projectId) {
                queryClient.invalidateQueries({queryKey: WorkflowKeys.projectWorkflows(parseInt(projectId))});
            }

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            queryClient.invalidateQueries({
                queryKey: [
                    ...WorkflowNodeDisplayConditionKeys.workflowNodeDisplayConditions,
                    workflow.id!,
                    currentNode.name,
                ],
            });
        },
    });

    const handleActionSelectChange = (value: string) => {
        setCurrentActionName(value);

        setComponentActions(
            componentActions.map((componentAction) => {
                if (componentAction.workflowNodeName === currentNode.name) {
                    return {
                        ...componentAction,
                        actionName: value,
                    };
                } else {
                    return componentAction;
                }
            })
        );

        if (currentComponent) {
            setComponentData([
                ...componentData.filter((component) => component.workflowNodeName !== currentNode.name),
                {
                    ...currentComponent,
                    actionName: value,
                    componentName: currentNode.componentName || currentNode.id,
                    workflowNodeName: currentNode.name,
                },
            ]);

            if (!currentComponentData) {
                return;
            }

            const {componentName, icon, title, workflowNodeName} = currentComponentData;

            saveWorkflowDefinition(
                {
                    actionName: value,
                    componentName,
                    icon,
                    label: title,
                    name: workflowNodeName!,
                    type: `${componentName}/v1/${value}`,
                },
                workflow,
                updateWorkflowMutation
            );
        }
    };

    const getActionName = (): string => {
        const currentComponentActionNames = currentComponent?.actions?.map((action) => action.name);

        return currentComponentActionNames?.includes(currentActionName)
            ? currentActionName
            : (currentComponent?.actions?.[0]?.name as string);
    };

    const {data: connectionComponentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {data: currentActionDefinition, isFetched: currentActionFetched} = useGetComponentActionDefinitionQuery(
        {
            actionName: getActionName(),
            componentName: currentComponent?.name as string,
            componentVersion: currentComponent?.version as number,
        },
        !!currentComponent?.actions && !!getActionName()
    );

    const {componentNames, nodeNames} = workflow;

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

    const {data: workflowNodeOutput} = useGetWorkflowNodeOutputQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentNode.name,
        },
        hasOutputData && activeTab === 'output'
    );

    const currentWorkflowTask = workflow.tasks?.find((task) => task.name === currentNode.name);

    const workflowConnections: WorkflowConnectionModel[] = currentWorkflowTask?.connections || [];

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
            return currentComponent?.name && componentDefinitionNames?.includes(currentComponent.name);
        }

        if (name === 'source' || name === 'destination') {
            return currentComponent?.name === 'dataStream';
        }

        if (name === 'output') {
            return hasOutputData;
        }

        if (name === 'properties') {
            return currentActionProperties?.length;
        }

        return true;
    });

    const otherComponentData = componentData.filter((component) => {
        if (component.workflowNodeName === currentComponent?.workflowNodeName) {
            return false;
        } else {
            return true;
        }
    });

    useEffect(() => {
        if (currentComponentDefinition) {
            // @ts-expect-error Backend needs to be updated to return the correct type
            const {icon, name, title, workflowNodeName} = currentComponentDefinition;

            setCurrentComponentData({
                actionName: currentActionName,
                componentName: name,
                icon,
                title,
                workflowNodeName,
            });
        }
    }, [currentActionName, currentComponent, currentComponentDefinition, workflow.tasks]);

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

    // Set componentDefinitionNames depending on componentDefinitions
    useEffect(() => {
        if (connectionComponentDefinitions?.length) {
            setComponentDefinitionNames(
                connectionComponentDefinitions.map((componentDefinition) => componentDefinition.name)
            );
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [connectionComponentDefinitions?.length]);

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

        if (
            activeTab === 'connection' &&
            currentComponent?.name &&
            !componentDefinitionNames?.includes(currentComponent.name)
        ) {
            setActiveTab('description');
        }

        if ((activeTab === 'source' || activeTab === 'destination') && currentComponent?.name !== 'dataStream') {
            setActiveTab('description');
        }

        if (currentComponent?.name === 'manual') {
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
        componentDefinitionNames.length,
        currentActionDefinition?.outputDefined,
        currentActionDefinition?.outputFunctionDefined,
        currentActionFetched,
        currentActionProperties?.length,
        currentComponent?.name,
    ]);

    // Close the panel if the current node is deleted
    useEffect(() => {
        if (currentNode.name && !nodeNames.includes(currentNode.name)) {
            setWorkflowNodeDetailsPanelOpen(false);
        }
    }, [currentNode.name, nodeNames, setWorkflowNodeDetailsPanelOpen]);

    return (
        <Dialog.Root modal={false} open={workflowNodeDetailsPanelOpen}>
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-0 bottom-4 right-[65px] top-[70px] z-10 w-screen max-w-[460px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                >
                    {currentComponent ? (
                        <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                            <Dialog.Title className="flex items-center p-4 text-lg font-medium">
                                {currentNode.label}

                                <span className="mx-2 text-sm text-gray-500">({currentNode.name})</span>

                                {currentComponent?.description && (
                                    <Tooltip delayDuration={500}>
                                        <TooltipTrigger>
                                            <InfoCircledIcon className="size-4" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-md" side="bottom">
                                            {currentComponent?.description}
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
                            </Dialog.Title>

                            <div className="flex h-full flex-col">
                                {!!currentComponent?.actions?.length && (
                                    <CurrentActionSelect
                                        actions={currentComponent.actions}
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
                                                    'grow justify-center whitespace-nowrap rounded-none border-0 border-b-2 border-gray-200 bg-white text-sm font-medium py-5 text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
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
                                                componentDefinition={currentComponent}
                                                currentComponentData={currentComponentData}
                                                otherComponentData={otherComponentData}
                                            />
                                        )}

                                        {activeTab === 'source' && <SourceTab />}

                                        {activeTab === 'destination' && <DestinationTab />}

                                        {activeTab === 'connection' && workflowConnections.length > 0 && (
                                            <ConnectionTab
                                                componentDefinition={currentComponent}
                                                workflowConnections={workflowConnections}
                                                workflowId={workflow.id!}
                                                workflowNodeName={currentNode.name}
                                            />
                                        )}

                                        {activeTab === 'properties' &&
                                            (currentActionProperties?.length ? (
                                                <Properties
                                                    actionName={currentActionName}
                                                    currentComponent={currentComponent}
                                                    currentComponentData={currentComponentData}
                                                    customClassName="p-4"
                                                    dataPills={dataPills}
                                                    mention={!!dataPills?.length}
                                                    properties={currentActionProperties}
                                                    updateWorkflowMutation={updateWorkflowMutation}
                                                />
                                            ) : (
                                                <div className="flex h-full items-center justify-center text-xl">
                                                    Loading...
                                                </div>
                                            ))}

                                        {activeTab === 'output' && workflowNodeOutput && (
                                            <OutputTab
                                                currentNode={currentNode}
                                                outputDefined={currentActionDefinition?.outputDefined ?? false}
                                                outputSchema={workflowNodeOutput.outputSchema}
                                                sampleOutput={workflowNodeOutput.sampleOutput!}
                                                workflowId={workflow.id!}
                                            />
                                        )}
                                    </div>
                                </div>
                            </div>

                            <footer className="z-50 mt-auto flex bg-white px-4 py-2">
                                <Select defaultValue={currentComponent.version.toString()}>
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
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default WorkflowNodeDetailsPanel;
