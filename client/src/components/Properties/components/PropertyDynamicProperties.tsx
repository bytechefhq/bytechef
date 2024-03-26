import LoadingIcon from '@/components/LoadingIcon';
import Properties from '@/components/Properties/Properties';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {PropertiesDataSourceModel, WorkflowModel} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/queries/platform/workflowNodeDynamicProperties.queries';
import {ComponentDataType, CurrentComponentType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

interface PropertyDynamicPropertiesProps {
    currentComponent: CurrentComponentType;
    currentComponentData: ComponentDataType;
    loadDependency?: {[key: string]: string};
    name?: string;
    propertiesDataSource?: PropertiesDataSourceModel;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const PropertyDynamicProperties = ({
    currentComponent,
    currentComponentData,
    loadDependency,
    name,
    propertiesDataSource,
    updateWorkflowMutation,
}: PropertyDynamicPropertiesProps) => {
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

    return properties ? (
        <Properties
            currentComponent={currentComponent}
            currentComponentData={currentComponentData}
            properties={properties}
            updateWorkflowMutation={updateWorkflowMutation}
        />
    ) : (
        <></>
    );
};

export default PropertyDynamicProperties;
