import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Component1Icon} from '@radix-ui/react-icons';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from 'middleware/hermes/configuration';
import {HTMLAttributes, MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';

interface DragEvent<T = Element> extends MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface WorkflowNodesTabsItemProps extends HTMLAttributes<HTMLLIElement> {
    handleClick?: () => void;
    node: ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel;
}

const WorkflowNodesTabsItem = ({draggable, handleClick, node}: WorkflowNodesTabsItemProps) => {
    const onDragStart = (event: DragEvent, name: string) => {
        event.dataTransfer.setData('application/reactflow', name);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="mb-2 flex h-[72px] cursor-pointer items-center rounded-md bg-white p-2 hover:bg-gray-50"
            draggable={draggable}
            id={node?.title}
            onClick={handleClick}
            onDragStart={(event) => onDragStart(event, node.name!)}
        >
            {node.icon ? (
                <InlineSVG className="mr-2 h-7 w-7 flex-none" src={node.icon} />
            ) : (
                <Component1Icon className="mr-2 h-7 w-7 flex-none" />
            )}

            <div className="flex flex-col">
                <p className="text-sm font-medium">{node?.title}</p>

                <p className="line-clamp-2 text-left text-xs text-gray-500">{node?.description}</p>
            </div>
        </li>
    );
};

type WorkflowNodesTabsProps = {
    actionComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    itemsDraggable?: boolean;
    onItemClick?: (clickedItem: ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel) => void;
    triggerComponentDefinitions: Array<ComponentDefinitionBasicModel>;
};

const WorkflowNodesTabs = ({
    actionComponentDefinitions,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    itemsDraggable = false,
    onItemClick,
    taskDispatcherDefinitions,
    triggerComponentDefinitions,
}: WorkflowNodesTabsProps) => (
    <div className="mt-2 w-full px-3">
        <Tabs className="w-full" defaultValue={hideActionComponents ? 'triggers' : 'components'}>
            <TabsList className="flex w-full justify-between">
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

            {!hideTriggerComponents && (
                <TabsContent value="triggers">
                    <ul className="mb-2" role="list">
                        {triggerComponentDefinitions.length ? (
                            triggerComponentDefinitions.map((componentDefinition: ComponentDefinitionBasicModel) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() => onItemClick && onItemClick(componentDefinition)}
                                    key={componentDefinition.name}
                                    node={componentDefinition}
                                />
                            ))
                        ) : (
                            <span className="block px-3 py-2 text-xs text-gray-500">No trigger components found.</span>
                        )}
                    </ul>
                </TabsContent>
            )}

            {!hideActionComponents && (
                <TabsContent value="components">
                    <ul className="mb-2" role="list">
                        {actionComponentDefinitions.length ? (
                            actionComponentDefinitions.map((componentDefinition: ComponentDefinitionBasicModel) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() => onItemClick && onItemClick(componentDefinition)}
                                    key={componentDefinition.name}
                                    node={componentDefinition}
                                />
                            ))
                        ) : (
                            <span className="block px-3 py-2 text-xs text-gray-500">No action components found.</span>
                        )}
                    </ul>
                </TabsContent>
            )}

            {!hideTaskDispatchers && (
                <TabsContent value="taskDispatchers">
                    <ul className="mb-2" role="list">
                        {taskDispatcherDefinitions.length ? (
                            taskDispatcherDefinitions.map((taskDispatcherDefinition: TaskDispatcherDefinitionModel) => (
                                <WorkflowNodesTabsItem
                                    draggable={itemsDraggable}
                                    handleClick={() => onItemClick && onItemClick(taskDispatcherDefinition)}
                                    key={taskDispatcherDefinition.name}
                                    node={taskDispatcherDefinition}
                                />
                            ))
                        ) : (
                            <span className="block px-3 py-2 text-xs text-gray-500">No flow controls found.</span>
                        )}
                    </ul>
                </TabsContent>
            )}
        </Tabs>
    </div>
);

export default WorkflowNodesTabs;
