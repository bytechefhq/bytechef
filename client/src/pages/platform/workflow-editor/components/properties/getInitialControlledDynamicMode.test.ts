import {Control, FieldValues} from 'react-hook-form';
import {describe, expect, it} from 'vitest';

import getInitialControlledDynamicMode from './getInitialControlledDynamicMode';

const buildControl = (formValues: Record<string, unknown>) =>
    ({_formValues: formValues}) as unknown as Control<FieldValues, FieldValues>;

describe('getInitialControlledDynamicMode', () => {
    it('reopens a plain value outside dynamic mode', () => {
        const control = buildControl({uri: 'https://example.com'});

        expect(getInitialControlledDynamicMode({control, controlPath: '', propertyName: 'uri'})).toBe(false);
    });

    it('reopens an expression in dynamic mode', () => {
        const control = buildControl({uri: "=trigger.body['url']"});

        expect(getInitialControlledDynamicMode({control, controlPath: '', propertyName: 'uri'})).toBe(true);
    });

    // A STRING tool property renders its from-AI value through the from-AI control, which sits outside dynamic
    // mode. Reading the leading "=" as an expression made a reopened tool popover show the Dynamic switch
    // turned on for a property whose switch the user never touched.
    it('reopens a STRING tool property holding a from-AI value outside dynamic mode', () => {
        const control = buildControl({uri: "=fromAi('uri', 'STRING', {'required': true})"});

        expect(
            getInitialControlledDynamicMode({
                control,
                controlPath: '',
                propertyName: 'uri',
                propertyType: 'STRING',
                toolsMode: true,
            })
        ).toBe(false);
    });

    // Non-STRING tool properties reach from-AI by switching to dynamic mode first, so they must reopen in it.
    it('reopens a non-STRING tool property holding a from-AI value in dynamic mode', () => {
        const control = buildControl({timeout: "=fromAi('timeout', 'INTEGER', {'required': false})"});

        expect(
            getInitialControlledDynamicMode({
                control,
                controlPath: '',
                propertyName: 'timeout',
                propertyType: 'INTEGER',
                toolsMode: true,
            })
        ).toBe(true);
    });

    it('resolves a nested property against its control path', () => {
        const control = buildControl({body: {bodyContentType: '=JSON'}});

        expect(getInitialControlledDynamicMode({control, controlPath: 'body', propertyName: 'bodyContentType'})).toBe(
            true
        );
    });

    it('stays out of dynamic mode without a control or a property name', () => {
        expect(getInitialControlledDynamicMode({controlPath: '', propertyName: 'uri'})).toBe(false);
        expect(getInitialControlledDynamicMode({control: buildControl({}), controlPath: ''})).toBe(false);
    });
});
