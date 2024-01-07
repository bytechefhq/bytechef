import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {PropertyType} from '@/types/projectTypes';
import {ComponentDataType, CurrentComponentType, DataPillType} from '@/types/types';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import Properties from 'components/Properties/Properties';
import {
    useGetActionDefinitionsQuery,
    useGetComponentActionDefinitionQuery,
    useGetComponentActionOutputSchemaQuery,
} from 'queries/actionDefinitions.queries';
import {useGetComponentDefinitionQuery, useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import getSubProperties from '../utils/getSubProperties';
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
    componentDefinitions,
}: {
    componentDefinitions: Array<ComponentDefinitionBasicModel>;
}) => {
    const [activeTab, setActiveTab] = useState('description');
    const [componentDefinitionNames, setComponentDefinitionNames] = useState<Array<string>>([]);
    const [currentActionName, setCurrentActionName] = useState('');

    const {currentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery({
        componentName: currentNode.componentName || currentNode.id,
    });

    const {componentData, setComponentData} = useWorkflowDefinitionStore();

    const {componentActions, componentNames, dataPills, setComponentActions, setDataPills} = useWorkflowDataStore();

    let currentComponent: CurrentComponentType | undefined;

    if (currentComponentDefinition) {
        currentComponent = currentComponentDefinition;

        currentComponent.workflowNodeName = currentNode.name;
    }

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
                ...componentData.filter(
                    (component) => component.workflowNodeName !== currentNode.name || component.action !== value
                ),
                {
                    action: value,
                    connection: currentComponent?.connection,
                    name: currentComponent?.name,
                    properties: {
                        ...currentComponentData?.properties,
                        [value]: {},
                    },
                    title: currentComponent?.title,
                    version: currentComponent?.version,
                    workflowNodeName: currentComponent?.workflowNodeName,
                },
            ]);
        }
    };

    const getActionName = (): string => {
        const currentComponentActionNames = currentComponent?.actions?.map((action) => action.name);

        return currentComponentActionNames?.includes(currentActionName)
            ? currentActionName
            : (currentComponent?.actions?.[0]?.name as string);
    };

    const {data: currentAction, isFetched: currentActionFetched} = useGetComponentActionDefinitionQuery(
        {
            actionName: getActionName(),
            componentName: currentComponent?.name as string,
            componentVersion: currentComponent?.version as number,
        },
        !!currentComponent?.actions && !!getActionName()
    );

    const currentActionProperties = currentAction?.properties;

    const taskTypes = componentActions?.map(
        (componentAction) => `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const currentNodeIndex = componentNames.indexOf(currentNode.name);

    const previousComponentNames = componentNames.length > 1 ? componentNames.slice(0, currentNodeIndex) : [];

    const normalizedPreviousComponentNames = previousComponentNames.map((name) =>
        name.match(new RegExp(/_\d$/)) ? name.slice(0, name.length - 2) : name
    );

    const {data: previousComponents} = useGetComponentDefinitionsQuery(
        {
            include: normalizedPreviousComponentNames,
        },
        !!normalizedPreviousComponentNames.length
    );

    const {data: actionData} = useGetActionDefinitionsQuery({taskTypes}, !!componentActions?.length);

    const previousComponentProperties = previousComponents?.map((componentDefinition, index) => {
        if (!actionData?.length) {
            return;
        }

        const outputSchema: PropertyType | undefined = actionData[index]?.outputSchema;

        const properties = outputSchema?.properties?.length ? outputSchema.properties : outputSchema?.items;

        return {
            componentDefinition,
            properties,
        };
    });

    const {data: actionOutputSchema} = useGetComponentActionOutputSchemaQuery(
        {
            actionName: getActionName(),
            componentName: currentComponent?.name as string,
            componentOperationRequestModel: {
                connectionId: undefined,
                parameters: {},
            },
            componentVersion: currentComponent?.version as number,
        },
        !!currentAction?.outputSchemaDataSource
    );

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

        const existingProperties = getExistingProperties(componentProperty.properties);

        const componentAlias = previousComponentNames[index];

        const formattedProperties: DataPillType[] = existingProperties.map((property) => {
            if (property.properties) {
                return getSubProperties(
                    componentAlias,
                    componentProperty.componentDefinition!,
                    property.properties,
                    property.name
                );
            } else if (property.items) {
                return getSubProperties(
                    componentAlias,
                    componentProperty.componentDefinition!,
                    property.items,
                    property.name
                );
            }

            return {
                componentAlias,
                componentDefinition: JSON.stringify(componentProperty.componentDefinition),
                id: property.name,
                value: property.label || property.name,
            };
        });

        if (existingProperties.length && formattedProperties.length) {
            availableDataPills.push(...formattedProperties);
        }
    });

    const nodeTabs = TABS.filter(({name}) => {
        if (name === 'connection') {
            return (
                currentComponent?.name &&
                componentDefinitionNames?.includes(currentComponent.name) &&
                currentActionProperties?.length
            );
        }

        if (name === 'output') {
            return (
                (currentAction?.outputSchema as PropertyType)?.properties?.length ||
                (currentAction?.outputSchema as PropertyType)?.items?.length ||
                currentAction?.outputSchemaDataSource
            );
        }

        if (name === 'properties') {
            return currentActionProperties?.length;
        }

        return true;
    });

    let currentComponentData: ComponentDataType | undefined;

    const otherComponentData = componentData.filter((component) => {
        if (component.workflowNodeName === currentComponent?.workflowNodeName) {
            currentComponentData = component;

            return false;
        } else {
            return true;
        }
    });

    const outputSchema = currentAction?.outputSchema || actionOutputSchema;

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
    }, [componentActions?.length, componentActions?.length, currentNode.name]);

    // Set componentDefinitionNames depending on componentDefinitions
    useEffect(() => {
        if (componentDefinitions?.length) {
            setComponentDefinitionNames(componentDefinitions.map((componentDefinition) => componentDefinition.name));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentDefinitions?.length]);

    // Set dataPills depending on availableDataPills
    useEffect(() => {
        if (availableDataPills) {
            setDataPills(availableDataPills.flat(Infinity));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [availableDataPills.length]);

    // Initialize or update component action
    useEffect(() => {
        if (currentComponentData?.action) {
            if (currentComponent) {
                setCurrentActionName(currentComponentData.action);

                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        action: currentComponentData.action,
                        name: currentComponent.name,
                        workflowNodeName: currentComponent.workflowNodeName,
                    },
                ]);
            }
        } else if (currentComponent?.actions?.[0]?.name) {
            if (currentComponent?.name) {
                setCurrentActionName(currentComponent.actions[0].name);

                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        action: currentComponent.actions[0].name,
                        name: currentComponent.name,
                        workflowNodeName: currentComponent.workflowNodeName,
                    },
                ]);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponent?.workflowNodeName, currentComponentData?.workflowNodeName]);

    // Tab switching logic
    useEffect(() => {
        if (currentActionFetched) {
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

        if (currentComponent?.name === 'manual') {
            setActiveTab('description');
        }

        if (activeTab === 'properties' && currentActionFetched && !currentActionProperties) {
            setActiveTab('description');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        componentDefinitionNames.length,
        currentAction?.outputSchema,
        currentActionFetched,
        currentActionProperties?.length,
        currentComponent?.name,
    ]);

    useEffect(() => {
        if (currentAction && currentActionFetched && currentComponent) {
            if (currentComponentData) {
                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        action: currentAction.name,
                        workflowNodeName: currentComponent.workflowNodeName,
                    },
                ]);
            }

            if (componentActions && currentComponent) {
                const {name, workflowNodeName} = currentComponent;

                const duplicateComponentActionIndex = componentActions.findIndex(
                    (action) =>
                        action.workflowNodeName?.match(new RegExp(/_\d$/)) &&
                        action.workflowNodeName === workflowNodeName
                );

                if (duplicateComponentActionIndex !== -1) {
                    componentActions.splice(duplicateComponentActionIndex, 1, {
                        actionName: currentAction.name,
                        componentName: name,
                        workflowNodeName,
                    });

                    setComponentActions(componentActions);
                } else {
                    const orderedComponentActions = componentNames.map((componentName) => {
                        const componentActionIndex = componentActions.findIndex(
                            (componentAction) => componentAction.workflowNodeName === componentName
                        );

                        return componentActions[componentActionIndex];
                    });

                    const updatedComponentActions = [
                        ...orderedComponentActions.slice(0, currentNodeIndex),
                        {
                            actionName: currentAction.name,
                            componentName: name,
                            workflowNodeName,
                        },
                        ...orderedComponentActions.slice(currentNodeIndex + 1),
                    ];

                    setComponentActions(updatedComponentActions);
                }
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponent?.workflowNodeName, currentAction?.name, currentActionFetched]);

    useEffect(() => {
        if (currentAction?.name) {
            const componentDataIndex = componentData.findIndex(
                (component) => component.workflowNodeName === currentComponent?.workflowNodeName
            );

            if (!componentDataIndex && currentComponent) {
                setComponentData([
                    ...componentData.filter((item) => item.workflowNodeName !== currentComponent!.workflowNodeName),
                    {
                        action: currentAction.name,
                        connection: currentComponent.connection,
                        name: currentComponent.name,
                        properties: {
                            ...currentComponentData?.properties,
                            [currentAction.name]: {},
                        },
                        title: currentComponent.title,
                        version: currentComponent.version,
                        workflowNodeName: currentComponent.workflowNodeName,
                    },
                ]);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentAction?.name, currentComponent?.workflowNodeName]);

    useEffect(() => {
        if (currentNode.componentName && !componentNames.includes(currentNode.componentName)) {
            setWorkflowNodeDetailsPanelOpen(false);
        }
    }, [componentNames, currentNode.componentName, setWorkflowNodeDetailsPanelOpen]);

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
                                            <InfoCircledIcon className="h-4 w-4" />
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
                                    <Cross2Icon aria-hidden="true" className="h-4 w-4 cursor-pointer" />
                                </button>
                            </Dialog.Title>

                            <div className="flex h-full flex-col">
                                {!!currentComponent?.actions?.length && (
                                    <CurrentActionSelect
                                        actions={currentComponent.actions}
                                        description={currentAction?.description}
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
                                    <div className="absolute left-0 top-0 h-full w-full">
                                        {activeTab === 'description' && (
                                            <DescriptionTab
                                                componentDefinition={currentComponent}
                                                currentComponentData={currentComponentData}
                                                otherComponentData={otherComponentData}
                                            />
                                        )}

                                        {activeTab === 'connection' && currentComponent.connection && (
                                            <ConnectionTab componentDefinition={currentComponent} />
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
                                                />
                                            ) : (
                                                <div className="flex h-full items-center justify-center text-xl">
                                                    Loading...
                                                </div>
                                            ))}

                                        {activeTab === 'output' && outputSchema && (
                                            <OutputTab outputSchema={outputSchema} />
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
