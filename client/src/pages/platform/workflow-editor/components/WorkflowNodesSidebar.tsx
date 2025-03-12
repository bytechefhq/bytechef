import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useEffect, useState} from 'react';

import WorkflowNodesTabs from './WorkflowNodesTabs';

const WorkflowNodesSidebar = ({
    data,
}: {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasic>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    };
}) => {
    const [filter, setFilter] = useState('');

    const [filteredActionComponentDefinitions, setFilteredActionComponentDefinitions] = useState<
        Array<ComponentDefinitionBasic>
    >([]);

    const [filteredTaskDispatcherDefinitions, setFilteredTaskDispatcherDefinitions] = useState<
        Array<TaskDispatcherDefinition>
    >([]);

    const [filteredTriggerComponentDefinitions, setFilteredTriggerComponentDefinitions] = useState<
        Array<ComponentDefinitionBasic>
    >([]);

    const {componentDefinitions, taskDispatcherDefinitions} = data;

    useEffect(() => {
        setFilteredActionComponentDefinitions(
            componentDefinitions.filter(
                (componentDefinition) =>
                    componentDefinition?.actionsCount &&
                    (componentDefinition.name?.toLowerCase().includes(filter.toLowerCase()) ||
                        componentDefinition?.title?.toLowerCase().includes(filter.toLowerCase()))
            )
        );

        setFilteredTaskDispatcherDefinitions(
            taskDispatcherDefinitions.filter(
                (taskDispatcherDefinition) =>
                    taskDispatcherDefinition.name?.toLowerCase().includes(filter.toLowerCase()) ||
                    taskDispatcherDefinition?.title?.toLowerCase().includes(filter.toLowerCase())
            )
        );

        setFilteredTriggerComponentDefinitions(
            componentDefinitions.filter(
                (componentDefinition) =>
                    componentDefinition?.triggersCount &&
                    (componentDefinition.name?.toLowerCase().includes(filter.toLowerCase()) ||
                        componentDefinition?.title?.toLowerCase().includes(filter.toLowerCase()))
            )
        );
    }, [componentDefinitions, filter, taskDispatcherDefinitions]);

    return (
        <aside className="absolute inset-y-2 right-16 mx-1 flex w-96 flex-col overflow-hidden rounded-md border border-stroke-neutral-secondary bg-background pb-4">
            <div className="p-3 text-center text-content-neutral-secondary">
                <Input
                    name="workflowNodeFilter"
                    onChange={(event) => setFilter(event.target.value)}
                    placeholder="Filter actions and flows"
                    value={filter}
                />
            </div>

            <div className="flex flex-1 flex-col overflow-hidden px-3 pt-1">
                <WorkflowNodesTabs
                    actionComponentDefinitions={filteredActionComponentDefinitions}
                    hideTriggerComponents
                    itemsDraggable
                    taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
                    triggerComponentDefinitions={filteredTriggerComponentDefinitions}
                />
            </div>
        </aside>
    );
};

export default WorkflowNodesSidebar;
