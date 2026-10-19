import type {Property} from '@/shared/middleware/platform/configuration';

import type {CompletionItemDraftType} from './scriptCompletions';

type NestedPropertyType = Property & {items?: Property[]; properties?: Property[]};

export function findNestedProperties(properties: Property[], path: string[]): Property[] | undefined {
    let current: Property[] | undefined = properties;
    let arrayItems: Property[] | undefined;

    for (const segment of path) {
        if (arrayItems) {
            if (!/^\d+$/.test(segment)) {
                return undefined;
            }

            const itemObject = (arrayItems as NestedPropertyType[]).find((item) => item.properties);

            current = itemObject?.properties;
            arrayItems = undefined;

            continue;
        }

        const property = (current as NestedPropertyType[] | undefined)?.find((candidate) => candidate.name === segment);

        if (property?.properties) {
            current = property.properties;
        } else if (property?.items) {
            current = undefined;
            arrayItems = property.items;
        } else {
            return undefined;
        }
    }

    return arrayItems ? undefined : current;
}

export function buildPropertyItems(properties: Property[], existingKeys: string[]): CompletionItemDraftType[] {
    return properties
        .map((property, index) => ({index, property}))
        .filter(
            ({property}) =>
                property.name &&
                !property.hidden &&
                property.type !== 'DYNAMIC_PROPERTIES' &&
                !existingKeys.includes(property.name)
        )
        .map(({index, property}) => ({
            detail: property.required ? `${property.type} · required` : property.type,
            documentation: property.description,
            insertAsSnippet: false,
            insertText: property.name!,
            kind: 'property' as const,
            label: property.name!,
            sortText: `${property.required ? 0 : 1}_${String(index).padStart(3, '0')}`,
        }))
        .sort((first, second) => first.sortText.localeCompare(second.sortText));
}
