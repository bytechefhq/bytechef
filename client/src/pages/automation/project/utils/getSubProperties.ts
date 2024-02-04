import {ComponentDefinitionBasicModel} from '@/middleware/platform/workflow/execution';
import {PropertyType} from '@/types/projectTypes';

export default function getSubProperties(
    path: string,
    componentDefinition: ComponentDefinitionBasicModel,
    properties: Array<PropertyType>,
    propertyName?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): any {
    return properties.map((subProperty) => {
        const subPropertyLabel = subProperty.label || subProperty.name;

        if (subProperty.properties?.length) {
            return getSubProperties(path, componentDefinition, subProperty.properties, propertyName);
        } else if (subProperty.items?.length) {
            return getSubProperties(path, componentDefinition, subProperty.items, propertyName);
        }

        return {
            componentDefinition: JSON.stringify(componentDefinition),
            id: subProperty.name,
            path,
            value: propertyName ? `${path}/${propertyName}/${subPropertyLabel}` : `${path}/${subPropertyLabel}`,
        };
    });
}
