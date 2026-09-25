import {Control, FieldValues} from 'react-hook-form';
import {describe, expect, it} from 'vitest';

import getInitialFormulaMode from './getInitialFormulaMode';

const buildControl = (formValues: Record<string, unknown>) =>
    ({_formValues: formValues}) as unknown as Control<FieldValues, FieldValues>;

describe('getInitialFormulaMode', () => {
    it('opens a saved formula parameter value in formula mode without a control', () => {
        expect(getInitialFormulaMode({controlPath: '', parameterValue: '=1', propertyName: 'count'})).toBe(true);
    });

    it('opens a plain parameter value outside formula mode without a control', () => {
        expect(getInitialFormulaMode({controlPath: '', parameterValue: 'hello', propertyName: 'uri'})).toBe(false);
    });

    it('opens a controlled formula value in formula mode', () => {
        const control = buildControl({count: '=1'});

        expect(getInitialFormulaMode({control, controlPath: '', propertyName: 'count'})).toBe(true);
    });

    it('opens a controlled constant outside formula mode', () => {
        const control = buildControl({uri: 'https://example.com'});

        expect(getInitialFormulaMode({control, controlPath: '', propertyName: 'uri'})).toBe(false);
    });

    it('reads the controlled value rather than the parameter value when a control is given', () => {
        const control = buildControl({count: 5});

        expect(getInitialFormulaMode({control, controlPath: '', parameterValue: '=1', propertyName: 'count'})).toBe(
            false
        );
    });

    it('keeps a STRING fromAi value outside formula mode', () => {
        const control = buildControl({uri: "=fromAi('uri', 'STRING', {'required': true})"});

        expect(getInitialFormulaMode({control, controlPath: '', propertyName: 'uri', propertyType: 'STRING'})).toBe(
            false
        );
        expect(
            getInitialFormulaMode({
                controlPath: '',
                parameterValue: "=fromAi('uri', 'STRING', {'required': true})",
                propertyName: 'uri',
                propertyType: 'STRING',
            })
        ).toBe(false);
    });

    it('opens a non-STRING fromAi value in formula mode', () => {
        const control = buildControl({timeout: "=fromAi('timeout', 'INTEGER', {'required': false})"});

        expect(
            getInitialFormulaMode({control, controlPath: '', propertyName: 'timeout', propertyType: 'INTEGER'})
        ).toBe(true);
    });

    it('always opens FORMULA_MODE in formula mode', () => {
        expect(getInitialFormulaMode({controlPath: '', controlType: 'FORMULA_MODE', parameterValue: 'x'})).toBe(true);
        expect(
            getInitialFormulaMode({
                control: buildControl({}),
                controlPath: '',
                controlType: 'FORMULA_MODE',
                propertyName: 'x',
            })
        ).toBe(true);
    });

    it('resolves a nested property against its control path', () => {
        const control = buildControl({body: {bodyContentType: '=JSON'}});

        expect(getInitialFormulaMode({control, controlPath: 'body', propertyName: 'bodyContentType'})).toBe(true);
    });

    it('stays out of formula mode without a value', () => {
        expect(getInitialFormulaMode({controlPath: '', propertyName: 'uri'})).toBe(false);
        expect(getInitialFormulaMode({control: buildControl({}), controlPath: ''})).toBe(false);
    });
});
