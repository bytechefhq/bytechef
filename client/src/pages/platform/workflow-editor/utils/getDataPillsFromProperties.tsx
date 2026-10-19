import {ComponentPropertiesType, DataPillType, PropertyAllType} from '@/shared/types';

import getSubProperties from './getSubProperties';

export default function getDataPillsFromProperties(
    properties: Array<ComponentPropertiesType>,
    previousNodeNames: Array<string>
) {
    const dataPills: Array<DataPillType> = [];

    properties.forEach((componentProperty) => {
        if (!componentProperty) {
            return;
        }

        const {componentDefinition, workflowNodeName: nodeName} = componentProperty;

        if (!nodeName || nodeName === 'manual' || nodeName.includes('condition')) {
            return;
        }

        if (!previousNodeNames.includes(nodeName)) {
            return;
        }

        dataPills.push({
            componentIcon: componentDefinition.icon,
            id: nodeName,
            nodeName,
            value: nodeName,
        });

        if (!componentProperty.properties?.length) {
            return;
        }

        const allPropertiesFlat: Array<DataPillType> = [];

        (componentProperty.properties as Array<PropertyAllType>).forEach((property) => {
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
