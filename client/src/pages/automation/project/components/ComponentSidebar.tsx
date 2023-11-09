import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/hermes/configuration';
import {useEffect, useState} from 'react';

import WorkflowNodesList from './WorkflowNodesList';

interface ComponentSidebarProps {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasicModel>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    };
    filter: string;
}

const ComponentSidebar = ({data, filter}: ComponentSidebarProps) => {
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
                (flowControl) =>
                    flowControl.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                    flowControl?.title
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

export default ComponentSidebar;
