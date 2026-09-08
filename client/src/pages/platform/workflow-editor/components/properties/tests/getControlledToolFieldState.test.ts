import getControlledToolFieldState from '@/pages/platform/workflow-editor/components/properties/getControlledToolFieldState';
import {describe, expect, it} from 'vitest';

const baseProps = {
    controlledFromAi: undefined,
    fieldValue: '',
    fromAiExpression: "=fromAi('body', 'STRING', {})",
    isToolsClusterElement: true,
    type: 'STRING',
};

describe('getControlledToolFieldState', () => {
    it('offers the from AI affordance for a string tool property', () => {
        const state = getControlledToolFieldState(baseProps);

        expect(state.showFromAi).toBe(true);
        expect(state.showControlledSwitch).toBe(false);
    });

    it('offers the dynamic switch instead for a non string tool property', () => {
        const state = getControlledToolFieldState({...baseProps, type: 'INTEGER'});

        expect(state.showFromAi).toBe(false);
        expect(state.showControlledSwitch).toBe(true);
    });

    it('offers neither outside a tool', () => {
        const state = getControlledToolFieldState({...baseProps, isToolsClusterElement: false});

        expect(state.showFromAi).toBe(false);
        expect(state.showControlledSwitch).toBe(false);
    });

    it('offers no dynamic switch for a non string property outside a tool', () => {
        const state = getControlledToolFieldState({
            ...baseProps,
            isToolsClusterElement: false,
            type: 'INTEGER',
        });

        expect(state.showControlledSwitch).toBe(false);
    });

    it('reads a stored fromAi call as from AI', () => {
        const state = getControlledToolFieldState({...baseProps, fieldValue: "=fromAi('body', 'STRING', {})"});

        expect(state.isFieldFromAi).toBe(true);
    });

    it('lets an explicit toggle override the stored value', () => {
        const state = getControlledToolFieldState({
            ...baseProps,
            controlledFromAi: false,
            fieldValue: "=fromAi('body', 'STRING', {})",
        });

        expect(state.isFieldFromAi).toBe(false);
    });

    it('treats a leading = as expression mode and strips it for display', () => {
        const state = getControlledToolFieldState({...baseProps, fieldValue: "=concat('a', 'b')"});

        expect(state.isExpressionMode).toBe(true);
        expect(state.strippedDisplayValue).toBe("concat('a', 'b')");
    });

    it('leaves a plain value alone', () => {
        const state = getControlledToolFieldState({...baseProps, fieldValue: 'Hello'});

        expect(state.isExpressionMode).toBe(false);
        expect(state.displayValue).toBe('Hello');
        expect(state.strippedDisplayValue).toBe('Hello');
    });

    it('never reports expression mode outside a tool', () => {
        const state = getControlledToolFieldState({
            ...baseProps,
            fieldValue: "=concat('a', 'b')",
            isToolsClusterElement: false,
        });

        expect(state.isExpressionMode).toBe(false);
        expect(state.strippedDisplayValue).toBe("=concat('a', 'b')");
    });

    it('coerces a non string field value for display', () => {
        expect(getControlledToolFieldState({...baseProps, fieldValue: 42}).displayValue).toBe('42');
        expect(getControlledToolFieldState({...baseProps, fieldValue: true}).displayValue).toBe('true');
        expect(getControlledToolFieldState({...baseProps, fieldValue: null}).displayValue).toBe('');
        expect(getControlledToolFieldState({...baseProps, fieldValue: undefined}).displayValue).toBe('');
    });

    it('shows nothing rather than [object Object] for a field value that is not a primitive', () => {
        expect(getControlledToolFieldState({...baseProps, fieldValue: {a: 1}}).displayValue).toBe('');
        expect(getControlledToolFieldState({...baseProps, fieldValue: [1, 2]}).displayValue).toBe('');
    });

    it('strips the leading = from the fromAi expression', () => {
        expect(getControlledToolFieldState(baseProps).strippedFromAiValue).toBe("fromAi('body', 'STRING', {})");
    });
});
