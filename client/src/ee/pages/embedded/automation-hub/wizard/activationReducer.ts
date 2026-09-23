import {toInputValueText} from '@/ee/pages/embedded/automation-hub/utils/inputValue';

export type ActivationStepType = 'activate' | 'configure' | 'connect' | 'done';

export interface ActivationStateI {
    error?: string;
    highlightedComponent?: string;
    inputValues: Record<string, unknown>;
    inputs: ActivationInputI[];
    requiredComponents: string[];
    selections: Record<string, number | undefined>;
    step: ActivationStepType;
    workflowUuid?: string;
}

interface ActivationInputI {
    label?: string;
    name: string;
    required?: boolean;
    type?: string;
}

export type ActivationActionType =
    | {componentName: string; connectionId: number; type: 'SELECT_CONNECTION'}
    | {name: string; type: 'SET_INPUT_VALUE'; value: unknown}
    | {type: 'NEXT'}
    | {type: 'BACK'}
    | {type: 'COPIED'; workflowUuid: string}
    | {componentName: string; type: 'MISSING_CONNECTION'}
    | {type: 'ACTIVATED'; workflowUuid: string}
    | {error: string; type: 'FAILED'};

export function initialActivationState(
    requiredComponents: string[],
    inputs: ActivationInputI[] = []
): ActivationStateI {
    return {
        inputValues: {},
        inputs,
        requiredComponents,
        selections: {},
        step: getFirstStep(requiredComponents, inputs),
    };
}

function getFirstStep(requiredComponents: string[], inputs: ActivationInputI[]): ActivationStepType {
    if (requiredComponents.length > 0) {
        return 'connect';
    }

    return inputs.length > 0 ? 'configure' : 'activate';
}

function getNextState(state: ActivationStateI): ActivationStateI {
    if (!canProceed(state)) {
        return state;
    }

    if (state.step === 'connect') {
        return {...state, error: undefined, step: state.inputs.length > 0 ? 'configure' : 'activate'};
    }

    if (state.step === 'configure') {
        return {...state, error: undefined, step: 'activate'};
    }

    return state;
}

function getPreviousState(state: ActivationStateI): ActivationStateI {
    if (state.step === 'activate' && state.inputs.length > 0) {
        return {...state, error: undefined, step: 'configure'};
    }

    if ((state.step === 'activate' || state.step === 'configure') && state.requiredComponents.length > 0) {
        return {...state, error: undefined, step: 'connect'};
    }

    return state;
}

export function canProceed(state: ActivationStateI): boolean {
    switch (state.step) {
        case 'activate':
            return true;
        case 'configure':
            return state.inputs.every(
                (input) => !input.required || toInputValueText(state.inputValues[input.name]).trim() !== ''
            );
        case 'connect':
            return (
                state.highlightedComponent === undefined &&
                state.requiredComponents.every((componentName) => state.selections[componentName] != null)
            );
        case 'done':
            return false;
    }
}

export function activationReducer(state: ActivationStateI, action: ActivationActionType): ActivationStateI {
    switch (action.type) {
        case 'SELECT_CONNECTION': {
            const highlightedComponent =
                state.highlightedComponent === action.componentName ? undefined : state.highlightedComponent;

            return {
                ...state,
                highlightedComponent,
                selections: {...state.selections, [action.componentName]: action.connectionId},
            };
        }
        case 'NEXT':
            return getNextState(state);
        case 'BACK':
            return getPreviousState(state);
        case 'SET_INPUT_VALUE':
            return {...state, inputValues: {...state.inputValues, [action.name]: action.value}};
        case 'COPIED':
            return {...state, error: undefined, workflowUuid: action.workflowUuid};
        case 'MISSING_CONNECTION':
            return {
                ...state,
                highlightedComponent: action.componentName,
                step: 'connect',
                workflowUuid: undefined,
            };
        case 'ACTIVATED':
            return {...state, error: undefined, step: 'done', workflowUuid: action.workflowUuid};
        case 'FAILED':
            return {...state, error: action.error};
    }
}
