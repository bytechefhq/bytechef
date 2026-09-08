interface ControlledToolFieldStatePropsI {
    controlledFromAi: boolean | undefined;
    fieldValue: unknown;
    fromAiExpression: string;
    isToolsClusterElement: boolean | undefined;
    type: string | undefined;
}

interface ControlledToolFieldStateI {
    displayValue: string;
    isExpressionMode: boolean;
    isFieldFromAi: boolean;
    showControlledSwitch: boolean;
    showFromAi: boolean;
    strippedDisplayValue: string;
    strippedFromAiValue: string;
}

export default function getControlledToolFieldState({
    controlledFromAi,
    fieldValue,
    fromAiExpression,
    isToolsClusterElement,
    type,
}: ControlledToolFieldStatePropsI): ControlledToolFieldStateI {
    const showControlledSwitch = !!isToolsClusterElement && type !== 'STRING';
    const showFromAi = !!isToolsClusterElement && type === 'STRING';

    const valueIsFromAi = showFromAi && typeof fieldValue === 'string' && fieldValue.startsWith('=fromAi(');

    const isFieldFromAi = showFromAi && (controlledFromAi ?? valueIsFromAi);

    let displayValue = '';

    if (typeof fieldValue === 'string') {
        displayValue = fieldValue;
    } else if (typeof fieldValue === 'number' || typeof fieldValue === 'boolean') {
        displayValue = String(fieldValue);
    }

    const isExpressionMode = showFromAi && displayValue.startsWith('=');

    return {
        displayValue,
        isExpressionMode,
        isFieldFromAi,
        showControlledSwitch,
        showFromAi,
        strippedDisplayValue: isExpressionMode ? displayValue.substring(1) : displayValue,
        strippedFromAiValue: fromAiExpression.startsWith('=') ? fromAiExpression.substring(1) : fromAiExpression,
    };
}
