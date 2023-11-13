import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {PropertyType} from '@/types/projectTypes';
import {
    ComponentDataType,
    CurrentComponentType,
    DataPillType,
} from '@/types/types';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import Properties from 'components/Properties/Properties';
import {
    useGetActionDefinitionQuery,
    useGetActionDefinitionsQuery,
} from 'queries/actionDefinitions.queries';
import {
    useGetComponentDefinitionQuery,
    useGetComponentDefinitionsQuery,
} from 'queries/componentDefinitions.queries';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import Select from '../../../../components/Select/Select';
import {useNodeDetailsPanelStore} from '../stores/useNodeDetailsPanelStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
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
    const [componentDefinitionNames, setComponentDefinitionNames] = useState<
        Array<string>
    >([]);
    const [currentActionName, setCurrentActionName] = useState('');

    const {currentNode, nodeDetailsPanelOpen, setNodeDetailsPanelOpen} =
        useNodeDetailsPanelStore();

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery({
        componentName: currentNode.originNodeName || currentNode.name,
    });

    const {componentData, setComponentData} = useWorkflowDefinitionStore();

    const {
        componentActions,
        componentNames,
        dataPills,
        setComponentActions,
        setDataPills,
    } = useWorkflowDataStore();

    let currentComponent: CurrentComponentType | undefined;

    if (currentComponentDefinition) {
        currentComponent = currentComponentDefinition;

        currentComponent.workflowAlias = currentNode.name;
    }

    const getActionName = (): string => {
        const currentComponentActionNames = currentComponent?.actions?.map(
            (action) => action.name
        );

        return currentComponentActionNames?.includes(currentActionName)
            ? currentActionName
            : (currentComponent?.actions?.[0]?.name as string);
    };

    const {data: currentAction, isFetched: currentActionFetched} =
        useGetActionDefinitionQuery(
            {
                actionName: getActionName(),
                componentName: currentComponent?.name as string,
                componentVersion: currentComponent?.version as number,
            },
            !!currentComponent?.actions && !!getActionName()
        );

    const currentActionProperties = currentAction?.properties?.filter(
        (property: PropertyType) => {
            if (
                property.controlType === 'SELECT' &&
                (!property.options || !property.options.length)
            ) {
                return false;
            } else {
                return true;
            }
        }
    );

    const taskTypes = componentActions?.map(
        (componentAction) =>
            `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const currentNodeIndex = componentNames.indexOf(currentNode.name);

    const previousComponentNames =
        componentNames.length > 1
            ? componentNames.slice(0, currentNodeIndex)
            : [];

    const normalizedPreviousComponentNames = previousComponentNames.map(
        (name) =>
            name.match(new RegExp(/-\d$/))
                ? name.slice(0, name.length - 2)
                : name
    );

    const {data: previousComponents} = useGetComponentDefinitionsQuery(
        {
            include: normalizedPreviousComponentNames,
        },
        !!normalizedPreviousComponentNames.length
    );

    const {data: actionData} = useGetActionDefinitionsQuery(
        {taskTypes},
        !!componentActions?.length
    );

    const previousComponentProperties = previousComponents?.map(
        (componentDefinition, index) => {
            if (!actionData?.length) {
                return;
            }

            const outputSchema: PropertyType | undefined =
                actionData[index]?.outputSchema;

            const properties = outputSchema?.properties?.length
                ? outputSchema.properties
                : outputSchema?.items;

            return {
                componentDefinition,
                properties,
            };
        }
    );

    const getExistingProperties = (
        properties: PropertyType[]
    ): PropertyType[] =>
        properties.filter((property) => {
            if (property.properties) {
                return getExistingProperties(property.properties);
            } else if (property.items) {
                return getExistingProperties(property.items);
            }

            return !!property.name;
        });

    const availableDataPills: Array<DataPillType> = [];

    previousComponentProperties?.forEach((componentProperty) => {
        if (!componentProperty || !componentProperty.properties?.length) {
            return;
        }

        const existingProperties = getExistingProperties(
            componentProperty.properties
        );

        const formattedProperties: DataPillType[] = existingProperties.map(
            (property) => {
                if (property.properties) {
                    return getSubProperties({
                        componentDefinition:
                            componentProperty.componentDefinition!,
                        properties: property.properties,
                    });
                } else if (property.items) {
                    return getSubProperties({
                        componentDefinition:
                            componentProperty.componentDefinition!,
                        properties: property.items,
                    });
                }

                return {
                    componentDefinition: JSON.stringify(
                        componentProperty.componentDefinition
                    ),
                    id: property.name,
                    value: property.label || property.name,
                };
            }
        );

        if (existingProperties.length && formattedProperties.length) {
            availableDataPills.push(...formattedProperties);
        }
    });

    const nodeTabs = TABS.filter(({name}) => {
        if (name === 'connection') {
            return (
                currentComponent?.name &&
                componentDefinitionNames?.includes(currentComponent.name)
            );
        }

        if (name === 'output') {
            return currentAction?.outputSchema;
        }

        if (name === 'properties') {
            return currentActionProperties?.length;
        }

        return true;
    });

    let currentComponentData: ComponentDataType | undefined;

    const otherComponentData = componentData.filter((component) => {
        if (component.name !== currentComponent?.name) {
            return true;
        } else {
            currentComponentData = component;

            return false;
        }
    });

    useEffect(() => {
        if (componentDefinitions?.length) {
            setComponentDefinitionNames(
                componentDefinitions.map(
                    (componentDefinition) => componentDefinition.name
                )
            );
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentDefinitions?.length]);

    useEffect(() => {
        if (availableDataPills) {
            setDataPills(availableDataPills.flat(Infinity));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [availableDataPills.length]);

    useEffect(() => {
        if (currentComponentData?.action) {
            setCurrentActionName(currentComponentData.action);
        } else if (currentComponent?.actions?.[0]?.name) {
            setCurrentActionName(currentComponent.actions[0].name);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponent?.actions?.length, currentComponentData?.action]);

    useEffect(() => {
        if (currentActionFetched) {
            if (!currentActionProperties?.length) {
                setActiveTab('description');
            }

            if (activeTab === 'output' && !currentAction?.outputSchema) {
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

        if (activeTab === 'properties' && !currentActionProperties) {
            setActiveTab('description');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        componentDefinitionNames,
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
                        name: currentComponentData.name,
                    },
                ]);
            }

            if (componentActions && currentComponent) {
                const {name, workflowAlias} = currentComponent;

                const duplicateComponentActionIndex =
                    componentActions.findIndex(
                        (action) =>
                            action.workflowAlias?.match(new RegExp(/-\d$/)) &&
                            action.workflowAlias === workflowAlias
                    );

                if (duplicateComponentActionIndex !== -1) {
                    componentActions.splice(duplicateComponentActionIndex, 1, {
                        actionName: currentAction.name,
                        componentName: name,
                        workflowAlias,
                    });

                    setComponentActions(componentActions);
                } else {
                    const orderedComponentActions = componentNames.map(
                        (componentName) => {
                            const componentActionIndex =
                                componentActions.findIndex(
                                    (componentAction) =>
                                        componentAction.workflowAlias ===
                                        componentName
                                );

                            return componentActions[componentActionIndex];
                        }
                    );

                    const updatedComponentActions = [
                        ...orderedComponentActions.slice(0, currentNodeIndex),
                        {
                            actionName: currentAction.name,
                            componentName: name,
                            workflowAlias,
                        },
                        ...orderedComponentActions.slice(currentNodeIndex + 1),
                    ];

                    setComponentActions(updatedComponentActions);
                }
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponent?.name, currentAction?.name, currentActionFetched]);

    useEffect(() => {
        if (currentAction?.name) {
            const componentDataIndex = componentData.findIndex(
                (component) => component.name === currentComponent?.name
            );

            if (componentDataIndex === -1 && currentComponent) {
                setComponentData([
                    ...componentData.filter(
                        (item) => item.name !== currentComponent!.name
                    ),
                    {
                        action: currentAction.name,
                        connection: currentComponent.connection,
                        name: currentComponent.name,
                        properties: {
                            [currentAction.name]: {},
                        },
                        title: currentComponent.title,
                        version: currentComponent.version,
                        workflowAlias: currentComponent.workflowAlias,
                    },
                ]);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentAction?.name, currentComponent?.name]);

    return (
        <Dialog.Root
            modal={false}
            onOpenChange={() => setNodeDetailsPanelOpen(!nodeDetailsPanelOpen)}
            open={nodeDetailsPanelOpen}
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-0 bottom-4 right-[74px] top-[70px] z-10 w-screen max-w-[460px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                >
                    {currentComponent ? (
                        <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                            <Dialog.Title className="flex items-center p-4 text-lg font-medium">
                                {currentNode.label}

                                <span className="mx-2 text-sm text-gray-500">
                                    ({currentNode.name})
                                </span>

                                {currentComponent?.description && (
                                    <Tooltip delayDuration={500}>
                                        <TooltipTrigger>
                                            <InfoCircledIcon className="h-4 w-4" />
                                        </TooltipTrigger>

                                        <TooltipContent
                                            className="max-w-md"
                                            side="bottom"
                                        >
                                            {currentComponent?.description}
                                        </TooltipContent>
                                    </Tooltip>
                                )}

                                <Button
                                    aria-label="Close the node details dialog"
                                    className="ml-auto pr-0"
                                    displayType="icon"
                                    icon={
                                        <Cross1Icon
                                            aria-hidden="true"
                                            className="h-3 w-3 cursor-pointer"
                                        />
                                    }
                                    onClick={() =>
                                        setNodeDetailsPanelOpen(false)
                                    }
                                />
                            </Dialog.Title>

                            <div className="flex h-full flex-col">
                                {!!currentComponent?.actions?.length && (
                                    <CurrentActionSelect
                                        actions={currentComponent.actions}
                                        description={currentAction?.description}
                                        handleValueChange={setCurrentActionName}
                                        value={currentActionName}
                                    />
                                )}

                                {currentActionFetched &&
                                    nodeTabs.length > 1 && (
                                        <div className="flex justify-center pt-4">
                                            {nodeTabs.map((tab) => (
                                                <Button
                                                    className={twMerge(
                                                        'grow justify-center whitespace-nowrap rounded-none border-0 border-b-2 border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                                        activeTab ===
                                                            tab?.name &&
                                                            'border-blue-500 text-blue-500 hover:text-blue-500'
                                                    )}
                                                    key={tab.name}
                                                    label={tab.label}
                                                    name={tab.name}
                                                    onClick={() =>
                                                        setActiveTab(tab.name)
                                                    }
                                                />
                                            ))}
                                        </div>
                                    )}

                                <div className="relative h-full overflow-y-scroll">
                                    <div className="absolute left-0 top-0 h-full w-full">
                                        {activeTab === 'description' && (
                                            <DescriptionTab
                                                componentDefinition={
                                                    currentComponent
                                                }
                                                currentComponentData={
                                                    currentComponentData
                                                }
                                                otherComponentData={
                                                    otherComponentData
                                                }
                                            />
                                        )}

                                        {activeTab === 'connection' &&
                                            currentComponent.connection && (
                                                <ConnectionTab
                                                    componentDefinition={
                                                        currentComponent
                                                    }
                                                />
                                            )}

                                        {activeTab === 'properties' &&
                                            !!currentActionProperties?.length && (
                                                <Properties
                                                    actionName={
                                                        currentActionName
                                                    }
                                                    currentComponent={
                                                        currentComponent
                                                    }
                                                    currentComponentData={
                                                        currentComponentData
                                                    }
                                                    customClassName="p-4"
                                                    dataPills={dataPills}
                                                    mention={
                                                        !!dataPills?.length
                                                    }
                                                    properties={
                                                        currentActionProperties
                                                    }
                                                />
                                            )}

                                        {activeTab === 'output' &&
                                            currentAction?.outputSchema && (
                                                <OutputTab
                                                    outputSchema={
                                                        currentAction.outputSchema
                                                    }
                                                />
                                            )}
                                    </div>
                                </div>
                            </div>

                            <footer className="z-50 mt-auto flex bg-white px-4 py-2">
                                <Select
                                    defaultValue={currentComponent.version.toString()}
                                    name="componentVersionSelect"
                                    options={[
                                        {label: 'v1', value: '1'},
                                        {label: 'v2', value: '2'},
                                        {label: 'v3', value: '3'},
                                    ]}
                                />
                            </footer>
                        </div>
                    ) : (
                        <div className="flex w-full justify-center p-4">
                            <span className="text-gray-500">
                                Something went wrong 👾
                            </span>
                        </div>
                    )}
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default WorkflowNodeDetailsPanel;
