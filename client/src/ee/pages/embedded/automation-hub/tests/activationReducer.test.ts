import {
    activationReducer,
    canProceed,
    initialActivationState,
} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {describe, expect, it} from 'vitest';

describe('initialActivationState', () => {
    it('starts on connect when there are required components', () => {
        const state = initialActivationState(['slack']);

        expect(state.step).toBe('connect');
    });

    it('skips connect and starts on activate when there is nothing at all to ask', () => {
        const state = initialActivationState([]);

        expect(state.step).toBe('activate');
    });

    it('starts on configure when the template declares inputs but needs no connections', () => {
        const state = initialActivationState([], [{name: 'sheetName', required: true}]);

        expect(state.step).toBe('configure');
    });

    it('still starts on connect when both are needed', () => {
        const state = initialActivationState(['slack'], [{name: 'sheetName'}]);

        expect(state.step).toBe('connect');
    });
});

describe('activationReducer', () => {
    it('does not advance past connect until every required component has a selection', () => {
        const state = initialActivationState(['slack', 'github']);

        const afterOneSelection = activationReducer(state, {
            componentName: 'slack',
            connectionId: 1,
            type: 'SELECT_CONNECTION',
        });
        const afterNextWithOneMissing = activationReducer(afterOneSelection, {type: 'NEXT'});

        expect(afterNextWithOneMissing.step).toBe('connect');

        const afterBothSelections = activationReducer(afterOneSelection, {
            componentName: 'github',
            connectionId: 2,
            type: 'SELECT_CONNECTION',
        });
        const afterNextWithBothSelected = activationReducer(afterBothSelections, {type: 'NEXT'});

        expect(afterNextWithBothSelected.step).toBe('activate');
    });

    it('returns to connect and highlights the offending component on MISSING_CONNECTION', () => {
        const state = {
            ...initialActivationState(['slack']),
            selections: {slack: 1},
            step: 'activate' as const,
            workflowUuid: 'automation-workflow-uuid',
        };

        const nextState = activationReducer(state, {componentName: 'slack', type: 'MISSING_CONNECTION'});

        expect(nextState.step).toBe('connect');
        expect(nextState.highlightedComponent).toBe('slack');
        expect(nextState.workflowUuid).toBeUndefined();
    });

    it('records the workflow uuid from COPIED without leaving activate', () => {
        const state = {...initialActivationState([]), step: 'activate' as const};

        const nextState = activationReducer(state, {type: 'COPIED', workflowUuid: 'copy-uuid'});

        expect(nextState.step).toBe('activate');
        expect(nextState.workflowUuid).toBe('copy-uuid');
    });

    it('records the workflow uuid activation switched on, alongside the step, on ACTIVATED', () => {
        const state = {...initialActivationState([]), step: 'activate' as const};

        const nextState = activationReducer(state, {type: 'ACTIVATED', workflowUuid: 'copy-uuid'});

        expect(nextState.step).toBe('done');
        expect(nextState.workflowUuid).toBe('copy-uuid');
    });

    it('moves to done on ACTIVATED', () => {
        const state = {
            ...initialActivationState([]),
            step: 'activate' as const,
            workflowUuid: 'copy-uuid',
        };

        const nextState = activationReducer(state, {type: 'ACTIVATED', workflowUuid: 'copy-uuid'});

        expect(nextState.step).toBe('done');
    });

    it('records an error without changing the step on FAILED', () => {
        const state = {
            ...initialActivationState([]),
            step: 'activate' as const,
            workflowUuid: 'copy-uuid',
        };

        const nextState = activationReducer(state, {error: 'Activation failed', type: 'FAILED'});

        expect(nextState.error).toBe('Activation failed');
        expect(nextState.step).toBe('activate');
    });

    it('clears the highlighted component when a new connection is selected for it', () => {
        const state = {
            ...initialActivationState(['slack']),
            highlightedComponent: 'slack',
        };

        const nextState = activationReducer(state, {
            componentName: 'slack',
            connectionId: 5,
            type: 'SELECT_CONNECTION',
        });

        expect(nextState.highlightedComponent).toBeUndefined();
        expect(nextState.selections.slack).toBe(5);
    });

    it('leaves an unrelated highlighted component untouched by SELECT_CONNECTION', () => {
        const state = {
            ...initialActivationState(['slack', 'github']),
            highlightedComponent: 'slack',
        };

        const nextState = activationReducer(state, {
            componentName: 'github',
            connectionId: 5,
            type: 'SELECT_CONNECTION',
        });

        expect(nextState.highlightedComponent).toBe('slack');
    });

    it('does not go back from activate when there were no required components to connect', () => {
        const state = initialActivationState([]);

        const nextState = activationReducer(state, {type: 'BACK'});

        expect(nextState.step).toBe('activate');
    });

    it('returns to connect from activate when there were required components', () => {
        const state = {
            ...initialActivationState(['slack']),
            selections: {slack: 1},
            step: 'activate' as const,
        };

        const nextState = activationReducer(state, {type: 'BACK'});

        expect(nextState.step).toBe('connect');
    });

    it('tracks the most recently reported component across repeated MISSING_CONNECTION actions', () => {
        const state = {
            ...initialActivationState(['slack', 'github']),
            selections: {github: 2, slack: 1},
            step: 'activate' as const,
            workflowUuid: 'automation-workflow-uuid',
        };

        const afterFirst = activationReducer(state, {componentName: 'slack', type: 'MISSING_CONNECTION'});
        const afterSecond = activationReducer(afterFirst, {componentName: 'github', type: 'MISSING_CONNECTION'});

        expect(afterSecond.highlightedComponent).toBe('github');
        expect(afterSecond.step).toBe('connect');
        expect(afterSecond.workflowUuid).toBeUndefined();
    });

    it('does not move past done on NEXT', () => {
        const state = {...initialActivationState([]), step: 'done' as const};

        const nextState = activationReducer(state, {type: 'NEXT'});

        expect(nextState.step).toBe('done');
    });

    it('clears a stale error once activation succeeds after an earlier failure', () => {
        const failedState = {
            ...initialActivationState([]),
            step: 'activate' as const,
            workflowUuid: 'copy-uuid',
        };

        const afterFailure = activationReducer(failedState, {error: 'Activation failed', type: 'FAILED'});

        expect(afterFailure.error).toBe('Activation failed');

        const afterRetry = activationReducer(afterFailure, {type: 'ACTIVATED', workflowUuid: 'copy-uuid'});

        expect(afterRetry.error).toBeUndefined();
        expect(afterRetry.step).toBe('done');
    });

    it('clears a stale error when NEXT actually advances the step', () => {
        const state = {
            ...initialActivationState(['slack']),
            error: 'stale error',
            selections: {slack: 1},
        };

        const nextState = activationReducer(state, {type: 'NEXT'});

        expect(nextState.error).toBeUndefined();
        expect(nextState.step).toBe('activate');
    });

    it('does not clear a stale error when NEXT is a no-op', () => {
        const state = {...initialActivationState(['slack']), error: 'stale error'};

        const nextState = activationReducer(state, {type: 'NEXT'});

        expect(nextState.error).toBe('stale error');
        expect(nextState.step).toBe('connect');
    });

    it('clears a stale error when BACK actually moves the step', () => {
        const state = {
            ...initialActivationState(['slack']),
            error: 'stale error',
            selections: {slack: 1},
            step: 'activate' as const,
            workflowUuid: 'copy-uuid',
        };

        const nextState = activationReducer(state, {type: 'BACK'});

        expect(nextState.error).toBeUndefined();
        expect(nextState.step).toBe('connect');
    });

    it('does not clear a stale error when BACK is a no-op', () => {
        const state = {...initialActivationState(['slack']), error: 'stale error'};

        const nextState = activationReducer(state, {type: 'BACK'});

        expect(nextState.error).toBe('stale error');
        expect(nextState.step).toBe('connect');
    });

    it('clears a stale error on COPIED', () => {
        const state = {
            ...initialActivationState([]),
            error: 'stale error',
            step: 'activate' as const,
        };

        const nextState = activationReducer(state, {type: 'COPIED', workflowUuid: 'copy-uuid'});

        expect(nextState.error).toBeUndefined();
    });

    it('walks the full missing-connection recovery loop: blocked until a new selection is made, then unblocked', () => {
        const provisioned = {
            ...initialActivationState(['slack']),
            selections: {slack: 1},
            step: 'activate' as const,
            workflowUuid: 'automation-workflow-uuid',
        };

        const afterMissingConnection = activationReducer(provisioned, {
            componentName: 'slack',
            type: 'MISSING_CONNECTION',
        });

        expect(afterMissingConnection.step).toBe('connect');
        expect(afterMissingConnection.highlightedComponent).toBe('slack');
        expect(canProceed(afterMissingConnection)).toBe(false);

        const afterNoOpNext = activationReducer(afterMissingConnection, {type: 'NEXT'});

        expect(afterNoOpNext.step).toBe('connect');

        const afterNewSelection = activationReducer(afterMissingConnection, {
            componentName: 'slack',
            connectionId: 2,
            type: 'SELECT_CONNECTION',
        });

        expect(afterNewSelection.highlightedComponent).toBeUndefined();
        expect(canProceed(afterNewSelection)).toBe(true);

        const afterNext = activationReducer(afterNewSelection, {type: 'NEXT'});

        expect(afterNext.step).toBe('activate');
    });
});

