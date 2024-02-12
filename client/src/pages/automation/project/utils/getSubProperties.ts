import {ComponentDefinitionBasicModel} from '@/middleware/platform/workflow/execution';
import {PropertyType} from '@/types/types';

export default function getSubProperties(
    componentDefinition: ComponentDefinitionBasicModel,
    path: string,
    properties: Array<PropertyType>,
    propertyName?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): any {
    return properties.map((subProperty) => {
        const subPropertyLabel = subProperty.label || subProperty.name;

        if (subProperty.properties?.length) {
            return getSubProperties(componentDefinition, path, subProperty.properties, propertyName);
        } else if (subProperty.items?.length) {
            return getSubProperties(componentDefinition, path, subProperty.items, propertyName);
        }

        return {
            componentDefinition: JSON.stringify(componentDefinition),
            id: subProperty.name,
            path,
            value: propertyName ? `${path}/${propertyName}/${subPropertyLabel}` : `${path}/${subPropertyLabel}`,
        };
    });
}
