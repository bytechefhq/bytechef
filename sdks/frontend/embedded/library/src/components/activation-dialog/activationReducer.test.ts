// sdks/frontend/embedded/library/src/components/activation-dialog/activationReducer.test.ts
import {describe, expect, it} from 'vitest';
import {activationReducer, canProceed, initialActivationState, toInputValueText} from './activationReducer';
import {TemplateComponentType} from './types';

const slack: TemplateComponentType = {connectionRequired: true, integrationId: 5, name: 'slack', title: 'Slack'};
const gmailNotSetUp: TemplateComponentType = {connectionRequired: true, integrationId: null, name: 'gmail'};
const logger: TemplateComponentType = {connectionRequired: false, name: 'logger'};

describe('initialActivationState', () => {
    it('starts on connect when a component needs a connection, listing only those components', () => {
        const state = initialActivationState([logger, slack], []);

        expect(state.step).toBe('connect');
        expect(state.requiredComponents).toEqual(['slack']);
        expect(state.unavailableComponents).toEqual([]);
    });

    it('starts on configure when nothing needs a connection but inputs exist', () => {
        expect(initialActivationState([logger], [{name: 'channel', required: true}]).step).toBe('configure');
    });

    it('starts on activate when there is nothing to connect or configure', () => {
        expect(initialActivationState([logger], []).step).toBe('activate');
    });

    it('records components whose integration is not set up', () => {
        expect(initialActivationState([slack, gmailNotSetUp], []).unavailableComponents).toEqual(['gmail']);
    });
});

describe('canProceed', () => {
    it('blocks connect until every required component has a selection', () => {
        const state = initialActivationState([slack], []);

        expect(canProceed(state)).toBe(false);
        expect(
            canProceed(activationReducer(state, {componentName: 'slack', connectionId: 9, type: 'SELECT_CONNECTION'}))
        ).toBe(true);
    });

    it('blocks connect while a component is not set up, whatever is selected', () => {
        const state = activationReducer(initialActivationState([slack, gmailNotSetUp], []), {
            componentName: 'slack',
            connectionId: 9,
            type: 'SELECT_CONNECTION',
        });

        expect(canProceed(state)).toBe(false);
    });

    it('blocks configure until required inputs have text', () => {
        const state = initialActivationState([], [{name: 'channel', required: true}, {name: 'note'}]);

        expect(canProceed(state)).toBe(false);
        expect(canProceed(activationReducer(state, {name: 'channel', type: 'SET_INPUT_VALUE', value: ' '}))).toBe(
            false
        );
        expect(
            canProceed(activationReducer(state, {name: 'channel', type: 'SET_INPUT_VALUE', value: '#general'}))
        ).toBe(true);
    });
});

describe('activationReducer', () => {
    it('walks connect → configure → activate and back, skipping empty steps', () => {
        let state = initialActivationState([slack], [{name: 'channel'}]);

        state = activationReducer(state, {componentName: 'slack', connectionId: 9, type: 'SELECT_CONNECTION'});
        state = activationReducer(state, {type: 'NEXT'});
        expect(state.step).toBe('configure');
        state = activationReducer(state, {type: 'NEXT'});
        expect(state.step).toBe('activate');
        state = activationReducer(state, {type: 'BACK'});
        expect(state.step).toBe('configure');
        state = activationReducer(state, {type: 'BACK'});
        expect(state.step).toBe('connect');
    });

    it('goes from activate straight back to connect when there are no inputs', () => {
        let state = initialActivationState([slack], []);

        state = activationReducer(state, {componentName: 'slack', connectionId: 9, type: 'SELECT_CONNECTION'});
        state = activationReducer(state, {type: 'NEXT'});
        expect(state.step).toBe('activate');
        expect(activationReducer(state, {type: 'BACK'}).step).toBe('connect');
    });

    it('does not advance when the step cannot proceed', () => {
        const state = initialActivationState([slack], []);

        expect(activationReducer(state, {type: 'NEXT'}).step).toBe('connect');
    });

    it('returns to connect and highlights the component the server reported missing', () => {
        let state = initialActivationState([slack], []);

        state = activationReducer(state, {componentName: 'slack', connectionId: 9, type: 'SELECT_CONNECTION'});
        state = activationReducer(state, {type: 'NEXT'});
        state = activationReducer(state, {componentName: 'slack', type: 'MISSING_CONNECTION'});

        expect(state.step).toBe('connect');
        expect(state.highlightedComponent).toBe('slack');
        expect(canProceed(state)).toBe(false);

        state = activationReducer(state, {componentName: 'slack', connectionId: 10, type: 'SELECT_CONNECTION'});

        expect(state.highlightedComponent).toBeUndefined();
        expect(canProceed(state)).toBe(true);
    });

    it('keeps the step and reports an error when the missing component has no row to connect', () => {
        let state = initialActivationState([slack], []);

        state = activationReducer(state, {componentName: 'slack', connectionId: 9, type: 'SELECT_CONNECTION'});
        state = activationReducer(state, {type: 'NEXT'});
        state = activationReducer(state, {componentName: 'openAi', type: 'MISSING_CONNECTION'});

        expect(state.step).toBe('activate');
        expect(state.highlightedComponent).toBeUndefined();
        expect(state.error).toBe('openAi needs a connection your administrator must set up.');
        expect(canProceed(state)).toBe(true);
    });

    it('records failures and clears them on the next transition', () => {
        let state = initialActivationState([], []);

        state = activationReducer(state, {error: 'boom', type: 'FAILED'});
        expect(state.error).toBe('boom');

        state = activationReducer(state, {type: 'ACTIVATED', workflowUuid: 'wf-1'});
        expect(state.step).toBe('done');
        expect(state.error).toBeUndefined();
        expect(state.workflowUuid).toBe('wf-1');
    });
});

describe('toInputValueText', () => {
    it('renders scalars, objects and nothing', () => {
        expect(toInputValueText(undefined)).toBe('');
        expect(toInputValueText(null)).toBe('');
        expect(toInputValueText(42)).toBe('42');
        expect(toInputValueText({region: 'eu'})).toBe('{"region":"eu"}');
    });
});
