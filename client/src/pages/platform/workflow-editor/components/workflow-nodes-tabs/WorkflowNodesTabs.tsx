import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType} from '@/shared/types';
import {useEffect, useMemo, useState} from 'react';

import {useComponentFiltering} from '../../hooks/useComponentFiltering';
import ActionComponentsFilter from '../filters/ActionComponentsFilter';
import TriggerComponentsFilter from '../filters/TriggerComponentsFilter';
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

    const [previousActionComponentsLength, setPreviousActionComponentsLength] = useState(
        actionComponentDefinitions.length
    );
    const [previousTriggerComponentsLength, setPreviousTriggerComponentsLength] = useState(
        triggerComponentDefinitions.length
    );

    const ff_1057 = useFeatureFlagsStore()('ff-1057');

    const actionFiltering = useComponentFiltering({
        componentDefinitions: actionComponentDefinitions,
    });

    const triggerFiltering = useComponentFiltering({
        componentDefinitions: triggerComponentDefinitions,
    });

    useEffect(() => {
        if (previousActionComponentsLength === actionComponentDefinitions.length) {
            return;
        }

        setPreviousActionComponentsLength(actionComponentDefinitions.length);

        if (actionFiltering.filterState.selectedCategories.length > 0) {
            actionFiltering.setActiveView(actionFiltering.filterState.activeView);
        } else if (actionFiltering.filterState.activeView === 'filtered') {
            actionFiltering.setActiveView('all');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        actionComponentDefinitions.length,
        actionFiltering.filterState.activeView,
        actionFiltering.filterState.selectedCategories.length,
        previousActionComponentsLength,
        actionFiltering.setActiveView,
    ]);

    useEffect(() => {
        if (previousTriggerComponentsLength === triggerComponentDefinitions.length) {
            return;
        }

        setPreviousTriggerComponentsLength(triggerComponentDefinitions.length);

        if (triggerFiltering.filterState.selectedCategories.length > 0) {
            triggerFiltering.setActiveView(triggerFiltering.filterState.activeView);
        } else if (triggerFiltering.filterState.activeView === 'filtered') {
            triggerFiltering.setActiveView('all');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        triggerComponentDefinitions.length,
        triggerFiltering.filterState.activeView,
        triggerFiltering.filterState.selectedCategories.length,
        previousTriggerComponentsLength,
        triggerFiltering.setActiveView,
    ]);

    const availableTriggers = useMemo(() => {
        return triggerFiltering.filteredComponents.map(
            (triggerDefinition: ComponentDefinitionBasic) =>
                ({
                    ...triggerDefinition,
                    trigger: true,
                }) as DefinitionType
        );
    }, [triggerFiltering.filteredComponents]);

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

    const tabContentConfigs = useMemo(
        () => ({
            clusterElements: {
                emptyMessage: 'No cluster element components found.',
                items: availableClusterElements,
            },
            components: {
                emptyMessage:
                    actionFiltering.filterState.activeView === 'all'
                        ? 'No action components found.'
                        : 'No filtered components found.',
                items: actionFiltering.filteredComponents,
            },
            taskDispatchers: {
                emptyMessage: 'No flow controls found.',
                items: availableTaskDispatchers,
            },
            triggers: {
                emptyMessage:
                    triggerFiltering.filterState.activeView === 'all'
                        ? 'No trigger components found.'
                        : 'No filtered components found.',
                items: availableTriggers,
            },
        }),
        [
            availableTriggers,
            actionFiltering.filteredComponents,
            actionFiltering.filterState.activeView,
            availableTaskDispatchers,
            availableClusterElements,
            triggerFiltering.filterState.activeView,
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

            {activeTab === 'triggers' && (
                <TriggerComponentsFilter
                    deselectAllCategories={triggerFiltering.deselectAllCategories}
                    filterState={triggerFiltering.filterState}
                    filteredCategories={triggerFiltering.filteredCategories}
                    filteredComponents={triggerFiltering.filteredComponents}
                    setActiveView={triggerFiltering.setActiveView}
                    setSearchValue={triggerFiltering.setSearchValue}
                    toggleCategory={triggerFiltering.toggleCategory}
                    triggerComponentDefinitions={triggerComponentDefinitions}
                />
            )}

            {activeTab === 'components' && (
                <ActionComponentsFilter
                    actionComponentDefinitions={actionComponentDefinitions}
                    deselectAllCategories={actionFiltering.deselectAllCategories}
                    filterState={actionFiltering.filterState}
                    filteredCategories={actionFiltering.filteredCategories}
                    filteredComponents={actionFiltering.filteredComponents}
                    setActiveView={actionFiltering.setActiveView}
                    setSearchValue={actionFiltering.setSearchValue}
                    toggleCategory={actionFiltering.toggleCategory}
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
