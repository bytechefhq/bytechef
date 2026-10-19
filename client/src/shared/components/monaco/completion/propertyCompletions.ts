import type {DynamicPropertiesProperty, Property} from '@/shared/middleware/platform/configuration';

import type {CompletionItemDraftType} from './scriptCompletions';

type NestedPropertyType = Property & {defaultValue?: unknown; items?: Property[]; properties?: Property[]};

export type PropertyCompletionRequestType = {
    existingKeys: string[];
    loadDisplayConditions?: (parameters: Record<string, unknown>) => Promise<Record<string, boolean>>;
    loadDynamicProperties?: (
        propertyName: string,
        lookupDependsOnPaths: string[],
        parameters: Record<string, unknown>
    ) => Promise<Property[]>;
    parameters: Record<string, unknown>;
    presentKeys: string[];
    properties: Property[];
    propertyPath: string[];
};

const CONDITION_STRING = /'(?:[^'\\]|\\.)*'|"(?:[^"\\]|\\.)*"/g;
const CONDITION_IDENTIFIER = /[A-Za-z_][\w]*/g;

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
        .filter(({property}) => property.name && !property.hidden && !existingKeys.includes(property.name))
        .map(({index, property}) => ({
            detail: detailOf(property),
            documentation: property.description,
            insertAsSnippet: false,
            insertText: property.name!,
            kind: 'property' as const,
            label: property.name!,
            sortText: `${property.required ? 0 : 1}_${String(index).padStart(3, '0')}`,
        }))
        .sort((first, second) => first.sortText.localeCompare(second.sortText));
}

function detailOf(property: Property): string {
    const type = property.type === 'DYNAMIC_PROPERTIES' ? 'OBJECT' : property.type;

    return property.required ? `${type} · required` : type;
}

function withDefaults(
    properties: Property[],
    parameters: Record<string, unknown>,
    presentKeys: string[]
): Record<string, unknown> {
    const values: Record<string, unknown> = {};

    for (const property of properties as NestedPropertyType[]) {
        if (property.name && property.defaultValue !== undefined && !presentKeys.includes(property.name)) {
            values[property.name] = property.defaultValue;
        }
    }

    return {...values, ...parameters};
}

function referencedNames(displayCondition: string, propertyNames: Set<string>): string[] {
    const identifiers = displayCondition.replace(CONDITION_STRING, ' ').match(CONDITION_IDENTIFIER) ?? [];

    return identifiers.filter((identifier) => propertyNames.has(identifier));
}

async function visibleProperties(
    request: PropertyCompletionRequestType,
    values: Record<string, unknown>
): Promise<Property[]> {
    const {loadDisplayConditions, properties} = request;

    if (
        !loadDisplayConditions ||
        Object.keys(values).length === 0 ||
        !properties.some((property) => property.displayCondition)
    ) {
        return properties;
    }

    let displayConditions: Record<string, boolean>;

    try {
        displayConditions = await loadDisplayConditions(values);
    } catch {
        return properties;
    }

    const propertyNames = new Set(properties.map((property) => property.name ?? ''));

    return properties.filter((property) => {
        if (!property.displayCondition || displayConditions[property.displayCondition] === true) {
            return true;
        }

        const referenced = referencedNames(property.displayCondition, propertyNames);

        return referenced.length === 0 || referenced.some((name) => !(name in values));
    });
}

async function dynamicPropertiesOf(
    request: PropertyCompletionRequestType,
    property: DynamicPropertiesProperty,
    values: Record<string, unknown>
): Promise<Property[]> {
    if (!request.loadDynamicProperties || !property.name) {
        return [];
    }

    try {
        return await request.loadDynamicProperties(
            property.name,
            property.propertiesDataSource?.propertiesLookupDependsOn ?? [],
            values
        );
    } catch {
        return [];
    }
}

export async function getPropertyCompletionItems(
    request: PropertyCompletionRequestType
): Promise<CompletionItemDraftType[]> {
    const {existingKeys, parameters, presentKeys, properties, propertyPath} = request;
    const values = withDefaults(properties, parameters, presentKeys);

    if (propertyPath.length === 0) {
        return buildPropertyItems(await visibleProperties(request, values), existingKeys);
    }

    const [firstSegment, ...restOfPath] = propertyPath;
    const dynamicProperty = properties.find(
        (property) => property.name === firstSegment && property.type === 'DYNAMIC_PROPERTIES'
    );

    if (dynamicProperty) {
        const nestedProperties = findNestedProperties(
            await dynamicPropertiesOf(request, dynamicProperty as DynamicPropertiesProperty, values),
            restOfPath
        );

        return nestedProperties ? buildPropertyItems(nestedProperties, existingKeys) : [];
    }

    const nestedProperties = findNestedProperties(properties, propertyPath);

    return nestedProperties ? buildPropertyItems(nestedProperties, existingKeys) : [];
}
