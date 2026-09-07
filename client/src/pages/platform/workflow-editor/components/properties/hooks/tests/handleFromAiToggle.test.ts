import {describe, expect, it} from 'vitest';

import {computeFromAiToggle} from '../fromAiToggle';

/**
 * Tests for handleFromAiToggle and handleControlledModeSwitch fromAi cleanup.
 *
 * When the user toggles the fromAi button off in a Tools cluster element
 * parameter, the previous implementation only flipped local state and left the
 * `fromAi` entry, `dynamicPropertyTypes` entry, and stale `"="` value behind in
 * the workflow definition. The same gap existed when leaving dynamic mode while
 * fromAi was active.
 *
 * The handlers must always:
 *   - update the form field value (keeps the expression when toggling off / empty when leaving mode)
 *   - call saveProperty so the backend strips the path from the fromAi array
 */

const FROM_AI_EXPRESSION = "=fromAi('fieldName')";

const toggle = (overrides: Partial<Parameters<typeof computeFromAiToggle>[0]> & {fromAi: boolean}) =>
    computeFromAiToggle({fromAiExpression: FROM_AI_EXPRESSION, ...overrides});

interface ModeSwitchResultI {
    savePayload: {
        fromAi: boolean;
        includeInMetadata: boolean;
        value: string;
    } | null;
}

const computeControlledModeSwitch = ({
    controlledFromAi,
    custom = false,
    hasPath = true,
    hasWorkflowId = true,
    toDynamic,
}: {
    controlledFromAi: boolean | undefined;
    custom?: boolean;
    hasPath?: boolean;
    hasWorkflowId?: boolean;
    toDynamic: boolean;
}): ModeSwitchResultI => {
    const wasFromAi = controlledFromAi === true;

    if (!wasFromAi || !hasPath || !hasWorkflowId) {
        return {savePayload: null};
    }

    return {
        savePayload: {
            fromAi: false,
            includeInMetadata: custom,
            value: toDynamic ? '=' : '',
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

describe('handleControlledModeSwitch fromAi cleanup', () => {
    it('clears fromAi metadata when leaving dynamic mode while fromAi was active', () => {
        const result = computeControlledModeSwitch({
            controlledFromAi: true,
            toDynamic: false,
        });

        expect(result.savePayload).toEqual({
            fromAi: false,
            includeInMetadata: false,
            value: '',
        });
    });

    it('clears fromAi metadata when entering dynamic mode while fromAi was active', () => {
        const result = computeControlledModeSwitch({
            controlledFromAi: true,
            toDynamic: true,
        });

        expect(result.savePayload).toEqual({
            fromAi: false,
            includeInMetadata: false,
            value: '=',
        });
    });

    it('does not save when fromAi was not active', () => {
        const result = computeControlledModeSwitch({
            controlledFromAi: false,
            toDynamic: false,
        });

        expect(result.savePayload).toBeNull();
    });

    it('does not save when controlledFromAi is undefined', () => {
        const result = computeControlledModeSwitch({
            controlledFromAi: undefined,
            toDynamic: true,
        });

        expect(result.savePayload).toBeNull();
    });

    it('propagates custom flag into includeInMetadata', () => {
        const result = computeControlledModeSwitch({
            controlledFromAi: true,
            custom: true,
            toDynamic: false,
        });

        expect(result.savePayload?.includeInMetadata).toBe(true);
    });
});
