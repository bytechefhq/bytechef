import {toStoredParameters} from './toolParameterConversion';

interface BuildToolParametersPayloadParamsI {
    aiValues: Record<string, string>;
    dirtyValues: Record<string, unknown>;
    savedParameters?: Record<string, unknown> | null;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

export function deepMerge(target: Record<string, unknown>, source: Record<string, unknown>): Record<string, unknown> {
    const result = {...target};

    for (const [key, value] of Object.entries(source)) {
        const existing = result[key];

        result[key] = isPlainObject(existing) && isPlainObject(value) ? deepMerge(existing, value) : value;
    }

    return result;
}

export function expandDottedPaths(values: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = {};

    for (const [path, value] of Object.entries(values)) {
        const segments = path.split('.');

        let current = result;

        for (const segment of segments.slice(0, -1)) {
            if (!isPlainObject(current[segment])) {
                current[segment] = {};
            }

            current = current[segment] as Record<string, unknown>;
        }

        current[segments[segments.length - 1]] = value;
    }

    return result;
}

function omitUndefinedValues(values: Record<string, unknown>): Record<string, unknown> {
    return Object.fromEntries(
        Object.entries(values)
            .filter(([, value]) => value !== undefined)
            .map(([key, value]) => [key, isPlainObject(value) ? omitUndefinedValues(value) : value])
    );
}

export default function buildToolParametersPayload({
    aiValues,
    dirtyValues,
    savedParameters,
}: BuildToolParametersPayloadParamsI): Record<string, unknown> {
    const merged = deepMerge(deepMerge(savedParameters ?? {}, expandDottedPaths(aiValues)), dirtyValues);

    return toStoredParameters(omitUndefinedValues(merged));
}
