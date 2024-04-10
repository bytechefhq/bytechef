import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/platform/configuration';
import {PropsWithChildren, useEffect, useState} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import WorkflowNodesPopoverMenuList from './WorkflowNodesPopoverMenuList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    id?: string;
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
}

const WorkflowNodesPopoverMenu = ({
    children,
    edge = false,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    id,
}: WorkflowNodesPopoverMenuProps) => {
    const [filter, setFilter] = useState('');
    const [filteredActionComponentDefinitions, setFilteredActionComponentDefinitions] = useState<
        Array<ComponentDefinitionBasicModel>
    >([]);
    const [filteredTaskDispatcherDefinitions, setFilteredTaskDispatcherDefinitions] = useState<
        Array<TaskDispatcherDefinitionModel>
    >([]);
    const [filteredTriggerComponentDefinitions, setFilteredTriggerComponentDefinitions] = useState<
        Array<ComponentDefinitionBasicModel>
    >([]);

    const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore();

    useEffect(() => {
        if (filter) {
            setFilter(filter.toLowerCase());
        }
    }, [filter]);

    useEffect(() => {
        if (taskDispatcherDefinitions) {
            setFilteredTaskDispatcherDefinitions(
                taskDispatcherDefinitions.filter(
                    ({name, title}) => name?.toLowerCase().includes(filter) || title?.toLowerCase().includes(filter)
                )
            );
        }
    }, [taskDispatcherDefinitions, filter, edge]);

    useEffect(() => {
        if (componentDefinitions) {
            setFilteredActionComponentDefinitions(
                componentDefinitions.filter(
                    ({actionsCount, name, title}) =>
                        actionsCount && (name?.toLowerCase().includes(filter) || title?.toLowerCase().includes(filter))
                )
            );

            setFilteredTriggerComponentDefinitions(
                componentDefinitions.filter(
                    ({name, title, triggersCount}) =>
                        triggersCount && (name?.toLowerCase().includes(filter) || title?.toLowerCase().includes(filter))
                )
            );
        }
    }, [componentDefinitions, filter, edge]);

    return (
        <Popover>
            <PopoverTrigger asChild>{children}</PopoverTrigger>

            <PopoverContent
                align="start"
                className="w-workflow-nodes-popover-menu-width rounded-lg p-0 will-change-auto"
                side="right"
                sideOffset={4}
            >
                {id && (
                    <div className="nowheel">
                        {typeof componentDefinitions === 'undefined' ||
                            (typeof taskDispatcherDefinitions === 'undefined' && (
                                <div className="px-3 py-2 text-xs text-gray-500">Something went wrong.</div>
                            ))}

                        <header className="border-b border-gray-200 p-3 text-center text-gray-600">
                            <Input
                                name="workflowNodeFilter"
                                onChange={(event) => setFilter(event.target.value)}
                                placeholder="Filter workflow nodes"
                                value={filter}
                            />
                        </header>

                        <WorkflowNodesPopoverMenuList
                            actionComponentDefinitions={filteredActionComponentDefinitions}
                            edge={edge}
                            hideActionComponents={hideActionComponents}
                            hideTaskDispatchers={hideTaskDispatchers}
                            hideTriggerComponents={hideTriggerComponents}
                            id={id}
                            taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
                            triggerComponentDefinitions={filteredTriggerComponentDefinitions}
                        />
                    </div>
                )}
            </PopoverContent>
        </Popover>
    );
};

export default WorkflowNodesPopoverMenu;
