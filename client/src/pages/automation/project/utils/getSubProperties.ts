import {ComponentDefinitionModel} from '@/middleware/helios/execution/models';
import {PropertyType} from '@/types/projectTypes';

export default function getSubProperties({
    componentDefinition,
    properties,
    propertyName,
}: {
    componentDefinition: ComponentDefinitionModel;
    properties: Array<PropertyType>;
    propertyName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
}): any {
    return properties.map((subProperty) => {
        const subPropertyLabel = subProperty.label || subProperty.name;

        if (subProperty.properties?.length) {
            return getSubProperties({
                componentDefinition,
                properties: subProperty.properties,
                propertyName: subPropertyLabel,
            });
        } else if (subProperty.items?.length) {
            return getSubProperties({
                componentDefinition,
                properties: subProperty.items,
                propertyName: subPropertyLabel,
            });
        }

        return {
            component: JSON.stringify(componentDefinition),
            id: subProperty.name,
            value: propertyName ? `${propertyName}/${subPropertyLabel}` : `${subPropertyLabel}`,
        };
    });
}
