import {Control, FieldValues} from 'react-hook-form';

type InitialControlledDynamicModeCandidateType = {
    control?: Control<FieldValues, FieldValues>;
    controlPath: string;
    propertyName?: string;
    propertyType?: string;
    toolsMode?: boolean;
};

export default function getInitialControlledDynamicMode({
    control,
    controlPath,
    propertyName,
    propertyType,
    toolsMode,
}: InitialControlledDynamicModeCandidateType): boolean {
    if (!control?._formValues || !propertyName) {
        return false;
    }

    const fieldPath = controlPath ? `${controlPath}.${propertyName}` : propertyName;
    const fieldValue = fieldPath
        .split('.')
        .reduce<unknown>(
            (currentObject, key) => (currentObject as Record<string, unknown>)?.[key],
            control._formValues
        );

    if (typeof fieldValue !== 'string' || !fieldValue.startsWith('=')) {
        return false;
    }

    if (toolsMode && propertyType === 'STRING' && fieldValue.startsWith('=fromAi(')) {
        return false;
    }

    return true;
}
