import {PropertyType} from '@/shared/types';

export default function getSubProperties(
    componentIcon: string,
    path: string,
    properties: Array<PropertyType>,
    propertyName?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): any {
    return properties.map((subProperty) => {
        if (subProperty.properties?.length) {
            return getSubProperties(componentIcon, path, subProperty.properties, propertyName);
        } else if (subProperty.items?.length) {
            return getSubProperties(componentIcon, path, subProperty.items, propertyName);
        }

        return {
            componentIcon,
            id: subProperty.name,
            path,
            value: propertyName ? `${path}/${propertyName}/${subProperty.name}` : `${path}/${subProperty.name}`,
        };
    });
}
