import {ComponentPropertiesType, DataPillType, PropertyAllType} from '@/shared/types';

import getSubProperties from './getSubProperties';

const getExistingProperties = (properties: Array<PropertyAllType>): Array<PropertyAllType> =>
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
    previousNodeNames: Array<string>
) {
    const dataPills: Array<DataPillType> = [];

    properties.forEach((componentProperty, index) => {
        if (!componentProperty?.properties?.length) {
            return;
        }

        const {componentDefinition} = componentProperty;

        const existingProperties = getExistingProperties(componentProperty.properties);

        const filteredNodeNames = previousNodeNames.filter((name) => name !== 'manual' && !name.includes('condition'));

        const nodeName = filteredNodeNames[index];

        dataPills.push({
            componentIcon: componentDefinition.icon,
            id: nodeName,
            nodeName,
            value: nodeName,
        });

        const formattedProperties: Array<DataPillType> = existingProperties.map((property) => {
            const {items, name, properties} = property;

            const subProperties = properties?.length ? properties : items;

            const value = `${nodeName}.${name}`;

            if (subProperties?.length) {
                return getSubProperties(componentDefinition.icon!, nodeName, subProperties, value);
            }

            return {
                componentIcon: componentDefinition.icon,
                id: name,
                nodeName,
                value,
            };
        });

        if (existingProperties.length && formattedProperties.length) {
            dataPills.push(...formattedProperties);
        }
    });

    return dataPills;
}
