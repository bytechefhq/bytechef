import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/platform/configuration';
import {useEffect, useState} from 'react';

import WorkflowNodesTabs from './WorkflowNodesTabs';

const WorkflowNodesSidebar = ({
    data,
}: {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasicModel>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    };
}) => {
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
        <div className="flex h-full flex-col bg-white pb-12">
            <header className="border-b border-gray-200 p-3 text-center text-gray-600">
                <Input
                    name="workflowNodeFilter"
                    onChange={(event) => setFilter(event.target.value)}
                    placeholder="Filter workflow nodes"
                    value={filter}
                />
            </header>

            <main className="overflow-y-auto">
                <WorkflowNodesTabs
                    actionComponentDefinitions={filteredActionComponentDefinitions}
                    itemsDraggable
                    taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
                    triggerComponentDefinitions={filteredTriggerComponentDefinitions}
                />
            </main>
        </div>
    );
};

export default WorkflowNodesSidebar;
