import ConnectionTabConnectionSelect from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionSelect';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';

export interface ConnectionTabConnectionFieldsetProps {
    componentConnection: ComponentConnection;
    componentConnectionsCount: number;
    currentComponentDefinition?: ComponentDefinition;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnection;
}

const ConnectionTabConnectionFieldset = ({
    componentConnection,
    componentConnectionsCount,
    currentComponentDefinition,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: ConnectionTabConnectionFieldsetProps) => {
    const {data: componentDefinitionData} = useGetComponentDefinitionQuery(
        {
            componentName: componentConnection.componentName,
            componentVersion: componentConnection.componentVersion,
        },
        currentComponentDefinition === undefined
    );

    const componentDefinition = currentComponentDefinition ?? componentDefinitionData;

    if (!componentDefinition?.connection) {
        return <></>;
    }

    return (
        <fieldset className="space-y-2" key={componentConnection.key}>
            <ConnectionTabConnectionSelect
                componentConnection={componentConnection}
                componentConnectionsCount={componentConnectionsCount}
                componentDefinition={componentDefinition}
                workflowId={workflowId}
                workflowNodeName={workflowNodeName}
                workflowTestConfigurationConnection={workflowTestConfigurationConnection}
            />
        </fieldset>
    );
};

export default ConnectionTabConnectionFieldset;