describe('canProceed', () => {
    it('requires every required component to have a selection on connect', () => {
        const state = initialActivationState(['slack', 'github']);

        expect(canProceed(state)).toBe(false);

        const withOneSelection = {...state, selections: {slack: 1}};

        expect(canProceed(withOneSelection)).toBe(false);

        const withBothSelections = {...state, selections: {github: 2, slack: 1}};

        expect(canProceed(withBothSelections)).toBe(true);
    });

    it('is always true on activate', () => {
        const state = {...initialActivationState([]), step: 'activate' as const};

        expect(canProceed(state)).toBe(true);
    });

    it('is false once the wizard is done, since there is nothing left to advance to', () => {
        const state = {...initialActivationState([]), step: 'done' as const};

        expect(canProceed(state)).toBe(false);
    });

    it('is blocked on connect while a component is highlighted, even with a stale selection on file', () => {
        const state = {
            ...initialActivationState(['slack']),
            highlightedComponent: 'slack',
            selections: {slack: 1},
        };

        expect(canProceed(state)).toBe(false);
    });
});

describe('configure step', () => {
    const inputs = [
        {name: 'sheetName', required: true},
        {label: 'Rows', name: 'rowLimit'},
    ];

    it('advances connect -> configure -> activate when the template declares inputs', () => {
        const connected = {
            ...initialActivationState(['slack'], inputs),
            selections: {slack: 1},
        };

        const afterFirstNext = activationReducer(connected, {type: 'NEXT'});

        expect(afterFirstNext.step).toBe('configure');

        const withValue = activationReducer(afterFirstNext, {
            name: 'sheetName',
            type: 'SET_INPUT_VALUE',
            value: 'Leads',
        });

        expect(activationReducer(withValue, {type: 'NEXT'}).step).toBe('activate');
    });

    it('holds on configure until every required input has a non-blank value', () => {
        const state = initialActivationState([], inputs);

        expect(canProceed(state)).toBe(false);

        const blank = activationReducer(state, {name: 'sheetName', type: 'SET_INPUT_VALUE', value: '   '});

        expect(canProceed(blank)).toBe(false);

        const filled = activationReducer(state, {name: 'sheetName', type: 'SET_INPUT_VALUE', value: 'Leads'});

        expect(canProceed(filled)).toBe(true);
    });

    it('does not require an optional input', () => {
        const state = initialActivationState([], [{name: 'rowLimit'}]);

        expect(canProceed(state)).toBe(true);
    });

    it('goes back from activate to configure, and from configure to connect only when connect was shown', () => {
        const withBoth = {
            ...initialActivationState(['slack'], inputs),
            step: 'activate' as const,
        };

        const backOnce = activationReducer(withBoth, {type: 'BACK'});

        expect(backOnce.step).toBe('configure');
        expect(activationReducer(backOnce, {type: 'BACK'}).step).toBe('connect');

        const inputsOnly = {...initialActivationState([], inputs), step: 'configure' as const};

        expect(activationReducer(inputsOnly, {type: 'BACK'}).step).toBe('configure');
    });
});
