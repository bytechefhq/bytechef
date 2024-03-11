import LoadingIcon from '@/components/LoadingIcon';
import Properties from '@/components/Properties/Properties';
import {PropertiesDataSourceModel} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/queries/platform/workflowNodeDynamicProperties.queries';
import {useEffect, useState} from 'react';

interface PropertyDynamicPropertiesProps {
    name?: string;
    loadDependency?: {[key: string]: string};
    propertiesDataSource?: PropertiesDataSourceModel;
}

const PropertyDynamicProperties = ({loadDependency, name, propertiesDataSource}: PropertyDynamicPropertiesProps) => {
    const [loadDependencyValues, setLoadDependencyValues] = useState<Array<string>>(
        Object.values(loadDependency ?? {})
    );

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {
        data: properties,
        isLoading,
        refetch,
    } = useGetWorkflowNodeDynamicPropertiesQuery(
        {
            id: workflow.id!,
            propertyName: name!,
            workflowNodeName: currentNode.name!,
        },
        !!propertiesDataSource &&
            loadDependencyValues.length > 0 &&
            loadDependencyValues.reduce((enabled: boolean, loadDependencyValue: string) => {
                return loadDependencyValue !== undefined;
            }, true)
    );

    useEffect(() => {
        if (loadDependency && typeof loadDependency === 'object') {
            setLoadDependencyValues(Object.values(loadDependency));
        }
    }, [loadDependency]);

    useEffect(() => {
        if (loadDependencyValues?.length) {
            refetch();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [loadDependencyValues]);

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
