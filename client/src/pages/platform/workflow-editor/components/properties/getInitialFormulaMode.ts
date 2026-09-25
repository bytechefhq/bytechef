import {Control, FieldValues} from 'react-hook-form';

interface GetInitialFormulaModePropsI {
    control?: Control<FieldValues, FieldValues>;
    controlPath: string;
    controlType?: string;
    parameterValue?: unknown;
    propertyName?: string;
    propertyType?: string;
}

const readControlledValue = (control: Control<FieldValues, FieldValues>, fieldPath: string) =>
    fieldPath
        .split('.')
        .reduce<unknown>(
            (currentObject, key) => (currentObject as Record<string, unknown>)?.[key],
            control._formValues
        );

export default function getInitialFormulaMode({
    control,
    controlPath,
    controlType,
    parameterValue,
    propertyName,
    propertyType,
}: GetInitialFormulaModePropsI): boolean {
    if (controlType === 'FORMULA_MODE') {
        return true;
    }

    let value = parameterValue;

    if (control?._formValues && propertyName) {
        value = readControlledValue(control, controlPath ? `${controlPath}.${propertyName}` : propertyName);
    }

    if (typeof value !== 'string' || !value.startsWith('=')) {
        return false;
    }

    return !(propertyType === 'STRING' && value.startsWith('=fromAi('));
}
