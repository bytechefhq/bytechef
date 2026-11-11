import buildFromAiExpression from '@/pages/platform/workflow-editor/components/properties/utils/buildFromAiExpression';
import {PropertyAllType} from '@/shared/types';

export type LoadPropertyOptionsType = (
    property: PropertyAllType,
    path: string,
    values: Record<string, unknown>
) => Promise<Array<{value?: unknown}> | null>;

export const loadNoPropertyOptions: LoadPropertyOptionsType = async () => null;

interface AiFillCandidatesParamsI {
    hiddenPaths?: Set<string>;
    optionsLoadable?: boolean;
    includeAdvanced: boolean;
    includeConditional: boolean;
    properties: PropertyAllType[];
    values: Record<string, unknown>;
}

interface ComputeAiFillValuesParamsI extends AiFillCandidatesParamsI {
    loadOptions: LoadPropertyOptionsType;
}

interface AiFillCandidateI {
    path: string;
    property: PropertyAllType;
}

const EXCLUDED_PROPERTY_NAMES = new Set(['toolDescription', 'toolName']);

const INPUT_CONTROL_TYPES = new Set([
    'DATE',
    'DATE_TIME',
    'EMAIL',
    'INTEGER',
    'NUMBER',
    'PASSWORD',
    'PHONE',
    'TEXT',
    'TIME',
    'URL',
]);

export function isEmptyToolValue(value: unknown): boolean {
    return value === undefined || value === null || value === '';
}

export function isFromAiEligibleProperty(property: PropertyAllType): boolean {
    if (property.expressionEnabled === false || property.hidden) {
        return false;
    }

    if (property.name && EXCLUDED_PROPERTY_NAMES.has(property.name)) {
        return false;
    }

    const controlType = property.controlType as string | undefined;

    if (!controlType) {
        return false;
    }

    if (controlType === 'SELECT' || controlType === 'MULTI_SELECT') {
        return true;
    }

    if (property.type === 'OBJECT' || property.type === 'ARRAY') {
        return false;
    }

    if (controlType === 'TEXT_AREA') {
        return property.type === 'STRING';
    }

    return INPUT_CONTROL_TYPES.has(controlType);
}

function resolveDefaultValue(property: PropertyAllType): unknown {
    const propertyRecord = property as unknown as Record<string, unknown>;

    return (
        propertyRecord.defaultValue ??
        propertyRecord.integerDefaultValue ??
        propertyRecord.numberDefaultValue ??
        propertyRecord.booleanDefaultValue ??
        propertyRecord.arrayDefaultValue ??
        propertyRecord.objectDefaultValue ??
        propertyRecord.dateDefaultValue ??
        propertyRecord.dateTimeDefaultValue ??
        propertyRecord.timeDefaultValue
    );
}

export function collectPropertyDefaults(properties: PropertyAllType[]): Record<string, unknown> {
    const propertyDefaults: Record<string, unknown> = {};

    for (const property of properties) {
        if (!property.name) {
            continue;
        }

        const resolvedDefault = resolveDefaultValue(property);

        if (property.type === 'OBJECT' && property.properties?.length) {
            const nestedDefaults = collectPropertyDefaults(property.properties);

            if (resolvedDefault !== null && typeof resolvedDefault === 'object' && !Array.isArray(resolvedDefault)) {
                propertyDefaults[property.name] = {...nestedDefaults, ...(resolvedDefault as Record<string, unknown>)};
            } else if (resolvedDefault !== undefined && resolvedDefault !== null) {
                propertyDefaults[property.name] = resolvedDefault;
            } else if (Object.keys(nestedDefaults).length > 0) {
                propertyDefaults[property.name] = nestedDefaults;
            }

            continue;
        }

        if (resolvedDefault !== undefined && resolvedDefault !== null) {
            propertyDefaults[property.name] = resolvedDefault;
        }
    }

    return propertyDefaults;
}

function readValue(values: Record<string, unknown>, path: string): unknown {
    if (path in values) {
        return values[path];
    }

    let current: unknown = values;

    for (const segment of path.split('.')) {
        if (current === null || typeof current !== 'object') {
            return undefined;
        }

        current = (current as Record<string, unknown>)[segment];
    }

    return current;
}

function collectAiFillCandidates({
    hiddenPaths,
    includeAdvanced,
    includeConditional,
    optionsLoadable = true,
    properties,
    values,
}: AiFillCandidatesParamsI): AiFillCandidateI[] {
    const candidates: AiFillCandidateI[] = [];

    function visit(propertyList: PropertyAllType[], parentPath: string) {
        for (const property of propertyList) {
            if (!property.name) {
                continue;
            }

            const path = parentPath ? `${parentPath}.${property.name}` : property.name;

            if (
                property.hidden ||
                hiddenPaths?.has(path) ||
                (!includeAdvanced && property.advancedOption) ||
                (!includeConditional && property.displayCondition)
            ) {
                continue;
            }

            if (property.type === 'OBJECT') {
                if (property.properties?.length) {
                    visit(property.properties, path);
                }

                continue;
            }

            if (
                isFromAiEligibleProperty(property) &&
                isEmptyToolValue(readValue(values, path)) &&
                (optionsLoadable || !property.optionsDataSource)
            ) {
                candidates.push({path, property});
            }
        }
    }

    visit(properties, '');

    return candidates;
}

export function collectHiddenToolPaths(
    properties: PropertyAllType[],
    displayConditions: Record<string, boolean> | undefined
): Set<string> {
    const hiddenPaths = new Set<string>();

    if (!displayConditions) {
        return hiddenPaths;
    }

    const evaluatedDisplayConditions = displayConditions;

    function visit(propertyList: PropertyAllType[], parentPath: string) {
        for (const property of propertyList) {
            if (!property.name) {
                continue;
            }

            const path = parentPath ? `${parentPath}.${property.name}` : property.name;

            if (property.displayCondition && !evaluatedDisplayConditions[property.displayCondition]) {
                hiddenPaths.add(path);
            }

            if (property.properties?.length) {
                visit(property.properties, path);
            }
        }
    }

    visit(properties, '');

    return hiddenPaths;
}

export function hasAiFillCandidates(params: AiFillCandidatesParamsI): boolean {
    return collectAiFillCandidates(params).length > 0;
}

export async function computeAiFillValues({
    loadOptions,
    ...candidateParams
}: ComputeAiFillValuesParamsI): Promise<Record<string, string>> {
    const result: Record<string, string> = {};

    for (const {path, property} of collectAiFillCandidates(candidateParams)) {
        let options: Array<{value?: unknown}> | null | undefined;

        if (property.optionsDataSource) {
            options = await loadOptions(property, path, candidateParams.values);

            if (options === null || options === undefined) {
                continue;
            }
        } else if (Array.isArray(property.options)) {
            options = property.options as Array<{value?: unknown}>;
        }

        result[path] = buildFromAiExpression({
            defaultValue: resolveDefaultValue(property),
            description: property.description,
            name: path.replaceAll('.', '_'),
            options,
            required: property.required,
            type: property.type as string,
        });
    }

    return result;
}
