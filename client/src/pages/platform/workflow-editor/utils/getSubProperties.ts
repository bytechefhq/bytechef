import {DataPillType, PropertyAllType} from '@/shared/types';

export default function getSubProperties(
    componentIcon: string,
    nodeName: string,
    subProperties: Array<PropertyAllType>,
    value: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): Array<DataPillType> | any {
    const parentProperty = {
        componentIcon,
        id: value,
        nodeName,
        value,
    };

    return [
        parentProperty,
        subProperties.map((subProperty) => {
            const {items, name, properties} = subProperty;

            const nestedSubProperties = properties?.length ? properties : items;

            const subValue = name ? `${value}.${name}` : `${value}[index]`;

            if (nestedSubProperties?.length) {
                return getSubProperties(componentIcon, nodeName, nestedSubProperties, subValue);
            }

            return {
                componentIcon,
                id: name,
                nodeName,
                value: subValue,
            } as DataPillType;
        }),
    ];
}
