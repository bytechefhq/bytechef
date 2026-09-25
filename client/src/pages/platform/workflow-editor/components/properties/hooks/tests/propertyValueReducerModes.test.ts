import {describe, expect, it} from 'vitest';

import {ParameterValueContextI, getInitialPropertyValueState, propertyValueReducer} from '../propertyValueReducer';

const integerContext: ParameterValueContextI = {
    controlType: 'INTEGER',
    formulaMode: false,
    isNumericalInput: true,
    mentionInput: false,
    type: 'INTEGER',
};

const initialIntegerState = () =>
    getInitialPropertyValueState({
        controlType: 'INTEGER',
        defaultValue: undefined,
        hasControl: false,
        parameterValue: 5,
    });

describe('propertyValueReducer modes', () => {
    it('no longer stores mentionInput', () => {
        expect('mentionInput' in initialIntegerState()).toBe(false);
    });

    it('syncs the editor value when a pill arrives for a field that was native', () => {
        const nextState = propertyValueReducer(initialIntegerState(), {
            context: integerContext,
            type: 'parameterValueResolved',
            value: '${trigger_1.count}',
        });

        expect(nextState.mentionInputValue).toBe('${trigger_1.count}');
        expect(nextState.propertyParameterValue).toBe('${trigger_1.count}');
        expect(nextState.inputValue).not.toBe('${trigger_1.count}');
    });

    it('keeps the pill when the server echoes it back after pillValueSet', () => {
        const withPill = propertyValueReducer(initialIntegerState(), {type: 'pillValueSet', value: '${a.b}'});

        const afterEcho = propertyValueReducer(withPill, {
            context: {...integerContext, mentionInput: true},
            type: 'parameterValueResolved',
            value: '${a.b}',
        });

        expect(afterEcho.mentionInputValue).toBe('${a.b}');
        expect(afterEcho.propertyParameterValue).toBe('${a.b}');
    });

    it('clears every display value on valueCleared', () => {
        const withPill = propertyValueReducer(initialIntegerState(), {type: 'pillValueSet', value: '${a.b}'});
        const cleared = propertyValueReducer(withPill, {type: 'valueCleared'});

        expect(cleared).toMatchObject({
            inputValue: '',
            mentionInputSyncedValue: undefined,
            mentionInputValue: '',
            multiSelectValue: [],
            propertyParameterValue: '',
            selectValue: '',
        });
    });

    it('does not turn a pill string into a multi-select array', () => {
        const multiSelectState = getInitialPropertyValueState({
            controlType: 'MULTI_SELECT',
            defaultValue: undefined,
            hasControl: false,
            parameterValue: ['a'],
        });

        const nextState = propertyValueReducer(multiSelectState, {
            context: {
                controlType: 'MULTI_SELECT',
                formulaMode: false,
                isNumericalInput: false,
                mentionInput: false,
                type: 'ARRAY',
            },
            type: 'parameterValueResolved',
            value: '${a.b}',
        });

        expect(Array.isArray(nextState.multiSelectValue)).toBe(true);
    });
});
