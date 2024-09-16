import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType} from '@/shared/types';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesTabsItem from './WorkflowNodesTabsItem';

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

            <div className="relative flex flex-1">
                <div className={twMerge('overflow-y-auto flex absolute w-full h-full')}>
                    {!hideTriggerComponents && (
                        <TabsContent className="mt-0 w-full" value="triggers">
                            <ul className="space-y-2" role="list">
                                {!triggerComponentDefinitions.length && (
                                    <span className="block px-3 py-2 text-xs text-gray-500">
                                        No trigger components found.
                                    </span>
                                )}

                                {triggerComponentDefinitions?.map((triggerDefinition) => {
                                    const clickedTriggerDefinition = {
                                        ...triggerDefinition,
                                        trigger: true,
                                    };

                                    return (
                                        <WorkflowNodesTabsItem
                                            draggable={itemsDraggable}
                                            handleClick={() =>
                                                onItemClick &&
                                                onItemClick(clickedTriggerDefinition as ClickedDefinitionType)
                                            }
                                            key={triggerDefinition.name}
                                            node={
                                                triggerDefinition as (
                                                    | ComponentDefinitionBasic
                                                    | TaskDispatcherDefinition
                                                ) & {taskDispatcher: boolean; trigger: boolean}
                                            }
                                        />
                                    );
                                })}
                            </ul>
                        </TabsContent>
                    )}

                    {!hideActionComponents && (
                        <TabsContent className="mt-0 w-full" value="components">
                            <ul className="space-y-2" role="list">
                                {!actionComponentDefinitions.length && (
                                    <span className="block px-3 py-2 text-xs text-gray-500">
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
                                        node={
                                            componentDefinition as (
                                                | ComponentDefinitionBasic
                                                | TaskDispatcherDefinition
                                            ) & {taskDispatcher: boolean; trigger: boolean}
                                        }
                                        selected={selectedComponentName === componentDefinition.name}
                                    />
                                ))}
                            </ul>
                        </TabsContent>
                    )}

                    {!hideTaskDispatchers && (
                        <TabsContent className="mt-0 w-full" value="taskDispatchers">
                            <ul className="space-y-2" role="list">
                                {taskDispatcherDefinitions.length ? (
                                    taskDispatcherDefinitions.map(
                                        (
                                            taskDispatcherDefinition: TaskDispatcherDefinition & {
                                                taskDispatcher?: boolean;
                                            }
                                        ) => (
                                            <WorkflowNodesTabsItem
                                                draggable={itemsDraggable}
                                                handleClick={() =>
                                                    onItemClick &&
                                                    onItemClick({
                                                        ...taskDispatcherDefinition,
                                                        taskDispatcher: true,
                                                    } as ClickedDefinitionType)
                                                }
                                                key={taskDispatcherDefinition.name}
                                                node={
                                                    taskDispatcherDefinition as (
                                                        | ComponentDefinitionBasic
                                                        | TaskDispatcherDefinition
                                                    ) & {taskDispatcher: boolean; trigger: boolean}
                                                }
                                            />
                                        )
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
