import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/shared/middleware/platform/configuration';
import {ClickedItemType} from '@/shared/types';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesTabsItem from './WorkflowNodesTabsItem';

interface WorkflowNodesTabsProps {
    actionComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    itemsDraggable?: boolean;
    onItemClick?: (clickedItem: ClickedItemType) => void;
    popover: boolean;
    triggerComponentDefinitions: Array<ComponentDefinitionBasicModel>;
}

const WorkflowNodesTabs = ({
    actionComponentDefinitions,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    itemsDraggable = false,
    onItemClick,
    popover,
    taskDispatcherDefinitions,
    triggerComponentDefinitions,
}: WorkflowNodesTabsProps) => (
    <div className="size-full px-3">
        <Tabs
            className="relative flex size-full flex-col"
            defaultValue={hideActionComponents ? 'triggers' : 'components'}
        >
            <TabsList className="my-2 flex w-full justify-between">
                {!hideTriggerComponents && (
                    <TabsTrigger className="w-full" value="triggers">
                        Triggers
                    </TabsTrigger>
                )}

                {!hideActionComponents && (
                    <TabsTrigger className="w-full" value="components">
                        Components
                    </TabsTrigger>
                )}

                {!hideTaskDispatchers && (
                    <TabsTrigger className="w-full" value="taskDispatchers">
                        Flows
                    </TabsTrigger>
                )}
            </TabsList>

            <div className="relative mb-4 flex flex-1">
                <div
                    className={twMerge('overflow-y-auto flex absolute w-full', popover && 'h-80', !popover && 'h-full')}
                >
                    {!hideTriggerComponents && (
                        <TabsContent className="mt-0 w-full" value="triggers">
                            <ul className="space-y-2" role="list">
                                {triggerComponentDefinitions.length ? (
                                    triggerComponentDefinitions.map(
                                        (componentDefinition: ComponentDefinitionBasicModel & {trigger?: boolean}) => {
                                            componentDefinition = {
                                                ...componentDefinition,
                                                trigger: true,
                                            };

                                            return (
                                                <WorkflowNodesTabsItem
                                                    draggable={itemsDraggable}
                                                    handleClick={() => onItemClick && onItemClick(componentDefinition)}
                                                    key={componentDefinition.name}
                                                    node={
                                                        componentDefinition as (
                                                            | ComponentDefinitionBasicModel
                                                            | TaskDispatcherDefinitionModel
                                                        ) & {taskDispatcher: boolean; trigger: boolean}
                                                    }
                                                />
                                            );
                                        }
                                    )
                                ) : (
                                    <span className="block px-3 py-2 text-xs text-gray-500">
                                        No trigger components found.
                                    </span>
                                )}
                            </ul>
                        </TabsContent>
                    )}

                    {!hideActionComponents && (
                        <TabsContent className="mt-0 w-full" value="components">
                            <ul className="space-y-2" role="list">
                                {actionComponentDefinitions.length ? (
                                    actionComponentDefinitions.map(
                                        (componentDefinition: ComponentDefinitionBasicModel) => (
                                            <WorkflowNodesTabsItem
                                                draggable={itemsDraggable}
                                                handleClick={() => onItemClick && onItemClick(componentDefinition)}
                                                key={componentDefinition.name}
                                                node={
                                                    componentDefinition as (
                                                        | ComponentDefinitionBasicModel
                                                        | TaskDispatcherDefinitionModel
                                                    ) & {taskDispatcher: boolean; trigger: boolean}
                                                }
                                            />
                                        )
                                    )
                                ) : (
                                    <span className="block px-3 py-2 text-xs text-gray-500">
                                        No action components found.
                                    </span>
                                )}
                            </ul>
                        </TabsContent>
                    )}

                    {!hideTaskDispatchers && (
                        <TabsContent className="mt-0 w-full" value="taskDispatchers">
                            <ul className="space-y-2" role="list">
                                {taskDispatcherDefinitions.length ? (
                                    taskDispatcherDefinitions.map(
                                        (
                                            taskDispatcherDefinition: TaskDispatcherDefinitionModel & {
                                                taskDispatcher?: boolean;
                                            }
                                        ) => {
                                            taskDispatcherDefinition = {
                                                ...taskDispatcherDefinition,
                                                taskDispatcher: true,
                                            };

                                            return (
                                                <WorkflowNodesTabsItem
                                                    draggable={itemsDraggable}
                                                    handleClick={() =>
                                                        onItemClick && onItemClick(taskDispatcherDefinition)
                                                    }
                                                    key={taskDispatcherDefinition.name}
                                                    node={
                                                        taskDispatcherDefinition as (
                                                            | ComponentDefinitionBasicModel
                                                            | TaskDispatcherDefinitionModel
                                                        ) & {taskDispatcher: boolean; trigger: boolean}
                                                    }
                                                />
                                            );
                                        }
                                    )
                                ) : (
                                    <span className="block px-3 py-2 text-xs text-gray-500">
                                        No flow controls found.
                                    </span>
                                )}
                            </ul>
                        </TabsContent>
                    )}
                </div>
            </div>
        </Tabs>
    </div>
);

export default WorkflowNodesTabs;
