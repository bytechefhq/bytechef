import React, {useEffect, useState} from 'react';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '../../../../middleware/definition-registry';
import WorkflowNodesList from './WorkflowNodesList';

interface SidebarProps {
    data: {
        components: Array<ComponentDefinitionBasicModel>;
        flowControls: Array<TaskDispatcherDefinitionModel>;
    };
    filter: string;
}

const LeftSidebar: React.FC<SidebarProps> = ({data, filter}): JSX.Element => {
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
                    component.display?.label
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
                    flowControl.display?.label
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

export default LeftSidebar;
