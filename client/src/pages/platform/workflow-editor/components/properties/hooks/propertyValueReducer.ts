import {getPropertyInputMode} from '@/pages/platform/workflow-editor/components/properties/propertyInputMode';

import {INPUT_PROPERTY_CONTROL_TYPES, MENTION_INPUT_PROPERTY_CONTROL_TYPES} from './propertyControlTypes';

export {INPUT_PROPERTY_CONTROL_TYPES, MENTION_INPUT_PROPERTY_CONTROL_TYPES};

const EMPTY_MULTI_SELECT_VALUE: string[] = [];

export interface PropertyValueStateI {
    inputValue: string;
    mentionInputSyncedValue: unknown;
    mentionInputValue: string;
    multiSelectValue: string[];
    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
    propertyParameterValue: any;
    selectValue: string;
}

export interface ParameterValueContextI {
    controlType?: string;
    formulaMode: boolean;
    isNumericalInput: boolean;
    mentionInput: boolean;
    type?: string;
}

export type PropertyValueActionType =
    | {
          authoritativeValue?: unknown;
          context: ParameterValueContextI;

          syncDisplayValues?: boolean;
          type: 'parameterValueResolved';
          value: unknown;
      }
    | {type: 'inputValueChanged'; value: string}
    | {type: 'inputValueCleared'}
    | {type: 'mentionInputValueChanged'; value: string}
    | {type: 'mentionInputSyncedFromValue'; value: string}
    | {type: 'selectValueChanged'; value: string}
    | {propertyParameterValue?: unknown; type: 'multiSelectValueChanged'; value: string[]}
    | {type: 'pillValueSet'; value: string}
    | {type: 'valueCleared'}
    | {defaultValue: string | string[]; type: 'valuesResetToDefault'};

export function getInitialPropertyValueState({
    controlType,
    defaultValue,
    hasControl,
    parameterValue,
}: {
    controlType: string | undefined;
    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
    defaultValue: any;
    hasControl: boolean;
    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
    parameterValue: any;
}): PropertyValueStateI {
    const initialValue = parameterValue !== undefined ? parameterValue : defaultValue;

    const isMentionCapable =
        getPropertyInputMode({
            controlType,
            formulaMode: controlType === 'FORMULA_MODE',
            hasControl,
            isFromAi: false,
            value: initialValue,
        }).renderer === 'mentions';

    let inputValue = '';

    if (!isMentionCapable && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType!)) {
        inputValue = defaultValue || '';
    }

    const initialMentionValue = parameterValue !== undefined ? parameterValue : defaultValue || '';

    let selectValue: string;

    if (parameterValue !== undefined && parameterValue !== null) {
        selectValue = typeof parameterValue === 'boolean' ? parameterValue.toString() : parameterValue;
    } else {
        selectValue = defaultValue !== undefined ? defaultValue : 'null';
    }

    return {
        inputValue,
        mentionInputSyncedValue: undefined,
        mentionInputValue: typeof initialMentionValue === 'string' ? initialMentionValue : '',
        multiSelectValue: defaultValue || EMPTY_MULTI_SELECT_VALUE,
        propertyParameterValue: parameterValue !== undefined ? parameterValue : defaultValue || '',
        selectValue,
    };
}

