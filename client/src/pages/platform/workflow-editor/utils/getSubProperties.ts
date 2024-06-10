import {PropertyType} from '@/shared/types';

export default function getSubProperties(
    componentIcon: string,
    nodeName: string,
    properties: Array<PropertyType>,
    propertyName?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): any {
    return properties.map((subProperty) => {
        if (subProperty.properties?.length) {
            return getSubProperties(componentIcon, nodeName, subProperty.properties, propertyName);
        } else if (subProperty.items?.length) {
            return getSubProperties(componentIcon, nodeName, subProperty.items, propertyName);
        }

        return {
            componentIcon,
            id: subProperty.name,
            nodeName,
            value: propertyName ? `${nodeName}.${propertyName}.${subProperty.name}` : `${nodeName}.${subProperty.name}`,
        };
    });
}
