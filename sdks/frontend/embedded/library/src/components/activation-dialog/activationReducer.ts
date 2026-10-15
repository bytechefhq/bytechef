// sdks/frontend/embedded/library/src/components/activation-dialog/activationReducer.ts
import {TemplateComponentType, TemplateInputType} from './types';

export type ActivationStepType = 'activate' | 'configure' | 'connect' | 'done';

export interface ActivationStateI {
    error?: string;
    highlightedComponent?: string;
    inputValues: Record<string, unknown>;
    inputs: TemplateInputType[];
    requiredComponents: string[];
    selections: Record<string, number | undefined>;
    step: ActivationStepType;
    unavailableComponents: string[];
    workflowUuid?: string;
}

export type ActivationActionType =
    | {componentName: string; connectionId: number; type: 'SELECT_CONNECTION'}
    | {name: string; type: 'SET_INPUT_VALUE'; value: unknown}
    | {type: 'NEXT'}
    | {type: 'BACK'}
    | {componentName: string; type: 'MISSING_CONNECTION'}
    | {type: 'ACTIVATED'; workflowUuid?: string}
    | {error: string; type: 'FAILED'};

export function toInputValueText(value: unknown): string {
    if (value === undefined || value === null) {
        return '';
    }

    if (typeof value === 'object') {
        return JSON.stringify(value);
    }

    return String(value);
}

export function initialActivationState(
    components: TemplateComponentType[],
    inputs: TemplateInputType[]
): ActivationStateI {
    const requiredComponents: string[] = [];
    const unavailableComponents: string[] = [];

    for (const component of components) {
        if (!component.connectionRequired || requiredComponents.includes(component.name)) {
            continue;
        }

        requiredComponents.push(component.name);

        if (component.integrationId === null || component.integrationId === undefined) {
            unavailableComponents.push(component.name);
        }
    }

    return {
        inputValues: {},
        inputs,
        requiredComponents,
        selections: {},
        step: getFirstStep(requiredComponents, inputs),
        unavailableComponents,
    };
}

function getFirstStep(requiredComponents: string[], inputs: TemplateInputType[]): ActivationStepType {
    if (requiredComponents.length > 0) {
        return 'connect';
    }

    return inputs.length > 0 ? 'configure' : 'activate';
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
                state.unavailableComponents.length === 0 &&
                state.requiredComponents.every((componentName) => state.selections[componentName] != null)
            );
        case 'done':
            return false;
    }
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
        case 'MISSING_CONNECTION':
            if (!state.requiredComponents.includes(action.componentName)) {
                return {
                    ...state,
                    error: `${action.componentName} needs a connection your administrator must set up.`,
                    highlightedComponent: undefined,
                };
            }

            return {...state, error: undefined, highlightedComponent: action.componentName, step: 'connect'};
        case 'ACTIVATED':
            return {...state, error: undefined, step: 'done', workflowUuid: action.workflowUuid};
        case 'FAILED':
            return {...state, error: action.error};
    }
}
