import {MENTION_INPUT_PROPERTY_CONTROL_TYPES} from '@/pages/platform/workflow-editor/components/properties/hooks/propertyControlTypes';

const SINGLE_DATA_PILL_REGEX = /^\$\{[^}]+}$/;
const INTEGER_LITERAL_REGEX = /^-?\d+$/;
const NUMBER_LITERAL_REGEX = /^-?\d+(\.\d+)?$/;
const QUOTED_STRING_LITERAL_REGEX = /^'((?:[^']|'')*)'$/;
const DATE_LITERAL_REGEX = /^\d{4}-\d{2}-\d{2}$/;
const DATE_TIME_LITERAL_REGEX = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/;
const TIME_LITERAL_REGEX = /^\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/;

export interface PropertyInputModeI {
    legacyMixed: boolean;
    mode: 'formula' | 'text';
    renderer: 'mentions' | 'native';
    singlePill: boolean;
}

interface GetPropertyInputModePropsI {
    controlType?: string;
    formulaMode: boolean;
    hasControl?: boolean;
    isFromAi: boolean;
    pillEntry?: boolean;
    type?: string;
    value: unknown;
}

const TEXT_MENTIONS: PropertyInputModeI = {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: false};

export function isSingleDataPill(value: unknown): boolean {
    return typeof value === 'string' && SINGLE_DATA_PILL_REGEX.test(value);
}

function isTextLikeControlType(controlType: string | undefined, hasControl: boolean): boolean {
    if (!controlType || hasControl) {
        return false;
    }

    return controlType === 'FILE_ENTRY' || MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(controlType);
}

export function isStringSelect(controlType: string | undefined, type: string | undefined): boolean {
    return controlType === 'SELECT' && type === 'STRING';
}

export function getPropertyInputMode({
    controlType,
    formulaMode,
    hasControl = false,
    isFromAi,
    pillEntry = false,
    type,
    value,
}: GetPropertyInputModePropsI): PropertyInputModeI {
    if (isFromAi) {
        return TEXT_MENTIONS;
    }

    const isFormulaValue = typeof value === 'string' && value.startsWith('=');

    if (formulaMode || controlType === 'FORMULA_MODE' || isFormulaValue) {
        return {legacyMixed: false, mode: 'formula', renderer: 'mentions', singlePill: false};
    }

    if (isTextLikeControlType(controlType, hasControl)) {
        return TEXT_MENTIONS;
    }

    const onePillLimit = !isStringSelect(controlType, type);

    if (isSingleDataPill(value)) {
        return {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: onePillLimit};
    }

    if (typeof value === 'string' && (value.includes('${') || value.includes('#{'))) {
        return {legacyMixed: onePillLimit, mode: 'text', renderer: 'mentions', singlePill: false};
    }

    if (pillEntry) {
        return {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: onePillLimit};
    }

    return {legacyMixed: false, mode: 'text', renderer: 'native', singlePill: false};
}

export function toFormulaValue(value: unknown, type?: string): string | undefined {
    if (value === undefined || value === null || value === '') {
        return undefined;
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
        return `=${value}`;
    }

    if (typeof value !== 'string') {
        return undefined;
    }

    if (value.startsWith('=')) {
        return value;
    }

    if (isSingleDataPill(value)) {
        return `=${value}`;
    }

    if (value.includes('${')) {
        return undefined;
    }

    const isLiteralOfType =
        ((type === 'INTEGER' || type === 'NUMBER') && NUMBER_LITERAL_REGEX.test(value)) ||
        (type === 'BOOLEAN' && (value === 'true' || value === 'false'));

    if (isLiteralOfType) {
        return `=${value}`;
    }

    return `='${value.replace(/'/g, "''")}'`;
}

export function fromFormulaValue(value: unknown, type?: string): unknown {
    if (typeof value !== 'string' || !value.startsWith('=')) {
        return undefined;
    }

    const body = value.substring(1).trim();

    if (body === '') {
        return undefined;
    }

    if (isSingleDataPill(body)) {
        return body;
    }

    if (type === 'INTEGER') {
        return INTEGER_LITERAL_REGEX.test(body) ? parseInt(body, 10) : undefined;
    }

    if (type === 'NUMBER') {
        return NUMBER_LITERAL_REGEX.test(body) ? parseFloat(body) : undefined;
    }

    if (type === 'BOOLEAN') {
        return body === 'true' || body === 'false' ? body === 'true' : undefined;
    }

    const quotedMatch = QUOTED_STRING_LITERAL_REGEX.exec(body);

    return quotedMatch ? quotedMatch[1].replace(/''/g, "'") : undefined;
}

export function shouldIncludeInMetadata(value: unknown, custom?: boolean): boolean {
    if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
        return true;
    }

    return !!custom;
}

function isJsonObjectString(value: string): boolean {
    try {
        const parsedValue = JSON.parse(value);

        return typeof parsedValue === 'object' && parsedValue !== null && !Array.isArray(parsedValue);
    } catch {
        return false;
    }
}

export function fitsNativeControl(
    controlType: string | undefined,
    value: unknown,
    selectOptionValues?: Array<string>
): boolean {
    if (value === undefined || value === null || value === '' || isSingleDataPill(value)) {
        return true;
    }

    if (controlType === 'SELECT' && selectOptionValues?.length) {
        return selectOptionValues.includes(String(value));
    }

    if (typeof value !== 'string') {
        return true;
    }

    switch (controlType) {
        case 'JSON_SCHEMA_BUILDER':
            return isJsonObjectString(value);
        case 'ARRAY_BUILDER':
        case 'MULTI_SELECT':
        case 'OBJECT_BUILDER':
            return false;
        case 'DATE':
            return DATE_LITERAL_REGEX.test(value);
        case 'DATE_TIME':
            return DATE_TIME_LITERAL_REGEX.test(value);
        case 'TIME':
            return TIME_LITERAL_REGEX.test(value);
        default:
            return true;
    }
}

export function parseJsonSchema(value: string | undefined): Record<string, unknown> | undefined {
    if (!value || !isJsonObjectString(value)) {
        return undefined;
    }

    return JSON.parse(value);
}
