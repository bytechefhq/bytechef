import {PropertyType} from '@/types/types';

export default function getParametersWithDefaultValues({
    data = {},
    properties,
}: {
    data?: {[key: string]: string | object};
    properties: Array<PropertyType>;
}) {
    if (!properties?.length) {
        return data;
    }

    properties.forEach((property) => {
        if (!property.name) {
            return;
        }

        if (property.properties?.length) {
            const nestedData = getParametersWithDefaultValues({
                data: {},
                properties: property.properties,
            });

            if (Object.keys(nestedData).length) {
                data[property.name!] = nestedData;
            }
        } else if (property.items?.length) {
            const nestedData = getParametersWithDefaultValues({
                data: {},
                properties: property.items,
            });

            if (Object.keys(nestedData).length) {
                data[property.name!] = [nestedData];
            }
        } else if (property.defaultValue) {
            data[property.name] = property.defaultValue;
        }
    });

    return data;
}
