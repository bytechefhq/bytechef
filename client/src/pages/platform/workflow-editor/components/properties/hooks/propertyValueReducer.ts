export const INPUT_PROPERTY_CONTROL_TYPES = [
    'DATE',
    'DATE_TIME',
    'EMAIL',
    'INTEGER',
    'NUMBER',
    'PASSWORD',
    'PHONE',
    'TEXT',
    'TIME',
    'URL',
];

export const MENTION_INPUT_PROPERTY_CONTROL_TYPES = [
    'EMAIL',
    'FORMULA_MODE',
    'PHONE',
    'RICH_TEXT',
    'TEXT',
    'TEXT_AREA',
    'URL',
];

const EMPTY_MULTI_SELECT_VALUE: string[] = [];

export interface PropertyValueStateI {
    inputValue: string;
    mentionInput: boolean;
    mentionInputSyncedValue: unknown;
    mentionInputValue: string;
    multiSelectValue: string[];
    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
    propertyParameterValue: any;
    selectValue: string;
}

export interface ParameterValueContextI {
    controlType?: string;
    isNumericalInput: boolean;
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
    | {mentionInput: boolean; type: 'mentionInputModeChanged'}
    | {type: 'mentionInputSyncedFromValue'; value: string}
    | {type: 'selectValueChanged'; value: string}
    | {propertyParameterValue?: unknown; type: 'multiSelectValueChanged'; value: string[]}
    | {mentionInput: boolean; mentionInputValue: string; propertyParameterValue: unknown; type: 'inputTypeSwitched'}
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
    const isMentionCapable = !hasControl && MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(controlType!);

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
        mentionInput: isMentionCapable,
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

            const {controlType, isNumericalInput, type} = context;

            if (value === '' || value === undefined) {
                if (state.mentionInput) {
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

            const shouldSyncMentionInputFromPlainStringParameter =
                state.mentionInput && state.mentionInputSyncedValue !== value && typeof value === 'string';

            if (shouldSyncMentionInputFromPlainStringParameter) {
                return {
                    ...state,
                    mentionInputSyncedValue: value,
                    mentionInputValue: value,
                    propertyParameterValue: value,
                };
            }

            const nextState: PropertyValueStateI = {...state, propertyParameterValue: value};

            if (!state.mentionInput && controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType) && value) {
                nextState.inputValue = value as string;
            }

            if (!state.mentionInput && controlType === 'JSON_SCHEMA_BUILDER') {
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
                nextState.multiSelectValue = value === null ? EMPTY_MULTI_SELECT_VALUE : (value as string[]);
            }

            if (isNumericalInput && value !== null) {
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

        case 'mentionInputModeChanged': {
            if (state.mentionInput === action.mentionInput) {
                return state;
            }

            return {...state, mentionInput: action.mentionInput};
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

        case 'inputTypeSwitched': {
            return {
                ...state,
                mentionInput: action.mentionInput,
                mentionInputSyncedValue: undefined,
                mentionInputValue: action.mentionInputValue,
                multiSelectValue: EMPTY_MULTI_SELECT_VALUE,
                propertyParameterValue: action.propertyParameterValue,
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
