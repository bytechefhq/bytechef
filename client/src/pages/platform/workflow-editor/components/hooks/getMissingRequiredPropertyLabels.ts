import {PropertyAllType} from '@/shared/types';

import {WorkflowNodeDetailsErrorI} from './getMissingRequiredConnectionErrors';

const getChildProperties = (property: PropertyAllType): PropertyAllType[] => {
    const items = (property.items ?? []) as PropertyAllType[];

    return [
        ...(property.properties ?? []),
        ...items,
        ...items.flatMap((item) => (item.name ? [] : (item.properties ?? []))),
    ];
};

const getPropertyPathLabel = (path: string, properties: PropertyAllType[]): string => {
    const labels: string[] = [];

    let candidateProperties = properties;

    for (const name of path.replace(/\[\d+\]/g, '').split('.')) {
        const property = candidateProperties.find((candidateProperty) => candidateProperty.name === name);

        labels.push(property?.label || name);

        candidateProperties = property ? getChildProperties(property) : [];
    }

    return labels.join(' > ');
};

export default function getMissingRequiredPropertyLabels(
    errors: WorkflowNodeDetailsErrorI[],
    properties: PropertyAllType[]
): string[] {
    const propertyIndex = (path: string) => {
        const rootName = path.split(/[.[]/)[0];

        const index = properties.findIndex((property) => property.name === rootName);

        return index === -1 ? properties.length : index;
    };

    const labels = errors
        .filter((error) => error.kind === 'PROPERTY')
        .map((error) => error.name)
        .sort((firstPath, secondPath) => propertyIndex(firstPath) - propertyIndex(secondPath))
        .map((path) => getPropertyPathLabel(path, properties));

    return [...new Set(labels)];
}
