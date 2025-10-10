import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType} from '@/shared/types';
import {useEffect, useMemo, useState} from 'react';

import {useComponentFiltering} from '../../hooks/useComponentFiltering';
import ActionComponentsFilter from '../ActionComponentsFilter';
import WorkflowNodesTabContent from './WorkflowNodesTabContent';

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

    const tabContentConfigs = useMemo(
        () => ({
            clusterElements: {
                emptyMessage: 'No cluster element components found.',
                items: availableClusterElements,
            },
            components: {
                emptyMessage:
                    filterState.activeView === 'all' ? 'No action components found.' : 'No filtered components found.',
                items: filteredComponents,
            },
            taskDispatchers: {
                emptyMessage: 'No flow controls found.',
                items: availableTaskDispatchers,
            },
            triggers: {
                emptyMessage: 'No trigger components found.',
                items: availableTriggers,
            },
        }),
        [
            availableTriggers,
            filteredComponents,
            availableTaskDispatchers,
            availableClusterElements,
            filterState.activeView,
        ]
    );

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
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.clusterElements.emptyMessage}
                    items={tabContentConfigs.clusterElements.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="clusterElements"
                />
            )}

            {!hideTriggerComponents && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.triggers.emptyMessage}
                    items={tabContentConfigs.triggers.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="triggers"
                />
            )}

            {!hideActionComponents && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.components.emptyMessage}
                    items={tabContentConfigs.components.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="components"
                />
            )}

            {!hideTaskDispatchers && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.taskDispatchers.emptyMessage}
                    items={tabContentConfigs.taskDispatchers.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="taskDispatchers"
                />
            )}
        </Tabs>
    );
};

export default WorkflowNodesTabs;
