import {ScrollArea} from '@/components/ui/scroll-area';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType} from '@/shared/types';
import {useEffect, useMemo, useState} from 'react';

import {useComponentFiltering} from '../hooks/useComponentFiltering';
import ActionComponentsFilter from './ActionComponentsFilter';
import WorkflowNodesTabsItem from './WorkflowNodesTabsItem';

type DefinitionType = (ComponentDefinitionBasic | TaskDispatcherDefinition) & {
    taskDispatcher: boolean;
    trigger: boolean;
};

interface WorkflowNodesTabsProps {
    actionComponentDefinitions: Array<ComponentDefinitionBasic>;
    clusterElementComponentDefinitions?: Array<ComponentDefinitionBasic>;
    hideActionComponents?: boolean;
    hideClusterElementComponents?: boolean;
    hideTaskDispatchers?: boolean;
    hideTriggerComponents?: boolean;
    itemsDraggable?: boolean;
    onItemClick?: (clickedItem: ClickedDefinitionType) => void;
    selectedComponentName?: string;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    triggerComponentDefinitions: Array<ComponentDefinitionBasic>;
}

const WorkflowNodesTabs = ({
    actionComponentDefinitions,
    clusterElementComponentDefinitions,
    hideActionComponents = false,
    hideClusterElementComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    itemsDraggable = false,
    onItemClick,
    selectedComponentName,
    taskDispatcherDefinitions,
    triggerComponentDefinitions,
}: WorkflowNodesTabsProps) => {
    const [activeTab, setActiveTab] = useState(
        !hideActionComponents ? 'components' : !hideClusterElementComponents ? 'clusterElements' : 'triggers'
    );
    const [previousComponentListLength, setPreviousComponentListLength] = useState(actionComponentDefinitions.length);

    const ff_1057 = useFeatureFlagsStore()('ff-1057');

    const {
        deselectAllCategories,
        filterState,
        filteredCategories,
        filteredComponents,
        setActiveView,
        setSearchValue,
        toggleCategory,
    } = useComponentFiltering({actionComponentDefinitions});

    const availableTriggers = useMemo(() => {
        return triggerComponentDefinitions.map(
            (triggerDefinition) =>
                ({
                    ...triggerDefinition,
                    trigger: true,
                }) as DefinitionType
        );
    }, [triggerComponentDefinitions]);

    const availableTaskDispatchers = useMemo(() => {
        let availableTaskDispatchers;

        if (ff_1057) {
            availableTaskDispatchers = taskDispatcherDefinitions;
        } else {
            availableTaskDispatchers = taskDispatcherDefinitions.filter(
                (taskDispatcherDefinition) =>
                    taskDispatcherDefinition.name === 'branch' ||
                    taskDispatcherDefinition.name === 'condition' ||
                    taskDispatcherDefinition.name === 'loop'
            );
        }

        return availableTaskDispatchers.map(
            (dispatcher) =>
                ({
                    ...dispatcher,
                    taskDispatcher: true,
                }) as DefinitionType
        );
    }, [ff_1057, taskDispatcherDefinitions]);

    const availableClusterElements = useMemo(
        () =>
            clusterElementComponentDefinitions?.map((clusterElementDefinition) => ({
                ...clusterElementDefinition,
                clusterElement: true,
                taskDispatcher: false,
                trigger: false,
            })),
        [clusterElementComponentDefinitions]
    );

    useEffect(() => {
        if (previousComponentListLength === actionComponentDefinitions.length) {
            return;
        }

        setPreviousComponentListLength(actionComponentDefinitions.length);

        if (filterState.selectedCategories.length > 0) {
            setActiveView(filterState.activeView);
        } else if (filterState.activeView === 'filtered') {
            setActiveView('all');
        }
    }, [
        actionComponentDefinitions.length,
        clusterElementComponentDefinitions,
        filterState.activeView,
        filterState.selectedCategories.length,
        previousComponentListLength,
        setActiveView,
    ]);

    return (
        <Tabs className="flex h-full flex-col" onValueChange={setActiveTab} value={activeTab}>
            <div className="px-2">
                <TabsList className="my-2 flex w-full justify-between bg-surface-neutral-secondary">
                    {!hideTriggerComponents && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="triggers">
                            Triggers
                        </TabsTrigger>
                    )}

                    {!hideActionComponents && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="components">
                            Actions
                        </TabsTrigger>
                    )}

                    {!hideTaskDispatchers && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="taskDispatchers">
                            Flows
                        </TabsTrigger>
                    )}

                    {!hideClusterElementComponents && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="clusterElements">
                            Cluster Elements
                        </TabsTrigger>
                    )}
                </TabsList>
            </div>

            {activeTab === 'components' && (
                <ActionComponentsFilter
                    actionComponentDefinitions={actionComponentDefinitions}
                    deselectAllCategories={deselectAllCategories}
                    filterState={filterState}
                    filteredCategories={filteredCategories}
                    filteredComponents={filteredComponents}
                    setActiveView={setActiveView}
                    setSearchValue={setSearchValue}
                    toggleCategory={toggleCategory}
                />
            )}

            {!hideClusterElementComponents && (
                <ScrollArea className="overflow-y-auto px-3">
                    <TabsContent className="mt-0 w-full flex-1" value="clusterElements">
                        <ul className="space-y-2" role="list">
                            {!clusterElementComponentDefinitions?.length && (
                                <span className="block px-3 py-2 text-xs text-content-neutral-secondary">
                                    No cluster element components found.
                                </span>
                            )}

                            {availableClusterElements?.map((clusterElementDefinition, index) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() =>
                                        onItemClick && onItemClick(clusterElementDefinition as ClickedDefinitionType)
                                    }
                                    key={index}
                                    node={clusterElementDefinition}
                                />
                            ))}
                        </ul>
                    </TabsContent>
                </ScrollArea>
            )}

            {!hideTriggerComponents && (
                <ScrollArea className="overflow-y-auto px-3">
                    <TabsContent className="mt-0 w-full flex-1" value="triggers">
                        <ul className="space-y-2" role="list">
                            {!triggerComponentDefinitions.length && (
                                <span className="block px-3 py-2 text-xs text-content-neutral-secondary">
                                    No trigger components found.
                                </span>
                            )}

                            {availableTriggers.map((triggerDefinition) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() =>
                                        onItemClick && onItemClick(triggerDefinition as ClickedDefinitionType)
                                    }
                                    key={triggerDefinition.name}
                                    node={triggerDefinition as DefinitionType}
                                />
                            ))}
                        </ul>
                    </TabsContent>
                </ScrollArea>
            )}

            {!hideActionComponents && (
                <ScrollArea className="overflow-y-auto px-3">
                    <TabsContent className="mt-0 w-full flex-1" value="components">
                        <ul className="space-y-2" role="list">
                            {actionComponentDefinitions.length === 0 && filterState.activeView === 'all' && (
                                <span className="block px-3 py-2 text-xs text-content-neutral-secondary">
                                    No action components found.
                                </span>
                            )}

                            {filteredComponents?.length === 0 && filterState.activeView === 'filtered' && (
                                <span className="block px-3 py-2 text-xs text-content-neutral-secondary">
                                    No filtered components found.
                                </span>
                            )}

                            {filteredComponents?.map((componentDefinition) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() =>
                                        onItemClick && onItemClick(componentDefinition as ClickedDefinitionType)
                                    }
                                    key={componentDefinition.name}
                                    node={componentDefinition as DefinitionType}
                                    selected={selectedComponentName === componentDefinition.name}
                                />
                            ))}
                        </ul>
                    </TabsContent>
                </ScrollArea>
            )}

            {!hideTaskDispatchers && (
                <ScrollArea className="overflow-y-auto px-3">
                    <TabsContent className="mt-0 w-full flex-1" value="taskDispatchers">
                        <ul className="space-y-2" role="list">
                            {!taskDispatcherDefinitions.length && (
                                <span className="block px-3 py-2 text-xs text-content-neutral-secondary">
                                    No flow controls found.
                                </span>
                            )}

                            {availableTaskDispatchers.map((taskDispatcherDefinition) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() =>
                                        onItemClick && onItemClick(taskDispatcherDefinition as ClickedDefinitionType)
                                    }
                                    key={taskDispatcherDefinition.name}
                                    node={taskDispatcherDefinition as DefinitionType}
                                />
                            ))}
                        </ul>
                    </TabsContent>
                </ScrollArea>
            )}
        </Tabs>
    );
};

export default WorkflowNodesTabs;
