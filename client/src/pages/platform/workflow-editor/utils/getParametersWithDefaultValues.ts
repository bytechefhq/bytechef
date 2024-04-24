import {PropertyType} from '@/types/types';

export default function getParametersWithDefaultValues({
    data = {},
    properties,
}: {
    data?: {[key: string]: string | object};
    properties: Array<PropertyType>;
}) {
    properties.forEach((property) => {
        if (!property.name) {
            return;
        }

        if (property.properties?.length) {
            data[property.name!] = getParametersWithDefaultValues({data: {}, properties: property.properties}) ?? {};
        } else if (property.items?.length) {
            data[property.name!] = [getParametersWithDefaultValues({data: {}, properties: property.items}) ?? {}];
        } else {
            data[property.name] = property.defaultValue ?? '';
        }
    });

    return data;
}
