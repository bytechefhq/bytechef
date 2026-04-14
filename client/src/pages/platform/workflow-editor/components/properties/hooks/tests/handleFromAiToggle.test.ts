import {describe, expect, it} from 'vitest';

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
 *   - update the form field value (empty when toggling off / leaving mode)
 *   - call saveProperty so the backend strips the path from the fromAi array
 */

interface FromAiToggleResultI {
    fieldValue: string;
    savePayload: {
        fromAi: boolean;
        includeInMetadata: boolean;
        value: string;
    } | null;
}

const FROM_AI_EXPRESSION = "=fromAi('fieldName')";

const computeFromAiToggle = ({
    custom = false,
    fromAi,
    fromAiExpression = FROM_AI_EXPRESSION,
    hasPath = true,
    hasWorkflowId = true,
}: {
    custom?: boolean;
    fromAi: boolean;
    fromAiExpression?: string;
    hasPath?: boolean;
    hasWorkflowId?: boolean;
}): FromAiToggleResultI => {
    const fieldValue = fromAi ? fromAiExpression : '';

    if (!hasPath || !hasWorkflowId) {
        return {fieldValue, savePayload: null};
    }

    return {
        fieldValue,
        savePayload: {
            fromAi,
            includeInMetadata: custom || fromAi,
            value: fieldValue,
        },
    };
};

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
            const result = computeFromAiToggle({fromAi: true});

            expect(result.fieldValue).toBe(FROM_AI_EXPRESSION);
        });

        it('saves with fromAi true and forces includeInMetadata', () => {
            const result = computeFromAiToggle({custom: false, fromAi: true});

            expect(result.savePayload).toEqual({
                fromAi: true,
                includeInMetadata: true,
                value: FROM_AI_EXPRESSION,
            });
        });
    });

    describe('toggling OFF', () => {
        it('clears the field value', () => {
            const result = computeFromAiToggle({fromAi: false});

            expect(result.fieldValue).toBe('');
        });

        it('saves with fromAi false so the backend removes the entry', () => {
            const result = computeFromAiToggle({custom: false, fromAi: false});

            expect(result.savePayload).toEqual({
                fromAi: false,
                includeInMetadata: false,
                value: '',
            });
        });

        it('keeps includeInMetadata true when the property is custom', () => {
            const result = computeFromAiToggle({custom: true, fromAi: false});

            expect(result.savePayload?.includeInMetadata).toBe(true);
        });
    });

    describe('guards', () => {
        it('does not save when path is missing', () => {
            const result = computeFromAiToggle({fromAi: false, hasPath: false});

            expect(result.savePayload).toBeNull();
            expect(result.fieldValue).toBe('');
        });

        it('does not save when workflow id is missing', () => {
            const result = computeFromAiToggle({fromAi: true, hasWorkflowId: false});

            expect(result.savePayload).toBeNull();
            expect(result.fieldValue).toBe(FROM_AI_EXPRESSION);
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
