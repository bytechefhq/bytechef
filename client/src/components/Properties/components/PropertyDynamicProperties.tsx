import LoadingIcon from '@/components/LoadingIcon';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {PropertiesDataSourceModel, WorkflowModel} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeDynamicPropertiesQuery} from '@/queries/platform/workflowNodeDynamicProperties.queries';
import {ComponentType, CurrentComponentDefinitionType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

import Property from '../Property';

interface PropertyDynamicPropertiesProps {
    currentActionName?: string;
    currentComponentDefinition: CurrentComponentDefinitionType;
    currentComponent: ComponentType;
    loadDependency?: {[key: string]: string};
    name?: string;
    propertiesDataSource?: PropertiesDataSourceModel;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    taskParameterValue?: any;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const PropertyDynamicProperties = ({
    currentActionName,
    currentComponent,
    currentComponentDefinition,
    loadDependency,
    name,
    propertiesDataSource,
    taskParameterValue,
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
        <ul>
            {properties.map((property, index) => {
                const propertyDefaultValue = property.name ? taskParameterValue?.[property.name] : '';

                return (
                    <Property
                        actionName={currentActionName}
                        currentComponent={currentComponent}
                        currentComponentDefinition={currentComponentDefinition}
                        key={`${property.name}_${index}`}
                        objectName={name}
                        path={name}
                        property={property}
                        taskParameterValue={propertyDefaultValue}
                        updateWorkflowMutation={updateWorkflowMutation}
                    />
                );
            })}
        </ul>
    ) : (
        <></>
    );
};

export default PropertyDynamicProperties;
