import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/hermes/configuration';
import {useEffect, useState} from 'react';

import WorkflowNodesTabs from './WorkflowNodesTabs';

interface WorkflowNodesSidebarProps {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasicModel>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    };
    filter: string;
}

const WorkflowNodesSidebar = ({data, filter}: WorkflowNodesSidebarProps) => {
    const [
        filteredActionComponentDefinitions,
        setFilteredActionComponentDefinitions,
    ] = useState<Array<ComponentDefinitionBasicModel>>([]);
    const [
        filteredTaskDispatcherDefinitions,
        setFilteredTaskDispatcherDefinitions,
    ] = useState<Array<TaskDispatcherDefinitionModel>>([]);
    const [
        filteredTriggerComponentDefinitions,
        setFilteredTriggerComponentDefinitions,
    ] = useState<Array<ComponentDefinitionBasicModel>>([]);

    const {componentDefinitions, taskDispatcherDefinitions} = data;

    useEffect(() => {
        setFilteredActionComponentDefinitions(
            componentDefinitions.filter(
                (componentDefinition) =>
                    componentDefinition?.actionsCount &&
                    (componentDefinition.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                        componentDefinition?.title
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()))
            )
        );

        setFilteredTaskDispatcherDefinitions(
            taskDispatcherDefinitions.filter(
                (taskDispatcherDefinition) =>
                    taskDispatcherDefinition.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                    taskDispatcherDefinition?.title
                        ?.toLowerCase()
                        .includes(filter.toLowerCase())
            )
        );

        setFilteredTriggerComponentDefinitions(
            componentDefinitions.filter(
                (componentDefinition) =>
                    componentDefinition?.triggersCount &&
                    (componentDefinition.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                        componentDefinition?.title
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()))
            )
        );
    }, [componentDefinitions, filter, taskDispatcherDefinitions]);

    return (
        <WorkflowNodesTabs
            actionComponentDefinitions={filteredActionComponentDefinitions}
            itemsDraggable
            taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
            triggerComponentDefinitions={filteredTriggerComponentDefinitions}
        />
    );
};

export default WorkflowNodesSidebar;
