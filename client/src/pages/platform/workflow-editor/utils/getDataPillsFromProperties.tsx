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

        const allPropertiesFlat: Array<DataPillType> = [];

        existingProperties.forEach((property) => {
            const {items, name, properties} = property;

            const subProperties = properties?.length ? properties : items;

            let value = `${nodeName}.${name ?? '[index]'}`;

            if (value.includes('.[index]')) {
                value = value.replace('.[index]', '[index]');
            }

            if (subProperties?.length) {
                const subResults = getSubProperties(componentDefinition.icon!, nodeName, subProperties, value);

                allPropertiesFlat.push(subResults[0]);

                if (subResults.length > 1) {
                    allPropertiesFlat.push(...subResults.slice(1));
                }
            } else {
                allPropertiesFlat.push({
                    componentIcon: componentDefinition.icon,
                    id: name ?? value,
                    nodeName,
                    value,
                });
            }
        });

        if (allPropertiesFlat.length) {
            dataPills.push(...allPropertiesFlat);
        }
    });

    return dataPills;
}
