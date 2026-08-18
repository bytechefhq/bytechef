export type ActivationStepType = 'activate' | 'configure' | 'connect' | 'done';

export interface ActivationStateI {
    error?: string;
    highlightedComponent?: string;
    kind: 'COPY' | 'REFERENCE';
    requiredComponents: string[]; // component names that need a connection
    selections: Record<string, number | undefined>;
    step: ActivationStepType;
    workflowUuid?: string; // copy uuid (COPY) or catalog uuid (REFERENCE)
}

export type ActivationActionType =
    | {componentName: string; connectionId: number; type: 'SELECT_CONNECTION'}
    | {type: 'NEXT'}
    | {type: 'BACK'}
    | {type: 'COPIED'; workflowUuid: string}
    | {type: 'PROVISIONED'; workflowUuid: string}
    | {componentName: string; type: 'MISSING_CONNECTION'}
    | {type: 'ACTIVATED'}
    | {error: string; type: 'FAILED'};

export function initialActivationState(kind: 'COPY' | 'REFERENCE', requiredComponents: string[]): ActivationStateI {
    return {
        kind,
        requiredComponents,
        selections: {},
        step: requiredComponents.length === 0 ? 'configure' : 'connect',
    };
}

export function canProceed(state: ActivationStateI): boolean {
    switch (state.step) {
        case 'activate':
            return true;
        case 'configure':
            return state.workflowUuid != null;
        case 'connect':
            // A highlighted component (from a just-failed MISSING_CONNECTION) blocks proceeding
            // even though its stale selection is still on file — the user must pick again.
            return (
                state.highlightedComponent === undefined &&
                state.requiredComponents.every((componentName) => state.selections[componentName] != null)
            );
        case 'done':
            // Nothing to advance to once the wizard has finished.
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
        case 'NEXT': {
            if (!canProceed(state)) {
                return state;
            }

            if (state.step === 'connect') {
                return {...state, error: undefined, step: 'configure'};
            }

            if (state.step === 'configure') {
                return {...state, error: undefined, step: 'activate'};
            }

            // 'activate' finishes via ACTIVATED, not NEXT; 'done' has nothing further.
            return state;
        }
        case 'BACK': {
            if (state.step === 'configure') {
                // When there were no required components, connect was never shown.
                if (state.requiredComponents.length === 0) {
                    return state;
                }

                return {...state, error: undefined, step: 'connect'};
            }

            if (state.step === 'activate') {
                return {...state, error: undefined, step: 'configure'};
            }

            // 'connect' is the first step and 'done' is terminal.
            return state;
        }
        case 'COPIED':
        case 'PROVISIONED':
            return {...state, error: undefined, workflowUuid: action.workflowUuid};
        case 'MISSING_CONNECTION':
            return {
                ...state,
                highlightedComponent: action.componentName,
                step: 'connect',
                workflowUuid: undefined,
            };
        case 'ACTIVATED':
            return {...state, error: undefined, step: 'done'};
        case 'FAILED':
            return {...state, error: action.error};
    }
}
