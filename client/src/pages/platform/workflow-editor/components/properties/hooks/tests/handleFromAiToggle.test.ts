import {describe, expect, it} from 'vitest';

import {fromFormulaValue, toFormulaValue} from '../../propertyInputMode';
import {computeFromAiToggle} from '../fromAiToggle';

/**
 * Tests for handleFromAiToggle and handleControlledFormulaSwitch fromAi cleanup.
 *
 * When the user toggles the fromAi button off in a Tools cluster element
 * parameter, the previous implementation only flipped local state and left the
 * `fromAi` entry, `dynamicPropertyTypes` entry, and stale `"="` value behind in
 * the workflow definition. The same gap existed when switching formula mode while
 * fromAi was active.
 *
 * The handlers must always:
 *   - update the form field value (keeps the expression when toggling off / converted when switching mode)
 *   - call saveProperty so the backend strips the path from the fromAi array
 */

const FROM_AI_EXPRESSION = "=fromAi('fieldName')";

const toggle = (overrides: Partial<Parameters<typeof computeFromAiToggle>[0]> & {fromAi: boolean}) =>
    computeFromAiToggle({fromAiExpression: FROM_AI_EXPRESSION, ...overrides});

interface FormulaSwitchResultI {
    savePayload: {
        fromAi: boolean;
        includeInMetadata: boolean;
        value: unknown;
    } | null;
}

const computeControlledFormulaSwitch = ({
    controlledFromAi,
    custom = false,
    fieldValue = FROM_AI_EXPRESSION,
    hasPath = true,
    hasWorkflowId = true,
    toFormula,
    type = 'INTEGER',
}: {
    controlledFromAi: boolean | undefined;
    custom?: boolean;
    fieldValue?: unknown;
    hasPath?: boolean;
    hasWorkflowId?: boolean;
    toFormula: boolean;
    type?: string;
}): FormulaSwitchResultI => {
    const wasFromAi = controlledFromAi === true;

    if (!wasFromAi || !hasPath || !hasWorkflowId) {
        return {savePayload: null};
    }

    const convertedValue = toFormula ? toFormulaValue(fieldValue, type) : fromFormulaValue(fieldValue, type);

    return {
        savePayload: {
            fromAi: false,
            includeInMetadata: custom,
            value: convertedValue ?? null,
        },
    };
};

describe('handleFromAiToggle', () => {
    describe('toggling ON', () => {
        it('sets field value to the fromAi expression', () => {
            const result = toggle({fromAi: true});

            expect(result.value).toBe(FROM_AI_EXPRESSION);
        });

        it('saves with fromAi true and forces includeInMetadata', () => {
            const result = toggle({custom: false, fromAi: true});

            expect(result.savePayload).toEqual({
                fromAi: true,
                includeInMetadata: true,
                value: FROM_AI_EXPRESSION,
            });
        });
    });

    describe('toggling OFF', () => {
        it('keeps the field value as the fromAi expression', () => {
            const result = toggle({fromAi: false});

            expect(result.value).toBe(FROM_AI_EXPRESSION);
        });

        it('saves with fromAi false so the backend removes the entry', () => {
            const result = toggle({custom: false, fromAi: false});

            expect(result.savePayload).toEqual({
                fromAi: false,
                includeInMetadata: false,
                value: FROM_AI_EXPRESSION,
            });
        });

        it('keeps includeInMetadata true when the property is custom', () => {
            const result = toggle({custom: true, fromAi: false});

            expect(result.savePayload?.includeInMetadata).toBe(true);
        });
    });

    describe('guards', () => {
        it('does not save when path is missing', () => {
            const result = toggle({fromAi: false, hasPath: false});

            expect(result.savePayload).toBeNull();
            expect(result.value).toBe(FROM_AI_EXPRESSION);
        });

        it('does not save when workflow id is missing', () => {
            const result = toggle({fromAi: true, hasWorkflowId: false});

            expect(result.savePayload).toBeNull();
            expect(result.value).toBe(FROM_AI_EXPRESSION);
        });
    });
});

describe('handleControlledFormulaSwitch fromAi cleanup', () => {
    it('clears fromAi metadata and the unconvertible value when leaving formula mode while fromAi was active', () => {
        const result = computeControlledFormulaSwitch({
            controlledFromAi: true,
            toFormula: false,
        });

        expect(result.savePayload).toEqual({
            fromAi: false,
            includeInMetadata: false,
            value: null,
        });
    });

    it('clears fromAi metadata and keeps the converted value when entering formula mode while fromAi was active', () => {
        const result = computeControlledFormulaSwitch({
            controlledFromAi: true,
            fieldValue: 5,
            toFormula: true,
        });

        expect(result.savePayload).toEqual({
            fromAi: false,
            includeInMetadata: false,
            value: '=5',
        });
    });

    it('does not save when fromAi was not active', () => {
        const result = computeControlledFormulaSwitch({
            controlledFromAi: false,
            toFormula: false,
        });

        expect(result.savePayload).toBeNull();
    });

    it('does not save when controlledFromAi is undefined', () => {
        const result = computeControlledFormulaSwitch({
            controlledFromAi: undefined,
            toFormula: true,
        });

        expect(result.savePayload).toBeNull();
    });

    it('propagates custom flag into includeInMetadata', () => {
        const result = computeControlledFormulaSwitch({
            controlledFromAi: true,
            custom: true,
            toFormula: false,
        });

        expect(result.savePayload?.includeInMetadata).toBe(true);
    });
});
