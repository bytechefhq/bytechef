import {MENTION_INPUT_PROPERTY_CONTROL_TYPES} from '@/pages/platform/workflow-editor/components/properties/hooks/propertyControlTypes';

const SINGLE_DATA_PILL_REGEX = /^\$\{[^}]+}$/;
const INTEGER_LITERAL_REGEX = /^-?\d+$/;
const NUMBER_LITERAL_REGEX = /^-?\d+(\.\d+)?$/;
const QUOTED_STRING_LITERAL_REGEX = /^'((?:[^']|'')*)'$/;

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

export function getPropertyInputMode({
    controlType,
    formulaMode,
    hasControl = false,
    isFromAi,
    pillEntry = false,
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

    if (isSingleDataPill(value)) {
        return {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: true};
    }

    if (typeof value === 'string' && (value.includes('${') || value.includes('#{'))) {
        return {legacyMixed: true, mode: 'text', renderer: 'mentions', singlePill: false};
    }

    if (pillEntry) {
        return {legacyMixed: false, mode: 'text', renderer: 'mentions', singlePill: true};
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
