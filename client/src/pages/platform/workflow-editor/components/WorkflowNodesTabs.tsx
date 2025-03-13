import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType} from '@/shared/types';
import {useMemo} from 'react';

import WorkflowNodesTabsItem from './WorkflowNodesTabsItem';

type DefinitionType = (ComponentDefinitionBasic | TaskDispatcherDefinition) & {
    taskDispatcher: boolean;
    trigger: boolean;
};

interface WorkflowNodesTabsProps {
    actionComponentDefinitions: Array<ComponentDefinitionBasic>;
    hideActionComponents?: boolean;
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
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    itemsDraggable = false,
    onItemClick,
    selectedComponentName,
    taskDispatcherDefinitions,
    triggerComponentDefinitions,
}: WorkflowNodesTabsProps) => {
    const ff_1057 = useFeatureFlagsStore()('ff-1057');

    const defaultTabValue = useMemo(() => {
        if (hideActionComponents) {
            return 'triggers';
        }

        return 'components';
    }, [hideActionComponents]);

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
                (taskDispatcherDefinition) => taskDispatcherDefinition.name === 'condition'
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

    return (
        <Tabs className="flex h-full flex-col" defaultValue={defaultTabValue}>
            <TabsList className="my-2 flex w-full justify-between">
                {!hideTriggerComponents && (
                    <TabsTrigger className="w-full" value="triggers">
                        Triggers
                    </TabsTrigger>
                )}

                {!hideActionComponents && (
                    <TabsTrigger className="w-full" value="components">
                        Actions
                    </TabsTrigger>
                )}

                {!hideTaskDispatchers && (
                    <TabsTrigger className="w-full" value="taskDispatchers">
                        Flows
                    </TabsTrigger>
                )}
            </TabsList>

            {!hideTriggerComponents && (
                <TabsContent className="mt-0 w-full flex-1 overflow-auto" value="triggers">
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
            )}

            {!hideActionComponents && (
                <TabsContent className="mt-0 w-full flex-1 overflow-auto" value="components">
                    <ul className="space-y-2" role="list">
                        {!actionComponentDefinitions.length && (
                            <span className="block px-3 py-2 text-xs text-content-neutral-secondary">
                                No action components found.
                            </span>
                        )}

                        {actionComponentDefinitions?.map((componentDefinition) => (
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
            )}

            {!hideTaskDispatchers && (
                <TabsContent className="mt-0 w-full flex-1 overflow-auto" value="taskDispatchers">
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
            )}
        </Tabs>
    );
};

export default WorkflowNodesTabs;
