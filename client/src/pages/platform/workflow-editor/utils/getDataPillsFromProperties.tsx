import {WorkflowModel} from '@/shared/middleware/platform/configuration';
import {ComponentPropertiesType, DataPillType, PropertyType} from '@/shared/types';

import getSubProperties from './getSubProperties';

const getExistingProperties = (properties: Array<PropertyType>): Array<PropertyType> =>
    properties.filter((property) => {
        if (property.properties) {
            return getExistingProperties(property.properties);
        } else if (property.items) {
            return getExistingProperties(property.items);
        }

        return !!property.name;
    });

export default function getDataPillsFromProperties(
    properties: Array<ComponentPropertiesType>,
    workflow: WorkflowModel,
    previousNodeNames: Array<string>
) {
    const dataPills: Array<DataPillType> = [];

    properties.forEach((componentProperty, index) => {
        if (!componentProperty || !componentProperty.properties?.length) {
            return;
        }

        const {componentDefinition} = componentProperty;

        const existingProperties = getExistingProperties(componentProperty.properties);

        const nodeName = workflow.triggers?.length ? previousNodeNames[index] : previousNodeNames[index + 1];

        dataPills.push({
            componentIcon: componentDefinition.icon,
            id: nodeName,
            nodeName,
            value: nodeName,
        });

        const formattedProperties: DataPillType[] = existingProperties.map((property) => {
            if (property.properties) {
                return getSubProperties(componentDefinition.icon!, nodeName, property.properties, property.name);
            } else if (property.items) {
                return getSubProperties(componentDefinition.icon!, nodeName, property.items, property.name);
            }

            return {
                componentIcon: componentDefinition.icon,
                id: property.name,
                nodeName,
                value: `${nodeName}.${property.name}`,
            };
        });

        if (existingProperties.length && formattedProperties.length) {
            dataPills.push(...formattedProperties);
        }
    });

    return dataPills;
}
