import {ComponentDefinitionBasicModel} from '@/middleware/platform/workflow/execution';
import {PropertyType} from '@/types/projectTypes';

export default function getSubProperties(
    componentAlias: string,
    componentDefinition: ComponentDefinitionBasicModel,
    properties: Array<PropertyType>,
    propertyName?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
): any {
    return properties.map((subProperty) => {
        const subPropertyLabel = subProperty.label || subProperty.name;

        if (subProperty.properties?.length) {
            return getSubProperties(componentAlias, componentDefinition, subProperty.properties, propertyName);
        } else if (subProperty.items?.length) {
            return getSubProperties(componentAlias, componentDefinition, subProperty.items, propertyName);
        }

        return {
            componentAlias,
            componentDefinition: JSON.stringify(componentDefinition),
            id: subProperty.name,
            value: propertyName
                ? `${componentAlias}/${propertyName}/${subPropertyLabel}`
                : `${componentAlias}/${subPropertyLabel}`,
        };
    });
}
