import type {
    DynamicPropertiesProperty,
    Option,
    OptionsDataSource,
    Property,
} from '@/shared/middleware/platform/configuration';

import type {CompletionItemDraftType} from './scriptCompletions';

type NestedPropertyType = Property & {defaultValue?: unknown; items?: Property[]; properties?: Property[]};

type OptionPropertyType = Property & {options?: Option[]; optionsDataSource?: OptionsDataSource};

export type PropertyValueRequestType = Omit<PropertyCompletionRequestType, 'existingKeys' | 'loadDisplayConditions'> & {
    loadOptions?: (
        propertyName: string,
        lookupDependsOnPaths: string[],
        parameters: Record<string, unknown>
    ) => Promise<Option[]>;
    replaceLength: number;
    toInsertText: (value: unknown) => string | undefined;
};

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
const CONDITION_PATH = /[A-Za-z_]\w*(?:\[(?:\d+|index)\]|\.[A-Za-z_]\w*)*/g;

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

function omitPath(values: Record<string, unknown>, path: string[]): Record<string, unknown> {
    const [firstSegment, ...restOfPath] = path;

    if (firstSegment === undefined || !(firstSegment in values)) {
        return values;
    }

    const {[firstSegment]: child, ...otherValues} = values;

    if (restOfPath.length === 0) {
        return otherValues;
    }

    if (Array.isArray(child)) {
        const index = Number(restOfPath[0]);
        const items = [...child];

        if (restOfPath.length === 1) {
            delete items[index];
        } else if (typeof items[index] === 'object' && items[index] !== null) {
            items[index] = omitPath(items[index] as Record<string, unknown>, restOfPath.slice(1));
        }

        return {...otherValues, [firstSegment]: items};
    }

    return typeof child === 'object' && child !== null
        ? {...otherValues, [firstSegment]: omitPath(child as Record<string, unknown>, restOfPath)}
        : values;
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

type ContainerType = {
    dynamic: boolean;
    properties: Property[];
};

const MISSING = Symbol('missing');

function indexesOf(path: string[]): string[] {
    return path.filter((segment) => /^\d+$/.test(segment));
}

function withIndexes(displayCondition: string, indexes: string[]): string {
    let position = 0;

    return displayCondition.replace(/\[index\]/g, (placeholder) => {
        const index = indexes[position++];

        return index === undefined ? placeholder : `[${index}]`;
    });
}

function referencedPaths(displayCondition: string): string[][] {
    const condition = displayCondition.replace(CONDITION_STRING, ' ');
    const paths: string[][] = [];

    for (const match of condition.matchAll(CONDITION_PATH)) {
        const following = condition.slice(match.index + match[0].length).trimStart();

        if (!following.startsWith('(')) {
            paths.push(match[0].split(/[.[\]]/).filter(Boolean));
        }
    }

    return paths;
}

function valueAt(values: unknown, path: string[]): unknown {
    let current = values;

    for (const segment of path) {
        if (typeof current !== 'object' || current === null || !(segment in current)) {
            return MISSING;
        }

        current = (current as Record<string, unknown>)[segment];
    }

    return current;
}

function toOptionsPropertyPath(path: string[]): string {
    return path.reduce(
        (propertyPath, segment) =>
            /^\d+$/.test(segment) ? `${propertyPath}[]` : propertyPath ? `${propertyPath}.${segment}` : segment,
        ''
    );
}

async function visibleProperties(
    request: Pick<PropertyCompletionRequestType, 'loadDisplayConditions' | 'properties'>,
    container: ContainerType,
    values: Record<string, unknown>,
    indexes: string[]
): Promise<Property[]> {
    const {loadDisplayConditions} = request;
    const {properties} = container;

    if (
        !loadDisplayConditions ||
        container.dynamic ||
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

    const topLevelNames = new Set(request.properties.map((property) => property.name ?? ''));

    return properties.filter((property) => {
        if (!property.displayCondition) {
            return true;
        }

        const displayCondition = withIndexes(property.displayCondition, indexes);

        if (displayConditions[displayCondition] === true) {
            return true;
        }

        const referenced = referencedPaths(displayCondition).filter((path) => topLevelNames.has(path[0]));

        return referenced.length === 0 || referenced.some((path) => valueAt(values, path) === MISSING);
    });
}

async function dynamicPropertiesOf(
    request: Pick<PropertyCompletionRequestType, 'loadDynamicProperties'>,
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
            omitPath(values, [property.name])
        );
    } catch {
        return [];
    }
}

