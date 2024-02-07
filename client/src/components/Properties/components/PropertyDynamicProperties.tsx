import LoadingIcon from '@/components/LoadingIcon';
import Properties from '@/components/Properties/Properties';
import {PropertiesDataSourceModel} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/queries/platform/workflowNodeDynamicProperties.queries';

type PropertyDynamicPropertiesProps = {
    name?: string;
    propertiesDataSource?: PropertiesDataSourceModel;
};

const PropertyDynamicProperties = ({name, propertiesDataSource}: PropertyDynamicPropertiesProps) => {
    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {data: properties, isLoading} = useGetWorkflowNodeDynamicPropertiesQuery(
        {
            id: workflow.id!,
            propertyName: name!,
            workflowNodeName: currentNode.name!,
        },
        !!propertiesDataSource
    );

    if (isLoading) {
        return (
            <div className="flex items-center justify-center">
                <LoadingIcon /> Loading properties...
            </div>
        );
    }

    return properties ? <Properties properties={properties} /> : <></>;
};

export default PropertyDynamicProperties;
