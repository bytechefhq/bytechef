import {DataPillType, PropertyAllType} from '@/shared/types';

import {transformValueForObjectAccess} from './encodingUtils';

export default function getSubProperties(
    componentIcon: string,
    nodeName: string,
    subProperties: Array<PropertyAllType>,
    value: string
): Array<DataPillType> {
    const dataPills: Array<DataPillType> = [
        {
            componentIcon,
            id: value,
            nodeName,
            value: transformValueForObjectAccess(value),
        },
    ];

    subProperties.forEach((subProperty) => {
        const {items, name, properties} = subProperty;

        const nestedSubProperties = properties?.length ? properties : items;

        const subValue = name ? `${value}.${name}` : `${value}[index]`;

        dataPills.push({
            componentIcon,
            id: name ?? subValue,
            nodeName,
            value: transformValueForObjectAccess(subValue),
        });

        if (nestedSubProperties?.length) {
            const nestedProperties = getSubProperties(componentIcon, nodeName, nestedSubProperties, subValue);

            dataPills.push(...nestedProperties.slice(1));
        }
    });

    return dataPills;
}