async function resolveContainer(
    request: Pick<PropertyCompletionRequestType, 'loadDynamicProperties' | 'properties'>,
    path: string[],
    values: Record<string, unknown>
): Promise<ContainerType | undefined> {
    const {properties} = request;
    const [firstSegment, ...restOfPath] = path;
    const dynamicProperty = properties.find(
        (property) => property.name === firstSegment && property.type === 'DYNAMIC_PROPERTIES'
    );

    if (dynamicProperty) {
        const dynamicProperties = findNestedProperties(
            await dynamicPropertiesOf(request, dynamicProperty as DynamicPropertiesProperty, values),
            restOfPath
        );

        return dynamicProperties && {dynamic: true, properties: dynamicProperties};
    }

    const nestedProperties = findNestedProperties(properties, path);

    return nestedProperties && {dynamic: false, properties: nestedProperties};
}

export async function getPropertyCompletionItems(
    request: PropertyCompletionRequestType
): Promise<CompletionItemDraftType[]> {
    const {existingKeys, parameters, presentKeys, properties, propertyPath} = request;
    const values = withDefaults(properties, parameters, presentKeys);
    const container = await resolveContainer(request, propertyPath, values);

    if (!container) {
        return [];
    }

    return buildPropertyItems(
        await visibleProperties(request, container, values, indexesOf(propertyPath)),
        existingKeys
    );
}

async function optionsOf(
    request: PropertyValueRequestType,
    property: OptionPropertyType,
    values: Record<string, unknown>,
    container: ContainerType
): Promise<Option[]> {
    if (property.options?.length) {
        return property.options;
    }

    if (property.optionsDataSource && request.loadOptions && !container.dynamic) {
        try {
            return await request.loadOptions(
                toOptionsPropertyPath(request.propertyPath),
                property.optionsDataSource.optionsLookupDependsOn ?? [],
                values
            );
        } catch {
            return [];
        }
    }

    return property.type === 'BOOLEAN' ? [{value: true}, {value: false}] : [];
}

function isPrimitive(value: unknown): value is boolean | number | string {
    return typeof value === 'boolean' || typeof value === 'number' || typeof value === 'string';
}

export async function getPropertyValueItems(request: PropertyValueRequestType): Promise<CompletionItemDraftType[]> {
    const {parameters, presentKeys, properties, propertyPath, replaceLength, toInsertText} = request;
    const propertyName = propertyPath[propertyPath.length - 1];

    if (propertyName === undefined) {
        return [];
    }

    const values = omitPath(withDefaults(properties, parameters, presentKeys), propertyPath);
    const container = await resolveContainer(request, propertyPath.slice(0, -1), values);
    const property = container?.properties.find((candidate) => candidate.name === propertyName) as
        OptionPropertyType | undefined;

    if (!container || !property) {
        return [];
    }

    const items: CompletionItemDraftType[] = [];

    for (const option of await optionsOf(request, property, values, container)) {
        const insertText = isPrimitive(option.value) ? toInsertText(option.value) : undefined;

        if (insertText === undefined) {
            continue;
        }

        const valueText = String(option.value);
        const label = option.label || valueText;

        items.push({
            ...(label !== valueText && {detail: valueText}),
            documentation: option.description,
            filterText: label === valueText ? valueText : `${valueText} ${label}`,
            insertAsSnippet: false,
            insertText,
            kind: 'value',
            label,
            replaceLength,
            sortText: String(items.length).padStart(3, '0'),
        });
    }

    return items;
}