export function propertyValueReducer(state: PropertyValueStateI, action: PropertyValueActionType): PropertyValueStateI {
    switch (action.type) {
        case 'parameterValueResolved': {
            const {authoritativeValue, context, syncDisplayValues = true, value} = action;

            if (!syncDisplayValues) {
                if (Object.is(state.propertyParameterValue, value)) {
                    return state;
                }

                return {...state, propertyParameterValue: value};
            }

            const {controlType, formulaMode, isNumericalInput, mentionInput, type} = context;

            if (value === '' || value === undefined) {
                if (mentionInput) {
                    const userHasUnsavedInput = !state.mentionInputSyncedValue && state.mentionInputValue;

                    if (userHasUnsavedInput) {
                        return state;
                    }

                    if (state.mentionInputValue === '' && state.mentionInputSyncedValue === undefined) {
                        return state;
                    }

                    return {...state, mentionInputSyncedValue: undefined, mentionInputValue: ''};
                }

                const hasAuthoritativeValue =
                    authoritativeValue !== undefined && authoritativeValue !== null && authoritativeValue !== '';

                if (hasAuthoritativeValue) {
                    return propertyValueReducer(state, {
                        ...action,
                        authoritativeValue: undefined,
                        value: authoritativeValue,
                    });
                }

                if (
                    state.inputValue === '' &&
                    state.selectValue === '' &&
                    state.multiSelectValue.length === 0 &&
                    state.propertyParameterValue === ''
                ) {
                    return state;
                }

                return {
                    ...state,
                    inputValue: '',
                    multiSelectValue: EMPTY_MULTI_SELECT_VALUE,
                    propertyParameterValue: '',
                    selectValue: '',
                };
            }

            if (typeof value === 'string' && value.startsWith('=')) {
                if (state.mentionInputSyncedValue !== value) {
                    return {
                        ...state,
                        mentionInputSyncedValue: value,
                        mentionInputValue: value.substring(1),
                        propertyParameterValue: value,
                    };
                }

                if (Object.is(state.propertyParameterValue, value)) {
                    return state;
                }

                return {...state, propertyParameterValue: value};
            }

            const nextUsesMentions =
                getPropertyInputMode({controlType, formulaMode, isFromAi: false, value}).renderer === 'mentions';

            const shouldSyncMentionInputFromPlainStringParameter =
                (mentionInput || nextUsesMentions) &&
                state.mentionInputSyncedValue !== value &&
                typeof value === 'string';

            if (shouldSyncMentionInputFromPlainStringParameter) {
                return {
                    ...state,
                    mentionInputSyncedValue: value,
                    mentionInputValue: value,
                    propertyParameterValue: value,
                };
            }

            const nextState: PropertyValueStateI = {...state, propertyParameterValue: value};

            if (!nextUsesMentions && controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType) && value) {
                nextState.inputValue = value as string;
            }

            if (!nextUsesMentions && controlType === 'JSON_SCHEMA_BUILDER') {
                nextState.inputValue = value as string;
            }

            if (controlType === 'SELECT') {
                if (value === null) {
                    nextState.selectValue = 'null';
                } else if (type === 'BOOLEAN') {
                    nextState.selectValue = String(value);
                } else {
                    nextState.selectValue = value as string;
                }
            }

            if (controlType === 'MULTI_SELECT') {
                nextState.multiSelectValue = Array.isArray(value) ? (value as string[]) : EMPTY_MULTI_SELECT_VALUE;
            }

            if (isNumericalInput && value !== null && !nextUsesMentions) {
                nextState.inputValue = value as string;
            }

            const isUnchanged =
                Object.is(nextState.inputValue, state.inputValue) &&
                Object.is(nextState.selectValue, state.selectValue) &&
                Object.is(nextState.multiSelectValue, state.multiSelectValue) &&
                Object.is(nextState.propertyParameterValue, state.propertyParameterValue);

            return isUnchanged ? state : nextState;
        }

        case 'inputValueChanged': {
            if (state.inputValue === action.value) {
                return state;
            }

            return {...state, inputValue: action.value};
        }

        case 'inputValueCleared': {
            if (state.inputValue === '') {
                return state;
            }

            return {...state, inputValue: ''};
        }

        case 'mentionInputValueChanged': {
            if (state.mentionInputValue === action.value) {
                return state;
            }

            return {...state, mentionInputValue: action.value};
        }

        case 'mentionInputSyncedFromValue': {
            return {
                ...state,
                mentionInputSyncedValue: action.value ? action.value : undefined,
                mentionInputValue: action.value,
            };
        }

        case 'selectValueChanged': {
            if (state.selectValue === action.value) {
                return state;
            }

            return {...state, propertyParameterValue: action.value, selectValue: action.value};
        }

        case 'multiSelectValueChanged': {
            return {
                ...state,
                multiSelectValue: action.value,
                propertyParameterValue:
                    action.propertyParameterValue !== undefined ? action.propertyParameterValue : action.value,
            };
        }

        case 'pillValueSet': {
            return {
                ...state,
                mentionInputSyncedValue: action.value,
                mentionInputValue: action.value,
                propertyParameterValue: action.value,
            };
        }

        case 'valueCleared': {
            return {
                ...state,
                inputValue: '',
                mentionInputSyncedValue: undefined,
                mentionInputValue: '',
                multiSelectValue: EMPTY_MULTI_SELECT_VALUE,
                propertyParameterValue: '',
                selectValue: '',
            };
        }

        case 'valuesResetToDefault': {
            const {defaultValue} = action;

            return {
                ...state,
                inputValue: defaultValue as string,
                mentionInputSyncedValue: undefined,
                mentionInputValue: defaultValue as string,
                multiSelectValue: Array.isArray(defaultValue) ? defaultValue : state.multiSelectValue,
                propertyParameterValue: defaultValue,
                selectValue: defaultValue.toString(),
            };
        }

        default:
            return state;
    }
}
