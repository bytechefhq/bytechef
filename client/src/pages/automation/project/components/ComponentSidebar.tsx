import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/hermes/configuration';
import {useEffect, useState} from 'react';

import WorkflowNodesList from './WorkflowNodesList';

interface ComponentSidebarProps {
    data: {
        components: Array<ComponentDefinitionBasicModel>;
        flowControls: Array<TaskDispatcherDefinitionModel>;
    };
    filter: string;
}

const ComponentSidebar = ({data, filter}: ComponentSidebarProps) => {
    const [filteredComponents, setFilteredComponents] = useState<
        Array<ComponentDefinitionBasicModel>
    >([]);
    const [filteredFlowControls, setFilteredFlowControls] = useState<
        Array<TaskDispatcherDefinitionModel>
    >([]);

    const {components, flowControls} = data;

    useEffect(() => {
        setFilteredComponents(
            components.filter(
                (component) =>
                    component.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                    component?.title
                        ?.toLowerCase()
                        .includes(filter.toLowerCase())
            )
        );

        setFilteredFlowControls(
            flowControls.filter(
                (flowControl) =>
                    flowControl.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                    flowControl?.title
                        ?.toLowerCase()
                        .includes(filter.toLowerCase())
            )
        );
    }, [components, filter, flowControls]);

    return (
        <WorkflowNodesList
            components={filteredComponents}
            flowControls={filteredFlowControls}
            itemsDraggable
        />
    );
};

export default ComponentSidebar;
