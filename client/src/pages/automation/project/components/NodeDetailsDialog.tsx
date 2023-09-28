import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {ComponentDefinitionModel} from '@/middleware/hermes/configuration';
import {PropertyType} from '@/types/projectTypes';
import {DataPillType} from '@/types/types';
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
import {useNodeDetailsDialogStore} from '../stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import getSubProperties from '../utils/getSubProperties';
import CurrentActionSelect from './CurrentActionSelect';
import ConnectionTab from './node-details-tabs/ConnectionTab';
import DescriptionTab from './node-details-tabs/DescriptionTab';
import OutputTab from './node-details-tabs/OutputTab';

const tabs = [
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

type CurrentComponentType =
    | ({
          workflowAlias?: string;
      } & ComponentDefinitionModel)
    | undefined;

const NodeDetailsDialog = () => {
    const [activeTab, setActiveTab] = useState('description');
    const [currentActionName, setCurrentActionName] = useState('');

    const {currentNode, nodeDetailsDialogOpen, setNodeDetailsDialogOpen} =
        useNodeDetailsDialogStore();

    const {
        componentActions,
        componentNames,
        dataPills,
        setComponentActions,
        setDataPills,
    } = useWorkflowDefinitionStore();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    let currentComponent: CurrentComponentType;

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery({
        componentName: currentNode.originNodeName || currentNode.name,
    });

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
            !!currentComponent?.actions
        );

    const componentDefinitionNames = componentDefinitions?.map(
        (component) => component.name
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
        (component, index) => {
            if (!actionData?.length) {
                return;
            }

            const outputSchema: PropertyType | undefined =
                actionData[index]?.outputSchema;

            const properties = outputSchema?.properties?.length
                ? outputSchema.properties
                : outputSchema?.items;

            return {
                component,
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
                        component: componentProperty.component!,
                        properties: property.properties,
                    });
                } else if (property.items) {
                    return getSubProperties({
                        component: componentProperty.component!,
                        properties: property.items,
                    });
                }

                return {
                    component: JSON.stringify(componentProperty.component),
                    id: property.name,
                    value: property.label || property.name,
                };
            }
        );

        if (existingProperties.length && formattedProperties.length) {
            availableDataPills.push(...formattedProperties);
        }
    });

    useEffect(() => {
        if (availableDataPills) {
            setDataPills(availableDataPills.flat(Infinity));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [availableDataPills.length]);

    useEffect(() => {
        if (currentActionFetched) {
            if (!currentAction?.properties?.length) {
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
    }, [
        activeTab,
        componentDefinitionNames,
        currentAction,
        currentActionFetched,
        currentComponent?.name,
    ]);

    useEffect(() => {
        if (currentAction && currentActionFetched) {
            setCurrentActionName(currentAction.name);

            if (componentActions && currentComponent) {
                const index = componentActions.findIndex(
                    (action) =>
                        action.workflowAlias?.match(new RegExp(/-\d$/)) &&
                        action.workflowAlias === currentComponent?.workflowAlias
                );

                if (index !== -1) {
                    componentActions.splice(index, 1, {
                        actionName: currentAction.name,
                        componentName: currentComponent.name,
                        workflowAlias: currentComponent.workflowAlias,
                    });

                    setComponentActions(componentActions);
                } else {
                    const otherComponentActions = componentActions.filter(
                        (action) =>
                            action.workflowAlias !==
                            currentComponent?.workflowAlias
                    );

                    setComponentActions([
                        ...otherComponentActions,
                        {
                            actionName: currentAction.name,
                            componentName: currentComponent.name,
                            workflowAlias: currentComponent.workflowAlias,
                        },
                    ]);
                }
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeTab, currentAction, currentActionFetched]);

    const componentTabs = tabs.filter((tab) => {
        const {name} = tab;

        const componentHasConnection =
            currentComponent?.name &&
            componentDefinitionNames?.includes(currentComponent.name);

        if (
            (name === 'connection' && !componentHasConnection) ||
            (name === 'output' && !currentAction?.outputSchema) ||
            (name === 'properties' && !currentAction?.properties?.length)
        ) {
            return;
        } else {
            return tab;
        }
    });

    return (
        <Dialog.Root
            open={nodeDetailsDialogOpen}
            onOpenChange={() =>
                setNodeDetailsDialogOpen(!nodeDetailsDialogOpen)
            }
            modal={false}
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-0 bottom-2 right-2 top-16 z-10 w-screen max-w-[460px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                >
                    {currentComponent ? (
                        <div className="flex h-full flex-col divide-y divide-gray-100 bg-white shadow-xl">
                            <Dialog.Title className="flex content-center items-center p-4 text-lg font-medium text-gray-900">
                                {currentNode.label}

                                <span className="mx-2 text-sm text-gray-500">
                                    ({currentNode.name})
                                </span>

                                {currentComponent?.description && (
                                    <TooltipProvider>
                                        <Tooltip>
                                            <TooltipTrigger>
                                                <InfoCircledIcon className="h-4 w-4" />
                                            </TooltipTrigger>

                                            <TooltipContent>
                                                {currentComponent?.description}
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                )}

                                <Button
                                    aria-label="Close the node details dialog"
                                    className="ml-auto pr-0"
                                    displayType="icon"
                                    icon={
                                        <Cross1Icon
                                            className="h-3 w-3 cursor-pointer text-gray-900"
                                            aria-hidden="true"
                                        />
                                    }
                                    onClick={() =>
                                        setNodeDetailsDialogOpen(false)
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

                                {componentTabs.length > 1 && (
                                    <div className="flex justify-center pt-4">
                                        {componentTabs.map((tab) => (
                                            <Button
                                                className={twMerge(
                                                    'grow justify-center whitespace-nowrap rounded-none border-0 border-b-2 border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                                    activeTab === tab.name &&
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

                                <div className="relative h-full">
                                    <div className="absolute left-0 top-0 h-full w-full">
                                        {activeTab === 'description' && (
                                            <DescriptionTab
                                                component={currentComponent}
                                            />
                                        )}

                                        {activeTab === 'properties' &&
                                            !!currentAction?.properties
                                                ?.length && (
                                                <Properties
                                                    actionName={
                                                        currentActionName
                                                    }
                                                    customClassName="p-4 overflow-y-auto relative"
                                                    dataPills={dataPills}
                                                    properties={
                                                        currentAction.properties
                                                    }
                                                    mention={
                                                        !!dataPills?.length
                                                    }
                                                />
                                            )}

                                        {activeTab === 'connection' &&
                                            currentComponent.connection && (
                                                <ConnectionTab
                                                    component={currentComponent}
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

                            <footer className="z-50 mt-auto flex bg-white p-4">
                                <Select
                                    defaultValue={currentComponent.version.toString()}
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

export default NodeDetailsDialog;
